package fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor;


/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface YamlTagVisitor {
    void visit(YamlServiceTag args);
}
