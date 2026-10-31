package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict;

import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataModel {

    private final Collection<DoctrineModelField> modelFields;

    @Nullable
    private String table;

    public DoctrineMetadataModel(Collection<DoctrineModelField> modelFields) {
        this.modelFields = modelFields;
    }

    @Nullable
    public DoctrineModelField getField(String name) {
        for (DoctrineModelField field : this.modelFields) {
            if(name.equals(field.getName())) {
                return field;
            }
        }

        return null;
    }

    @Nullable
    public String getTable() {
        return table;
    }

    /**
     * make Immutable @TODO:
     */
    public void setTable(@Nullable String table) {
        this.table = table;
    }

    public boolean isEmpty() {
        return this.modelFields.isEmpty() && this.table == null;
    }

    public Collection<DoctrineModelField> getFields() {
        return modelFields;
    }
}
