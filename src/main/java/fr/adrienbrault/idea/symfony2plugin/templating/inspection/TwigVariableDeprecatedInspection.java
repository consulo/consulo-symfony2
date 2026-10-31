package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.twig.TwigLanguage;
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
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigVariableDeprecatedInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TwigVariableDeprecatedInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Deprecated Twig variable");
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

        private @Nullable ElementPattern<PsiElement> myTypeCompletionPattern;

        private MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        private ElementPattern<PsiElement> getTypeCompletionPattern() {
            if (myTypeCompletionPattern == null) {
                myTypeCompletionPattern = TwigPattern.getTypeCompletionPattern();
            }
            return myTypeCompletionPattern;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (getTypeCompletionPattern().accepts(element)) {
                visit(element);
            }

            super.visitElement(element);
        }

        private void visit(PsiElement element) {
            Collection<String> beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(element);
            if (beforeLeaf.isEmpty()) {
                return;
            }

            Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf);
            if (types.isEmpty()) {
                return;
            }

            String text = element.getText();
            Set<String> visitedTargets = new HashSet<>();

            for (TwigTypeContainer twigTypeContainer : types) {
                for (PhpClass phpClass : TwigTypeResolveUtil.resolveTwigTypeClasses(element.getProject(), twigTypeContainer)) {
                    for (PhpNamedElement namedElement : TwigTypeResolveUtil.getTwigPhpNameTargets(phpClass, text)) {
                        String targetKey = getDeprecatedTargetKind(namedElement) + ":" + phpClass.getFQN() + "::" + namedElement.getName();
                        if (visitedTargets.add(targetKey) && namedElement.isDeprecated()) {
                            myHolder.registerProblem(element, getDeprecatedMessage(phpClass, namedElement), ProblemHighlightType.LIKE_DEPRECATED);
                        }
                    }
                }
            }
        }

        private String getDeprecatedTargetKind(PhpNamedElement namedElement) {
            if (namedElement instanceof Method) {
                return "method";
            }
            else if (namedElement instanceof Field) {
                return "field";
            }
            return "element";
        }

        private String getDeprecatedMessage(PhpClass phpClass, PhpNamedElement namedElement) {
            if (namedElement instanceof Method) {
                return "Method '" + phpClass.getName() + "::" + namedElement.getName() + "' is deprecated";
            }
            else if (namedElement instanceof Field) {
                return "Field '" + phpClass.getName() + "::$" + namedElement.getName() + "' is deprecated";
            }
            return "Element '" + phpClass.getName() + "::" + namedElement.getName() + "' is deprecated";
        }
    }
}
