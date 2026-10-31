package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.ScrollType;
import consulo.disposer.Disposable;
import consulo.document.util.TextRange;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.Label;
import consulo.ui.ModalityState;
import consulo.ui.RadioGroup;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.language.XmlFileType;
import consulo.xml.language.psi.XmlElementFactory;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.SymfonyProjectState;
import fr.adrienbrault.idea.symfony2plugin.module.extension.SymfonyModuleExtension;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.ClassCompletionPanelWrapper;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCreateService extends DialogDescriptor {
    private final Project myProject;

    private final @Nullable PsiFile myPsiFile;

    private final @Nullable Editor myEditor;

    private final @Nullable String myClassInit;

    private final MutableFlatDataModel<MethodParameter.MethodModelParameter> myModelList = FlatDataModel.of(List.of());

    private final RadioGroup<ServiceBuilder.OutputType> myOutputType = RadioGroup.create();

    private Map<String, ContainerService> myServiceClass = Map.of();

    private @Nullable ClassCompletionPanelWrapper myClassCompletionPanelWrapper;
    private @Nullable EditorBox myEditorOutput;
    private @Nullable TextBox myTextFieldServiceName;
    private @Nullable CheckBox myCheckBoxSymfonyIdClass;
    private @Nullable Component myServiceNamePanel;

    private SymfonyCreateService(Project project, @Nullable PsiFile psiFile, @Nullable Editor editor, @Nullable String className) {
        super(LocalizeValue.localizeTODO("Symfony: Service Generator"));
        myProject = project;
        myPsiFile = psiFile;
        myEditor = editor;
        myClassInit = className;
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofPixel(800, 600);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        ClassCompletionPanelWrapper classCompletionPanelWrapper = new ClassCompletionPanelWrapper(myProject, uiDisposable, s -> update());
        myClassCompletionPanelWrapper = classCompletionPanelWrapper;

        Table<MethodParameter.MethodModelParameter> tableView = Table.create(myModelList);
        tableView.addColumn(LocalizeValue.empty(), SymfonyCreateService::getMethodIcon)
            .setWidth(32)
            .setResizable(false)
            .setRenderer((presentation, item) -> presentation.withIcon(item.getValue()));
        tableView.addColumn(LocalizeValue.localizeTODO("Method"), modelParameter -> StringUtils.defaultString(modelParameter.getName()));
        tableView.addColumn(LocalizeValue.localizeTODO("Parameter"), modelParameter -> StringUtils.defaultString(modelParameter.getParameter().getName()));
        tableView.addColumn(LocalizeValue.localizeTODO("Service"), modelParameter -> StringUtils.defaultString(modelParameter.getCurrentService()))
            .setEditor(new TableItemEditor<>() {
                @RequiredUIAccess
                @Override
                public ValueComponent<String> createComponent(MethodParameter.MethodModelParameter modelParameter) {
                    TextBoxWithHistory comboBox = TextBoxWithHistory.create(StringUtils.defaultString(modelParameter.getCurrentService()));
                    comboBox.setHistory(new ArrayList<>(modelParameter.getPossibleServices()));
                    return comboBox;
                }

                @RequiredUIAccess
                @Override
                public void commit(MethodParameter.MethodModelParameter modelParameter, @Nullable String value) {
                    modelParameter.setCurrentService(value);
                    myModelList.update(modelParameter);
                }
            });
        tableView.addColumn(LocalizeValue.localizeTODO("Act"), MethodParameter.MethodModelParameter::isPossibleService)
            .setWidth(36)
            .setResizable(false)
            .setRenderer(ComponentItemRenderer.reusable(
                () -> CheckBox.create(LocalizeValue.empty()),
                (checkBox, item) -> checkBox.setValue(Boolean.TRUE.equals(item.getValue()), false)
            ))
            .setEditor(new TableItemEditor<>() {
                @RequiredUIAccess
                @Override
                public ValueComponent<Boolean> createComponent(MethodParameter.MethodModelParameter modelParameter) {
                    return CheckBox.create(LocalizeValue.empty(), modelParameter.isPossibleService());
                }

                @RequiredUIAccess
                @Override
                public void commit(MethodParameter.MethodModelParameter modelParameter, @Nullable Boolean value) {
                    boolean selected = Boolean.TRUE.equals(value);
                    if (selected && modelParameter.getCurrentService() == null) {
                        modelParameter.setCurrentService(ServiceBuilder.resolveServiceName(myProject, modelParameter.getParameter(), myServiceClass));
                    }

                    modelParameter.setPossibleService(selected);
                    myModelList.update(modelParameter);
                }
            });

        myServiceClass = ContainerCollectionResolver.getServices(myProject);

        myModelList.addListener(event -> generateServiceDefinition());

        TextBox textFieldServiceName = TextBox.create();
        myTextFieldServiceName = textFieldServiceName;

        CheckBox checkBoxSymfonyIdClass = CheckBox.create(LocalizeValue.localizeTODO("Class as id"), getSettings().serviceClassAsIdAttribute);
        myCheckBoxSymfonyIdClass = checkBoxSymfonyIdClass;

        EditorBox editorOutput = myProject.getApplication().getInstance(EditorBoxBuilderFactory.class)
            .create(myProject)
            .multiline()
            .viewer()
            .fileType(YAMLFileType.YML)
            .build();
        myEditorOutput = editorOutput;

        Button generateButton = Button.create(LocalizeValue.localizeTODO("Generate"), event -> update());

        // North: Class label + class field + Generate button
        DockLayout northPanel = DockLayout.create();
        northPanel.left(Label.create(LocalizeValue.localizeTODO("Class:")));
        northPanel.center(classCompletionPanelWrapper.getComponent());
        northPanel.right(generateButton);

        // Service name row (hidden when class as id is checked)
        HorizontalLayout serviceNamePanel = HorizontalLayout.create();
        serviceNamePanel.add(Label.create(LocalizeValue.localizeTODO("Service name:")));
        serviceNamePanel.add(textFieldServiceName);
        myServiceNamePanel = serviceNamePanel;

        // Output format radios
        HorizontalLayout outputRow = HorizontalLayout.create();
        outputRow.add(myOutputType.newButton(LocalizeValue.localizeTODO("Yaml"), ServiceBuilder.OutputType.Yaml));
        outputRow.add(myOutputType.newButton(LocalizeValue.localizeTODO("XML"), ServiceBuilder.OutputType.XML));
        outputRow.add(myOutputType.newButton(LocalizeValue.localizeTODO("Fluent"), ServiceBuilder.OutputType.Fluent));
        outputRow.add(myOutputType.newButton(LocalizeValue.localizeTODO("Array"), ServiceBuilder.OutputType.PhpArray));

        DockLayout outputPanel = DockLayout.create();
        outputPanel.top(outputRow);
        outputPanel.center(editorOutput);

        VerticalLayout optionsPanel = VerticalLayout.create();
        optionsPanel.add(checkBoxSymfonyIdClass);
        optionsPanel.add(UiSettingsUtil.createHintLabel(
            LocalizeValue.localizeTODO("Use the class name as the service id (e.g. App\\Service\\MyService)")
        ));
        optionsPanel.add(serviceNamePanel);

        DockLayout southPanel = DockLayout.create();
        southPanel.top(optionsPanel);
        southPanel.center(LabeledLayout.create(LocalizeValue.localizeTODO("Output"), outputPanel));

        DockLayout content = DockLayout.create();
        content.top(northPanel);
        content.center(ScrollableLayout.create(tableView));
        content.bottom(southPanel);

        // set default output language on last user selection
        ServiceBuilder.OutputType outputType = null;
        String lastServiceGeneratorLanguage = SymfonyProjectState.getInstance(myProject).lastServiceGeneratorLanguage;
        if ("xml".equalsIgnoreCase(lastServiceGeneratorLanguage)) {
            outputType = ServiceBuilder.OutputType.XML;
        }
        else if ("yaml".equalsIgnoreCase(lastServiceGeneratorLanguage)) {
            outputType = ServiceBuilder.OutputType.Yaml;
        }
        else if ("fluent".equalsIgnoreCase(lastServiceGeneratorLanguage)) {
            outputType = ServiceBuilder.OutputType.Fluent;
        }
        else if ("phparray".equalsIgnoreCase(lastServiceGeneratorLanguage)) {
            outputType = ServiceBuilder.OutputType.PhpArray;
        }

        // overwrite language output on direct file context
        if (myPsiFile instanceof YAMLFile) {
            outputType = ServiceBuilder.OutputType.Yaml;
        }
        else if (myPsiFile instanceof XmlFile) {
            outputType = ServiceBuilder.OutputType.XML;
        }

        // lets use yaml as default
        myOutputType.setValue(outputType != null ? outputType : ServiceBuilder.OutputType.Yaml, false);

        updateServiceNameVisibility();
        checkBoxSymfonyIdClass.addValueListener(event -> {
            updateServiceNameVisibility();
            generateServiceDefinition();
        });

        initClassName();

        myOutputType.addValueListener(value -> generateServiceDefinition());
        textFieldServiceName.addValueListener(event -> generateServiceDefinition());

        return content;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        ClassCompletionPanelWrapper classCompletionPanelWrapper = myClassCompletionPanelWrapper;
        return classCompletionPanelWrapper != null ? classCompletionPanelWrapper.getComponent() : null;
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        List<AnAction> actions = new ArrayList<>();
        actions.add(new SettingsAction());
        actions.add(new CloseAction());
        actions.add(new CopyAction());

        // insert
        if (myPsiFile instanceof XmlFile || myPsiFile instanceof YAMLFile) {
            actions.add(new InsertAction());
        }

        return actions.toArray(AnAction.EMPTY_ARRAY);
    }

    @Override
    public boolean isDefaultAction(AnAction action) {
        return action instanceof InsertAction;
    }

    @RequiredUIAccess
    private void updateServiceNameVisibility() {
        CheckBox checkBoxSymfonyIdClass = myCheckBoxSymfonyIdClass;
        Component serviceNamePanel = myServiceNamePanel;
        if (checkBoxSymfonyIdClass != null && serviceNamePanel != null) {
            serviceNamePanel.setVisible(!checkBoxSymfonyIdClass.getValueOrError());
        }
    }

    @RequiredUIAccess
    private void initClassName() {
        ClassCompletionPanelWrapper classCompletionPanelWrapper = myClassCompletionPanelWrapper;
        if (classCompletionPanelWrapper == null) {
            return;
        }

        if (myClassInit != null) {
            classCompletionPanelWrapper.setClassName(StringUtils.stripStart(myClassInit, "\\"));
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        uiAccess.getClipboard().getText().whenCompleteAsync((stringData, error) -> {
            if (error != null || stringData == null) {
                return;
            }

            if (stringData.length() <= 255 && stringData.matches("[_A-Za-z0-9\\\\]+")) {
                classCompletionPanelWrapper.setClassName(stringData);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private boolean insertYamlServiceTag() {
        if (!(myPsiFile instanceof YAMLFile yamlFile)) {
            return false;
        }

        String text = createServiceAsText(ServiceBuilder.OutputType.Yaml, yamlFile);
        YAMLKeyValue fromText = YamlPsiElementFactory.createFromText(myProject, YAMLKeyValue.class, text);
        if (fromText == null) {
            return false;
        }

        PsiElement psiElement = YamlHelper.insertKeyIntoFile(yamlFile, fromText, "services");
        if (psiElement != null) {
            navigateToElement(new TextRange[]{psiElement.getTextRange()});
        }

        return true;
    }

    @RequiredUIAccess
    private boolean insertXmlServiceTag() {
        if (!(myPsiFile instanceof XmlFile xmlFile)) {
            return false;
        }

        final XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag == null) {
            return false;
        }

        String serviceText = createServiceAsText(ServiceBuilder.OutputType.XML);

        final TextRange[] textRange = {null};
        WriteCommandAction.runWriteCommandAction(myProject, "Generate Service", null, () -> {

            XmlTag services = rootTag.findFirstSubTag("services");
            XmlElementFactory instance = XmlElementFactory.getInstance(myProject);

            if (services == null) {
                services = rootTag.addSubTag(instance.createTagFromText("<services/>", rootTag.getLanguage()), false);
            }

            XmlTag tag = instance.createTagFromText(serviceText.replace("\r\n", "\n").replace("\n", " "), services.getLanguage());

            textRange[0] = services.addSubTag(tag, false).getTextRange();
        });

        navigateToElement(textRange);

        return true;
    }

    private void navigateToElement(TextRange[] textRange) {
        if (myEditor != null && textRange[0] != null) {
            myEditor.getCaretModel().moveToOffset(textRange[0].getStartOffset() + 1);
            myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }
    }

    private String getClassName() {
        ClassCompletionPanelWrapper classCompletionPanelWrapper = myClassCompletionPanelWrapper;
        return classCompletionPanelWrapper != null ? classCompletionPanelWrapper.getClassName() : "";
    }

    private String getServiceName() {
        TextBox textFieldServiceName = myTextFieldServiceName;
        return textFieldServiceName != null ? StringUtils.defaultString(textFieldServiceName.getValue()) : "";
    }

    private boolean isClassAsId() {
        CheckBox checkBoxSymfonyIdClass = myCheckBoxSymfonyIdClass;
        return checkBoxSymfonyIdClass != null && checkBoxSymfonyIdClass.getValueOrError();
    }

    private List<MethodParameter.MethodModelParameter> getItems() {
        List<MethodParameter.MethodModelParameter> items = new ArrayList<>();
        for (MethodParameter.MethodModelParameter item : myModelList) {
            items.add(item);
        }
        return items;
    }

    private String createServiceAsText(ServiceBuilder.OutputType outputType, PsiFile psiFile) {
        String className = StringUtils.stripStart(getClassName(), "\\");

        ServiceBuilder builder = new ServiceBuilder(getItems(), psiFile, isClassAsId());
        return builder.build(outputType, className, getServiceName());
    }

    private String createServiceAsText(ServiceBuilder.OutputType outputType) {
        String className = StringUtils.stripStart(getClassName(), "\\");

        ServiceBuilder builder = new ServiceBuilder(getItems(), myProject, isClassAsId());
        return builder.build(outputType, className, getServiceName());
    }

    @RequiredUIAccess
    private void generateServiceDefinition() {
        String className = getClassName();
        if (StringUtils.isBlank(className)) {
            return;
        }

        className = StringUtils.stripStart(className, "\\");

        // after cleanup class is empty
        if (StringUtils.isBlank(className)) {
            return;
        }

        ServiceBuilder.OutputType selectedOutputType = myOutputType.getValue();
        ServiceBuilder.OutputType outputType = selectedOutputType != null ? selectedOutputType : ServiceBuilder.OutputType.XML;

        // save last selection
        SymfonyProjectState.getInstance(myProject).lastServiceGeneratorLanguage = outputType.toString().toLowerCase();

        // the ui values are read here; the builder itself runs in the background
        List<MethodParameter.MethodModelParameter> items = getItems();
        String serviceName = getServiceName();
        boolean classAsId = isClassAsId();
        String finalClassName = className;

        FileType fileType = getFileType(outputType);
        ReadAction.nonBlocking(() -> new ServiceBuilder(items, myProject, classAsId).build(outputType, finalClassName, serviceName))
            .finishOnUiThread(app -> ModalityState.any(), text -> {
                EditorBox editorOutput = myEditorOutput;
                if (text != null && editorOutput != null) {
                    editorOutput.setDocument(EditorFactory.getInstance().createDocument(text), fileType);
                }
            })
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    private FileType getFileType(ServiceBuilder.OutputType outputType) {
        return switch (outputType) {
            case XML -> XmlFileType.INSTANCE;
            case Fluent, PhpArray -> PhpFileType.INSTANCE;
            case Yaml -> YAMLFileType.YML;
        };
    }

    @RequiredUIAccess
    private void update() {
        String className = getClassName();
        if (className.startsWith("\\")) {
            className = className.substring(1);
        }

        if (className.isEmpty()) {
            return;
        }

        final String finalClassName = className;

        ReadAction.nonBlocking(() -> {
                ServiceBuilder.ServiceDefinitionModel model = ServiceBuilder.createModel(myProject, finalClassName);
                if (model == null) {
                    return null;
                }

                return new UpdateResult(model.getServiceName(), model.getModelParameters());
            })
            .finishOnUiThread(app -> ModalityState.any(), result -> {
                TextBox textFieldServiceName = myTextFieldServiceName;
                if (textFieldServiceName == null) {
                    return;
                }

                if (result == null) {
                    textFieldServiceName.setValue("");
                    return;
                }

                textFieldServiceName.setValue(StringUtils.defaultString(result.serviceName));
                myModelList.replaceAll(result.modelParameters);
                generateServiceDefinition();
            })
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    private record UpdateResult(@Nullable String serviceName, List<MethodParameter.MethodModelParameter> modelParameters) {
    }

    private static @Nullable Image getMethodIcon(MethodParameter.MethodModelParameter modelParameter) {
        return ReadAction.compute(() -> IconDescriptorUpdaters.getIcon(modelParameter.getMethod(), 0));
    }

    private class SettingsAction extends DumbAwareAction {
        SettingsAction() {
            super(LocalizeValue.localizeTODO("Settings"));
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            SymfonyJavascriptServiceNameForm.create(myProject, getClassName());
        }
    }

    private static class CloseAction extends DumbAwareAction {
        CloseAction() {
            super(LocalizeValue.localizeTODO("Close"));
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Dialog dialog = e.getData(Dialog.KEY);
            if (dialog != null) {
                dialog.doCancelAction();
            }
        }
    }

    private class CopyAction extends DumbAwareAction {
        CopyAction() {
            super(LocalizeValue.localizeTODO("To Clipboard"));
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            EditorBox editorOutput = myEditorOutput;
            String text = editorOutput != null ? editorOutput.getValue() : null;
            if (StringUtils.isBlank(text)) {
                return;
            }

            UIAccess.current().getClipboard().setText(text);
            saveServiceClassAsIdAttribute();
        }
    }

    private class InsertAction extends DumbAwareAction {
        InsertAction() {
            super(LocalizeValue.localizeTODO("Insert"));
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            boolean inserted = myPsiFile instanceof XmlFile ? insertXmlServiceTag() : insertYamlServiceTag();
            if (!inserted) {
                return;
            }

            saveServiceClassAsIdAttribute();

            Dialog dialog = e.getData(Dialog.KEY);
            if (dialog != null) {
                dialog.doOkAction(DialogValue.OK_VALUE);
            }
        }
    }

    public static class ContainerServicePriorityNameComparator implements Comparator<ContainerService> {
        @Override
        public int compare(ContainerService o1, ContainerService o2) {

            if (ServiceContainerUtil.isLowerPriority(o1.getName()) && ServiceContainerUtil.isLowerPriority(o2.getName())) {
                return 0;
            }

            if (ServiceContainerUtil.isLowerPriority(o1.getName())) {
                return 1;
            }

            return -1;
        }
    }

    public static class ContainerServicePriorityWeakComparator implements Comparator<ContainerService> {
        @Override
        public int compare(ContainerService o1, ContainerService o2) {

            if (o1.isWeak() == o2.isWeak()) {
                return 0;
            }

            return (o1.isWeak() ? 1 : -1);
        }
    }

    @RequiredUIAccess
    private static void show(SymfonyCreateService service) {
        service.myProject.getApplication().getInstance(DialogService.class).build(service.myProject, service).showAsync();
    }

    @RequiredUIAccess
    public static void create(Project project, PsiFile psiFile, @Nullable Editor editor) {
        show(new SymfonyCreateService(project, psiFile, editor, null));
    }

    @RequiredUIAccess
    public static void create(Project project, PsiFile psiFile, PhpClass phpClass, @Nullable Editor editor) {
        show(new SymfonyCreateService(project, psiFile, editor, phpClass.getFQN()));
    }

    private Settings getSettings() {
        PsiFile psiFile = myPsiFile;
        return psiFile != null ? Settings.getInstance(psiFile) : Settings.getInstance(myProject);
    }

    /**
     * "Class as id" is part of the Symfony module configuration, so it is committed into the module only once the service is generated
     */
    @RequiredUIAccess
    private void saveServiceClassAsIdAttribute() {
        CheckBox checkBox = myCheckBoxSymfonyIdClass;
        if (checkBox == null) {
            return;
        }

        boolean value = checkBox.getValueOrError();
        if (getSettings().serviceClassAsIdAttribute == value) {
            return;
        }

        PsiFile psiFile = myPsiFile;
        SymfonyModuleExtension extension = psiFile != null ? Settings.findExtension(psiFile) : null;
        if (extension != null) {
            Settings.modify(extension.getModule(), settings -> settings.serviceClassAsIdAttribute = value);
        }
        else {
            Settings.modify(myProject, settings -> settings.serviceClassAsIdAttribute = value);
        }
    }
}
