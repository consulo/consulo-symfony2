package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import consulo.language.psi.PsiFile;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineMetadataPattern;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineXmlMappingDriver implements DoctrineMappingDriverInterface {

    private static final Collection<String> RELATIONS = Arrays.asList("reference-one", "reference-many", "embed-many", "embed-one");

    @Nullable
    public DoctrineMetadataModel getMetadata(DoctrineMappingDriverArguments args) {

        PsiFile psiFile = args.getPsiFile();
        if(!(psiFile instanceof XmlFile)) {
            return null;
        }

        XmlTag rootTag = ((XmlFile) psiFile).getRootTag();
        if(rootTag == null || !rootTag.getName().matches(DoctrineMetadataPattern.DOCTRINE_MAPPING)) {
            return null;
        }

        Collection<DoctrineModelField> fields = new ArrayList<>();
        DoctrineMetadataModel model = new DoctrineMetadataModel(fields);

        for (XmlTag xmlTag : rootTag.getSubTags()) {
            String name = xmlTag.getAttributeValue("name");
            if(name == null) {
                continue;
            }

            if("entity".equals(xmlTag.getName()) && args.isEqualClass(name)) {
                // Doctrine ORM
                fields.addAll(EntityHelper.getXmlModelFields(xmlTag));
                addEmbeddedFields(args, rootTag, xmlTag, fields);

                // get table for dbal
                String table = xmlTag.getAttributeValue("table");
                if(StringUtils.isNotBlank(table)) {
                    model.setTable(table);
                }
            } else if("embeddable".equals(xmlTag.getName()) && args.isEqualClass(name)) {
                fields.addAll(EntityHelper.getXmlModelFields(xmlTag));
            } else if("document".equals(xmlTag.getName()) && args.isEqualClass(name)) {
                // Doctrine ODM
                getOdmFields(xmlTag, fields);
            }
        }

        if(model.isEmpty()) {
            return null;
        }

        return model;
    }

    private void addEmbeddedFields(
        DoctrineMappingDriverArguments args,
        XmlTag rootTag,
        XmlTag entityTag,
        Collection<DoctrineModelField> fields
    ) {
        String entityClass = entityTag.getAttributeValue("name");
        if (StringUtils.isBlank(entityClass)) {
            return;
        }

        Collection<DoctrineEmbeddedFieldUtil.Mapping> mappings = new ArrayList<>();
        for (XmlTag embeddedTag : entityTag.findSubTags("embedded")) {
            String propertyName = embeddedTag.getAttributeValue("name");
            String embeddedClass = embeddedTag.getAttributeValue("class");
            if (propertyName != null && embeddedClass != null) {
                mappings.add(new DoctrineEmbeddedFieldUtil.Mapping(propertyName, embeddedClass, getColumnPrefix(embeddedTag, propertyName)));
            }
        }

        DoctrineEmbeddedFieldUtil.addEmbeddedFields(entityClass, mappings, className -> {
            DoctrineMetadataModel metadata = getLocalEmbeddableMetadata(rootTag, className);
            return metadata != null ? metadata : DoctrineMetadataUtil.getModelFields(args.getProject(), className);
        }, fields);
    }

    @Nullable
    private DoctrineMetadataModel getLocalEmbeddableMetadata(XmlTag rootTag, String className) {
        for (XmlTag embeddableTag : rootTag.findSubTags("embeddable")) {
            String name = embeddableTag.getAttributeValue("name");
            if (name == null || !StringUtils.stripStart(name, "\\").equals(StringUtils.stripStart(className, "\\"))) {
                continue;
            }

            Collection<DoctrineModelField> fields = EntityHelper.getXmlModelFields(embeddableTag);
            return fields.isEmpty() ? null : new DoctrineMetadataModel(fields);
        }

        return null;
    }

    private String getColumnPrefix(XmlTag embeddedTag, String propertyName) {
        if ("false".equalsIgnoreCase(embeddedTag.getAttributeValue("use-column-prefix"))) {
            return "";
        }

        String columnPrefix = embeddedTag.getAttributeValue("column-prefix");
        return columnPrefix != null ? columnPrefix : propertyName + "_";
    }

    private void getOdmFields(XmlTag xmlTag, Collection<DoctrineModelField> fields) {
        for (XmlTag tag : xmlTag.getSubTags()) {
            String tagName = tag.getName();
            if("field".equals(tagName)) {
                String name = tag.getAttributeValue("name");
                if(StringUtils.isNotBlank(name)) {
                    DoctrineModelField type = new DoctrineModelField(name, tag.getAttributeValue("type"));
                    type.setColumn(tag.getAttributeValue("fieldName"));
                    fields.add(type);
                }
            } else if(RELATIONS.contains(tagName)) {
                String field = tag.getAttributeValue("field");
                if(StringUtils.isNotBlank(field)) {
                    DoctrineModelField type = new DoctrineModelField(field);
                    type.setRelationType(tagName);
                    type.setRelation(tag.getAttributeValue("target-document"));
                    fields.add(type);
                }
            }
        }
    }
}
