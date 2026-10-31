package fr.adrienbrault.idea.symfony2plugin.action.generator;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.codeEditor.Editor;
import consulo.language.editor.action.CodeInsightAction;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.xml.language.XmlFileType;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ActionImpl(
    id = "Symfony2.ServiceArgumentGenerator",
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_GENERATE), anchor = ActionRefAnchor.LAST)
)
public class ServiceArgumentGenerateAction extends CodeInsightAction {
    public ServiceArgumentGenerateAction() {
        super(LocalizeValue.localizeTODO("Generate Arguments"), LocalizeValue.empty(), Symfony2Icons.SYMFONY);
    }

    @Override
    protected boolean isValidForFile(Project project, Editor editor, PsiFile file) {
        return file.getFileType() == XmlFileType.INSTANCE &&
            Symfony2ProjectComponent.isEnabled(project) &&
            getMatchXmlTag(editor, file) != null;
    }

    @Override
    protected CodeInsightActionHandler getHandler() {
        return new CodeInsightActionHandler() {
            @Override
            @RequiredUIAccess
            public void invoke(Project project, Editor editor, PsiFile psiFile) {
                XmlTag serviceTag = getMatchXmlTag(editor, psiFile);
                if (serviceTag == null) {
                    return;
                }

                if (!ServiceActionUtil.isValidXmlParameterInspectionService(serviceTag)) {
                    IdeHelper.showErrorHintIfAvailable(editor, "Sry, not supported service definition");
                    return;
                }

                List<String> args = ServiceActionUtil.getXmlMissingArgumentTypes(
                    serviceTag,
                    true,
                    new ContainerCollectionResolver.LazyServiceCollector(project)
                );
                if (args.isEmpty()) {
                    return;
                }

                ServiceActionUtil.fixServiceArgument(args, serviceTag);
            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        };
    }

    private static @Nullable XmlTag getMatchXmlTag(Editor editor, PsiFile file) {
        if (!(file instanceof XmlFile xmlFile)) {
            return null;
        }

        int offset = editor.getCaretModel().getOffset();
        if (offset <= 0) {
            return null;
        }

        PsiElement psiElement = xmlFile.findElementAt(offset);
        if (psiElement == null) {
            return null;
        }

        return ServiceActionUtil.getServiceTagValid(psiElement);
    }
}
