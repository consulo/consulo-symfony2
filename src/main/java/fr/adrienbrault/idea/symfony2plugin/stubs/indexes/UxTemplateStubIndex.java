package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.UxComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.UxComponentExternalizer;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class UxTemplateStubIndex extends FileBasedIndexExtension<String, UxComponent> {
    public static final ID<String, UxComponent> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.ux_template_index");
    private static final UxComponentExternalizer EXTERNALIZER = UxComponentExternalizer.INSTANCE;

    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    @Override
    public ID<String, UxComponent> getName() {
        return KEY;
    }

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, UxComponent, FileContent> getIndexer() {
        return inputData -> {
            Map<String, UxComponent> map = new HashMap<>();

            if(inputData.getPsiFile() instanceof PhpFile phpFile) {
                UxUtil.visitComponentsForIndex(phpFile, t -> map.put(t.phpClass().getFQN(), new UxComponent(t.name(), t.phpClass().getFQN(), t.template(), t.type())));
            }

            return map;
        };
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public DataExternalizer<UxComponent> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return 4;
    }

    public FileBasedIndex.InputFilter getInputFilter() {
        return PhpConstantNameIndex.PHP_INPUT_FILTER;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}
