package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorStringDescriptor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see com.jetbrains.php.lang.psi.stubs.indexes.PhpTraitUsageIndex
 * @see com.jetbrains.php.lang.psi.stubs.indexes.StringSetDataExternalizer
 */
public class StringSetDataExternalizer implements DataExternalizer<Set<String>> {

    public static StringSetDataExternalizer INSTANCE = new StringSetDataExternalizer();

    public synchronized void save(DataOutput out, Set<String> value) throws IOException {
        out.writeInt(value.size());

        for (String s : value) {
            EnumeratorStringDescriptor.INSTANCE.save(out, s);
        }
    }

    public synchronized Set<String> read(DataInput in) throws IOException {
        Set<String> set = new HashSet<>();

        for(int r = in.readInt(); r > 0; --r) {
            set.add(EnumeratorStringDescriptor.INSTANCE.read(in));
        }

        return set;
    }
}