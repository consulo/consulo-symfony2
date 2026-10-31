package fr.adrienbrault.idea.symfony2plugin.translation;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.IntentionAndQuickFixAction;
import consulo.language.editor.ui.navigation.PsiTargetNavigationService;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.navigation.NavigationService;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationKeyIntentionAndQuickFixAction extends IntentionAndQuickFixAction {
    private final String key;

    private final String domain;

    private final DomainCollector domainCollector;

    public TranslationKeyIntentionAndQuickFixAction(String key, String domain) {
        this(key, domain, new AllDomainCollector());
    }

    public TranslationKeyIntentionAndQuickFixAction(String key, String domain, DomainCollector domainCollector) {
        this.key = key;
        this.domain = domain;
        this.domainCollector = domainCollector;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Symfony: Add translations");
    }

    private String getPresentableName(Project project, VirtualFile virtualFile) {
        // try to find suitable presentable filename
        String filename = virtualFile.getPath();

        String relativePath = VirtualFileUtil.getRelativePath(virtualFile, ProjectUtil.getProjectDir(project), '/');
        if(relativePath != null) {
            filename =  relativePath;
        }

        return StringUtils.abbreviate(filename, 180);
    }

    @Override
    public void applyFix(Project project, PsiFile psiFile, @Nullable Editor editor) {
        // consulo-port: no intention preview
        if(editor == null) {
            return;
        }

        List<PsiFile> files = new ArrayList<>();

        for(PsiFile translationPsiFile: this.domainCollector.collect(project, key, domain)) {
            if(translationPsiFile instanceof YAMLFile || TranslationUtil.isSupportedXlfFile(translationPsiFile)) {
                String relativePath = VirtualFileUtil.getRelativePath(translationPsiFile.getVirtualFile(), ProjectUtil.getProjectDir(project), '/');

                // sort collection. eg vendor last
                if(relativePath != null && (relativePath.startsWith("app") || relativePath.startsWith("src"))) {
                    files.add(0, translationPsiFile);
                } else {
                    files.add(translationPsiFile);
                }
            }
        }

        if(files.isEmpty()) {
            IdeHelper.showErrorHintIfAvailable(editor, "Ops, no domain file found");
            return;
        }

        Application.get().getInstance(PsiTargetNavigationService.class)
            .<PsiFile>newNavigator(() -> files)
            .presentationProvider(file -> Application.get().getInstance(NavigationService.class)
                .presentationBuilder(LocalizeValue.of(getPresentableName(project, file.getVirtualFile())))
                .build()
            )
            .title(LocalizeValue.localizeTODO("Symfony: Translation files"))
            .processor(selectedFile -> {
                CommandProcessor.getInstance().newCommand()
                    .project(selectedFile.getProject())
                    .name(LocalizeValue.of("Translation insert " + selectedFile.getName()))
                    .inWriteAction()
                    .run(() -> {
                        TranslationInsertUtil.invokeTranslation(selectedFile, key, domain);
                    });

                return true;
            })
            .navigate(editor, project);
    }

    public interface DomainCollector {
        Collection<PsiFile> collect(Project project, String key, String domain);
    }

    private static class AllDomainCollector implements DomainCollector {
        @Override
        public Collection<PsiFile> collect(Project project, String key, String domain) {
            return TranslationUtil.getDomainPsiFiles(project, domain);
        }
    }
}
