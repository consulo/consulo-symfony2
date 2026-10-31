package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import consulo.index.io.data.DataExternalizer;

import java.io.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectStreamDataExternalizer<T extends Serializable> implements DataExternalizer<T> {

    @Override
    public void save(DataOutput out, T value) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutput output = new ObjectOutputStream(stream);

        output.writeObject(value);

        out.writeInt(stream.size());
        out.write(stream.toByteArray());
    }

    @Override
    public T read(DataInput in) throws IOException {
        int bufferSize = in.readInt();
        byte[] buffer = new byte[bufferSize];
        in.readFully(buffer, 0, bufferSize);

        ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
        ObjectInput input  = new ObjectInputStream(stream);

        T object = null;
        try {
            @SuppressWarnings("unchecked")
            T readObject = (T) input.readObject();
            object = readObject;
        } catch (ClassNotFoundException | ClassCastException ignored) {
        }

        return object;
    }
}
