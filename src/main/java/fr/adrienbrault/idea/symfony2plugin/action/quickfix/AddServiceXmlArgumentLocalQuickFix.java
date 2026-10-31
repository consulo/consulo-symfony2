package fr.adrienbrault.idea.symfony2plugin.action.quickfix;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AddServiceXmlArgumentLocalQuickFix implements LocalQuickFix {

    private final List<String> args;

    public AddServiceXmlArgumentLocalQuickFix(List<String> args) {
        this.args = args;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Symfony: missing argument");
    }

    @Override
    public void applyFix(final Project project, ProblemDescriptor problemDescriptor) {

        final PsiElement parent = problemDescriptor.getPsiElement().getParent();
        if(!(parent instanceof XmlTag)) {
            return;
        }

        ServiceActionUtil.fixServiceArgument(args, (XmlTag) parent);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
