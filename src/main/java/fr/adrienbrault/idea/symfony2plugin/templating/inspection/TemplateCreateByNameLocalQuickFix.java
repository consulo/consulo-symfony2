package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.twig.TwigFileType;
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.IntentionAndQuickFixAction;
import consulo.language.editor.intention.HighPriorityAction;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateCreateByNameLocalQuickFix extends IntentionAndQuickFixAction implements HighPriorityAction {
    private final String[] templateNames;

    public TemplateCreateByNameLocalQuickFix(String... templateNames) {
        this.templateNames = templateNames;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Twig: Create Template");
    }

    // consulo-port: applyFix(Project, ProblemDescriptor) is final in Consulo IntentionAndQuickFixAction and delegates to
    // applyFix(Project, PsiFile, Editor), which ends up in applyFix(Project) as upstream did
    @Override
    public void applyFix(Project project, PsiFile psiFile, @Nullable Editor editor) {
        applyFix(project);
    }

    private void applyFix(Project project) {
        Collection<String> templatePaths = new LinkedHashSet<>();

        for (String templateName : templateNames) {
            templatePaths.addAll(TwigUtil.getCreateAbleTemplatePaths(project, templateName));
        }

        if(templatePaths.isEmpty()) {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

            // notify error if editor is focused
            if (editor != null) {
                IdeHelper.showErrorHintIfAvailable(editor, "Can not find a target dir");
            }

            return;
        }

        List<String> list = new ArrayList<>(templatePaths);
        list.sort(new ProjectTemplateComparator());

        JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(list)
            .setTitle("Twig: Template Path")
            .setItemChosenCallback(selectedValue -> {
                String commandName = "Create Template: " + (selectedValue.length() > 15 ? selectedValue.substring(selectedValue.length() - 15) : selectedValue);
                WriteCommandAction.runWriteCommandAction(project, commandName, null, () -> createFile(project, selectedValue));
            })
            .createPopup();

        // show popup in scope
        Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if(selectedTextEditor != null) {
            selectedTextEditor.showPopupInBestPositionFor(popup);
        } else {
            popup.showCenteredInCurrentWindow(project);
        }
    }

    private static void createFile(Project project, String relativePath) {
        VirtualFile relativeBlockScopeFile = null;

        int i = relativePath.lastIndexOf("/");
        if(i > 0) {
            relativeBlockScopeFile = VirtualFileUtil.findRelativeFile(ProjectUtil.getProjectDir(project), relativePath.substring(0, i).split("/"));
        }

        String content = TwigUtil.buildStringFromTwigCreateContainer(project, relativeBlockScopeFile);

        IdeHelper.RunnableCreateAndOpenFile runnableCreateAndOpenFile = IdeHelper.getRunnableCreateAndOpenFile(project, TwigFileType.INSTANCE, ProjectUtil.getProjectDir(project), relativePath);
        if(content != null) {
            runnableCreateAndOpenFile.setContent(content);
        }

        runnableCreateAndOpenFile.run();
    }

    private static class ProjectTemplateComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            int[] weights = new int[]{0, 0};

            int index = 0;
            for (String s : Arrays.asList(o1, o2)) {
                int weight = 0;

                // low priority for vendor and symfony
                if (s.toLowerCase().contains("/vendor/")) {
                    weight += 2;
                }

                if (s.toLowerCase().contains("/symfony/")) {
                    weight += 1;
                }

                // project finds
                if (s.toLowerCase().contains("/templates/")) {
                    weight -= 4;
                }

                // foobundle is better then any other
                if (s.toLowerCase().contains("bundle/")) {
                    weight -= 3;
                }

                weights[index] = weight;
                index += 1;
            }

            return weights[0] - weights[1];
        }
    }
}