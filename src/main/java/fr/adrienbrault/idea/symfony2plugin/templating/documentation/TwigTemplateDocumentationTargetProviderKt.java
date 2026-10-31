package fr.adrienbrault.idea.symfony2plugin.templating.documentation;

import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigArrayLiteral;
import com.jetbrains.twig.elements.TwigElementTypes;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.editor.documentation.DocumentationMarkup;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.StringUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Top-level declarations of the former TwigTemplateDocumentationTargetProvider.kt
 */
public final class TwigTemplateDocumentationTargetProviderKt {
    private static final List<Map.Entry<String, ElementPattern<PsiElement>>> TEMPLATE_USAGE_PATTERNS = List.of(
        Map.entry("extends", TwigPattern.getTemplateFileReferenceTagPattern("extends")),
        Map.entry("extends", TwigPattern.getTagTernaryPattern(TwigElementTypes.EXTENDS_TAG)),
        Map.entry("include", TwigPattern.getTemplateFileReferenceTagPattern("include")),
        Map.entry("include", TwigPattern.getIncludeTagArrayPattern()),
        Map.entry("include", TwigPattern.getTagTernaryPattern(TwigElementTypes.INCLUDE_TAG)),
        Map.entry("include", TwigPattern.getPrintBlockOrTagFunctionPattern("include")),
        Map.entry("embed", TwigPattern.getTemplateFileReferenceTagPattern("embed")),
        Map.entry("import", TwigPattern.getTemplateFileReferenceTagPattern("import")),
        Map.entry("from", TwigPattern.getTemplateFileReferenceTagPattern("from")),
        Map.entry("source", TwigPattern.getPrintBlockOrTagFunctionPattern("source")),
        Map.entry("form_theme", TwigPattern.getFormThemeFileTagPattern()),
        Map.entry("block", TwigPattern.getPrintBlockOrTagFunctionSecondParameterPattern("block"))
    );

    private TwigTemplateDocumentationTargetProviderKt() {
    }

    public static @Nullable String templateUsageType(PsiElement element) {
        if (elementType(element) != TwigTokenTypes.STRING_TEXT || !TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
            return null;
        }

        // Current Twig PSI nests array strings below TwigArrayValue.
        PsiElement parent = element.getParent();
        if (parent != null && parent.getParent() instanceof TwigArrayLiteral array) {
            IElementType parentType = array.getParent() != null ? elementType(array.getParent()) : null;
            PsiElement previous = PsiTreeUtil.prevLeaf(array);
            while (previous != null && previous.getText().isBlank()) {
                previous = PsiTreeUtil.prevLeaf(previous);
            }

            if (parentType == TwigElementTypes.INCLUDE_TAG && previous != null && "include".equals(previous.getText())) {
                return "include";
            }

            if (array.getParent().getText().stripLeading().startsWith("{% form_theme ") && previous != null && "with".equals(previous.getText())) {
                return "form_theme";
            }
        }

        for (Map.Entry<String, ElementPattern<PsiElement>> pattern : TEMPLATE_USAGE_PATTERNS) {
            if (pattern.getValue().accepts(element)) {
                return pattern.getKey();
            }
        }

        return null;
    }

    private static @Nullable IElementType elementType(PsiElement element) {
        ASTNode node = element.getNode();
        return node != null ? node.getElementType() : null;
    }

    public static String renderTwigTemplateDocumentation(TwigTemplateDocumentationData data) {
        StringBuilder builder = new StringBuilder();

        builder.append(DocumentationMarkup.CONTENT_START);

        List<Map.Entry<String, String>> paths = new ArrayList<>(data.getPaths().entrySet());
        paths.sort(Map.Entry.comparingByValue());
        for (int index = 0; index < paths.size(); index++) {
            String url = paths.get(index).getKey();
            String path = paths.get(index).getValue();
            if (index > 0) {
                builder.append("<br>");
            }

            builder.append("Path: ").append(code(path));
            Set<String> parents = data.getParents().get(url);
            if (parents != null && !parents.isEmpty()) {
                List<String> sortedParents = new ArrayList<>(parents);
                sortedParents.sort(null);

                List<String> codes = new ArrayList<>();
                for (String parent : sortedParents) {
                    codes.add(code(parent));
                }

                builder.append("<br>Extends: ").append(String.join(", ", codes));
            }
        }

        List<String> roles = new ArrayList<>();
        if (!data.getPhpFiles().isEmpty()) {
            roles.add("PHP rendering");
        }
        if (data.getParents().values().stream().anyMatch(it -> !it.isEmpty())) {
            roles.add("extends");
        }
        if (data.getTwigUsages().entrySet().stream().anyMatch(it -> !"extends".equals(it.getKey()) && !it.getValue().isEmpty())) {
            roles.add("included");
        }
        Set<String> extended = data.getTwigUsages().get("extends");
        if (extended != null && !extended.isEmpty()) {
            roles.add("extended");
        }

        if (!roles.isEmpty()) {
            builder.append("<br>Roles: ").append(String.join(", ", roles));
        }

        if (data.getPaths().size() > 1) {
            builder.append("<br>Usages match template names; attribution to an individual path is ambiguous.");
        }

        List<String> callers = new ArrayList<>(data.getCallers());
        callers.sort(null);
        builder.append("<br>PHP:");
        if (data.getPhpFiles().size() != callers.size()) {
            builder.append(" ").append(data.getPhpFiles().size()).append(" files");
        }

        for (String caller : callers.subList(0, Math.min(5, callers.size()))) {
            builder.append("<br>&nbsp;&nbsp;- ").append(code(caller));
        }

        if (callers.size() > 5) {
            builder.append("<br>&nbsp;&nbsp;...<br>&nbsp;&nbsp;5 / ").append(callers.size());
        }

        builder.append("<br>Twig:");
        for (Map.Entry<String, Set<String>> entry : new TreeMap<>(data.getTwigUsages()).entrySet()) {
            String type = entry.getKey();
            builder.append("<br>&nbsp;&nbsp;- ").append("extends".equals(type) ? "Extended by" : escape(type))
                .append(": ").append(entry.getValue().size()).append(" files");
        }

        if (!data.getPhpFiles().isEmpty() || data.getTwigUsages().values().stream().anyMatch(it -> !it.isEmpty())) {
            builder.append("<br><a href=\"").append(TwigTemplateDocumentationTargetKt.TEMPLATE_FIND_USAGES_LINK).append("\">Find usages</a>");
        }

        builder.append(DocumentationMarkup.CONTENT_END);

        return builder.toString();
    }

    private static String escape(String value) {
        return StringUtil.escapeXmlEntities(value);
    }

    private static String code(String value) {
        return "<code>" + escape(value) + "</code>";
    }
}
