package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.language.psi.PsiFile;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.SerializableServiceExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter.FileInputFilter;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ServicesDefinitionStubIndex extends FileBasedIndexExtension<String, ServiceSerializable> {

    private static final int MAX_FILE_BYTE_SIZE = 5242880;

    public static final ID<String, ServiceSerializable> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.service_definition");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final SerializableServiceExternalizer EXTERNALIZER = SerializableServiceExternalizer.INSTANCE;

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, ServiceSerializable, FileContent> getIndexer() {

        return inputData -> {

            Map<String, ServiceSerializable> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !isValidForIndex(inputData, psiFile)) {
                return map;
            }

            for (ServiceSerializable service : ServiceContainerUtil.getServicesInFile(psiFile)) {
                map.put(service.getId().toLowerCase(), service);
            }

            return map;
        };
    }

    @Override
    public ID<String, ServiceSerializable> getName() {
        return KEY;
    }


    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    public DataExternalizer<ServiceSerializable> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return FileInputFilter.XML_YAML_PHP;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 10;
    }

    public static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {
        if (!StubIndexValidationUtil.isValidForIndex(
            inputData,
            psiFile,
            MAX_FILE_BYTE_SIZE,
            true,
            Set.of("xml", "yml", "yaml", "php")
        )) {
            return false;
        }

        String extension = inputData.getFile().getExtension();

        // possible fixture or test file
        // to support also library paths, only filter them on project files
        String relativePath = VirtualFileUtil.getRelativePath(inputData.getFile(), ProjectUtil.getProjectDir(psiFile.getProject()), '/');

        // exclude settings-configured service container paths (lightweight path comparison, no VFS lookups)
        List<ContainerFile> settingsContainerFiles = Settings.getInstance(psiFile.getProject()).containerFiles;
        if (settingsContainerFiles != null) {
            String inputPath = FileUtil.toSystemIndependentName(inputData.getFile().getPath());
            String inputRelativePath = relativePath != null ? FileUtil.toSystemIndependentName(relativePath) : null;
            for (ContainerFile containerFile : settingsContainerFiles) {
                String path = containerFile.getPath();
                if (path != null) {
                    String normalizedPath = FileUtil.toSystemIndependentName(path);
                    if (FileUtil.pathsEqual(inputPath, normalizedPath) || (inputRelativePath != null && FileUtil.pathsEqual(inputRelativePath, normalizedPath))) {
                        return false;
                    }
                }
            }
        }

        // exclude XML files in var/cache and app/cache directories
        if (relativePath != null && extension.equalsIgnoreCase("xml")) {
            String lowerPath = FileUtil.toSystemIndependentName(relativePath).toLowerCase(Locale.ROOT);
            if (lowerPath.startsWith("var/cache/") || lowerPath.startsWith("app/cache/")) {
                return false;
            }
        }

        return true;
    }
}
