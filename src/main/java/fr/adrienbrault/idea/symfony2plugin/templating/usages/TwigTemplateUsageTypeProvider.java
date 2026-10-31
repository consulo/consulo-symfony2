package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.xml.language.psi.XmlTag;
import consulo.language.pattern.ElementPattern;
import consulo.usage.PsiElementUsageTarget;
import consulo.usage.UsageTarget;
import consulo.usage.UsageType;
import consulo.localize.LocalizeValue;
import consulo.usage.UsageTypeProvider;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;

/**
 * Classifies Twig template usages by type (extends, include, embed, controller, component, etc.)
 * so IntelliJ's built-in UsageTypeGroupingRule shows them as named top-level groups.
 *
 * <p>The provider is entirely inert unless the Find Usages target is a {@link TwigFile},
 * so it never interferes with PHP, Java, or any other language's Find Usages grouping.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigTemplateUsageTypeProvider implements UsageTypeProvider {

    private static final UsageType EXTENDS    = new UsageType(LocalizeValue.localizeTODO("extends"));
    private static final UsageType INCLUDE    = new UsageType(LocalizeValue.localizeTODO("include"));
    private static final UsageType EMBED      = new UsageType(LocalizeValue.localizeTODO("embed"));
    private static final UsageType IMPORT     = new UsageType(LocalizeValue.localizeTODO("import"));
    private static final UsageType FROM       = new UsageType(LocalizeValue.localizeTODO("from"));
    private static final UsageType FORM_THEME = new UsageType(LocalizeValue.localizeTODO("form_theme"));
    private static final UsageType CONTROLLER = new UsageType(LocalizeValue.localizeTODO("controller"));
    private static final UsageType COMPONENT  = new UsageType(LocalizeValue.localizeTODO("component"));

    /** Required by {@link UsageTypeProvider}; not called when the target-aware variant is available. */
    @Override
    public @Nullable UsageType getUsageType(PsiElement element) {
        return null;
    }

    @Override
    public @Nullable UsageType getUsageType(PsiElement element, UsageTarget[] targets) {
        // Only activate when doing Find Usages for a Twig template file
        if (!isTwigFileTarget(targets)) {
            return null;
        }

        // PHP controller: StringLiteralExpression / FunctionReference / method identifier
        if (element.getContainingFile() instanceof PhpFile) {
            return CONTROLLER;
        }

        // Twig extends: {% extends "..." %} or {% extends cond ? "..." : "..." %}
        // The element is the STRING_TEXT leaf inside the tag, not the TwigExtendsTag itself.
        if (hasPatternMatch(element, TwigPattern.getTemplateFileReferenceTagPattern("extends"))
            || hasPatternMatch(element, TwigPattern.getTagTernaryPattern(TwigElementTypes.EXTENDS_TAG))) {
            return EXTENDS;
        }

        // Twig component: <twig:ComponentName> XML tag
        if (element instanceof XmlTag xmlTag) {
            String name = xmlTag.getName();
            if (name != null && (name.startsWith("twig:") || "twig".equals(xmlTag.getNamespacePrefix()))) {
                return COMPONENT;
            }
        }

        // Twig component: {{ component('Name') }} / {% component Name %}
        if (hasPatternMatch(element, TwigPattern.getComponentPattern()) || isComponentTagValue(element)) {
            return COMPONENT;
        }

        // {% from ... %} and {% import ... %}
        if (hasPatternMatch(element, TwigPattern.getFromTemplateElementPattern())) {
            return FROM;
        }
        if (hasPatternMatch(element, TwigPattern.getTagNameParameterPattern(TwigElementTypes.IMPORT_TAG, "import"))) {
            return IMPORT;
        }

        // {% include ... %} / {{ include(...) }} / {{ source(...) }}
        if (hasTagName(element, "include")
            || hasPatternMatch(element, TwigPattern.getTagNameParameterPattern(TwigElementTypes.INCLUDE_TAG, "include"))
            || hasPatternMatch(element, TwigPattern.getIncludeTagArrayPattern())
            || hasPatternMatch(element, TwigPattern.getIncludeSourcePrintBlockOrTagFunctionPattern())
            || hasPatternMatch(element, TwigPattern.getPrintBlockOrTagFunctionSecondParameterPattern("block"))) {
            return INCLUDE;
        }

        // {% embed ... %}
        if (hasPatternMatch(element, TwigPattern.getEmbedPattern())) {
            return EMBED;
        }

        // {% form_theme form with "..." %}
        if (hasTagName(element, "form_theme")
            || hasPatternMatch(element, TwigPattern.getFormThemeFileTagPattern())) {
            return FORM_THEME;
        }

        return null;
    }

    private static boolean isTwigFileTarget(UsageTarget[] targets) {
        for (UsageTarget target : targets) {
            if (target instanceof PsiElementUsageTarget psiTarget
                    && psiTarget.getElement() instanceof TwigFile) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true for the component name element in {% component Name %}.
     */
    private static boolean isComponentTagValue(PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent == null || parent.getNode() == null || parent.getNode().getElementType() != TwigElementTypes.TAG) {
            return false;
        }

        if (!hasTagName(parent, "component")) {
            return false;
        }

        PsiElement tagStart = parent.getFirstChild();
        if (tagStart == null) {
            return false;
        }

        PsiElement tagName = PsiElementUtils.getNextSiblingAndSkip(tagStart, TwigTokenTypes.TAG_NAME);
        if (tagName == null) {
            return false;
        }

        PsiElement componentValue = PsiElementUtils.getNextSiblingAndSkip(
            tagName,
            TwigTokenTypes.IDENTIFIER,
            TwigTokenTypes.STRING_TEXT,
            TwigTokenTypes.RESERVED_ID,
            TwigTokenTypes.SINGLE_QUOTE,
            TwigTokenTypes.DOUBLE_QUOTE
        );

        return componentValue == element;
    }

    private static boolean hasPatternMatch(PsiElement element, ElementPattern<? super PsiElement> pattern) {
        ArrayDeque<PsiElement> stack = new ArrayDeque<>();
        stack.push(element);

        while (!stack.isEmpty()) {
            PsiElement current = stack.pop();
            if (pattern.accepts(current)) {
                return true;
            }

            for (PsiElement child = current.getFirstChild(); child != null; child = child.getNextSibling()) {
                stack.push(child);
            }
        }

        return false;
    }

    private static boolean hasTagName(PsiElement element, String tagName) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode() != null
                && child.getNode().getElementType() == TwigTokenTypes.TAG_NAME
                && tagName.equals(child.getText())) {
                return true;
            }
        }

        return false;
    }
}
