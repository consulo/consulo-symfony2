package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.xml.language.psi.XmlText;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.psi.YAMLScalar;

/**
 * consulo-port: the registered nested inspections were moved to the top-level classes
 * {@link ContainerConstantYamlInspection} and {@link ContainerConstantXmlInspection}.
 * This class is abstract because Consulo's InspectionTool requires display name, group and level, and it is not registered.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public abstract class ContainerConstantInspection extends LocalInspectionTool {
    public static final String MESSAGE = "Symfony: constant not found";

    static void visitYamlElement(YAMLScalar psiElement, ProblemsHolder holder) {
        String textValue = psiElement.getTextValue();
        if (textValue.startsWith("!php/const:")) {
            String constantName = textValue.substring(11);
            if (StringUtils.isNotBlank(constantName) && ServiceContainerUtil.getTargetsForConstant(holder.getProject(), constantName).isEmpty()) {
                holder.registerProblem(psiElement, ContainerConstantInspection.MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            }
        }
    }

    static void visitXmlElement(PsiElement psiElement, ProblemsHolder holder, ElementPattern<PsiElement> xmlConstantPattern) {
        if (!xmlConstantPattern.accepts(psiElement)) {
            return;
        }

        PsiElement xmlText = psiElement.getParent();
        if (!(xmlText instanceof XmlText)) {
            return;
        }

        String value = ((XmlText) xmlText).getValue();
        if (StringUtils.isBlank(value)) {
            return;
        }

        if (ServiceContainerUtil.getTargetsForConstant(holder.getProject(), value).isEmpty()) {
            holder.registerProblem(xmlText, ContainerConstantInspection.MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }
    }
}
