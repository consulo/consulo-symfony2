package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateLookupElement extends LookupElement {
    private final VirtualFile virtualFile;

    private final VirtualFile projectBaseDir;

    private final String templateName;

    private boolean bold = false;

    public TemplateLookupElement(String templateName, VirtualFile virtualFile, VirtualFile projectBaseDir) {
        this.templateName = templateName;
        this.virtualFile = virtualFile;
        this.projectBaseDir = projectBaseDir;
    }

    public TemplateLookupElement(String templateName, VirtualFile virtualFile, VirtualFile projectBaseDir, boolean bold) {
        this(templateName, virtualFile, projectBaseDir);
        this.bold = bold;
    }

    @Override
    public String getLookupString() {
        return templateName;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(this.virtualFile.getFileType().getIcon());
        presentation.setTypeText(VirtualFileUtil.getRelativePath(this.virtualFile, this.projectBaseDir, '/'));
        presentation.setTypeGrayed(true);
        presentation.setItemTextBold(bold);
    }
}
