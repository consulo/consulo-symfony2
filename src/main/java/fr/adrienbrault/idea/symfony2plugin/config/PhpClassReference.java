package fr.adrienbrault.idea.symfony2plugin.config;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpClassLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassReferenceInsertHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpClassReference extends PsiPolyVariantReferenceBase<PsiElement> implements SymfonyCompletionReference {

    private final String classFQN;
    private boolean provideVariants = false;

    private boolean useClasses = true;
    private boolean useInterfaces = false;

    public PhpClassReference(StringLiteralExpression element) {
        super(element);
        this.classFQN = PsiElementUtils.getMethodParameter(element);
    }

    public PhpClassReference(PsiElement element, String classFQN) {
        super(element);
        this.classFQN = classFQN;
    }

    public PhpClassReference(PsiElement element, String classFQN, boolean provideVariants) {
        this(element, classFQN);
        this.provideVariants = provideVariants;
    }

    public PhpClassReference(StringLiteralExpression element, boolean provideVariants) {
        this(element);
        this.provideVariants = provideVariants;
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        List<ResolveResult> results = new ArrayList<>();
        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());

        if(this.useClasses) {
            this.attachPhpClassResolveResults(phpIndex.getClassesByFQN(classFQN), results);
        }

        if(this.useInterfaces) {
            this.attachPhpClassResolveResults(phpIndex.getInterfacesByFQN(classFQN.startsWith("\\") ? classFQN : "\\" + classFQN), results);
        }

        return results.toArray(new ResolveResult[0]);
    }

    private void attachPhpClassResolveResults(Collection<PhpClass> phpClasses, List<ResolveResult> results) {
        for (PhpClass phpClass : phpClasses) {
            results.add(new PsiElementResolveResult(phpClass));
        }
    }

    @Override
    public Object[] getCompletionVariants() {

        List<LookupElement> results = new ArrayList<>();

        // not break old calls to this
        // @TODO: check performance and migrate all custom completion to this method
        if(!this.provideVariants) {
            return results.toArray();
        }


        CamelHumpMatcher camelHumpMatcher = new CamelHumpMatcher(this.classFQN);
        PhpIndex phpIndex = PhpIndex.getInstance(this.getElement().getProject());

        if(this.useClasses) {
            for (String name : phpIndex.getAllClassNames(camelHumpMatcher)) {
                attachPhpClassToVariants(results, phpIndex.getClassesByName(name));
            }
        }

        // phpindex interface dont support filter
        if(this.useInterfaces) {
            for (String name : phpIndex.getAllInterfaceNames()) {

                if(!this.classFQN.isEmpty() && !name.toLowerCase().contains(this.classFQN.toLowerCase())) {
                    continue;
                }

                attachPhpClassToVariants(results, phpIndex.getInterfacesByName(name));

            }
        }

        return results.toArray();
    }

    private void attachPhpClassToVariants(List<LookupElement> results, Collection<PhpClass> phpClasses) {
        for(PhpClass phpClass: phpClasses) {
            results.add(new PhpClassLookupElement(phpClass, true, PhpClassReferenceInsertHandler.getInstance()));
        }
    }

    public PhpClassReference setUseInterfaces(boolean useInterfaces) {
        this.useInterfaces = useInterfaces;
        return this;
    }

    public PhpClassReference setUseClasses(boolean useClasses) {
        this.useClasses = useClasses;
        return this;
    }

}
