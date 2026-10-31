package fr.adrienbrault.idea.symfony2plugin.util.dict;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.virtualFileSystem.VirtualFilePresentation;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyBundleFileLookupElement extends LookupElement {
    private final String bundleName;
    private final BundleFile bundleFile;
    private InsertHandler<LookupElement> insertHandler = null;
    private final @Nullable String shortcutName;

    public SymfonyBundleFileLookupElement(BundleFile bundleFile) {
        this.bundleFile = bundleFile;
        this.shortcutName = bundleFile.getShortcutPath();
        this.bundleName = bundleFile.getSymfonyBundle().getName();
    }

    public SymfonyBundleFileLookupElement(BundleFile bundleFile, InsertHandler<LookupElement> insertHandler) {
        this(bundleFile);
        this.insertHandler = insertHandler;
    }

    @Override
    public String getLookupString() {
        if (shortcutName == null) {
            return "";
        }

        // we strip any control char, so only use the pathname
        if (shortcutName.startsWith("@")) {
            return shortcutName.substring(1);
        }

        return shortcutName;
    }

    @Override
    public void handleInsert(InsertionContext context) {
        if (this.insertHandler != null) {
            this.insertHandler.handleInsert(context, this);
            return;
        }

        super.handleInsert(context);
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(bundleName);
        presentation.setTypeGrayed(true);
        presentation.setIcon(VirtualFilePresentation.getIcon(this.bundleFile.getVirtualFile()));
    }
}