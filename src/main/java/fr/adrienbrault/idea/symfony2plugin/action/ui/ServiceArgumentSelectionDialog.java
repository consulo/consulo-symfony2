package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.disposer.Disposable;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.image.Image;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceArgumentSelectionDialog extends DialogDescriptor {
    private final Project myProject;
    private final MutableFlatDataModel<ServiceParameter> myModelList = FlatDataModel.of(List.of());

    @RequiredUIAccess
    private ServiceArgumentSelectionDialog(Project project, Map<String, Set<String>> arguments) {
        super(LocalizeValue.localizeTODO("Symfony: Add Argument"));
        myProject = project;

        List<ServiceParameter> parameters = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : arguments.entrySet()) {
            parameters.add(new ServiceParameter(entry.getKey(), entry.getValue()));
        }
        myModelList.replaceAll(parameters);
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofPixel(620, 300);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        Table<ServiceParameter> tableView = Table.create(myModelList);
        tableView.addColumn(LocalizeValue.empty(), this::getClassIcon)
            .setWidth(32)
            .setResizable(false)
            .setRenderer((presentation, item) -> presentation.withIcon(item.getValue()));
        tableView.addColumn(LocalizeValue.localizeTODO("Class"), ServiceArgumentSelectionDialog::getClassPresentation);
        tableView.addColumn(LocalizeValue.localizeTODO("Service"), modelParameter -> StringUtils.defaultString(modelParameter.getCurrentService()))
            .setEditor(new TableItemEditor<>() {
                @RequiredUIAccess
                @Override
                public ValueComponent<String> createComponent(ServiceParameter modelParameter) {
                    TextBoxWithHistory comboBox = TextBoxWithHistory.create(StringUtils.defaultString(modelParameter.getCurrentService()));
                    comboBox.setHistory(new ArrayList<>(modelParameter.getPossibleServices()));
                    return comboBox;
                }

                @RequiredUIAccess
                @Override
                public void commit(ServiceParameter modelParameter, @Nullable String value) {
                    modelParameter.setCurrentService(value);
                    myModelList.update(modelParameter);
                }
            });

        return ScrollableLayout.create(tableView);
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(LocalizeValue.localizeTODO("Generate"));
    }

    private List<String> getItems() {
        List<String> items = new ArrayList<>();
        for (ServiceParameter serviceParameter : myModelList) {
            items.add(serviceParameter.getCurrentService());
        }
        return items;
    }

    private @Nullable Image getClassIcon(ServiceParameter modelParameter) {
        return ReadAction.compute(() -> {
            PhpClass classInterface = PhpElementsUtil.getClassInterface(myProject, modelParameter.getClassFqn());
            return classInterface != null ? IconDescriptorUpdaters.getIcon(classInterface, 0) : null;
        });
    }

    private static String getClassPresentation(ServiceParameter modelParameter) {
        String classFqn = modelParameter.getClassFqn();
        if (StringUtils.isBlank(classFqn)) {
            return "unknown";
        }

        if (classFqn.startsWith("\\")) {
            classFqn = classFqn.substring(1);
        }

        int i = classFqn.lastIndexOf("\\");
        if (i > 0) {
            String ns = classFqn.substring(0, i);
            String clazz = classFqn.substring(i + 1);
            classFqn = String.format("%s [%s]", clazz, ns);
        }

        return classFqn;
    }

    private static class ServiceParameter {
        private final String myClassFqn;
        private final Set<String> myPossibleServices;

        private @Nullable String myCurrentService;

        public ServiceParameter(String classFqn, Set<String> possibleServices) {
            myClassFqn = classFqn;
            myPossibleServices = possibleServices;

            if (!possibleServices.isEmpty()) {
                myCurrentService = possibleServices.iterator().next();
            }
        }

        public String getClassFqn() {
            return myClassFqn;
        }

        public Set<String> getPossibleServices() {
            return myPossibleServices;
        }

        public @Nullable String getCurrentService() {
            return myCurrentService;
        }

        public void setCurrentService(@Nullable String currentService) {
            if (StringUtils.isBlank(currentService)) {
                myCurrentService = null;
                return;
            }

            myCurrentService = currentService;
        }
    }

    public interface Callback {
        void onOk(List<String> items);
    }

    @RequiredUIAccess
    public static void createDialog(Project project, Map<String, Set<String>> arguments, Callback callback) {
        ServiceArgumentSelectionDialog descriptor = new ServiceArgumentSelectionDialog(project, arguments);

        UIAccess uiAccess = UIAccess.current();
        project.getApplication().getInstance(DialogService.class)
            .build(project, descriptor)
            .showAsync()
            .whenCompleteAsync((value, error) -> {
                if (error != null || value == null) {
                    return;
                }

                List<String> items = descriptor.getItems();
                Application.get().runWriteAction(() -> callback.onOk(items));
            }, uiAccess);
    }
}
