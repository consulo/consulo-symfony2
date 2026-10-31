package fr.adrienbrault.idea.symfony2plugin;

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import fr.adrienbrault.idea.symfony2plugin.stubs.util.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * "General" section of the Symfony module extension panel: actions, paths, features, code folding.
 * Every change is written into the bound {@link Settings} bean.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SettingsForm {
    private final Project myProject;
    private final ModuleRootLayer myModuleRootLayer;
    private final Settings mySettings;

    private final List<Runnable> myResetters = new ArrayList<>();

    public SettingsForm(Project project, ModuleRootLayer moduleRootLayer, Settings settings) {
        myProject = project;
        myModuleRootLayer = moduleRootLayer;
        mySettings = settings;
    }

    @RequiredUIAccess
    public Component createComponent(Disposable uiDisposable) {
        TextBox pathToTranslationRoot = createPathBox(
            uiDisposable,
            () -> mySettings.pathToTranslation,
            value -> mySettings.pathToTranslation = value
        );
        TextBox directoryToWeb = createPathBox(
            uiDisposable,
            () -> mySettings.directoryToWeb,
            value -> mySettings.directoryToWeb = value
        );
        TextBox directoryToApp = createPathBox(
            uiDisposable,
            () -> mySettings.directoryToApp,
            value -> mySettings.directoryToApp = value
        );

        CheckBox codeFoldingPhpRoute = createCheckBox("Route (PHP)", () -> mySettings.codeFoldingPhpRoute, value -> mySettings.codeFoldingPhpRoute = value);
        CheckBox codeFoldingPhpModel = createCheckBox("Repository Entity (PHP)", () -> mySettings.codeFoldingPhpModel, value -> mySettings.codeFoldingPhpModel = value);
        CheckBox codeFoldingPhpTemplate = createCheckBox("Template (PHP)", () -> mySettings.codeFoldingPhpTemplate, value -> mySettings.codeFoldingPhpTemplate = value);
        CheckBox codeFoldingTwigRoute = createCheckBox("Route (Twig)", () -> mySettings.codeFoldingTwigRoute, value -> mySettings.codeFoldingTwigRoute = value);
        CheckBox codeFoldingTwigTemplate = createCheckBox("Template (Twig)", () -> mySettings.codeFoldingTwigTemplate, value -> mySettings.codeFoldingTwigTemplate = value);
        CheckBox codeFoldingTwigConstant = createCheckBox("Constant (Twig)", () -> mySettings.codeFoldingTwigConstant, value -> mySettings.codeFoldingTwigConstant = value);

        CheckBox featureTwigIcon = createCheckBox("Twig Icon Decoration", () -> mySettings.featureTwigIcon, value -> mySettings.featureTwigIcon = value);
        CheckBox featurePhpClassIcon = createCheckBox("PHP Class Icon Decoration", () -> mySettings.featurePhpClassIcon, value -> mySettings.featurePhpClassIcon = value);
        CheckBox featurePropertyInjection = createCheckBox("Service Property Injection", () -> mySettings.featurePropertyInjection, value -> mySettings.featurePropertyInjection = value);
        CheckBox featureTypeProvider = createCheckBox("Type Provider", () -> mySettings.featureTypeProvider, value -> mySettings.featureTypeProvider = value);

        VerticalLayout root = VerticalLayout.create();

        Button buttonAutoConfigure = Button.create(LocalizeValue.localizeTODO("Autoconfigure"), event -> {
            Collection<String> messages = IdeHelper.autoConfigure(myProject, mySettings, myModuleRootLayer);
            resetUI();

            List<String> list = messages.stream().map(it -> "- " + it).toList();
            String message = list.isEmpty() ? "Nothing to configure" : "Configured with:\n" + StringUtils.join(list, "\n");
            MessageBoxes.okInfo(LocalizeValue.localizeTODO(message))
                .title(LocalizeValue.localizeTODO("Symfony"))
                .showAsync(root);
        });
        Button buttonHelp = Button.create(
            LocalizeValue.localizeTODO("Open Help Page"),
            event -> IdeHelper.openUrl("https://espend.de/phpstorm/plugin/symfony")
        );
        Button buttonReindex = Button.create(LocalizeValue.localizeTODO("Clear Index"), event -> IndexUtil.forceReindex());

        // ── General ──────────────────────────────────────────────────────────
        VerticalLayout general = VerticalLayout.create();
        general.add(HorizontalLayout.create()
            .add(buttonAutoConfigure)
            .add(buttonHelp)
            .add(buttonReindex)
        );
        general.add(UiSettingsUtil.createHintLabel(
            LocalizeValue.localizeTODO("Autoconfigure detects the Symfony project structure and excludes generated directories like var/ from indexing")
        ));

        // ── Paths ─────────────────────────────────────────────────────────────
        VerticalLayout paths = VerticalLayout.create();
        paths.add(createFieldWithHint(
            "Translation Root Path",
            pathToTranslationRoot,
            Settings.DEFAULT_TRANSLATION_PATH,
            "Root directory for translation files (e.g. translations/)"
        ));
        paths.add(createFieldWithHint(
            "Web Directory",
            directoryToWeb,
            Settings.DEFAULT_WEB_DIRECTORY,
            "Public web root directory, used to resolve asset paths (e.g. public/ or web/)"
        ));

        // ── Features ──────────────────────────────────────────────────────────
        VerticalLayout features = VerticalLayout.create();
        List<Map.Entry<CheckBox, String>> featureItems = List.of(
            Map.entry(featureTwigIcon, "Overlay Twig file icons with a badge indicating usage: extends, include, controller, ..."),
            Map.entry(featurePhpClassIcon, "Overlay Symfony-related PHP class file icons with badges"),
            Map.entry(featurePropertyInjection, "Autocomplete properties in service and auto injection class via constructor"),
            Map.entry(featureTypeProvider, "Resolve return types for container calls like ContainerInterface::get() and EntityManager::find()")
        );
        for (Map.Entry<CheckBox, String> item : featureItems) {
            features.add(createCheckWithHint(item.getKey(), item.getValue()));
        }

        // ── Code Folding ──────────────────────────────────────────────────────
        VerticalLayout folding = VerticalLayout.create();
        List<Map.Entry<CheckBox, String>> foldingItems = List.of(
            Map.entry(codeFoldingPhpRoute, "Route name strings in PHP files to a short label"),
            Map.entry(codeFoldingPhpModel, "Doctrine entity repository class names in PHP"),
            Map.entry(codeFoldingPhpTemplate, "Template paths in PHP render() calls"),
            Map.entry(codeFoldingTwigRoute, "Route name strings in Twig templates to url"),
            Map.entry(codeFoldingTwigTemplate, "Template paths in Twig include and extends tags"),
            Map.entry(codeFoldingTwigConstant, "Fold PHP constant / enums references in Twig")
        );
        for (Map.Entry<CheckBox, String> item : foldingItems) {
            folding.add(createCheckWithHint(item.getKey(), item.getValue()));
        }

        // ── Legacy ────────────────────────────────────────────────────────────
        VerticalLayout legacy = VerticalLayout.create();
        legacy.add(createFieldWithHint(
            "App Directory",
            directoryToApp,
            Settings.DEFAULT_APP_DIRECTORY,
            "Legacy Symfony 2/3 app/ directory containing config, cache, and logs"
        ));

        root.add(LabeledLayout.create(LocalizeValue.localizeTODO("General"), general));
        root.add(LabeledLayout.create(LocalizeValue.localizeTODO("Paths"), paths));
        root.add(LabeledLayout.create(LocalizeValue.localizeTODO("Features"), features));
        root.add(LabeledLayout.create(LocalizeValue.localizeTODO("Code Folding"), folding));
        root.add(LabeledLayout.create(LocalizeValue.localizeTODO("Legacy"), legacy));
        return root;
    }

    @RequiredUIAccess
    private void resetUI() {
        for (Runnable resetter : myResetters) {
            resetter.run();
        }
    }

    @RequiredUIAccess
    private CheckBox createCheckBox(String text, BooleanSupplier getter, Consumer<Boolean> setter) {
        CheckBox checkBox = CheckBox.create(LocalizeValue.localizeTODO(text), getter.getAsBoolean());
        checkBox.addValueListener(event -> setter.accept(Boolean.TRUE.equals(event.getValue())));
        myResetters.add(() -> checkBox.setValue(getter.getAsBoolean(), false));
        return checkBox;
    }

    @RequiredUIAccess
    private TextBox createPathBox(Disposable uiDisposable, Supplier<String> getter, Consumer<String> setter) {
        TextBox textBox = UiSettingsUtil.createProjectRelativePathBox(
            myProject,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            uiDisposable
        );
        textBox.setValue(StringUtils.defaultString(getter.get()));
        textBox.addValueListener(event -> setter.accept(StringUtils.defaultString(event.getValue())));
        myResetters.add(() -> textBox.setValue(StringUtils.defaultString(getter.get()), false));
        return textBox;
    }

    @RequiredUIAccess
    private static Component createCheckWithHint(CheckBox checkBox, String hint) {
        return VerticalLayout.create(Space.NONE)
            .add(checkBox)
            .add(indent(UiSettingsUtil.createHintLabel(LocalizeValue.localizeTODO(hint))));
    }

    /**
     * Shifts a hint, so it aligns with the text of the check box above
     */
    @RequiredUIAccess
    private static Component indent(Component component) {
        component.paddingBuilder().leftSet(Space.XX_LARGE).apply();
        return component;
    }

    @RequiredUIAccess
    private static Component createFieldWithHint(String label, TextBox field, String defaultValue, String hint) {
        Button resetButton = Button.create(LocalizeValue.localizeTODO("Default"), event -> field.setValue(defaultValue));

        DockLayout inputRow = DockLayout.create();
        inputRow.center(field);
        inputRow.right(resetButton);

        return VerticalLayout.create(Space.NONE)
            .add(Label.create(LocalizeValue.localizeTODO(label)))
            .add(inputRow)
            .add(UiSettingsUtil.createHintLabel(LocalizeValue.localizeTODO(hint)));
    }
}
