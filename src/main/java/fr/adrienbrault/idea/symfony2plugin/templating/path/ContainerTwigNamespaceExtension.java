package fr.adrienbrault.idea.symfony2plugin.templating.path;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Collects Twig paths from the compiled container (e.g., appDevDebugProjectContainer.xml).
 * Path normalization is handled by TwigPathServiceParser.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after ConfigAddPathTwigNamespaces")
public class ContainerTwigNamespaceExtension implements TwigNamespaceExtension {

    @Override
    public Collection<TwigPath> getNamespaces(TwigNamespaceExtensionParameter parameter) {
        Project project = parameter.getProject();

        TwigPathServiceParser parser = ServiceXmlParserFactory.getInstance(project, TwigPathServiceParser.class);
        return new ArrayList<>(parser.getTwigPaths());
    }
}
