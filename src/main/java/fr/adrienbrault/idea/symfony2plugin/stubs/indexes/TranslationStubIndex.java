package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import consulo.project.Project;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StringSetDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor.TranslationArrayReturnVisitor;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationVisitor;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TranslationStubIndex extends FileBasedIndexExtension<String, Set<String>> {
    public static final ID<String, Set<String>> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.translations");
    private static final StringSetDataExternalizer DATA_EXTERNALIZER = new StringSetDataExternalizer();
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {

        return new DataIndexer<>() {
            @Override
            public Map<String, Set<String>> map(FileContent inputData) {
                Project project = inputData.getProject();
                if (project == null || !Symfony2ProjectComponent.isEnabledForIndex(project)) {
                    return Collections.emptyMap();
                }

                String extension = inputData.getFile().getExtension();
                if ("xlf".equalsIgnoreCase(extension) || "xliff".equalsIgnoreCase(extension)) {
                    return getXlfStringMap(inputData);
                }

                PsiFile psiFile = inputData.getPsiFile();

                // check physical file position
                if (!isValidTranslationFile(inputData)) {
                    return Collections.emptyMap();
                }

                String domainName = getDomainName(inputData.getFileName());
                if (domainName == null) {
                    return Collections.emptyMap();
                }

                if (psiFile instanceof PhpFile) {
                    Set<String> translationKeySet = new HashSet<>();
                    TranslationArrayReturnVisitor.visitPhpReturn((PhpFile) psiFile, pair -> translationKeySet.add(pair.getFirst()));

                    if (translationKeySet.isEmpty()) {
                        return Collections.emptyMap();
                    }
                    
                    Map<String, Set<String>> map = new HashMap<>();
                    map.put(domainName, translationKeySet);

                    return map;
                } else if (psiFile instanceof YAMLFile) {
                    Set<String> translationKeySet = new HashSet<>();
                    YamlTranslationVisitor.collectFileTranslations((YAMLFile) psiFile, (keyName, yamlKeyValue) -> {
                        translationKeySet.add(keyName);
                        return true;
                    });

                    if (translationKeySet.isEmpty()) {
                        return Collections.emptyMap();
                    }

                    Map<String, Set<String>> map = new HashMap<>();
                    map.put(domainName, translationKeySet);
                    return map;
                }

                return Collections.emptyMap();
            }

            private boolean isValidTranslationFile(FileContent inputData) {
                String fileName = inputData.getFileName();

                // every direct match
                if (fileName.contains("+intl-icu") || fileName.startsWith("messages.") || fileName.startsWith("validators.")) {
                    return true;
                }

                VirtualFile file = inputData.getFile();
                String name = file.getNameWithoutExtension();

                // unknown-2.fr.yml
                Matcher matcher = Pattern.compile("^.*\\.([\\w]{2})$").matcher(name);
                if (matcher.find()) {
                    return ArrayUtils.contains(Locale.getISOLanguages(), matcher.group(1));
                }

                // unknown-3.sr_Cyrl.yml
                Matcher matcher2 = Pattern.compile("^.*\\.([\\w]{2})_[\\w]{2,4}$").matcher(name);
                if (matcher2.find()) {
                    return ArrayUtils.contains(Locale.getISOLanguages(), matcher2.group(1));
                }

                // dont index all yaml files; valid:
                //  - "Resources/translations"
                //  - "translations/[.../]foo.de.yml"
                String relativePath = VirtualFileUtil.getRelativePath(file, ProjectUtil.getProjectDir(inputData.getProject()), '/');
                if (relativePath != null) {
                    String replace = relativePath.replace("\\", "/");
                    return replace.contains("/translations") || replace.startsWith("translations/");
                }

                // Resources/translations/messages.de.yml
                String path = file.getPath();
                return path.replace("\\", "/").endsWith("/translations/" + fileName);
            }

            private Map<String, Set<String>> getXlfStringMap(FileContent inputData) {
                // testing files are not that nice
                String relativePath = VirtualFileUtil.getRelativePath(inputData.getFile(), ProjectUtil.getProjectDir(inputData.getProject()), '/');
                if (relativePath != null && (relativePath.contains("/Test/") || relativePath.contains("/Tests/") || relativePath.contains("/Fixture/") || relativePath.contains("/Fixtures/"))) {
                    return Collections.emptyMap();
                }

                // extract domain name
                String domainName = getDomainName(inputData.getFileName());
                if (domainName == null) {
                    return Collections.emptyMap();
                }

                Set<String> set = TranslationUtil.getXliffTranslations(new ByteArrayInputStream(inputData.getContent()));
                if (set.isEmpty()) {
                    return Collections.emptyMap();
                }

                // wrap with domain
                Map<String, Set<String>> map = new HashMap<>();
                map.put(domainName, set);
                return map;
            }
        };
    }

    @Nullable
    private static String getDomainName(String fileName) {
        String[] split = fileName.split("\\.");
        if (split.length < 2 || Arrays.stream(split).anyMatch(s -> s.isEmpty())) {
            return null;
        }

        // foo.fr.yml
        // dont index fr.yml
        int domainSplit = fileName.lastIndexOf(".");
        if (domainSplit <= 2) {
            return null;
        }

        String domain = StringUtils.join(Arrays.copyOfRange(split, 0, split.length - 2), ".");

        if (domain.endsWith("+intl-icu")) {
            // Remove +intl-icu suffix, as it is not part of the domain
            // https://symfony.com/blog/new-in-symfony-4-2-intlmessageformatter
            domain = domain.substring(0, domain.length() - 9);
        }

        if (StringUtils.isBlank(domain)) {
            return null;
        }

        return domain;
    }

    @Override
    public ID<String, Set<String>> getName() {
        return KEY;
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    public DataExternalizer<Set<String>> getValueExternalizer() {
        return DATA_EXTERNALIZER;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return (project, file) -> {
            FileType fileType = file.getFileType();
            if (fileType == PhpFileType.INSTANCE) {
                return getDomainName(file.getName()) != null;
            }

            return fileType == YAMLFileType.YML || "xlf".equalsIgnoreCase(file.getExtension()) || "xliff".equalsIgnoreCase(file.getExtension());
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 6;
    }
}
