package fr.adrienbrault.idea.symfony2plugin.vite;

import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import consulo.language.ast.IElementType;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Consulo port: top-level declarations of RepriseTwigCallUtil.kt
 */
public final class RepriseTwigCallUtilKt {
    private static final Set<String> REPRISE_FUNCTIONS = Set.of(
        "reprise_entry_script_tags", "reprise_entry_link_tags",
        "reprise_entry_js_files", "reprise_entry_css_files", "reprise_entry_exists"
    );

    private RepriseTwigCallUtilKt() {
    }

    /** Shared, PSI-local recognition for completion, navigation and the Twig usage index. */
    public static ElementPattern<PsiElement> viteEntryPattern() {
        return PlatformPatterns.or(
            TwigPattern.getPrintBlockOrTagFunctionPattern("vite_entry_link_tags", "vite_entry_script_tags"),
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT).with(new PatternCondition<PsiElement>("repriseEntry") {
                @Override
                public boolean accepts(PsiElement element, ProcessingContext context) {
                    return isRepriseDefaultEntry(element);
                }
            })
        );
    }

    /**
     * Matches the entry string in Reprise calls:
     * - `{{ reprise_entry_script_tags('checkout') }}`
     * - `{{ reprise_entry_link_tags(entryName='checkout') }}`
     * - `{{ reprise_entry_css_files(build=null, entryName='checkout') }}`
     * - `{{ reprise_entry_link_tags(entryName: 'checkout', build: null) }}`
     * - `{% set files = reprise_entry_js_files('checkout', 'cdn', null) %}`
     * - `{% if reprise_entry_exists('checkout', null) %}{% endif %}`
     */
    private static boolean isRepriseDefaultEntry(PsiElement element) {
        PsiElement call = element.getParent();
        if (call == null) {
            return false;
        }
        if (call.getNode().getElementType() != TwigElementTypes.FUNCTION_CALL) {
            return false;
        }

        List<PsiElement> children = new ArrayList<>();
        for (PsiElement child = call.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!child.getText().isBlank()) {
                children.add(child);
            }
        }

        if (children.isEmpty()) {
            return false;
        }

        String functionName = children.get(0).getText();
        if (!REPRISE_FUNCTIONS.contains(functionName)) {
            return false;
        }

        int opening = -1;
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getNode().getElementType() == TwigTokenTypes.LBRACE) {
                opening = i;
                break;
            }
        }
        if (opening < 0) {
            return false;
        }

        // Nested calls and hashes remain whole PSI nodes, so their commas are not separators.
        List<List<PsiElement>> arguments = new ArrayList<>();
        List<PsiElement> current = new ArrayList<>();
        for (PsiElement child : children.subList(opening + 1, children.size())) {
            if (child.getNode().getElementType() == TwigTokenTypes.RBRACE) {
                break;
            }
            if (child.getNode().getElementType() == TwigTokenTypes.COMMA) {
                arguments.add(current);
                current = new ArrayList<>();
            }
            else {
                current.add(child);
            }
        }
        arguments.add(current);

        List<PsiElement> entry = null;
        List<PsiElement> build = null;
        int buildIndex = "reprise_entry_exists".equals(functionName) ? 1 : 2;
        for (int index = 0; index < arguments.size(); index++) {
            List<PsiElement> argument = arguments.get(index);
            boolean named = false;
            if (argument.size() >= 2) {
                IElementType separator = argument.get(1).getNode().getElementType();
                named = separator == TwigTokenTypes.EQ || separator == TwigTokenTypes.COLON;
            }

            @Nullable String name;
            if (named) {
                name = argument.get(0).getText();
            }
            else if (index == 0) {
                name = "entryName";
            }
            else if (index == buildIndex) {
                name = "build";
            }
            else {
                name = null;
            }

            List<PsiElement> value = named ? argument.subList(2, argument.size()) : argument;
            if ("entryName".equals(name)) {
                entry = value;
            }
            else if ("build".equals(name)) {
                build = value;
            }
        }

        // Named/dynamic builds need Reprise's YAML-to-config mapping; never use default entries for them.
        if (build != null && !(build.size() == 1 && build.get(0).getText().equals("null"))) {
            return false;
        }

        List<PsiElement> value = entry;
        if (value == null) {
            return false;
        }

        if (value.size() != 3 || !value.get(1).equals(element)) {
            return false;
        }

        IElementType quoteType = value.get(0).getNode().getElementType();
        return (quoteType == TwigTokenTypes.SINGLE_QUOTE || quoteType == TwigTokenTypes.DOUBLE_QUOTE)
            && value.get(2).getNode().getElementType() == quoteType;
    }
}
