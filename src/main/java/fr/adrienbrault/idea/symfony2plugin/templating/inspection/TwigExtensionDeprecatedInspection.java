package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Inspection for deprecated Twig extensions (tags, filters, and functions)
 *
 * Examples:
 * {% spaceless %} - deprecated tag
 * {{ value|deprecated_filter }} - deprecated filter
 * {{ deprecated_function() }} - deprecated function
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigExtensionDeprecatedInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TwigExtensionDeprecatedInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Deprecated Twig Extension");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Twig");
    }

    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.localizeTODO("Symfony"), getGroupDisplayName()};
    }

    @Override
    public @Nullable Language getLanguage() {
        return TwigLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;

        private @Nullable ElementPattern<PsiElement> myFilterPattern;
        private @Nullable ElementPattern<PsiElement> myApplyFilterPattern;
        private @Nullable ElementPattern<PsiElement> myPrintBlockFunctionPattern;
        private @Nullable Map<String, String> myNamedDeprecatedTokenParserTags;
        private @Nullable Set<String> myDeprecatedFilters;
        private @Nullable Set<String> myDeprecatedFunctions;

        private MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        private ElementPattern<PsiElement> getFilterPattern() {
            if (myFilterPattern == null) {
                myFilterPattern = TwigPattern.getFilterPattern();
            }
            return myFilterPattern;
        }

        private ElementPattern<PsiElement> getApplyFilterPattern() {
            if (myApplyFilterPattern == null) {
                myApplyFilterPattern = TwigPattern.getApplyFilterPattern();
            }
            return myApplyFilterPattern;
        }

        private ElementPattern<PsiElement> getPrintBlockFunctionPattern() {
            if (myPrintBlockFunctionPattern == null) {
                myPrintBlockFunctionPattern = TwigPattern.getPrintBlockFunctionPattern();
            }
            return myPrintBlockFunctionPattern;
        }

        private Map<String, String> getNamedDeprecatedTokenParserTags() {
            if (myNamedDeprecatedTokenParserTags == null) {
                myNamedDeprecatedTokenParserTags = TwigUtil.getNamedDeprecatedTokenParserTags(myHolder.getProject());
            }
            return myNamedDeprecatedTokenParserTags;
        }

        private Set<String> getDeprecatedFilters() {
            if (myDeprecatedFilters == null) {
                myDeprecatedFilters = TwigUtil.getDeprecatedFilters(myHolder.getProject());
            }
            return myDeprecatedFilters;
        }

        private Set<String> getDeprecatedFunctions() {
            if (myDeprecatedFunctions == null) {
                myDeprecatedFunctions = TwigUtil.getDeprecatedFunctions(myHolder.getProject());
            }
            return myDeprecatedFunctions;
        }

        @Override
        public void visitElement(PsiElement element) {
            // {% tag %}
            if (element.getNode().getElementType() == TwigTokenTypes.TAG_NAME) {
                visitTagTokenName(element);
            }

            // {{ value|filter }}
            // {% apply filter %}
            if (getFilterPattern().accepts(element) || getApplyFilterPattern().accepts(element)) {
                visitFilter(element);
            }

            // {{ function() }}
            if (getPrintBlockFunctionPattern().accepts(element)) {
                visitFunction(element);
            }

            super.visitElement(element);
        }

        private void visitTagTokenName(PsiElement element) {
            String tagName = element.getText();
            if (tagName.isBlank()) {
                return;
            }

            // {% endspaceless % }
            if (tagName.length() > 3 && tagName.startsWith("end")) {
                tagName = tagName.substring("end".length());
            }

            Map<String, String> namedDeprecatedTokenParserTags = getNamedDeprecatedTokenParserTags();
            if (namedDeprecatedTokenParserTags.containsKey(tagName)) {
                // "Deprecated" highlight is not visible, so we are going here for weak warning
                // WEAK_WARNING would be match; but not really visible
                String message = namedDeprecatedTokenParserTags.get(tagName);
                myHolder.registerProblem(
                    element.getParent(),
                    message != null ? message : "Deprecated Twig tag",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                );
            }
        }

        private void visitFilter(PsiElement element) {
            String filterName = element.getText();

            if (filterName.isBlank()) {
                return;
            }

            if (getDeprecatedFilters().contains(filterName)) {
                myHolder.registerProblem(
                    element,
                    "Deprecated Twig filter",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                );
            }
        }

        private void visitFunction(PsiElement element) {
            String functionName = element.getText();

            if (functionName.isBlank()) {
                return;
            }

            if (getDeprecatedFunctions().contains(functionName)) {
                myHolder.registerProblem(
                    element,
                    "Deprecated Twig function",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                );
            }
        }

    }
}
