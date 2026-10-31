package fr.adrienbrault.idea.symfony2plugin.ui;

import com.jetbrains.php.lang.PhpFileType;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.virtualFileSystem.fileType.FileType;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutingFile;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiFilePathSettingsForm;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;

import java.util.Collection;
import java.util.List;

/**
 * "Routing" section of the Symfony module extension panel
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RoutingSettingsForm extends UiFilePathSettingsForm<RoutingFile> {
    public RoutingSettingsForm(Project project, Settings settings) {
        super(project, settings);
    }

    @Override
    protected FileType getFileType() {
        return PhpFileType.INSTANCE;
    }

    @Override
    protected RoutingFile createItem(String path) {
        return new RoutingFile(path);
    }

    @Override
    protected Collection<RoutingFile> getSettingsItems() {
        List<RoutingFile> routingFiles = mySettings.routingFiles;
        return routingFiles != null ? routingFiles : List.of();
    }

    @Override
    protected Collection<String> getDefaultPaths() {
        return Settings.DEFAULT_ROUTES;
    }

    @Override
    protected void applyItems(List<RoutingFile> items) {
        mySettings.routingFiles = items;
    }

    @RequiredUIAccess
    @Override
    protected Component createDescription() {
        VerticalLayout layout = VerticalLayout.create();
        layout.add(Label.create(LocalizeValue.localizeTODO("Add PHP Routing")));
        layout.add(UiSettingsUtil.createHintLabel(
            LocalizeValue.localizeTODO("If your application does not support guessing the value for compiled route files, you can add custom ones.")
        ));
        layout.add(UiSettingsUtil.createHintLabel(
            LocalizeValue.localizeTODO("Examples: var/cache/dev/[appDevUrlGenerator.php, UrlGenerator.php, url_generating_routes.php]")
        ));
        return layout;
    }
}
