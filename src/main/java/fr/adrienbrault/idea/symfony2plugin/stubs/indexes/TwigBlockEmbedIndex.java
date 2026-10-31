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
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StringSetDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigBlockEmbedIndex extends FileBasedIndexExtension<String, Set<String>> {

    public static final ID<String, Set<String>> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_block_names_embed");

    private final KeyDescriptor<String> KEY_DESCRIPTOR = new EnumeratorStringDescriptor();
    private static final StringSetDataExternalizer DATA_EXTERNALIZER = new StringSetDataExternalizer();

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {
        return fileContent -> {
            Map<String, Set<String>> blocks = new HashMap<>();

            PsiFile psiFile = fileContent.getPsiFile();
            if (psiFile instanceof TwigFile twigFile) {
                TwigUtil.visitEmbedBlocks(twigFile, embedBlock -> {
                    String templateName = embedBlock.templateName();

                    blocks.putIfAbsent(templateName, new HashSet<>());
                    blocks.get(templateName).add(embedBlock.blockName());
                });
            }

            return blocks;
        };
    }

    @Override
    public ID<String, Set<String>> getName() {
        return KEY;
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return KEY_DESCRIPTOR;
    }

    @Override
    public DataExternalizer<Set<String>> getValueExternalizer() {
        return DATA_EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return (project, file) -> file.getFileType() == TwigFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}
