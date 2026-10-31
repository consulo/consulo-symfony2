package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import consulo.application.ApplicationManager;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesOptions;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.usage.UsageInfo;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Handles Find Usages started from Twig, either by delegating to resolved PHP targets or by
 * searching exact Twig extension symbol names when the Twig symbol itself is the usage identity.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigFindUsagesHandler extends FindUsagesHandler {
    private final TwigFindUsagesTarget myTarget;

    public TwigFindUsagesHandler(TwigFindUsagesTarget target) {
        super(target.getPrimaryElement());
        myTarget = target;
    }

    /**
     * Exposes the effective primary search targets for the Usage View.
     */
    @Override
    public PsiElement[] getPrimaryElements() {
        return myTarget.getPrimaryElements().toArray(PsiElement.EMPTY_ARRAY);
    }

    /**
     * Delegates member/class usages to PHP targets and searches Twig extension symbols directly by name.
     */
    @Override
    public boolean processElementUsages(PsiElement element, Predicate<UsageInfo> processor, FindUsagesOptions options) {
        if (myTarget instanceof TwigPhpFindUsagesTarget phpFindUsagesTarget) {
            List<PsiElement> targets = getEffectivePhpTargets(element, phpFindUsagesTarget.getPhpTargets());

            for (PsiElement phpTarget : targets) {
                if (!super.processElementUsages(phpTarget, processor, options)) {
                    return false;
                }
            }

            return true;
        }

        if (myTarget instanceof TwigExtensionSymbolFindUsagesTarget symbolFindUsagesTarget) {
            return processTwigExtensionUsages(symbolFindUsagesTarget.getSymbol(), processor, options);
        }

        return true;
    }

    /**
     * Applies the same Twig-to-PHP delegation for plain text occurrences.
     */
    @Override
    public boolean processUsagesInText(PsiElement element, Predicate<UsageInfo> processor, GlobalSearchScope searchScope) {
        if (myTarget instanceof TwigPhpFindUsagesTarget phpFindUsagesTarget) {
            List<PsiElement> targets = getEffectivePhpTargets(element, phpFindUsagesTarget.getPhpTargets());

            for (PsiElement phpTarget : targets) {
                if (!super.processUsagesInText(phpTarget, processor, searchScope)) {
                    return false;
                }
            }

            return true;
        }

        return true;
    }

    /**
     * Supports both direct calls with the original Twig PSI and platform calls with one of the primary PHP targets.
     */
    private List<PsiElement> getEffectivePhpTargets(PsiElement element, List<PsiElement> phpTargets) {
        for (PsiElement phpTarget : phpTargets) {
            if (phpTarget.isEquivalentTo(element)) {
                return Collections.singletonList(phpTarget);
            }
        }

        return phpTargets;
    }

    /**
     * Searches Twig files directly for the exact extension symbol name under Find Usages, for example `form_start` in `{{ form_start() }}`.
     */
    private boolean processTwigExtensionUsages(
        TwigExtensionUsageSymbol symbol,
        Predicate<UsageInfo> processor,
        FindUsagesOptions options
    ) {
        return ApplicationManager.getApplication().runReadAction((Supplier<Boolean>) () -> {
            Project project = myTarget.getPrimaryElement().getProject();
            GlobalSearchScope searchScope = options.searchScope instanceof GlobalSearchScope globalSearchScope
                ? globalSearchScope
                : GlobalSearchScope.projectScope(project);
            GlobalSearchScope twigScope = GlobalSearchScope.getScopeRestrictedByFileTypes(searchScope, TwigFileType.INSTANCE);

            for (TwigFile twigFile : new TwigMethodReferencesSearchExecutor().collectTwigFiles(project, twigScope, Set.of(symbol.getName()))) {
                twigFile.accept(new PsiRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        if (matchesTwigExtensionSymbol(element, symbol)) {
                            processor.test(new UsageInfo(element));
                        }

                        super.visitElement(element);
                    }
                });
            }

            return true;
        });
    }

    /**
     * Matches only the exact Twig extension symbol leaf, such as `form_start` in `{{ form_start() }}` or `trans` in `{% apply trans %}`.
     */
    private static boolean matchesTwigExtensionSymbol(PsiElement element, TwigExtensionUsageSymbol symbol) {
        if (!element.getText().equalsIgnoreCase(symbol.getName())) {
            return false;
        }

        return switch (symbol.getKind()) {
            case FUNCTION -> TwigPattern.getPrintBlockFunctionPattern().accepts(element);
            case FILTER -> TwigPattern.getFilterPattern().accepts(element) || TwigPattern.getApplyFilterPattern().accepts(element);
        };
    }
}
