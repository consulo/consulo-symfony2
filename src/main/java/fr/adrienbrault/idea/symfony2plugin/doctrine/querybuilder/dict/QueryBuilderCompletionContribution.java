package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record QueryBuilderCompletionContribution(QueryBuilderCompletionContributionType type, String prefix) {
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.type)
            .append(this.prefix)
            .toHashCode();
    }
}
