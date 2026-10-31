package fr.adrienbrault.idea.symfony2plugin.config.dic;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.completion.SymfonyCompletionReference;

import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import fr.adrienbrault.idea.symfony2plugin.config.EventDispatcherSubscriberUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventDispatcherEventReference extends PsiPolyVariantReferenceBase<PsiElement> implements SymfonyCompletionReference {

    private final String eventName;

    public EventDispatcherEventReference(PsiElement element, String eventName) {
        super(element);
        this.eventName = eventName;
    }

    public EventDispatcherEventReference(StringLiteralExpression element) {
        super(element);
        this.eventName = element.getContents();
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        List<ResolveResult> resolveResults = new ArrayList<>();

        for(PsiElement psiElement: EventDispatcherSubscriberUtil.getEventPsiElements(getElement().getProject(), this.eventName)) {
            resolveResults.add(new PsiElementResolveResult(psiElement));
        }

        return resolveResults.toArray(new ResolveResult[0]);
    }

    @Override
    public Object[] getCompletionVariants() {
        return EventDispatcherSubscriberUtil.getEventNameLookupElements(getElement().getProject()).toArray();
    }
}
