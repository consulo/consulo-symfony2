package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface AttributeValueInterface {

    @Nullable
    String getString(String key);

    Collection<String> getStringArray(String key);

    @Nullable
    Boolean getBoolean(String key);

    @Nullable
    String getString(String key, @Nullable String defaultValue);

    @Nullable
    Boolean getBoolean(String key, @Nullable Boolean defaultValue);

    Collection<String> getTags();

    PsiElement getPsiElement();
}
