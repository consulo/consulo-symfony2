package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

final class DoctrineEmbeddedFieldUtil {

    private DoctrineEmbeddedFieldUtil() {
    }

    static void addEmbeddedFields(
        String modelClass,
        Collection<Mapping> mappings,
        Function<String, DoctrineMetadataModel> metadataLoader,
        Collection<DoctrineModelField> fields
    ) {
        for (Mapping mapping : mappings) {
            if (StringUtils.isBlank(mapping.propertyName()) || StringUtils.isBlank(mapping.className())) {
                continue;
            }

            DoctrineMetadataModel embeddedMetadata = metadataLoader.apply(resolveEmbeddedClass(modelClass, mapping.className()));
            if (embeddedMetadata == null) {
                continue;
            }

            for (DoctrineModelField embeddedField : embeddedMetadata.getFields()) {
                fields.add(copyEmbeddedField(mapping.propertyName(), mapping.columnPrefix(), embeddedField));
            }
        }
    }

    private static String resolveEmbeddedClass(String modelClass, String embeddedClass) {
        String normalizedEmbeddedClass = StringUtils.stripStart(embeddedClass, "\\");
        if (normalizedEmbeddedClass.contains("\\")) {
            return normalizedEmbeddedClass;
        }

        String normalizedModelClass = StringUtils.stripStart(modelClass, "\\");
        int namespaceSeparator = normalizedModelClass.lastIndexOf('\\');
        if (namespaceSeparator < 0) {
            return normalizedEmbeddedClass;
        }

        return normalizedModelClass.substring(0, namespaceSeparator + 1) + normalizedEmbeddedClass;
    }

    private static DoctrineModelField copyEmbeddedField(
        String propertyName,
        String columnPrefix,
        DoctrineModelField embeddedField
    ) {
        DoctrineModelField field = new DoctrineModelField(propertyName + "." + embeddedField.getName(), embeddedField.getTypeName());
        field.setEnumType(embeddedField.getEnumType());
        field.setRelation(embeddedField.getRelation());
        field.setRelationType(embeddedField.getRelationType());
        field.setPropertyTypes(new ArrayList<>(embeddedField.getPropertyTypes()));

        String column = embeddedField.getColumn();
        field.setColumn(columnPrefix + (StringUtils.isNotBlank(column) ? column : embeddedField.getName()));
        embeddedField.getTargets().forEach(field::addTarget);

        return field;
    }

    record Mapping(String propertyName, String className, String columnPrefix) {
    }
}
