package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Shared Twig extension usage helpers for Find Usages delegation and reverse Twig search from PHP methods.
 */
public final class TwigExtensionUsageUtil {
    public static final TwigExtensionUsageUtil INSTANCE = new TwigExtensionUsageUtil();

    private TwigExtensionUsageUtil() {
    }

    /**
     * Resolves the current Twig caret leaf to one logical Twig extension symbol.
     */
    public @Nullable TwigExtensionUsageSymbol getTwigExtensionSymbol(PsiElement element) {
        TwigExtensionUsageSymbol functionSymbol = getFunctionSymbol(element);
        return functionSymbol != null ? functionSymbol : getFilterSymbol(element);
    }

    /**
     * Returns all Twig function names that currently resolve back to the given PHP method.
     */
    public Set<String> getFunctionNames(Method targetMethod) {
        return getExtensionNames(targetMethod, TwigExtensionParser.getFunctions(targetMethod.getProject()));
    }

    /**
     * Returns all Twig filter names that currently resolve back to the given PHP method.
     */
    public Set<String> getFilterNames(Method targetMethod) {
        return getExtensionNames(targetMethod, TwigExtensionParser.getFilters(targetMethod.getProject()));
    }

    /**
     * Cheap guard for method-based references search to decide whether Twig extension search branches should run.
     */
    public boolean hasExtensionNames(Method targetMethod) {
        return !getFunctionNames(targetMethod).isEmpty() || !getFilterNames(targetMethod).isEmpty();
    }

    /**
     * Resolves a Twig function usage leaf to its canonical function symbol name.
     */
    public @Nullable TwigExtensionUsageSymbol getFunctionSymbol(PsiElement element) {
        if (!TwigPattern.getPrintBlockFunctionPattern().accepts(element)) {
            return null;
        }

        String name = getRegisteredExtensionName(element.getText(), TwigExtensionParser.getFunctions(element.getProject()));
        if (name == null) {
            return null;
        }

        return new TwigExtensionUsageSymbol(TwigExtensionUsageKind.FUNCTION, name);
    }

    /**
     * Resolves a Twig filter usage leaf to its canonical filter symbol name.
     */
    public @Nullable TwigExtensionUsageSymbol getFilterSymbol(PsiElement element) {
        if (!TwigPattern.getFilterPattern().accepts(element) && !TwigPattern.getApplyFilterPattern().accepts(element)) {
            return null;
        }

        String name = getRegisteredExtensionName(element.getText(), TwigExtensionParser.getFilters(element.getProject()));
        if (name == null) {
            return null;
        }

        return new TwigExtensionUsageSymbol(TwigExtensionUsageKind.FILTER, name);
    }

    /**
     * Reverses the Twig extension registry and collects every extension name that points at the requested method.
     */
    private Set<String> getExtensionNames(Method targetMethod, Map<String, TwigExtension> extensions) {
        Set<String> names = new LinkedHashSet<>();

        for (Map.Entry<String, TwigExtension> entry : extensions.entrySet()) {
            String extensionName = entry.getKey();
            TwigExtension twigExtension = entry.getValue();

            if (!(TwigExtensionParser.getExtensionTarget(targetMethod.getProject(), twigExtension) instanceof Method target)) {
                continue;
            }

            if (isTargetMethod(target, targetMethod)) {
                names.add(extensionName);
            }
        }

        return names;
    }

    /**
     * Finds the canonical registered Twig extension name for one symbol leaf.
     */
    private @Nullable String getRegisteredExtensionName(String twigName, Map<String, TwigExtension> extensions) {
        if (twigName.isBlank()) {
            return null;
        }

        for (String key : extensions.keySet()) {
            if (key.equalsIgnoreCase(twigName)) {
                return key;
            }
        }

        return null;
    }

    /**
     * Compares PHP methods by PSI identity first and then by method name plus containing class.
     */
    private boolean isTargetMethod(Method candidate, Method targetMethod) {
        if (candidate.isEquivalentTo(targetMethod)) {
            return true;
        }

        if (!Objects.equals(candidate.getName(), targetMethod.getName())) {
            return false;
        }

        PhpClass candidateClass = candidate.getContainingClass();
        if (candidateClass == null) {
            return false;
        }

        PhpClass targetClass = targetMethod.getContainingClass();
        if (targetClass == null) {
            return false;
        }

        return candidateClass.isEquivalentTo(targetClass) || Objects.equals(candidateClass.getFQN(), targetClass.getFQN());
    }
}
