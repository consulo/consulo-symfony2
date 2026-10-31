package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.dict;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record QueryBuilderClassJoin(String className, String alias) {
    public QueryBuilderClassJoin(String className, String alias) {
        this.className = className;
        this.alias = alias;
    }
}
