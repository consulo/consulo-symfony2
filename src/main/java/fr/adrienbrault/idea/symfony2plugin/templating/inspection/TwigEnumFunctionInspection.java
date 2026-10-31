package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.LeafPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jspecify.annotations.Nullable;

/**
 * Inspection for Twig enum() and enum_cases() functions to validate that:
 * - The provided class name exists
 * - The class is actually an enum type
 *
 * Examples:
 * {{ enum('App\\SomeEnum') }} - valid if SomeEnum is an enum
 * {{ enum('App\\NotAnEnum') }} - warning if NotAnEnum exists but is not an enum
 * {{ enum('App\\MissingClass') }} - error if class doesn't exist
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigEnumFunctionInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TwigEnumFunctionInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Twig Enum Function Validation");
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

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;

        private @Nullable ElementPattern<PsiElement> myEnumFunctionPattern;

        private MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        private ElementPattern<PsiElement> getEnumFunctionPattern() {
            if (myEnumFunctionPattern == null) {
                myEnumFunctionPattern = TwigPattern.getPrintBlockOrTagFunctionPattern("enum", "enum_cases");
            }
            return myEnumFunctionPattern;
        }

        @Override
        public void visitElement(PsiElement element) {
            // Fast pre-filter: only STRING_TEXT elements can be enum/enum_cases arguments
            // enum('App\Config\SomeOption')
            // enum_cases('App\Config\SomeOption')
            if (element instanceof LeafPsiElement &&
                element.getNode().getElementType() == TwigTokenTypes.STRING_TEXT &&
                getEnumFunctionPattern().accepts(element)
            ) {
                visitEnumFunction(element);
            }

            super.visitElement(element);
        }

        private void visitEnumFunction(PsiElement element) {
            String contents = element.getText();
            if (contents.isBlank()) {
                return;
            }

            // Unescape backslashes: 'App\\Bike\\FooEnum' => 'App\Bike\FooEnum'
            String className = contents.replace("\\\\", "\\");

            PhpClass phpClass = PhpElementsUtil.getClassInterface(element.getProject(), className);

            if (phpClass == null) {
                // Class doesn't exist
                myHolder.registerProblem(
                    element,
                    "Missing class: " + className,
                    ProblemHighlightType.WARNING
                );
            }
            else if (!phpClass.isEnum()) {
                // Class exists but is not an enum
                myHolder.registerProblem(
                    element,
                    "Class '" + phpClass.getName() + "' is not an enum",
                    ProblemHighlightType.WARNING
                );
            }
        }
    }
}
