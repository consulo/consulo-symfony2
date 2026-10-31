package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiRecursiveElementVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Top-level functions of the former {@code TwigRouteUsageStubIndex.kt}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class TwigRouteUsageStubIndexKt {
    private TwigRouteUsageStubIndexKt() {
    }

    public static Collection<TwigRouteUsageStubIndex.Usage> getTwigRouteUsages(TwigFile twigFile, Collection<String> routeNames) {
        if (routeNames.isEmpty()) {
            return List.of();
        }

        Set<String> routeNameSet = new HashSet<>(routeNames);
        Set<TwigRouteUsageStubIndex.Usage> usages = new LinkedHashSet<>();
        for (TwigRouteUsageStubIndex.Usage usage : collectTwigRouteUsages(twigFile)) {
            if (routeNameSet.contains(usage.getRouteName())) {
                usages.add(usage);
            }
        }

        return usages;
    }

    public static TwigRouteUsageStubIndex.@Nullable UsageKind getTwigRouteUsageKind(PsiElement element) {
        if (element.getNode() == null ||
            element.getNode().getElementType() != TwigTokenTypes.STRING_TEXT ||
            !TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)
        ) {
            return null;
        }

        if (TwigPattern.getAutocompletableRoutePattern().accepts(element)) {
            TwigRouteUsageStubIndex.Usage usage = getTwigFunctionUsage(element);
            return usage != null ? usage.getKind() : null;
        }

        if (TwigPattern.getTwigRouteComparePattern().accepts(element) && TwigPattern.isRouteCompareContext(element)) {
            return TwigRouteUsageStubIndex.UsageKind.COMPARE;
        }

        if (TwigPattern.getTwigRouteSameAsPattern().accepts(element) && TwigPattern.isRouteCompareContext(element)) {
            return TwigRouteUsageStubIndex.UsageKind.SAME_AS;
        }

        if (TwigPattern.getTwigRouteInArrayPattern().accepts(element) && TwigPattern.isRouteCompareContext(element)) {
            return TwigRouteUsageStubIndex.UsageKind.IN_ARRAY;
        }

        return null;
    }

    static Collection<TwigRouteUsageStubIndex.Usage> collectTwigRouteUsages(TwigFile twigFile) {
        Set<TwigRouteUsageStubIndex.Usage> usages = new LinkedHashSet<>();
        twigFile.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element.getNode() != null && element.getNode().getElementType() == TwigTokenTypes.STRING_TEXT) {
                    TwigRouteUsageStubIndex.UsageKind usageKind = getTwigRouteUsageKind(element);
                    TwigRouteUsageStubIndex.Usage usage = usageKind != null
                        ? new TwigRouteUsageStubIndex.Usage(element.getText(), element, usageKind)
                        : null;
                    if (usage != null && StringUtils.isNotBlank(usage.getRouteName())) {
                        usages.add(usage);
                    }
                }

                super.visitElement(element);
            }
        });

        return usages;
    }

    private static TwigRouteUsageStubIndex.@Nullable Usage getTwigFunctionUsage(PsiElement element) {
        String functionName = getTwigPrecedingFunctionName(element);
        if ("path".equals(functionName)) {
            return new TwigRouteUsageStubIndex.Usage(element.getText(), element, TwigRouteUsageStubIndex.UsageKind.PATH);
        }
        else if ("url".equals(functionName)) {
            return new TwigRouteUsageStubIndex.Usage(element.getText(), element, TwigRouteUsageStubIndex.UsageKind.URL);
        }
        return null;
    }

    private static @Nullable String getTwigPrecedingFunctionName(PsiElement element) {
        PsiElement prev = PsiTreeUtil.prevLeaf(element);
        while (prev != null) {
            if (prev.getNode() == null) {
                prev = PsiTreeUtil.prevLeaf(prev);
                continue;
            }

            if (prev.getNode().getElementType() == TwigTokenTypes.IDENTIFIER) {
                String text = prev.getText();
                if ("path".equals(text) || "url".equals(text)) {
                    return text;
                }
            }

            prev = PsiTreeUtil.prevLeaf(prev);
        }

        return null;
    }
}
