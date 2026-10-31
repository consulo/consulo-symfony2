package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormFieldDataHolder;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormViewDataHolder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTypeContainer {

    private final Set<String> types = new HashSet<>();
    private final @Nullable String stringElement;
    private final @Nullable FormFieldDataHolder formFieldDataHolder;
    private final @Nullable FormViewDataHolder formViewDataHolder;

    public TwigTypeContainer(Collection<String> types) {
        this(types, null);
    }

    private TwigTypeContainer(Collection<String> types, @Nullable FormViewDataHolder formViewDataHolder) {
        this(types, null, null, formViewDataHolder);
    }

    public TwigTypeContainer(String stringElement) {
        this(stringElement, null);
    }

    public TwigTypeContainer(String stringElement, @Nullable FormFieldDataHolder formFieldDataHolder) {
        this(Collections.emptySet(), stringElement, formFieldDataHolder, null);
    }

    private TwigTypeContainer(Collection<String> types, @Nullable String stringElement, @Nullable FormFieldDataHolder formFieldDataHolder, @Nullable FormViewDataHolder formViewDataHolder) {
        this.types.addAll(types);
        this.stringElement = stringElement;
        this.formFieldDataHolder = formFieldDataHolder;
        this.formViewDataHolder = formViewDataHolder;
    }

    public static TwigTypeContainer forFormView(FormFieldDataHolder field) {
        return new TwigTypeContainer(Collections.singleton("\\Symfony\\Component\\Form\\FormView"), null, field, null);
    }

    public static TwigTypeContainer forFormChildren(FormFieldDataHolder field) {
        return new TwigTypeContainer(Collections.emptySet(), null, field, null);
    }

    public Set<String> getTypes() {
        return Collections.unmodifiableSet(types);
    }

    @Nullable
    public String getStringElement() {
        return stringElement;
    }

    public static Collection<TwigTypeContainer> fromCollection(Collection<PsiVariable> psiVariables) {

        List<TwigTypeContainer> twigTypeContainerList = new ArrayList<>();

        for(PsiVariable psiVariable : psiVariables) {
            if (psiVariable.getTypes().isEmpty()) {
                continue;
            }

            FormViewDataHolder formViewDataHolder = psiVariable.getFormTypeFqns().isEmpty()
                ? null
                : new FormViewDataHolder(psiVariable.getFormTypeFqns());

            Set<String> types = new HashSet<>(psiVariable.getTypes());
            if (!psiVariable.getFormFields().isEmpty() && formViewDataHolder == null) {
                types.remove("\\Symfony\\Component\\Form\\FormView");
            }
            if (!types.isEmpty()) {
                twigTypeContainerList.add(new TwigTypeContainer(types, formViewDataHolder));
            }
            for (FormFieldDataHolder field : psiVariable.getFormFields()) {
                twigTypeContainerList.add(forFormView(field));
            }
        }

        return twigTypeContainerList;
    }

    @Nullable
    public FormFieldDataHolder getFormFieldDataHolder() {
        return formFieldDataHolder;
    }

    @Nullable
    public FormViewDataHolder getFormViewDataHolder() {
        return formViewDataHolder;
    }

}
