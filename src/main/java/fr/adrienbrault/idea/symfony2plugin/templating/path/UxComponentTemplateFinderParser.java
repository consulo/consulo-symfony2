package fr.adrienbrault.idea.symfony2plugin.templating.path;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.xml.SecureXmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Parses the compiled Symfony container XML to extract the anonymous template directory
 * configured for the UX TwigComponent bundle.
 *
 * Reads the second argument of the service "ux.twig_component.component_template_finder":
 *
 *   <service id="ux.twig_component.component_template_finder" ...>
 *       <argument type="service" id="twig.loader.native_filesystem"/>
 *       <argument>foobar-ux/</argument>
 *   </service>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class UxComponentTemplateFinderParser extends AbstractServiceParser {

    private final Set<String> myTemplateDirectories = new HashSet<>();

    public Set<String> getTemplateDirectories() {
        return myTemplateDirectories;
    }

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id='ux.twig_component.component_template_finder']";
    }

    @Override
    public synchronized void parser(InputStream file, VirtualFile sourceFile, Project project) {
        Document document = parseDocument(file);
        if (document == null) {
            return;
        }

        String kernelProjectDir = TwigPathServiceParser.extractKernelProjectDir(document);
        String symfonyRootPrefix = TwigPathServiceParser.findSymfonyRootPrefix(project, sourceFile);

        NodeList services = queryServices(document);
        if (services == null) {
            return;
        }

        for (int i = 0; i < services.getLength(); i++) {
            Element service = (Element) services.item(i);
            NodeList args = service.getElementsByTagName("argument");

            // 1st argument = service ref (twig.loader.native_filesystem)
            // 2nd argument = template directory string
            if (args.getLength() < 2) {
                continue;
            }

            String path = args.item(1).getTextContent().strip();
            if (path.isBlank()) {
                continue;
            }

            String relativePath;
            if (VfsExUtil.isAbsolutePath(path)) {
                if (kernelProjectDir == null) {
                    continue;
                }

                relativePath = TwigPathServiceParser.normalizeAbsolutePath(path, kernelProjectDir);
                if (relativePath == null) {
                    continue;
                }
            }
            else {
                relativePath = path;
            }

            if (symfonyRootPrefix != null && !symfonyRootPrefix.isEmpty()) {
                relativePath = symfonyRootPrefix + "/" + relativePath;
            }

            relativePath = StringUtils.stripEnd(relativePath, "/");

            if (TwigPathServiceParser.existsInProjectRoot(project, relativePath)) {
                myTemplateDirectories.add(relativePath);
            }
        }
    }

    private @Nullable Document parseDocument(InputStream inputStream) {
        try {
            return SecureXmlUtil.createDocumentBuilder().parse(inputStream);
        }
        catch (Exception e) {
            return null;
        }
    }

    private @Nullable NodeList queryServices(Document document) {
        try {
            return (NodeList) XPathFactory.newInstance().newXPath()
                .compile(getXPathFilter())
                .evaluate(document, XPathConstants.NODESET);
        }
        catch (Exception e) {
            return null;
        }
    }
}
