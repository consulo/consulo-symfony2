package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil;
import org.jspecify.annotations.Nullable;

/**
 * Detects invalid {@code {% from _self import ... %}} usage inside Symfony UX Twig Components.
 *
 * Inside a component context, {@code _self} does not refer to the current template, so macro
 * imports via {@code _self} will silently fail at runtime.
 *
 * Invalid:
 * <pre>
 * {@code
 * <twig:Alert>
 *     {% from _self import message_formatter %}   <-- ERROR
 * </twig:Alert>
 * }
 * </pre>
 *
 * Valid:
 * <pre>
 * {@code
 * <twig:Alert>
 *     {% from 'path/to/template.html.twig' import message_formatter %}
 * </twig:Alert>
 * }
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see <a href="https://symfony.com/bundles/ux-twig-component/current/index.html#using-macros-in-components">Symfony UX Twig Components - Macros</a>
 */
@ExtensionImpl
public class TwigComponentSelfMacroImportInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                // Match the RESERVED_ID token (_self) that appears in an import tag
                ASTNode node = element.getNode();
                if (node != null && node.getElementType() == TwigTokenTypes.RESERVED_ID
                    && "_self".equals(element.getText())
                    && isInsideFromImportTag(element)
                    && isInsideComponentContext(element)
                ) {
                    holder.registerProblem(
                        element,
                        "Cannot use '_self' to import macros inside a Twig component. Use the full template path instead.",
                        ProblemHighlightType.GENERIC_ERROR
                    );
                }

                super.visitElement(element);
            }

            /**
             * Returns true if this {@code _self} token is the template source in a
             * {@code {% from _self import ... %}} statement.
             */
            private boolean isInsideFromImportTag(PsiElement element) {
                PsiElement parent = element.getParent();
                if (parent == null) {
                    return false;
                }

                ASTNode parentNode = parent.getNode();
                return parentNode != null && parentNode.getElementType() == TwigElementTypes.IMPORT_TAG;
            }

            /**
             * Returns true when the element is inside a Twig component context:
             * either an HTML-syntax component ({@code <twig:Name>}) or a
             * Twig-syntax component ({@code {% component 'Name' %}}).
             */
            private boolean isInsideComponentContext(PsiElement element) {
                if (!(element.getContainingFile() instanceof TwigFile containingFile)) {
                    return false;
                }

                // Check for HTML component context via the HTML language view
                if (TwigHtmlCompletionUtil.isInsideHtmlComponentTag(element, containingFile)) {
                    return true;
                }

                // Check for Twig {% component %} tag context
                return isInsideTwigComponentTag(element);
            }

            /**
             * Checks if the element is inside a {@code {% component '...' %}...{% endcomponent %}} block
             * by counting occurrences of component/endcomponent tags in the text before the element.
             */
            private boolean isInsideTwigComponentTag(PsiElement element) {
                PsiFile file = element.getContainingFile();
                if (file == null) {
                    return false;
                }

                String fileText = file.getText();
                int offset = element.getTextOffset();
                if (offset <= 0 || offset > fileText.length()) {
                    return false;
                }

                String textBefore = fileText.substring(0, offset);

                // Count open and close component tags before the element position
                int openCount = countTagOccurrences(textBefore, "component");
                int closeCount = countTagOccurrences(textBefore, "endcomponent");

                return openCount > closeCount;
            }

            private int countTagOccurrences(String text, String tagName) {
                int count = 0;
                int index = 0;
                // Match {%- component or {% component (optional whitespace/dash)
                String pattern = "{%";
                while (true) {
                    index = text.indexOf(pattern, index);
                    if (index == -1) {
                        break;
                    }

                    int remaining = index + pattern.length();
                    // Skip optional whitespace and dash
                    while (remaining < text.length() && " \t-".indexOf(text.charAt(remaining)) >= 0) {
                        remaining++;
                    }
                    // Check if the tag name matches
                    if (text.startsWith(tagName, remaining)) {
                        int afterTag = remaining + tagName.length();
                        // Must be followed by whitespace, -, or %}
                        if (afterTag >= text.length() || isWhitespace(text.charAt(afterTag))
                            || "-%".indexOf(text.charAt(afterTag)) >= 0
                        ) {
                            count++;
                        }
                    }
                    index++;
                }
                return count;
            }

            // consulo-port: same semantics as Kotlin Char.isWhitespace()
            private boolean isWhitespace(char ch) {
                return Character.isWhitespace(ch) || Character.isSpaceChar(ch);
            }
        };
    }

    @Override
    public String getShortName() {
        return "TwigComponentSelfMacroImportInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Twig Component _self Macro Import");
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
        return HighlightDisplayLevel.ERROR;
    }
}
