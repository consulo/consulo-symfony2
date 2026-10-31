package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DummyAttributeValue implements AttributeValueInterface {

    private final PsiElement psiElement;

    public DummyAttributeValue(PsiElement psiElement) {
        this.psiElement = psiElement;
    }

    @Nullable
    @Override
    public String getString(String key) {
        return null;
    }

    @Override
    public Collection<String> getStringArray(String key) {
        return Collections.emptySet();
    }

    @Nullable
    @Override
    public Boolean getBoolean(String key) {
        return null;
    }

    @Nullable
    @Override
    public String getString(String key, String defaultValue) {
        return defaultValue;
    }

    @Nullable
    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public Collection<String> getTags() {
        return Collections.emptySet();
    }

    @Override
    public PsiElement getPsiElement() {
        return this.psiElement;
    }
}
