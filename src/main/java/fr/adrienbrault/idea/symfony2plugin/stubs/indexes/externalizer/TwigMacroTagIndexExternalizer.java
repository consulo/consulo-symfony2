package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import consulo.index.io.data.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TwigMacroTagIndex;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigMacroTagIndexExternalizer implements DataExternalizer<TwigMacroTagIndex> {

    public static final TwigMacroTagIndexExternalizer INSTANCE = new TwigMacroTagIndexExternalizer();

    @Override
    public void save(DataOutput out, TwigMacroTagIndex value) throws IOException {
        out.writeUTF(value.name());
        writeNullableString(out, value.parameters());
    }

    @Override
    public TwigMacroTagIndex read(DataInput in) throws IOException {
        return new TwigMacroTagIndex(in.readUTF(), readNullableString(in));
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
