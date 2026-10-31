package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jspecify.annotations.Nullable;

/**
 * consulo-port: the registered nested inspections were moved to the top-level classes
 * {@link ContainerSettingDeprecatedInspectionYaml} and {@link ContainerSettingDeprecatedInspectionXml}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerSettingDeprecatedInspection {
    static void registerYmlRoutePatternProblem(ProblemsHolder holder, YAMLKeyValue element, ElementPattern<PsiElement> insideServiceKeyPattern) {
        String s = PsiElementUtils.trimQuote(element.getKeyText());
        if (("factory_class".equals(s) || "factory_method".equals(s) || "factory_service".equals(s)) && insideServiceKeyPattern.accepts(element)) {
            // services:
            //   foo:
            //      factory_*:
            registerProblem(holder, element.getKey());
        }
    }

    static void registerXmlAttributeProblem(ProblemsHolder holder, XmlAttribute xmlAttribute) {
        String name = xmlAttribute.getName();
        if (!("factory-class".equals(name) || "factory-method".equals(name) || "factory-service".equals(name))) {
            return;
        }

        XmlTag xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttribute, XmlTag.class, "service");
        if (xmlTagRoute != null) {
            registerProblem(holder, xmlAttribute.getFirstChild());
        }
    }

    private static void registerProblem(ProblemsHolder holder, @Nullable PsiElement target) {
        if (target == null) {
            return;
        }

        holder.registerProblem(target, "Symfony: this factory pattern is deprecated use 'factory' instead", ProblemHighlightType.LIKE_DEPRECATED);
    }
}
