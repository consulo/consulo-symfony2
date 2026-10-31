package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.jetbrains.php.lang.psi.elements.Constant;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpEnumCase;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.content.scope.SearchScope;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.PsiSearchScopeUtil;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TemplateMoveRenameUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormFieldDataHolder;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Adds Twig usages to Find Usages for PHP symbols that are referenced from Twig.
 *
 * Search paths are kept explicit inside one executor so the plugin only needs a single Twig
 * `referencesSearch` registration:
 * 1. Twig member paths like `foo.id`, `foo.getId()`, or `foo.primaryValue`
 * 2. Twig `constant('Foo\\Bar::BAZ')` calls for class constants and enum cases
 * 3. Twig class-name usages like `enum(...)`, `enum_cases(...)`, and `@var ... Class`
 *
 * Candidate files are prefetched via the word index and then resolved with the same Twig type pipeline.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigMethodReferencesSearchExecutor implements ReferencesSearchQueryExecutor {
    /**
     * Starts the custom Twig-member usage search only for Twig-accessible PHP members.
     */
    @Override
    public boolean execute(ReferencesSearch.SearchParameters queryParameters, Predicate<? super PsiReference> consumer) {
        PsiElement target = queryParameters.getElementToSearch();

        ApplicationManager.getApplication().runReadAction(() -> {
            if (target instanceof Method method && (TwigTypeResolveUtil.isTwigAccessibleMethod(method) || TwigExtensionUsageUtil.INSTANCE.hasExtensionNames(method))) {
                doMethodSearch(method.getProject(), method, queryParameters, consumer);
            }
            else if (target instanceof Field field && !field.isConstant()) {
                doFieldSearch(field.getProject(), field, queryParameters, consumer);
            }
            else if (target instanceof Field field && field.isConstant()) {
                doConstantFieldSearch(field.getProject(), field, queryParameters, consumer);
            }
            else if (target instanceof PhpEnumCase enumCase) {
                doEnumCaseSearch(enumCase.getProject(), enumCase, queryParameters, consumer);
            }
            else if (target instanceof Constant constant) {
                doGlobalConstantSearch(constant.getProject(), constant, queryParameters, consumer);
            }
            else if (target instanceof PhpClass phpClass) {
                doClassSearch(phpClass.getProject(), phpClass, queryParameters, consumer);
            }
        });

        return true;
    }

    /**
     * Twig exposes public non-constant fields as properties.
     */
    private boolean isTwigAccessibleField(Field field) {
        return field.getModifier().isPublic() && !field.isConstant();
    }

    /**
     * Searches Twig method usages including property shortcuts like `foo.id` for `getId()`.
     */
    private void doMethodSearch(
        Project project,
        Method targetMethod,
        ReferencesSearch.SearchParameters queryParameters,
        Predicate<? super PsiReference> consumer
    ) {
        Set<PsiElement> processedMemberUsages = new HashSet<>();
        Set<PsiElement> processedFunctionUsages = new HashSet<>();
        Set<PsiElement> processedFilterUsages = new HashSet<>();
        Set<String> memberSearchWords = TwigTypeResolveUtil.isTwigAccessibleMethod(targetMethod) ? getTwigMethodSearchWords(targetMethod) : Collections.emptySet();
        Set<String> functionNames = TwigExtensionUsageUtil.INSTANCE.getFunctionNames(targetMethod);
        Set<String> filterNames = TwigExtensionUsageUtil.INSTANCE.getFilterNames(targetMethod);
        Set<String> searchWords = new LinkedHashSet<>();
        searchWords.addAll(memberSearchWords);
        searchWords.addAll(functionNames);
        searchWords.addAll(filterNames);

        if (searchWords.isEmpty()) {
            return;
        }

        searchTwigFiles(project, queryParameters, searchWords, twigPsiFile -> {
            if (!memberSearchWords.isEmpty()) {
                findTwigMethodUsages(twigPsiFile, targetMethod, processedMemberUsages, consumer);
            }

            if (!functionNames.isEmpty()) {
                findTwigFunctionUsages(twigPsiFile, targetMethod, functionNames, processedFunctionUsages, consumer);
            }

            if (!filterNames.isEmpty()) {
                findTwigFilterUsages(twigPsiFile, targetMethod, filterNames, processedFilterUsages, consumer);
            }

            return true;
        });
    }

    /**
     * Searches public PHP properties and attributed form fields in Twig.
     */
    private void doFieldSearch(
        Project project,
        Field targetField,
        ReferencesSearch.SearchParameters queryParameters,
        Predicate<? super PsiReference> consumer
    ) {
        Set<PsiElement> processed = new HashSet<>();
        Set<String> searchWords = getTwigFieldNames(targetField);
        if (searchWords.isEmpty()) {
            return;
        }

        // Form fields can be private in PHP while still being used in Twig.
        SearchScope searchScope = queryParameters.getScopeDeterminedByUser();
        GlobalSearchScope globalScope = searchScope instanceof GlobalSearchScope scope ? scope : GlobalSearchScope.projectScope(project);
        GlobalSearchScope twigScope = GlobalSearchScope.getScopeRestrictedByFileTypes(globalScope, TwigFileType.INSTANCE);
        Predicate<PsiReference> scopedConsumer = reference ->
            !PsiSearchScopeUtil.isInScope(searchScope, reference.getElement()) || consumer.test(reference);

        for (TwigFile twigPsiFile : collectTwigFiles(project, twigScope, searchWords)) {
            findTwigFieldUsages(twigPsiFile, targetField, searchWords, processed, scopedConsumer);
        }
    }

    /**
     * Searches Twig `constant(...)` calls for PHP class constants.
     */
    private void doConstantFieldSearch(
        Project project,
        Field target,
        ReferencesSearch.SearchParameters queryParameters,
        Predicate<? super PsiReference> consumer
    ) {
        Set<PsiElement> processed = new HashSet<>();
        Set<String> searchWords = TwigUsageTargetUtil.INSTANCE.getTwigConstantFieldSearchWords(target);
        if (searchWords.isEmpty()) {
            return;
        }

        searchTwigFiles(project, queryParameters, searchWords, twigPsiFile -> {
            twigPsiFile.accept(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if (TwigPattern.getPrintBlockOrTagFunctionPattern("constant").accepts(element) &&
                        processed.add(element) &&
                        TwigUsageTargetUtil.INSTANCE.resolvesToTwigConstantField(element, target)
                    ) {
                        consumer.test(new TwigMethodUsageReference(element, target, new TextRange(0, element.getTextLength())));
                    }

                    super.visitElement(element);
                }
            });

            return true;
        });
    }

    /**
     * Searches Twig `constant(...)` calls for PHP enum cases.
     */
    private void doEnumCaseSearch(
        Project project,
        PhpEnumCase target,
        ReferencesSearch.SearchParameters queryParameters,
        Predicate<? super PsiReference> consumer
    ) {
        Set<PsiElement> processed = new HashSet<>();
        Set<String> searchWords = TwigUsageTargetUtil.INSTANCE.getTwigEnumCaseSearchWords(target);
        if (searchWords.isEmpty()) {
            return;
        }

        searchTwigFiles(project, queryParameters, searchWords, twigPsiFile -> {
            twigPsiFile.accept(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if (TwigPattern.getPrintBlockOrTagFunctionPattern("constant").accepts(element) &&
                        processed.add(element) &&
                        TwigUsageTargetUtil.INSTANCE.resolvesToTwigEnumCase(element, target)
                    ) {
                        consumer.test(new TwigMethodUsageReference(element, target, new TextRange(0, element.getTextLength())));
                    }

                    super.visitElement(element);
                }
            });

            return true;
        });
    }

    /**
     * Searches Twig `constant(...)` calls for PHP global constants.
     */
    private void doGlobalConstantSearch(
        Project project,
        Constant target,
        ReferencesSearch.SearchParameters queryParameters,
        Predicate<? super PsiReference> consumer
    ) {
        Set<PsiElement> processed = new HashSet<>();
        Set<String> searchWords = TwigUsageTargetUtil.INSTANCE.getTwigGlobalConstantSearchWords(target);
        if (searchWords.isEmpty()) {
            return;
        }

        searchTwigFiles(project, queryParameters, searchWords, twigPsiFile -> {
            twigPsiFile.accept(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if (TwigPattern.getPrintBlockOrTagFunctionPattern("constant").accepts(element) &&
                        processed.add(element) &&
                        TwigUsageTargetUtil.INSTANCE.resolvesToTwigGlobalConstant(element, target)
                    ) {
                        consumer.test(new TwigMethodUsageReference(element, target, new TextRange(0, element.getTextLength())));
                    }

                    super.visitElement(element);
                }
            });

            return true;
        });
    }

    /**
     * Searches Twig class-name usages that spell the PHP class directly.
     */
    private void doClassSearch(
        Project project,
        PhpClass targetClass,
        ReferencesSearch.SearchParameters queryParameters,
        Predicate<? super PsiReference> consumer
    ) {
        String shortName = targetClass.getName();
        searchTwigFiles(project, queryParameters, Collections.singleton(shortName), twigPsiFile -> {
            twigPsiFile.accept(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    // Match direct class-name usages in Twig enum helpers like `enum(...)` and `enum_cases(...)`.
                    if (TwigPattern.getPrintBlockOrTagFunctionPattern("enum", "enum_cases").accepts(element)) {
                        TextRange fullRange = new TextRange(0, element.getTextLength());
                        boolean matches = false;
                        for (PhpClass enumTarget : TwigUsageTargetUtil.INSTANCE.getEnumTargets(element)) {
                            if (TwigUsageTargetUtil.INSTANCE.isClassTarget(enumTarget, targetClass)) {
                                matches = true;
                                break;
                            }
                        }

                        if (matches) {
                            consumer.test(new TwigMethodUsageReference(element, targetClass, fullRange));
                        }
                    }

                    for (TwigVarClassMatch match : TwigUsageTargetUtil.INSTANCE.getVarClassMatches(element)) {
                        if (!TwigUsageTargetUtil.INSTANCE.isClassTarget(match.getTargetClass(), targetClass)) {
                            continue;
                        }

                        consumer.test(new TwigMethodUsageReference(element, targetClass, match.getRangeInElement()));
                    }

                    super.visitElement(element);
                }
            });

            return true;
        });
    }

    /**
     * Shared word-index prefilter for Twig files. Each search path keeps its own visitor logic,
     * while scope restriction and file resolution stay centralized here.
     */
    private void searchTwigFiles(
        Project project,
        ReferencesSearch.SearchParameters queryParameters,
        Set<String> searchWords,
        java.util.function.Function<TwigFile, Boolean> visitor
    ) {
        GlobalSearchScope twigScope = getTwigSearchScope(project, queryParameters);
        for (TwigFile twigFile : collectTwigFiles(project, twigScope, searchWords)) {
            if (!visitor.apply(twigFile)) {
                return;
            }
        }
    }

    /**
     * Collects candidate Twig files for a set of indexed words while visiting each file only once.
     * This keeps getter shortcuts like `getFoo` + `foo` from rescanning the same template.
     */
    public List<TwigFile> collectTwigFiles(
        Project project,
        GlobalSearchScope twigScope,
        Set<String> searchWords
    ) {
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiSearchHelper searchHelper = PsiSearchHelper.getInstance(project);
        Set<VirtualFile> visitedFiles = new LinkedHashSet<>();
        List<TwigFile> twigFiles = new ArrayList<>();

        for (String searchWord : searchWords) {
            searchHelper.processAllFilesWithWord(searchWord, twigScope, (PsiFile psiFile) -> {
                VirtualFile virtualFile = psiFile.getVirtualFile();
                if (virtualFile == null) {
                    return true;
                }

                TwigFile twigPsiFile;
                if (psiFile instanceof TwigFile twigFile) {
                    twigPsiFile = twigFile;
                }
                else {
                    twigPsiFile = psiManager.findFile(virtualFile) instanceof TwigFile foundTwigFile ? foundTwigFile : null;
                }

                if (twigPsiFile == null) {
                    return true;
                }

                if (!visitedFiles.add(twigPsiFile.getVirtualFile())) {
                    return true;
                }

                twigFiles.add(twigPsiFile);
                return true;
            }, false);
        }

        return twigFiles;
    }

    private GlobalSearchScope getTwigSearchScope(
        Project project,
        ReferencesSearch.SearchParameters queryParameters
    ) {
        SearchScope searchScope = queryParameters.getEffectiveSearchScope();
        GlobalSearchScope scope = searchScope instanceof GlobalSearchScope globalSearchScope ? globalSearchScope : GlobalSearchScope.projectScope(project);
        return GlobalSearchScope.getScopeRestrictedByFileTypes(scope, TwigFileType.INSTANCE);
    }

    /**
     * Walks a candidate Twig file and emits synthetic references for matching method usages.
     */
    private void findTwigMethodUsages(
        TwigFile twigFile,
        Method targetMethod,
        Set<PsiElement> processed,
        Predicate<? super PsiReference> consumer
    ) {
        Set<String> methodNames = getTwigMethodNames(targetMethod);

        twigFile.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (!methodNames.isEmpty() &&
                    isTwigMethodCandidate(element, methodNames) &&
                    processed.add(element) &&
                    resolvesToTargetMethod(element, targetMethod)
                ) {
                    consumer.test(new TwigMethodUsageReference(element, targetMethod, new TextRange(0, element.getTextLength())));
                }

                super.visitElement(element);
            }
        });
    }

    /**
     * Walks a candidate Twig file and emits synthetic references for Twig function usages backed by a PHP method.
     */
    private void findTwigFunctionUsages(
        TwigFile twigFile,
        Method targetMethod,
        Set<String> functionNames,
        Set<PsiElement> processed,
        Predicate<? super PsiReference> consumer
    ) {
        twigFile.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (!functionNames.isEmpty() &&
                    TwigPattern.getPrintBlockFunctionPattern().accepts(element) &&
                    containsIgnoreCase(functionNames, element.getText()) &&
                    processed.add(element)
                ) {
                    consumer.test(new TwigMethodUsageReference(element, targetMethod, new TextRange(0, element.getTextLength())));
                }

                super.visitElement(element);
            }
        });
    }

    /**
     * Walks a candidate Twig file and emits synthetic references for Twig filter usages backed by a PHP method.
     */
    private void findTwigFilterUsages(
        TwigFile twigFile,
        Method targetMethod,
        Set<String> filterNames,
        Set<PsiElement> processed,
        Predicate<? super PsiReference> consumer
    ) {
        twigFile.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (!filterNames.isEmpty() &&
                    (TwigPattern.getFilterPattern().accepts(element) || TwigPattern.getApplyFilterPattern().accepts(element)) &&
                    containsIgnoreCase(filterNames, element.getText()) &&
                    processed.add(element)
                ) {
                    consumer.test(new TwigMethodUsageReference(element, targetMethod, new TextRange(0, element.getTextLength())));
                }

                super.visitElement(element);
            }
        });
    }

    /**
     * Walks a candidate Twig file and emits synthetic references for matching field usages.
     */
    private void findTwigFieldUsages(
        TwigFile twigFile,
        Field targetField,
        Set<String> fieldNames,
        Set<PsiElement> processed,
        Predicate<? super PsiReference> consumer
    ) {
        twigFile.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (!fieldNames.isEmpty() &&
                    isTwigMethodCandidate(element, fieldNames) &&
                    processed.add(element)
                ) {
                    TwigMethodUsageReference reference = getFieldUsageReference(element, targetField);
                    if (reference != null) {
                        consumer.test(reference);
                    }
                }

                super.visitElement(element);
            }
        });
    }

    /**
     * Filters down to Twig member leaves that textually match one of the supported method or field names.
     *
     * Examples:
     * `{{ bar.foo }}` -> leaf `foo`
     * `{{ bar.getFoo() }}` -> leaf `getFoo`
     */
    private boolean isTwigMethodCandidate(PsiElement element, Set<String> twigNames) {
        if (!TwigPattern.getTypeCompletionPattern().accepts(element)) {
            return false;
        }

        String text = element.getText();
        return containsIgnoreCase(twigNames, text);
    }

    /**
     * Reuses Twig type resolution so only real, typed method accesses count as usages.
     */
    private boolean resolvesToTargetMethod(PsiElement element, Method targetMethod) {
        Collection<String> beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(element);
        if (beforeLeaf.isEmpty()) {
            return false;
        }

        Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf);
        if (types.isEmpty()) {
            return false;
        }

        for (TwigTypeContainer twigTypeContainer : types) {
            for (PhpClass phpClass : TwigTypeResolveUtil.resolveTwigTypeClasses(element.getProject(), twigTypeContainer)) {
                if (TwigTypeResolveUtil.isWeakCollectionLikeClass(phpClass)) {
                    continue;
                }

                for (PhpNamedElement target : TwigTypeResolveUtil.getTwigPhpNameTargets(phpClass, element.getText())) {
                    if (isTargetMethod(target, targetMethod)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Nullable
    private TwigMethodUsageReference getFieldUsageReference(PsiElement element, Field targetField) {
        Collection<String> beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(element);
        if (beforeLeaf.isEmpty()) {
            return null;
        }

        Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf);
        if (types.isEmpty()) {
            return null;
        }

        TwigMethodUsageReference reference = getFormFieldUsageReference(element, targetField, types);
        return reference != null ? reference : getPropertyUsageReference(element, targetField, types);
    }

    /**
     * Form fields resolve through their PHP property declaration, including private fields and aliases.
     */
    @Nullable
    private TwigMethodUsageReference getFormFieldUsageReference(PsiElement element, Field targetField, Collection<TwigTypeContainer> types) {
        for (TwigTypeContainer twigTypeContainer : types) {
            FormFieldDataHolder formField = twigTypeContainer.getFormFieldDataHolder();
            if (formField == null
                || !Objects.equals(twigTypeContainer.getStringElement(), element.getText())
                || !Objects.equals(formField.propertyName(), targetField.getName())) {
                continue;
            }

            PhpClass owner = PhpElementsUtil.getClassInterface(element.getProject(), formField.ownerFormTypeFqn());
            if (owner == null) {
                continue;
            }

            Field property = FormFieldResolver.resolveAttributeField(owner, targetField.getName());
            if (property == null) {
                continue;
            }

            if (targetField.getManager().areElementsEquivalent(property, targetField)) {
                return new TwigMethodUsageReference(element, targetField, new TextRange(0, element.getTextLength()), false);
            }
        }

        return null;
    }

    /**
     * Ordinary Twig properties resolve only to public PHP fields.
     */
    @Nullable
    private TwigMethodUsageReference getPropertyUsageReference(PsiElement element, Field targetField, Collection<TwigTypeContainer> types) {
        if (!isTwigAccessibleField(targetField)) {
            return null;
        }

        for (TwigTypeContainer twigTypeContainer : types) {
            for (PhpClass phpClass : TwigTypeResolveUtil.resolveTwigTypeClasses(element.getProject(), twigTypeContainer)) {
                if (TwigTypeResolveUtil.isWeakCollectionLikeClass(phpClass)) {
                    continue;
                }

                for (PhpNamedElement target : TwigTypeResolveUtil.getTwigPhpNameTargets(phpClass, element.getText())) {
                    if (isTargetField(target, targetField)) {
                        return new TwigMethodUsageReference(element, targetField, new TextRange(0, element.getTextLength()));
                    }
                }
            }
        }

        return null;
    }

    /**
     * Compares resolved Twig targets against the original PHP method declaration.
     */
    private boolean isTargetMethod(PhpNamedElement candidate, Method targetMethod) {
        if (candidate.isEquivalentTo(targetMethod)) {
            return true;
        }

        return candidate instanceof Method && Objects.equals(candidate.getFQN(), targetMethod.getFQN());
    }

    /**
     * Compares resolved Twig targets against the original PHP field declaration.
     */
    private boolean isTargetField(PhpNamedElement candidate, Field targetField) {
        if (candidate.isEquivalentTo(targetField)) {
            return true;
        }

        if (!(candidate instanceof Field candidateField) || !Objects.equals(candidateField.getName(), targetField.getName())) {
            return false;
        }

        PhpClass candidateClass = candidateField.getContainingClass();
        if (candidateClass == null) {
            return false;
        }

        PhpClass targetClass = targetField.getContainingClass();
        if (targetClass == null) {
            return false;
        }

        return candidateClass.isEquivalentTo(targetClass) || Objects.equals(candidateClass.getFQN(), targetClass.getFQN());
    }

    /**
     * Collects all Twig-visible names for a PHP method, including getter shortcuts.
     */
    private Set<String> getTwigMethodNames(Method method) {
        Set<String> names = new LinkedHashSet<>();

        names.add(method.getName());
        names.add(TwigTypeResolveUtil.getPropertyShortcutMethodName(method));

        return filterNotBlank(names);
    }

    /**
     * Collects PHP property names and visible names of attributed form fields.
     */
    private Set<String> getTwigFieldNames(Field field) {
        Set<String> names = new LinkedHashSet<>();

        if (isTwigAccessibleField(field)) {
            names.add(field.getName());
        }
        names.addAll(FormFieldResolver.getAttributeFieldNames(field));

        return names;
    }

    /**
     * Prefetch words for Twig method search paths.
     */
    private Set<String> getTwigMethodSearchWords(Method method) {
        Set<String> names = new LinkedHashSet<>();

        names.addAll(getTwigMethodNames(method));

        return filterNotBlank(names);
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

    private static boolean containsIgnoreCase(Set<String> names, String text) {
        for (String name : names) {
            if (name.equalsIgnoreCase(text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Synthetic reference that points a Twig usage back to the originating PHP member declaration.
     */
    public static class TwigMethodUsageReference extends PsiReferenceBase<PsiElement> {
        private final PsiElement myTargetElement;
        private final boolean myRenameUsage;

        public TwigMethodUsageReference(PsiElement sourceElement, PsiElement targetElement, TextRange rangeInElement) {
            this(sourceElement, targetElement, rangeInElement, true);
        }

        public TwigMethodUsageReference(PsiElement sourceElement, PsiElement targetElement, TextRange rangeInElement, boolean renameUsage) {
            super(sourceElement, rangeInElement, false);
            myTargetElement = targetElement;
            myRenameUsage = renameUsage;
        }

        /**
         * Resolves the synthetic usage directly to the PHP member under Find Usages.
         */
        @Override
        public PsiElement resolve() {
            return myTargetElement;
        }

        /**
         * These synthetic references are not used for completion.
         */
        @Override
        public Object[] getVariants() {
            return new Object[0];
        }

        /**
         * Rewrites the matched Twig token, e.g. `test.RestVar` -> `test.RenamedVar`.
         */
        @Override
        public PsiElement handleElementRename(String newElementName) {
            if (!myRenameUsage) {
                return myElement;
            }

            String renamedUsageText = getRenamedUsageText(newElementName);
            if (renamedUsageText.equals(getValue())) {
                return myElement;
            }

            PsiElement result = TemplateMoveRenameUtil.applyRangeReplacement(
                myElement,
                getRangeInElement(),
                renamedUsageText
            );

            return result != null ? result : myElement;
        }

        /**
         * Maps PHP rename names to Twig text, e.g. `$foo` -> `foo` or `\App\Old` -> `\App\New`.
         */
        private String getRenamedUsageText(String newElementName) {
            // Drop PHP field sigils, e.g. `$foo` -> `foo`.
            String normalizedName = newElementName.startsWith("$") ? newElementName.substring(1) : newElementName;
            String currentText = getValue();

            if (myTargetElement instanceof Field field && field.isConstant()) {
                return replaceScopedMemberName(currentText, normalizedName);
            }

            if (myTargetElement instanceof Method method) {
                return getRenamedMethodUsageText(currentText, method, normalizedName);
            }
            else if (myTargetElement instanceof PhpEnumCase) {
                return replaceScopedMemberName(currentText, normalizedName);
            }
            else if (myTargetElement instanceof Constant) {
                return replaceQualifiedNameTail(currentText, normalizedName);
            }
            else if (myTargetElement instanceof PhpClass) {
                return replaceQualifiedNameTail(currentText, normalizedName);
            }

            return normalizedName;
        }

        /**
         * Preserves Twig getter shortcuts, e.g. `foo` + `getBar` -> `bar`.
         */
        private String getRenamedMethodUsageText(String currentText, Method targetMethod, String newElementName) {
            if (currentText.equalsIgnoreCase(targetMethod.getName())) {
                return newElementName;
            }

            String propertyShortcut = TwigTypeResolveUtil.getPropertyShortcutMethodName(targetMethod);
            if (currentText.equalsIgnoreCase(propertyShortcut)) {
                return TwigTypeResolveUtil.getPropertyShortcutMethodName(newElementName);
            }

            return currentText;
        }

        /**
         * Replaces scoped member names, e.g. `Foo\\Bar::OLD` -> `Foo\\Bar::NEW`.
         */
        private String replaceScopedMemberName(String currentText, String newElementName) {
            int scopeSeparator = currentText.lastIndexOf("::");
            if (scopeSeparator >= 0) {
                return currentText.substring(0, scopeSeparator + 2) + newElementName;
            }

            return replaceQualifiedNameTail(currentText, newElementName);
        }

        /**
         * Replaces the final FQN segment, e.g. `\App\Old[]` -> `\App\New[]`.
         */
        private String replaceQualifiedNameTail(String currentText, String newElementName) {
            String arraySuffix = currentText.endsWith("[]") ? "[]" : "";
            String qualifiedName = currentText.endsWith(arraySuffix) ? currentText.substring(0, currentText.length() - arraySuffix.length()) : currentText;
            int namespaceSeparator = qualifiedName.lastIndexOf('\\');

            String renamed;
            if (namespaceSeparator >= 0) {
                renamed = qualifiedName.substring(0, namespaceSeparator + 1) + newElementName;
            }
            else {
                renamed = newElementName;
            }

            return renamed + arraySuffix;
        }
    }
}
