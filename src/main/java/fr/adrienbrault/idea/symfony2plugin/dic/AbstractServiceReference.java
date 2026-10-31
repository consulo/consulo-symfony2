package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import com.jetbrains.php.PhpIndex;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import consulo.project.DumbService;
import consulo.util.collection.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class AbstractServiceReference extends PsiPolyVariantReferenceBase<PsiElement> implements SymfonyCompletionReference {

    protected String serviceId;
    protected boolean usePrivateServices = true;

    public AbstractServiceReference(PsiElement psiElement) {
        super(psiElement);
    }

    public AbstractServiceReference(PsiElement psiElement, TextRange range) {
        super(psiElement, range);
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        if (DumbService.isDumb(getElement().getProject())) {
            return ResolveResult.EMPTY_ARRAY;
        }

        var definitions = ServiceIndexUtil.findServiceDefinitions(
            getElement().getProject(),
            serviceId
        );

        // Return the PsiElement for the service definition corresponding to the serviceId
        var results = new ArrayList<ResolveResult>();
        for (var definition : definitions) {
            results.add(new PsiElementResolveResult(definition));
        }

        ContainerCollectionResolver.ServiceCollector collector = ContainerCollectionResolver
            .ServiceCollector.create(getElement().getProject());

        // Return the PsiElement for the class corresponding to the serviceId
        String serviceClass = collector.resolve(serviceId);
        if (serviceClass != null) {
            var classes = PsiElementResolveResult.createResults(PhpIndex.getInstance(getElement().getProject()).getAnyByFQN(serviceClass));
            results.addAll(Arrays.asList(classes));
        }

        return results.toArray(ResolveResult.EMPTY_ARRAY);
    }

    @Override
    public Object[] getCompletionVariants() {
        if (DumbService.isDumb(getElement().getProject())) {
            return new Object[0];
        }

        ContainerCollectionResolver.ServiceCollector collector = ContainerCollectionResolver
            .ServiceCollector.create(getElement().getProject());

        Collection<ContainerService> values = collector.getServices().values();

        if(!usePrivateServices) {
            values = ContainerUtil.filter(values, service -> !service.isPrivate());
        }

        List<LookupElement> results = new ArrayList<>(ServiceCompletionProvider.getLookupElements(getElement(), values).getLookupElements());
        return results.toArray();
    }
}
