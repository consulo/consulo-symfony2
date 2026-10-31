package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.ContainerUtil;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlAttributeValue;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.XmlToken;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jspecify.annotations.Nullable;

/**
 * consulo-port: the registered nested inspections RouteSettingDeprecatedInspection$RouteSettingDeprecatedInspectionYaml / $RouteSettingDeprecatedInspectionXml
 * moved to the top-level classes {@link RouteSettingDeprecatedInspectionYaml} and {@link RouteSettingDeprecatedInspectionXml}
 * (nested classes cannot carry @ExtensionImpl). The former private top-level functions of the Kotlin file live here.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteSettingDeprecatedInspection {
    static void registerYmlRoutePatternProblem(ProblemsHolder holder, YAMLKeyValue element) {
        PsiElement key = element.getKey();
        if (key == null) {
            return;
        }

        String s = PsiElementUtils.trimQuote(element.getKeyText());
        if ("pattern".equals(s) && YamlHelper.isRoutingFile(element.getContainingFile())) {
            // pattern: foo
            holder.registerProblem(key, "Pattern is deprecated; use path instead", ProblemHighlightType.LIKE_DEPRECATED);

        }
        else if (("_method".equals(s) || "_scheme".equals(s)) && YamlHelper.isRoutingFile(element.getContainingFile())) {
            // requirements: { _method: 'foo', '_scheme': 'foo' }
            YAMLKeyValue parentOfType = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class);
            if (parentOfType != null && "requirements".equals(parentOfType.getKeyText())) {
                holder.registerProblem(key, String.format("The '%s' requirement is deprecated", s), ProblemHighlightType.LIKE_DEPRECATED);
            }
        }
    }

    static void registerRoutePatternProblem(ProblemsHolder holder, XmlAttribute xmlAttribute) {
        if (!"pattern".equals(xmlAttribute.getName())) {
            return;
        }

        XmlTag xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttribute, XmlTag.class, "route");
        if (xmlTagRoute != null && xmlAttribute.getFirstChild() != null) {
            holder.registerProblem(xmlAttribute.getFirstChild(), "Pattern is deprecated; use path instead", ProblemHighlightType.LIKE_DEPRECATED);
        }
    }

    static void registerAttributeRequirementProblem(ProblemsHolder holder, XmlAttributeValue xmlAttributeValue, String requirementAttribute) {
        if (!requirementAttribute.equals(xmlAttributeValue.getValue())) {
            return;
        }

        XmlAttribute xmlAttributeKey = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttributeValue, XmlAttribute.class, "key");
        if (xmlAttributeKey != null) {
            XmlTag xmlTagDefault = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttributeKey, XmlTag.class, "requirement");
            if (xmlTagDefault != null) {
                XmlTag xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlTagDefault, XmlTag.class, "route");
                if (xmlTagRoute != null) {
                    // attach to attribute token only we dont want " or ' char included
                    PsiElement target = findAttributeValueToken(xmlAttributeValue, requirementAttribute);

                    holder.registerProblem(target != null ? target : xmlAttributeValue, String.format("The '%s' requirement is deprecated", requirementAttribute), ProblemHighlightType.LIKE_DEPRECATED);
                }
            }
        }
    }

    /**
     * Find child token which stores value
     *
     * XmlToken: "'"
     * XmlToken: "attributeText"
     * XmlToken: "'"
     */
    private static @Nullable PsiElement findAttributeValueToken(XmlAttributeValue xmlAttributeValue, String attributeText) {
        return ContainerUtil.find(xmlAttributeValue.getChildren(), psiElement ->
            psiElement instanceof XmlToken && attributeText.equals(psiElement.getText())
        );
    }
}
