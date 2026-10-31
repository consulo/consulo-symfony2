package fr.adrienbrault.idea.symfony2plugin.profiler.ui;

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.RadioGroup;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * "Profiler" section of the Symfony module extension panel
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ProfilerSettingsDialog {
    private enum ProfilerMode {
        DEFAULT,
        LOCAL,
        HTTP
    }

    private final Project myProject;
    private final Settings mySettings;

    public ProfilerSettingsDialog(Project project, Settings settings) {
        myProject = project;
        mySettings = settings;
    }

    @RequiredUIAccess
    public Component createComponent(Disposable uiDisposable) {
        Settings settings = mySettings;

        TextBox textHttpProfilerUrl = TextBox.create(StringUtils.defaultString(settings.profilerHttpUrl));
        TextBox textLocalProfilerUrl = TextBox.create(StringUtils.defaultString(settings.profilerLocalUrl));
        TextBox textLocalProfilerCsvPath = UiSettingsUtil.createProjectRelativePathBox(
            myProject,
            // IntelliJ: FileChooserDescriptorFactory.createSingleFileDescriptor("csv")
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withExtensionFilter("csv"),
            uiDisposable
        );
        textLocalProfilerCsvPath.setValue(StringUtils.defaultString(settings.profilerCsvPath));

        RadioGroup<ProfilerMode> modeGroup = RadioGroup.create();

        FormBuilder localForm = FormBuilder.create();
        localForm.addLabeled(LocalizeValue.localizeTODO("CSV File"), textLocalProfilerCsvPath);
        localForm.addBottom(UiSettingsUtil.createHintLabel(LocalizeValue.localizeTODO("Location: var/cache/dev/index.csv")));
        FormBuilder localUrlForm = FormBuilder.create();
        localUrlForm.addLabeled(LocalizeValue.localizeTODO("Base url"), textLocalProfilerUrl);
        localUrlForm.addBottom(UiSettingsUtil.createHintLabel(
            LocalizeValue.localizeTODO("Overwrite base url: http://127.0.0.1:8080, http://127.0.0.1/app_dev.php")
        ));

        VerticalLayout localComponents = VerticalLayout.create();
        localComponents.add(localForm.build());
        localComponents.add(localUrlForm.build());

        FormBuilder httpForm = FormBuilder.create();
        httpForm.addLabeled(LocalizeValue.localizeTODO("Base url"), textHttpProfilerUrl);
        httpForm.addBottom(UiSettingsUtil.createHintLabel(LocalizeValue.localizeTODO("Profiler url without \"_profiler\": http://127.0.0.1:8080")));
        Component httpComponents = httpForm.build();

        VerticalLayout root = VerticalLayout.create();
        root.add(modeGroup.newButton(LocalizeValue.localizeTODO("Default Profiler"), ProfilerMode.DEFAULT));
        root.add(UiSettingsUtil.createHintLabel(LocalizeValue.localizeTODO("Extract index.csv from container configuration")));
        root.add(modeGroup.newButton(LocalizeValue.localizeTODO("Local Profiler"), ProfilerMode.LOCAL));
        root.add(localComponents);
        root.add(modeGroup.newButton(LocalizeValue.localizeTODO("HTTP Profiler"), ProfilerMode.HTTP));
        root.add(httpComponents);

        ProfilerMode mode = getMode(settings);
        modeGroup.setValue(mode);
        localComponents.setVisible(mode == ProfilerMode.LOCAL);
        httpComponents.setVisible(mode == ProfilerMode.HTTP);

        modeGroup.addValueListener(newMode -> {
            settings.profilerLocalEnabled = newMode == ProfilerMode.LOCAL;
            settings.profilerHttpEnabled = newMode == ProfilerMode.HTTP;
            localComponents.setVisible(newMode == ProfilerMode.LOCAL);
            httpComponents.setVisible(newMode == ProfilerMode.HTTP);
        });

        textLocalProfilerUrl.addValueListener(event -> settings.profilerLocalUrl = StringUtils.defaultString(event.getValue()));
        textLocalProfilerCsvPath.addValueListener(event -> settings.profilerCsvPath = StringUtils.defaultString(event.getValue()));
        textHttpProfilerUrl.addValueListener(event -> settings.profilerHttpUrl = StringUtils.defaultString(event.getValue()));

        return root;
    }

    private static ProfilerMode getMode(Settings settings) {
        if (settings.profilerLocalEnabled) {
            return ProfilerMode.LOCAL;
        }

        if (settings.profilerHttpEnabled) {
            return ProfilerMode.HTTP;
        }

        return ProfilerMode.DEFAULT;
    }
}
