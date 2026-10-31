package fr.adrienbrault.idea.symfony2plugin.ui;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.language.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiFilePathSettingsForm;

import java.util.Collection;
import java.util.List;

/**
 * "Container" section of the Symfony module extension panel
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerSettingsForm extends UiFilePathSettingsForm<ContainerFile> {
    public ContainerSettingsForm(Project project, Settings settings) {
        super(project, settings);
    }

    @Override
    protected FileType getFileType() {
        return XmlFileType.INSTANCE;
    }

    @Override
    protected ContainerFile createItem(String path) {
        return new ContainerFile(path);
    }

    @Override
    protected Collection<ContainerFile> getSettingsItems() {
        List<ContainerFile> containerFiles = mySettings.containerFiles;
        return containerFiles != null ? containerFiles : List.of();
    }

    @Override
    protected Collection<String> getDefaultPaths() {
        return ServiceContainerUtil.getContainerFiles(myProject);
    }

    @Override
    protected void applyItems(List<ContainerFile> items) {
        mySettings.containerFiles = items;
    }

    @RequiredUIAccess
    @Override
    protected Component createDescription() {
        return Label.create(LocalizeValue.localizeTODO("Add additional Symfony xml container files"));
    }
}
