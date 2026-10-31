package fr.adrienbrault.idea.symfony2plugin.routing.dict;

import consulo.util.xml.serializer.annotation.Tag;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.AbstractUiFilePath;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@Tag("routing_file")
public class RoutingFile extends AbstractUiFilePath {

    public RoutingFile() {
    }

    public RoutingFile(String path) {
        this.path = path;
    }
}
