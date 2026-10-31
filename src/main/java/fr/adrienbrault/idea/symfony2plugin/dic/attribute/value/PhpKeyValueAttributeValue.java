package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpKeyValueAttributeValue extends AttributeValueAbstract {
    private final Map<String, String> values;
    private final Map<String, Collection<String>> arrayValues;
    private final Collection<String> tags;

    public PhpKeyValueAttributeValue(PsiElement psiElement, Map<String, String> values) {
        this(psiElement, values, Collections.emptyMap(), Collections.emptyList());
    }

    public PhpKeyValueAttributeValue(PsiElement psiElement, Map<String, String> values, Map<String, Collection<String>> arrayValues, Collection<String> tags) {
        super(psiElement);
        this.values = values;
        this.arrayValues = arrayValues;
        this.tags = tags;
    }

    @Nullable
    @Override
    public String getString(String key) {
        return this.values.get(key);
    }

    @Override
    public Collection<String> getStringArray(String key) {
        return this.arrayValues.getOrDefault(key, Collections.emptyList());
    }

    @Override
    public Collection<String> getTags() {
        return this.tags;
    }
}
