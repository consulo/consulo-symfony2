package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import consulo.xml.language.psi.XmlTag;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlTagAttributeValue extends AttributeValueAbstract {
    private final XmlTag xmlTag;

    public XmlTagAttributeValue(XmlTag xmlTag) {
        super(xmlTag);
        this.xmlTag = xmlTag;
    }

    @Override
    public Collection<String> getStringArray(String key) {
        Set<String> values = new HashSet<>();

        String string = getString(key);
        if (StringUtils.isNotBlank(string)) {
            values.add(string);
        }

        // <prototype exclude="../src/{DependencyInjection,Entity,Tests,Kernel.php}">"
        //  <exclude>../foobar</exclude>"
        // </prototype>"
        if (key.equals("exclude")) {
            for (XmlTag excludeTag : xmlTag.findSubTags(key)) {
                String text = excludeTag.getValue().getText();
                if (StringUtils.isNotBlank(text)) {
                    values.add(text);
                }
            }
        }

        return values;
    }

    @Nullable
    @Override
    public String getString(String key) {
        String value = this.xmlTag.getAttributeValue(key);
        if(StringUtils.isBlank(value)) {
            return null;
        }

        return value;
    }

    @Override
    public Collection<String> getTags() {
        return Arrays.stream(xmlTag.getSubTags())
            .filter(subTag -> "tag".equals(subTag.getName()))
            .map(subTag -> subTag.getAttributeValue("name"))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }
}