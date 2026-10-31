package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.VoidDataExternalizer;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {{ controller("foo::action") }}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigControllerStubIndex extends FileBasedIndexExtension<String, Void> {

    public static final ID<String, Void> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_controller_usage");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @Override
    public ID<String, Void> getName() {
        return KEY;
    }

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return inputData -> {
            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !(psiFile instanceof TwigFile)) {
                return Collections.emptyMap();
            }

            final Map<String, Void> map = new HashMap<>();
            TwigUtil.visitControllerFunctions(psiFile, pair -> {
                String name = pair.getFirst();
                if (name.length() < 255) {
                    map.put(name, null);
                }
            });
            return map;
        };

    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public DataExternalizer<Void> getValueExternalizer() {
        return VoidDataExternalizer.INSTANCE;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return (project, file) -> file.getFileType() == TwigFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
