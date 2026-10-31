package fr.adrienbrault.idea.symfony2plugin.dic.tags.xml;

import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.dic.tags.ServiceTagInterface;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlServiceTag implements ServiceTagInterface {

    private final String name;

    private final String serviceId;
    private final XmlTag xmlTag;

    private XmlServiceTag(String name, String serviceId, XmlTag xmlTag) {
        this.name = name;
        this.serviceId = serviceId;
        this.xmlTag = xmlTag;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Nullable
    @Override
    public String getAttribute(String attr) {
        return this.xmlTag.getAttributeValue(attr);
    }

    @Override
    public String getServiceId() {
        return this.serviceId;
    }

    @Nullable
    public static ServiceTagInterface create(String serviceId, XmlTag xmlTag) {
        String name = xmlTag.getAttributeValue("name");
        if(StringUtils.isBlank(name)) {
            return null;
        }

        return new XmlServiceTag(name, serviceId, xmlTag);
    }
}
