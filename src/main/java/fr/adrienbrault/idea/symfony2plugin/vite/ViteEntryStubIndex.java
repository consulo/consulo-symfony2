package fr.adrienbrault.idea.symfony2plugin.vite;

import com.intellij.lang.javascript.psi.JSFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File-based index for Vite entry points defined in vite.config.js / vite.config.ts.
 *
 * Key:   normalized target file path (e.g. "assets/app.ts") — leading "./" stripped
 * Value: entry name (e.g. "app")
 *
 * Allows efficient reverse lookup: given a JS/TS source file, find the Vite entry name(s)
 * that reference it, used by {@link ViteJavaScriptLineMarkerProvider}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ViteEntryStubIndex extends FileBasedIndexExtension<String, String> {

    @Override
    public ID<String, String> getName() {
        return ViteEntryStubIndexKt.VITE_ENTRY_STUB_INDEX_KEY;
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
        return (project, file) -> file.getName().equals("vite.config.js") || file.getName().equals("vite.config.ts");
    }

    // Index-safe only: no PhpIndex/type resolution here.
    @Override
    public DataIndexer<String, String, FileContent> getIndexer() {
        return inputData -> {
            Map<String, String> result = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if (psiFile instanceof JSFile) {
                List<ViteEntry> entries = ViteConfigParser.INSTANCE.parseEntries(psiFile);
                for (ViteEntry entry : entries) {
                    String raw = entry.getTargetPath();
                    if (raw == null) {
                        continue;
                    }

                    String normalized = raw;
                    if (normalized.startsWith("./")) {
                        normalized = normalized.substring(2);
                    }
                    if (normalized.startsWith("/")) {
                        normalized = normalized.substring(1);
                    }

                    if (!normalized.isEmpty()) {
                        result.put(normalized, entry.getName());
                    }
                }
            }

            return result;
        };
    }
}
