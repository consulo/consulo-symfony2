package fr.adrienbrault.idea.symfony2plugin.action.ui;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextArea;
import consulo.ui.TextBox;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.JavascriptServiceNameStrategy;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import javax.script.ScriptException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyJavascriptServiceNameForm extends DialogDescriptor {
    private static final String EXAMPLE = "" +
        "var className = args.className;\n" +
        "var projectName = args.projectName;\n" +
        "var projectBasePath = args.projectBasePath;\n" +
        "var defaultNaming = args.defaultNaming;\n" +
        "\n" +
        "// nullable\n" +
        "var relativePath = args.relativePath;\n" +
        "var absolutePath = args.absolutePath;\n" +
        "\n" +
        "return className.replace(/\\\\/g, '.').toLowerCase();";

    private final Project myProject;
    private final String myClassName;

    private @Nullable TextArea myTextJavascript;

    private SymfonyJavascriptServiceNameForm(Project project, String className) {
        super(LocalizeValue.localizeTODO("Symfony: Service Generator - JavaScript Naming"));
        myProject = project;
        myClassName = className;
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofPixel(500, 400);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        TextBox textClass = TextBox.create(myClassName);
        TextArea textJavascript = TextArea.create();
        myTextJavascript = textJavascript;
        TextBox textResult = TextBox.create();
        textResult.setEditable(false);
        textResult.setEnabled(false);

        Button buttonTest = Button.create(LocalizeValue.localizeTODO("Test"), event -> {
            try {
                Object eval = JavascriptServiceNameStrategy.run(
                    myProject,
                    StringUtils.defaultString(textClass.getValue()),
                    StringUtils.defaultString(textJavascript.getValue())
                );
                if (!(eval instanceof String)) {
                    textResult.setValue("Error: invalid string response");
                    return;
                }

                textResult.setValue((String) eval);
            }
            catch (ScriptException e1) {
                textResult.setValue("Error: " + e1.getMessage());
            }
        });

        Button buttonExample = Button.create(LocalizeValue.localizeTODO("Example"), event -> textJavascript.setValue(EXAMPLE));

        String jsText = Settings.getInstance(myProject).serviceJsNameStrategy;
        if (StringUtils.isNotBlank(jsText)) {
            textJavascript.setValue(jsText);
        }

        VerticalLayout north = VerticalLayout.create();
        north.add(Label.create(LocalizeValue.localizeTODO("PhpClass Name:")));
        north.add(textClass);
        north.add(Label.create(LocalizeValue.localizeTODO("Javascript:")));

        VerticalLayout south = VerticalLayout.create();
        south.add(textResult);
        south.add(Label.create(LocalizeValue.localizeTODO("Provide value by 'return'; empty textfield will clear name strategy to project default")));
        south.add(HorizontalLayout.create().add(buttonTest).add(buttonExample));

        DockLayout layout = DockLayout.create();
        layout.top(north);
        layout.center(ScrollableLayout.create(textJavascript));
        layout.bottom(south);
        return layout;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(LocalizeValue.localizeTODO("Save"));
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        if (value != null) {
            onSave();
        }
        return true;
    }

    @RequiredUIAccess
    private void onSave() {
        TextArea textJavascript = myTextJavascript;
        if (textJavascript == null) {
            return;
        }

        String text = textJavascript.getValue();
        if (StringUtils.isBlank(text)) {
            text = null;
        }

        String strategy = text;
        Settings.modify(myProject, settings -> settings.serviceJsNameStrategy = strategy);
    }

    @RequiredUIAccess
    public static void create(Project project, String className) {
        project.getApplication().getInstance(DialogService.class)
            .build(project, new SymfonyJavascriptServiceNameForm(project, className))
            .showAsync();
    }
}
