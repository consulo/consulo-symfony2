package fr.adrienbrault.idea.symfony2plugin.routing.usages;

import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Shared route-usage helper methods.
 * This centralizes declaration detection, usage detection, and the labels used by the Find Usages UI.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class RouteUsageUtil {
    /**
     * Kotlin companion instance, kept for binary compatibility with callers compiled against the former Kotlin class.
     */
    public static final Companion Companion = new Companion();

    private RouteUsageUtil() {
    }

    /**
     * Normalizes a PSI element under the caret to the route declaration element that owns it.
     * Supported declarations are YAML keys, XML route ids, and the PHP #[Route] name string.
     */
    public static @Nullable PsiElement getRouteDeclarationTarget(PsiElement element) {
        YAMLKeyValue yamlTarget = getYamlRouteDeclarationTarget(element);
        if (yamlTarget != null) {
            return yamlTarget;
        }

        XmlAttribute xmlTarget = getXmlRouteDeclarationTarget(element);
        if (xmlTarget != null) {
            return xmlTarget;
        }

        StringLiteralExpression phpTarget = getPhpRouteDeclarationTarget(element);
        if (phpTarget != null) {
            return phpTarget;
        }

        return null;
    }

    /**
     * Extracts the logical route name from a declaration target, independent of YAML, XML, or PHP syntax.
     */
    public static @Nullable String getRouteNameForDeclaration(PsiElement element) {
        PsiElement declarationTarget = getRouteDeclarationTarget(element);
        if (declarationTarget instanceof YAMLKeyValue) {
            return ((YAMLKeyValue) declarationTarget).getKeyText();
        }
        else if (declarationTarget instanceof XmlAttribute) {
            return ((XmlAttribute) declarationTarget).getValue();
        }
        else if (declarationTarget instanceof StringLiteralExpression) {
            return ((StringLiteralExpression) declarationTarget).getContents();
        }
        else {
            return null;
        }
    }

    /**
     * Returns the primary label shown for a route declaration in Find Usages.
     */
    public static String getPresentableText(PsiElement element) {
        String routeName = getRouteNameForDeclaration(element);
        return StringUtils.defaultIfBlank(routeName, element.getText());
    }

    /**
     * Builds the gray location text shown after the route name, combining route path and project-relative file path.
     */
    public static @Nullable String getLocationString(PsiElement element) {
        String relativePath = getRelativeFilePath(element);
        String routePath = getRoutePath(element);
        if (StringUtils.isBlank(relativePath) && StringUtils.isBlank(routePath)) {
            return null;
        }

        if (StringUtils.isBlank(routePath)) {
            return relativePath;
        }

        if (StringUtils.isBlank(relativePath)) {
            return routePath;
        }

        return routePath + ", " + relativePath;
    }

    /**
     * Detects a route name when the caret is on a PHP or Twig usage string rather than on the declaration.
     */
    public static @Nullable String getRouteNameForUsage(PsiElement element) {
        for (PsiElement candidate : getCandidateElements(element)) {
            if (candidate instanceof StringLiteralExpression) {
                return ((StringLiteralExpression) candidate).getContents();
            }

            if (candidate.getNode() != null &&
                TwigUtil.isValidStringWithoutInterpolatedOrConcat(candidate) &&
                (TwigPattern.getAutocompletableRoutePattern().accepts(candidate) ||
                    ((TwigPattern.getTwigRouteComparePattern().accepts(candidate) ||
                        TwigPattern.getTwigRouteSameAsPattern().accepts(candidate) ||
                        TwigPattern.getTwigRouteInArrayPattern().accepts(candidate)) &&
                        TwigPattern.isRouteCompareContext(candidate)))
            ) {
                return candidate.getText();
            }
        }

        return null;
    }

    /**
     * Resolves the initial Find Usages targets for the caret location.
     * On declarations it returns the declaration target itself; on usage strings it resolves back to the owning declaration.
     */
    public static Collection<PsiElement> getRouteSearchTargets(PsiElement element) {
        for (PsiElement candidate : getCandidateElements(element)) {
            PsiElement declarationTarget = getRouteDeclarationTarget(candidate);
            if (declarationTarget != null) {
                return Collections.singletonList(declarationTarget);
            }
        }

        String routeName = getRouteNameForUsage(element);
        if (StringUtils.isBlank(routeName)) {
            return Collections.emptyList();
        }

        return RouteHelper.getRouteNameTarget(element.getProject(), routeName);
    }

    /**
     * Expands the caret context slightly so usage detection still works when IntelliJ gives a neighboring leaf node.
     * <p>
     * consulo-port: was an "internal" member of the Kotlin companion object; exposed as static method and via {@link Companion}.
     */
    public static Collection<PsiElement> getCandidateElements(PsiElement element) {
        Set<PsiElement> elements = new LinkedHashSet<>();
        elements.add(element);

        PsiElement parent = element.getParent();
        if (parent != null) {
            elements.add(parent);
        }

        if (element.getContainingFile() == null || element.getNode() == null) {
            return new ArrayList<>(elements);
        }

        PsiElement prevLeaf = PsiTreeUtil.prevLeaf(element);
        if (prevLeaf != null) {
            elements.add(prevLeaf);
        }

        PsiElement nextLeaf = PsiTreeUtil.nextLeaf(element);
        if (nextLeaf != null) {
            elements.add(nextLeaf);
        }

        return new ArrayList<>(elements);
    }

    /**
     * Checks whether the XML attribute under the caret is the id of a <route> declaration.
     */
    private static boolean isXmlRouteIdAttribute(@Nullable XmlAttribute xmlAttribute) {
        if (xmlAttribute == null || !"id".equals(xmlAttribute.getName())) {
            return false;
        }

        XmlTag parent = xmlAttribute.getParent();
        return parent != null && "route".equals(parent.getName());
    }

    private static @Nullable YAMLKeyValue getYamlRouteDeclarationTarget(PsiElement element) {
        YAMLKeyValue yamlKeyValue;
        if (element instanceof YAMLKeyValue) {
            yamlKeyValue = (YAMLKeyValue) element;
        }
        else {
            yamlKeyValue = element.getParent() instanceof YAMLKeyValue ? (YAMLKeyValue) element.getParent() : null;
        }

        if (yamlKeyValue != null && StringUtils.isNotBlank(yamlKeyValue.getKeyText()) && yamlKeyValue.getValue() instanceof YAMLCompoundValue) {
            return yamlKeyValue;
        }

        return null;
    }

    private static @Nullable XmlAttribute getXmlRouteDeclarationTarget(PsiElement element) {
        XmlAttribute xmlAttribute;
        if (element instanceof XmlAttribute) {
            xmlAttribute = (XmlAttribute) element;
        }
        else {
            xmlAttribute = element.getParent() instanceof XmlAttribute ? (XmlAttribute) element.getParent() : null;
        }

        return isXmlRouteIdAttribute(xmlAttribute) ? xmlAttribute : null;
    }

    private static @Nullable StringLiteralExpression getPhpRouteDeclarationTarget(PsiElement element) {
        StringLiteralExpression literalExpression = getPhpRouteNameLiteral(element);
        if (literalExpression == null) {
            return null;
        }

        PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(literalExpression, PhpAttribute.class, false);
        if (phpAttribute == null) {
            return null;
        }

        return isSupportedPhpAttributeDeclaration(literalExpression, phpAttribute) ? literalExpression : null;
    }

    /**
     * Verifies that the PHP attribute under the caret is a supported #[Route] declaration and that the caret belongs to its name string.
     */
    private static boolean isSupportedPhpAttributeDeclaration(PsiElement element, PhpAttribute phpAttribute) {
        String fqn = phpAttribute.getFQN();
        if (fqn == null || !RouteHelper.isRouteClassAnnotation(fqn)) {
            return false;
        }

        String routeName = PhpPsiAttributesUtil.getAttributeValueByNameAsString(phpAttribute, 1, "name");
        if (StringUtils.isBlank(routeName)) {
            return false;
        }

        if (element instanceof StringLiteralExpression) {
            return routeName.equals(((StringLiteralExpression) element).getContents());
        }

        StringLiteralExpression literalExpression = PsiTreeUtil.getParentOfType(element, StringLiteralExpression.class, false);
        return literalExpression != null && routeName.equals(literalExpression.getContents());
    }

    private static @Nullable StringLiteralExpression getPhpRouteNameLiteral(PsiElement element) {
        if (element instanceof StringLiteralExpression) {
            return (StringLiteralExpression) element;
        }
        else {
            return PsiTreeUtil.getParentOfType(element, StringLiteralExpression.class, false);
        }
    }

    /**
     * Resolves the route path either from the indexed route model or directly from the declaration PSI as a fallback.
     */
    private static @Nullable String getRoutePath(PsiElement element) {
        String routeName = getRouteNameForDeclaration(element);
        if (StringUtils.isBlank(routeName)) {
            return extractInlineRoutePath(element);
        }

        for (Route route : RouteHelper.getRoute(element.getProject(), routeName)) {
            String path = route.getPath();
            if (StringUtils.isNotBlank(path)) {
                return path;
            }
        }

        return extractInlineRoutePath(element);
    }

    /**
     * Extracts the inline path value directly from YAML, XML, or PHP attribute declarations when needed for presentation.
     */
    private static @Nullable String extractInlineRoutePath(PsiElement element) {
        YAMLKeyValue yamlKeyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue.class, false);
        if (yamlKeyValue != null) {
            for (YAMLKeyValue child : PsiTreeUtil.findChildrenOfType(yamlKeyValue, YAMLKeyValue.class)) {
                if ("path".equals(child.getKeyText())) {
                    return child.getValueText();
                }
            }
        }

        XmlAttribute xmlAttribute = PsiTreeUtil.getParentOfType(element, XmlAttribute.class, false);
        if (xmlAttribute != null) {
            XmlTag xmlTag = xmlAttribute.getParent();
            String path = xmlTag != null ? xmlTag.getAttributeValue("path") : null;
            if (StringUtils.isNotBlank(path)) {
                return path;
            }
        }

        PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(element, PhpAttribute.class, false);
        if (phpAttribute != null) {
            String path = PhpPsiAttributesUtil.getAttributeValueByNameAsString(phpAttribute, 0, "path");
            if (StringUtils.isNotBlank(path)) {
                return path;
            }
        }

        return null;
    }

    /**
     * Returns the project-relative file path used in Find Usages labels.
     */
    private static @Nullable String getRelativeFilePath(PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (file == null) {
            return null;
        }

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        return StringUtils.defaultIfBlank(
            VfsExUtil.getRelativeProjectPathStrict(element.getProject(), virtualFile),
            VfsExUtil.getRelativeProjectPath(element.getProject(), virtualFile)
        );
    }

    /**
     * Binary-compatible replacement of the former Kotlin companion object; all members delegate to the static methods.
     */
    public static final class Companion {
        private Companion() {
        }

        public @Nullable PsiElement getRouteDeclarationTarget(PsiElement element) {
            return RouteUsageUtil.getRouteDeclarationTarget(element);
        }

        public @Nullable String getRouteNameForDeclaration(PsiElement element) {
            return RouteUsageUtil.getRouteNameForDeclaration(element);
        }

        public String getPresentableText(PsiElement element) {
            return RouteUsageUtil.getPresentableText(element);
        }

        public @Nullable String getLocationString(PsiElement element) {
            return RouteUsageUtil.getLocationString(element);
        }

        public @Nullable String getRouteNameForUsage(PsiElement element) {
            return RouteUsageUtil.getRouteNameForUsage(element);
        }

        public Collection<PsiElement> getRouteSearchTargets(PsiElement element) {
            return RouteUsageUtil.getRouteSearchTargets(element);
        }

        public Collection<PsiElement> getCandidateElements(PsiElement element) {
            return RouteUsageUtil.getCandidateElements(element);
        }
    }
}
