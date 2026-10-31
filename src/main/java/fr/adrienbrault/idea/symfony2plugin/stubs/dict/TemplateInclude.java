package fr.adrienbrault.idea.symfony2plugin.stubs.dict;

import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude.TYPE;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable usage types for one normalized template name in one source file.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateInclude implements Serializable {

    private final String template;
    private final Set<TYPE> types;

    public TemplateInclude(String template, Set<TYPE> types) {
        if (types.isEmpty()) {
            throw new IllegalArgumentException("Template include types must not be empty");
        }
        this.template = template;
        this.types = Collections.unmodifiableSet(EnumSet.copyOf(types));
    }

    public Set<TYPE> getTypes() {
        return types;
    }

    public String getTemplate() {
        return template;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.types)
            .append(this.template)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TemplateInclude
            && Objects.equals(((TemplateInclude) obj).template, this.template)
            && Objects.equals(((TemplateInclude) obj).types, this.types);
    }
}
