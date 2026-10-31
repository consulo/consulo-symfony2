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
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TwigMacroTagIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.TwigMacroTagIndexExternalizer;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigMacroFunctionStubIndex extends FileBasedIndexExtension<String, TwigMacroTagIndex> {

    public static final ID<String, TwigMacroTagIndex> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_macro_function");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final TwigMacroTagIndexExternalizer EXTERNALIZER = TwigMacroTagIndexExternalizer.INSTANCE;

    @Override
    public ID<String, TwigMacroTagIndex> getName() {
        return KEY;
    }

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, TwigMacroTagIndex, FileContent> getIndexer() {
        return inputData -> {
            final Map<String, TwigMacroTagIndex> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            if(!(psiFile instanceof TwigFile)) {
                return map;
            }

            TwigUtil.visitMacros(psiFile, pair -> {
                String templateName = pair.getFirst().name();
                if (templateName.length() < 255) {
                    map.put(templateName, new TwigMacroTagIndex(templateName, pair.getFirst().parameters()));
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
    public DataExternalizer<TwigMacroTagIndex> getValueExternalizer() {
        return EXTERNALIZER;
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
        return 4;
    }
}



