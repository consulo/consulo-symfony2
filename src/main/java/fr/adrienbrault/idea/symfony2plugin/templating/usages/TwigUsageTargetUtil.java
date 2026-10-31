package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.jetbrains.php.lang.psi.elements.Constant;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpEnumCase;
import com.jetbrains.twig.TwigTokenTypes;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateGoToDeclarationHandler;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigConstantEnumResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared Twig usage helpers for Find Usages handlers and ReferencesSearch executors.
 */
public final class TwigUsageTargetUtil {
    public static final TwigUsageTargetUtil INSTANCE = new TwigUsageTargetUtil();

    private TwigUsageTargetUtil() {
    }

    /**
     * Resolves all PHP targets that can start a Twig Find Usages session from the current caret leaf.
     */
    public List<PsiElement> getTwigFindUsagesTargets(PsiElement element) {
        Set<PsiElement> targets = new LinkedHashSet<>();

        targets.addAll(TwigTemplateGoToDeclarationHandler.getTypeGoto(element));
        targets.addAll(getConstantTargets(element));
        targets.addAll(getEnumTargets(element));
        targets.addAll(getVarClassTargets(element));

        return new ArrayList<>(targets);
    }

    /**
     * Resolves Twig `constant('Foo\\Bar::BAZ')` arguments to class constants or enum cases.
     */
    public List<PsiElement> getConstantTargets(PsiElement element) {
        return new ArrayList<>(TwigConstantEnumResolver.getConstantTargets(element));
    }

    /**
     * Resolves Twig `enum(...)` and `enum_cases(...)` arguments to the referenced enum class.
     */
    public List<PhpClass> getEnumTargets(PsiElement element) {
        return new ArrayList<>(TwigConstantEnumResolver.getEnumTargets(element));
    }

    /**
     * Resolves Twig `@var` declarations. `variable-first` keeps precedence over `class-first`,
     * mirroring the existing annotator and completion/navigation behavior.
     */
    public List<PhpClass> getVarClassTargets(PsiElement element) {
        List<PhpClass> result = new ArrayList<>();
        for (TwigVarClassMatch match : getVarClassMatches(element)) {
            result.add(match.getTargetClass());
        }
        return result;
    }

    /**
     * Extracts class usages and their precise ranges from Twig `@var` comments.
     */
    public List<TwigVarClassMatch> getVarClassMatches(PsiElement element) {
        if (element.getNode().getElementType() != TwigTokenTypes.COMMENT_TEXT) {
            return Collections.emptyList();
        }

        String text = element.getText();
        if (!text.contains("@var")) {
            return Collections.emptyList();
        }

        List<TwigVarClassMatch> matches = new ArrayList<>();
        Set<Integer> handledVarStarts = new HashSet<>();
        Pattern[] patterns = TwigTypeResolveUtil.INLINE_DOC_REGEX;

        for (int index = 0; index < patterns.length; index++) {
            Pattern pattern = patterns[index];
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                int matchStart = matcher.start();
                if (index > 0 && handledVarStarts.contains(matchStart)) {
                    continue;
                }

                handledVarStarts.add(matchStart);
                TwigVarClassMatch match = createVarClassMatch(
                    element,
                    matcher.group("class"),
                    matcher.start("class"),
                    matcher.end("class")
                );

                if (match != null) {
                    matches.add(match);
                }
            }
        }

        return matches;
    }

    /**
     * Matches a resolved Twig `constant(...)` argument against the requested PHP constant field.
     */
    public boolean resolvesToTwigConstantField(PsiElement element, Field target) {
        for (PsiElement candidate : TwigConstantEnumResolver.getConstantTargetsForArgument(element)) {
            if (candidate instanceof Field && candidate.isEquivalentTo(target)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Matches a resolved Twig `constant(...)` argument against the requested enum case.
     */
    public boolean resolvesToTwigEnumCase(PsiElement element, PhpEnumCase target) {
        for (PsiElement candidate : TwigConstantEnumResolver.getConstantTargetsForArgument(element)) {
            if (candidate instanceof PhpEnumCase && candidate.isEquivalentTo(target)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Matches a resolved Twig `constant(...)` argument against the requested global PHP constant.
     */
    public boolean resolvesToTwigGlobalConstant(PsiElement element, Constant target) {
        for (PsiElement candidate : TwigConstantEnumResolver.getConstantTargetsForArgument(element)) {
            if (candidate instanceof Constant constant
                && (constant.isEquivalentTo(target) || Objects.equals(constant.getFQN(), target.getFQN()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Builds the cheap word-index prefilter terms for Twig constant field lookups.
     */
    public Set<String> getTwigConstantFieldSearchWords(Field target) {
        Set<String> names = new LinkedHashSet<>();

        names.add(target.getName());
        PhpClass containingClass = target.getContainingClass();
        if (containingClass != null && containingClass.getName() != null) {
            names.add(containingClass.getName());
        }

        return filterNotBlank(names);
    }

    /**
     * Builds the cheap word-index prefilter terms for Twig enum-case lookups.
     */
    public Set<String> getTwigEnumCaseSearchWords(PhpEnumCase target) {
        Set<String> names = new LinkedHashSet<>();

        names.add(target.getName());
        PhpClass containingClass = target.getContainingClass();
        if (containingClass != null && containingClass.getName() != null) {
            names.add(containingClass.getName());
        }

        return filterNotBlank(names);
    }

    /**
     * Builds the cheap word-index prefilter terms for Twig global-constant lookups.
     */
    public Set<String> getTwigGlobalConstantSearchWords(Constant target) {
        Set<String> names = new LinkedHashSet<>();

        names.add(target.getName());
        for (String part : target.getFQN().split(Pattern.quote("\\"))) {
            if (!part.isBlank() && !part.equals(target.getName())) {
                names.add(part);
            }
        }

        return filterNotBlank(names);
    }

    /**
     * Compares a resolved Twig class target against the requested PHP class.
     */
    public boolean isClassTarget(PsiElement candidate, PhpClass targetClass) {
        return candidate instanceof PhpClass phpClass && isSamePhpClass(phpClass, targetClass);
    }

    /**
     * Compares two PHP classes by PSI identity first and only then by fully qualified name.
     */
    private boolean isSamePhpClass(PhpClass first, PhpClass second) {
        if (first.isEquivalentTo(second)) {
            return true;
        }

        return Objects.equals(first.getFQN(), second.getFQN());
    }

    /**
     * Creates one resolved `@var` class match together with the precise class-name text range.
     */
    private @Nullable TwigVarClassMatch createVarClassMatch(
        PsiElement element,
        String rawClassName,
        int startOffset,
        int endOffset
    ) {
        String className = (rawClassName.endsWith("[]") ? rawClassName.substring(0, rawClassName.length() - 2) : rawClassName)
            .replace("\\\\", "\\");

        PhpClass phpClass = PhpElementsUtil.getClassInterface(element.getProject(), className);
        if (phpClass == null) {
            return null;
        }

        return new TwigVarClassMatch(phpClass, new TextRange(startOffset, endOffset));
    }

    private static Set<String> filterNotBlank(Set<String> names) {
        Set<String> result = new LinkedHashSet<>();
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                result.add(name);
            }
        }
        return result;
    }
}
