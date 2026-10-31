package fr.adrienbrault.idea.symfony2plugin.config.xml.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.xml.language.psi.XmlAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class XmlDuplicateParameterKeyInspection extends XmlDuplicateServiceKeyInspection {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof XmlAttributeValue) {
                    XmlDuplicateServiceKeyInspectionKt.visitRoot((XmlAttributeValue) element, holder, "parameters", "parameter", "key", "Symfony: Duplicate Key");
                }

                super.visitElement(element);
            }
        };
    }

    // displayName, groupName, groupPath, language and level are inherited (same plugin.xml values)
    @Override
    public String getShortName() {
        return "XmlDuplicateParameterKeyInspection";
    }
}
