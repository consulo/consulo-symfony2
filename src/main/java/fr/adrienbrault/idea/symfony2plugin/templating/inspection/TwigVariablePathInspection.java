package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.php.lang.psi.elements.PhpClass;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reports unresolved Twig property or method path segments.
 *
 * Examples:
 * <ul>
 *   <li>{@code bar.unknown.public} highlights {@code unknown}</li>
 *   <li>{@code bar.getNext().apple.public} highlights {@code apple}</li>
 * </ul>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigVariablePathInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TwigVariablePathInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Field or method not found");
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
            if (TwigTypeResolveUtil.hasNextPsiTypeNameElement(element)) {
                return;
            }

            List<PsiElement> pathElements = TwigTypeResolveUtil.collectPsiTypeNameElementsWithCurrent(element);
            if (pathElements.size() < 2) {
                return;
            }

            List<String> pathNames = new ArrayList<>(pathElements.size());
            for (PsiElement pathElement : pathElements) {
                pathNames.add(pathElement.getText());
            }

            int lastIndex = pathElements.size() - 1;
            PsiElement lastPathElement = pathElements.get(lastIndex);

            // Fast path: if the tail resolves, all earlier segments are already usable.
            switch (inspectPathElement(lastPathElement, pathNames.subList(0, lastIndex), lastPathElement.getText())) {
                case FOUND:
                    return;
                case MISSING:
                    myHolder.registerProblem(lastPathElement, "Field or method not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                    return;
                case UNKNOWN:
                    break;
            }

            // Tail is ambiguous; report the first earlier segment that is known missing.
            for (int i = 1; i < lastIndex; i++) {
                PsiElement pathElement = pathElements.get(i);
                switch (inspectPathElement(pathElement, pathNames.subList(0, i), pathElement.getText())) {
                    case MISSING:
                        myHolder.registerProblem(pathElement, "Field or method not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                        return;
                    case UNKNOWN:
                        return;
                    case FOUND:
                        break;
                }
            }
        }

        /**
         * Resolves the parent path and checks one Twig-visible name.
         */
        private PathElementState inspectPathElement(PsiElement element, Collection<String> beforeLeaf, String name) {
            Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf);
            if (types.isEmpty()) {
                return PathElementState.UNKNOWN;
            }

            for (TwigTypeContainer twigTypeContainer : types) {
                Collection<PhpClass> phpClasses = TwigTypeResolveUtil.resolveTwigTypeClasses(element.getProject(), twigTypeContainer);
                if (phpClasses.isEmpty()) {
                    return PathElementState.UNKNOWN;
                }

                for (PhpClass phpClass : phpClasses) {
                    if (TwigTypeResolveUtil.isWeakCollectionLikeClass(phpClass)) {
                        return PathElementState.UNKNOWN;
                    }

                    if (!TwigTypeResolveUtil.getTwigPhpNameTargets(phpClass, name).isEmpty()) {
                        return PathElementState.FOUND;
                    }
                }
            }

            return PathElementState.MISSING;
        }

        /**
         * UNKNOWN means no reliable type information, so inspection stays silent.
         */
        private enum PathElementState {
            FOUND,
            MISSING,
            UNKNOWN
        }
    }
}
