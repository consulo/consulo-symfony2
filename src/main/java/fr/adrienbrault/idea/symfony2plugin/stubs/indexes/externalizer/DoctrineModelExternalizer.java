package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import consulo.index.io.data.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelSerializable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineModelExternalizer implements DataExternalizer<DoctrineModelSerializable> {

    public static final DoctrineModelExternalizer INSTANCE = new DoctrineModelExternalizer();

    @Override
    public void save(DataOutput out, DoctrineModelSerializable value) throws IOException {
        out.writeUTF(value.getClassName());
        writeNullableString(out, value.getRepositoryClass());
        writeNullableString(out, value.getTableName());
    }

    @Override
    public DoctrineModelSerializable read(DataInput in) throws IOException {
        return new DoctrineModel(in.readUTF(), readNullableString(in), readNullableString(in));
    }

    private static void writeNullableString(DataOutput out, String value) throws IOException {
        out.writeBoolean(value != null);
        if (value != null) {
            out.writeUTF(value);
        }
    }

    private static String readNullableString(DataInput in) throws IOException {
        return in.readBoolean() ? in.readUTF() : null;
    }
}
