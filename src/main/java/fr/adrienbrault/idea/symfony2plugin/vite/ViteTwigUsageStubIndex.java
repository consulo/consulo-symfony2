package fr.adrienbrault.idea.symfony2plugin.vite;

import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * File-based index for Twig templates that use Vite or Reprise entry point functions:
 * - vite_entry_link_tags('entryName')
 * - vite_entry_script_tags('entryName')
 * - reprise_entry_*('entryName')
 *
 * Key:   entry name (e.g. "app")
 * Value: empty string (file is the carrier)
 *
 * Allows the {@link ViteJavaScriptLineMarkerProvider} to navigate from a JS/TS entry
 * file to all Twig templates that reference it.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ViteTwigUsageStubIndex extends FileBasedIndexExtension<String, String> {

    @Override
    public ID<String, String> getName() {
        return ViteTwigUsageStubIndexKt.VITE_TWIG_USAGE_STUB_INDEX_KEY;
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public DataExternalizer<String> getValueExternalizer() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return (project, file) -> file.getFileType() == TwigFileType.INSTANCE;
    }

    // Index-safe only: no PhpIndex/type resolution here.
    @Override
    public DataIndexer<String, String, FileContent> getIndexer() {
        return inputData -> {
            Map<String, String> result = new HashMap<>();
            PsiFile psiFile = inputData.getPsiFile();

            if (psiFile instanceof TwigFile) {
                ElementPattern<PsiElement> pattern = RepriseTwigCallUtilKt.viteEntryPattern();
                psiFile.accept(new PsiRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        if (pattern.accepts(element)) {
                            String entryName = PsiElementUtils.trimQuote(element.getText());
                            if (!entryName.isBlank()) {
                                result.put(entryName, "");
                            }
                        }
                        super.visitElement(element);
                    }
                });
            }

            return result;
        };
    }
}
