package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract class AttributeValueAbstract implements AttributeValueInterface {

    private final PsiElement psiElement;

    public AttributeValueAbstract(PsiElement psiElement) {
        this.psiElement = psiElement;
    }

    @Override
    public Boolean getBoolean(String key) {
        String value = getString(key);
        if(value == null) {
            return null;
        }

        if(value.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        } else if(value.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }

        return null;
    }

    @Nullable
    @Override
    public Boolean getBoolean(String key, @Nullable Boolean defaultValue) {
        Boolean aBoolean = getBoolean(key);
        return aBoolean != null ? aBoolean : defaultValue;
    }

    @Override
    public Collection<String> getTags() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defaultValue) {
        String string = getString(key);
        return string != null ? string : defaultValue;
    }

    public PsiElement getPsiElement() {
        return this.psiElement;
    }

    @Override
    public Collection<String> getStringArray(String key) {
        return Collections.emptyList();
    }
}
