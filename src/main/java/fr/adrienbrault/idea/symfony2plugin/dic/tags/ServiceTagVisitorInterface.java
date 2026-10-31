package fr.adrienbrault.idea.symfony2plugin.dic.tags;


/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceTagVisitorInterface {
    void visit(ServiceTagInterface tag);
}
