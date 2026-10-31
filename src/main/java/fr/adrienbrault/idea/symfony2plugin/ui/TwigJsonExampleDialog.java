package fr.adrienbrault.idea.symfony2plugin.ui;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextArea;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigJsonExampleDialog extends DialogDescriptor {
    private static final String EXAMPLE =
        "// \"path\" is relative to file (ide-twig.json)\n" +
            "{\n" +
            "  \"namespaces\": [\n" +
            "    {\n" +
            "      \"namespace\": \"foo\", // @foo/bar/foo.htm.twig\n" +
            "      \"path\": \"res\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"path\": \"res\" // bar/foo.htm.twig\n" +
            "    },\n" +
            "    {\n" +
            "      \"path\": \"res\", // FooBundle:bar:foo.htm.twig\n" +
            "      \"type\": \"Bundle\",\n" +
            "      \"namespace\": \"FooBundle\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private TwigJsonExampleDialog() {
        super(LocalizeValue.localizeTODO("Example: ide-twig.json"));
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        TextArea pathIsRelativeToTextArea = TextArea.create(EXAMPLE);
        pathIsRelativeToTextArea.setEditable(false);

        DockLayout layout = DockLayout.create();
        layout.top(Label.create(LocalizeValue.localizeTODO("Example \"ide-twig.json\"; supports multiple locations")));
        layout.center(ScrollableLayout.create(pathIsRelativeToTextArea));
        return layout;
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        return new AnAction[]{new DialogOkAction(CommonLocalize.buttonOk())};
    }

    @RequiredUIAccess
    public static void open(Project project) {
        project.getApplication().getInstance(DialogService.class).build(project, new TwigJsonExampleDialog()).showAsync();
    }
}
