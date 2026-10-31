package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
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
 * Detects mixing of Twig block syntax and HTML component syntax in Symfony UX Twig Components.
 *
 * Invalid: using {@code {% block name %}} inside {@code <twig:Component>}
 * <pre>
 * {@code
 * <twig:Card>
 *     {% block footer %}...{% endblock %}   <-- ERROR
 * </twig:Card>
 * }
 * </pre>
 *
 * Valid:
 * <pre>
 * {@code
 * <twig:Card>
 *     <twig:block name="footer">...</twig:block>
 * </twig:Card>
 * }
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see <a href="https://symfony.com/bundles/ux-twig-component/current/index.html#nested-components">Symfony UX Twig Components</a>
 */
@ExtensionImpl
public class TwigComponentMixedSyntaxInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                // Detect {% block name %} inside <twig:Component>
                PsiFile containingFile = element.getContainingFile();
                ASTNode node = element.getNode();
                if (node != null && node.getElementType() == TwigTokenTypes.TAG_NAME
                    && "block".equals(element.getText())
                    && containingFile instanceof TwigFile
                    && TwigHtmlCompletionUtil.isInsideHtmlComponentTag(element, containingFile)
                ) {
                    holder.registerProblem(
                        element,
                        "Cannot use Twig block syntax inside HTML component syntax. Use <twig:block name=\"...\"> instead.",
                        ProblemHighlightType.GENERIC_ERROR
                    );
                }

                super.visitElement(element);
            }
        };
    }

    @Override
    public String getShortName() {
        return "TwigComponentMixedSyntaxInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Twig Component Mixed Syntax");
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
