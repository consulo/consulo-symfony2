package fr.adrienbrault.idea.symfony2plugin.asset;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFilePresentation;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ResourceFileInsertHandler;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class AssetLookupElement extends LookupElement {
    protected final AssetFile assetFile;
    protected final Project project;
    protected InsertHandler<AssetLookupElement> insertHandler;

    public AssetLookupElement(AssetFile assetfile, Project project) {
        this.assetFile = assetfile;
        this.project = project;
    }

    @Override
    public String getLookupString() {

        if(assetFile.getAssetPosition().equals(AssetEnum.Position.Bundle)) {
            return assetFile.toString().substring(1);
        }

        return assetFile.toString();
    }

    @Override
    public void handleInsert(InsertionContext context) {

        if(insertHandler != null) {
            insertHandler.handleInsert(context, this);
            super.handleInsert(context);
            return;
        }

        if(assetFile.getAssetPosition().equals(AssetEnum.Position.Bundle)) {
            ResourceFileInsertHandler.getInstance().handleInsert(context, this);
        }

        super.handleInsert(context);
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {

        String typeText = "Web";
        if(assetFile.getAssetPosition().equals(AssetEnum.Position.Bundle)) {
            typeText = "Bundle";
        }

        presentation.setItemText(assetFile.toString());
        presentation.setTypeText(typeText);
        presentation.setIcon(VirtualFilePresentation.getIcon(assetFile.getFile()));

    }

    public AssetLookupElement withInsertHandler(InsertHandler<AssetLookupElement> insertHandler) {
        this.insertHandler = insertHandler;
        return this;
    }

}
