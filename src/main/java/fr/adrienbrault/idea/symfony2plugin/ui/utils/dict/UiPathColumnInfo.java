package fr.adrienbrault.idea.symfony2plugin.ui.utils.dict;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Table;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.UiFilePathInterface;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UiPathColumnInfo {
    /**
     * "Path" and "Info" (exists / not found) columns of a file path table
     */
    public static <T extends UiFilePathInterface> void addColumns(Table<T> table, Project project) {
        table.addColumn(LocalizeValue.localizeTODO("Path"), file -> StringUtils.defaultString(file.getPath()));
        table.addColumn(LocalizeValue.localizeTODO("Info"), file -> UiSettingsUtil.getPresentableFilePath(project, file).info());
    }
}
