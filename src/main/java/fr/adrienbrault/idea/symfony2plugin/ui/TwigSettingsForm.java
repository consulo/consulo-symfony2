package fr.adrienbrault.idea.symfony2plugin.ui;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.Label;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.io.FileUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * "Twig / Template" section of the Symfony module extension panel. Every change of the namespace table is written into the
 * bound {@link Settings} bean; loading the table is not a change.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigSettingsForm {
    private final Project myProject;
    private final Settings mySettings;
    private @Nullable MutableFlatDataModel<TwigPath> myModel;
    private @Nullable Table<TwigPath> myTable;
    private boolean myLoading = false;
    private final AtomicInteger myReloadRequestId = new AtomicInteger();

    private volatile boolean myDisposed = false;

    public TwigSettingsForm(Project project, Settings settings) {
        myProject = project;
        mySettings = settings;
    }

    @RequiredUIAccess
    private void reloadTwigPaths(boolean includeSettings, boolean forceEnableAll) {
        if (myProject.isDefault()) {
            return;
        }

        // a reload from the settings is not a modification, "Reset To Default" is
        boolean changed = !includeSettings;
        resetList();

        // TODO consulo-port: the unified Table has no empty text; upstream shows "Loading after indexing completes..." / "Loading Twig namespaces..."

        int requestId = myReloadRequestId.incrementAndGet();

        ReadAction
            .nonBlocking(() -> {
                List<TwigPath> sortableLookupItems = new ArrayList<>(TwigUtil.getTwigNamespaces(myProject, includeSettings));
                sortableLookupItems.sort(new TwigUtil.TwigPathNamespaceComparator());
                return sortableLookupItems;
            })
            .inSmartMode(myProject)
            .expireWhen(() -> myDisposed || myProject.isDisposed() || requestId != myReloadRequestId.get())
            .finishOnUiThread(Application::getAnyModalityState, twigPaths -> applyTwigPaths(requestId, twigPaths, forceEnableAll, changed))
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    @RequiredUIAccess
    private void applyTwigPaths(int requestId, List<TwigPath> twigPaths, boolean forceEnableAll, boolean changed) {
        MutableFlatDataModel<TwigPath> model = myModel;
        if (myDisposed || myProject.isDisposed() || requestId != myReloadRequestId.get() || model == null) {
            return;
        }

        List<TwigPath> items = new ArrayList<>();
        for (TwigPath twigPath : twigPaths) {
            items.add(forceEnableAll ? TwigPath.createClone(twigPath, true) : TwigPath.createClone(twigPath));
        }

        myLoading = true;
        try {
            model.replaceAll(items);
        }
        finally {
            myLoading = false;
        }

        if (changed) {
            writeSettings(model);
        }
    }

    @RequiredUIAccess
    public Component createComponent(Disposable uiDisposable) {
        myDisposed = false;
        Disposer.register(uiDisposable, () -> {
            myDisposed = true;
            myReloadRequestId.incrementAndGet();
            myModel = null;
            myTable = null;
        });

        CheckBox chkTwigBundleNamespaceSupport = CheckBox.create(LocalizeValue.localizeTODO("Support Bundle Namespaces"), mySettings.twigBundleNamespaceSupport);
        chkTwigBundleNamespaceSupport.setToolTipText(LocalizeValue.localizeTODO("Example: Foobar:Bar:Foo.html.twig (for older Symfony Versions)"));
        chkTwigBundleNamespaceSupport.addValueListener(event -> mySettings.twigBundleNamespaceSupport = event.getValue());

        MutableFlatDataModel<TwigPath> model = FlatDataModel.of(List.of());
        myModel = model;
        model.addListener(event -> {
            if (!myLoading) {
                writeSettings(model);
            }
        });

        Table<TwigPath> table = Table.create(model);
        table.addColumn(LocalizeValue.localizeTODO("Namespace"), item -> StringUtils.defaultString(item.getNamespace()));
        table.addColumn(LocalizeValue.localizeTODO("Path"), item -> StringUtils.defaultString(item.getRelativePath(myProject)));
        table.addColumn(LocalizeValue.localizeTODO("Type"), item -> item.getNamespaceType().toString());
        table.addColumn(LocalizeValue.localizeTODO("Parser"), item -> item.isCustomPath() ? "Custom" : "Internal");
        table.addColumn(LocalizeValue.localizeTODO("on"), TwigPath::isEnabled)
            .setWidth(50)
            .setResizable(false)
            .setRenderer(ComponentItemRenderer.reusable(
                () -> CheckBox.create(LocalizeValue.empty()),
                (checkBox, item) -> checkBox.setValue(Boolean.TRUE.equals(item.getValue()), false)
            ))
            .setEditor(new TableItemEditor<>() {
                @RequiredUIAccess
                @Override
                public ValueComponent<Boolean> createComponent(TwigPath item) {
                    return CheckBox.create(LocalizeValue.empty(), item.isEnabled());
                }

                @RequiredUIAccess
                @Override
                public void commit(TwigPath item, @Nullable Boolean value) {
                    UiSettingsUtil.replaceItem(model, item, TwigPath.createClone(item, Boolean.TRUE.equals(value)));
                }
            });
        myTable = table;

        Component tablePanel = ToolbarDecoratorBuilderFactory.getInstance()
            .create(table)
            .addOrReplaceAction(new AddAction<>() {
                @RequiredUIAccess
                @Override
                protected void doAdd(AnActionEvent e) {
                    openTwigPathDialog(null);
                }
            })
            .addOrReplaceAction(new EditAction<>() {
                @RequiredUIAccess
                @Override
                public void updateAtUI(AnActionEvent e) {
                    super.updateAtUI(e);
                    disableForInternalPath(e, getSelectedValue(e));
                }

                @RequiredUIAccess
                @Override
                protected void doEdit(TwigPath value, AnActionEvent e) {
                    openTwigPathDialog(value);
                }
            })
            .addOrReplaceAction(new RemoveAction<>() {
                @RequiredUIAccess
                @Override
                public void updateAtUI(AnActionEvent e) {
                    super.updateAtUI(e);
                    disableForInternalPath(e, getSelectedValue(e));
                }

                @RequiredUIAccess
                @Override
                protected void doRemove(TwigPath value, AnActionEvent e) {
                    model.remove(value);
                }
            })
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .build();

        Button buttonJsonExample = Button.create(LocalizeValue.localizeTODO("JSON Example"), event -> TwigJsonExampleDialog.open(myProject));
        Button resetToDefault = Button.create(LocalizeValue.localizeTODO("Reset To Default"), event -> reloadTwigPaths(false, true));

        HorizontalLayout northPanel = HorizontalLayout.create();
        northPanel.add(Label.create(LocalizeValue.localizeTODO("Manage Twig Namespaces")));
        northPanel.add(chkTwigBundleNamespaceSupport);
        northPanel.add(buttonJsonExample);
        northPanel.add(resetToDefault);

        DockLayout root = DockLayout.create();
        root.top(northPanel);
        root.center(tablePanel);

        reloadTwigPaths(true, false);

        return root;
    }

    private static void disableForInternalPath(AnActionEvent e, @Nullable TwigPath selectedObject) {
        if (selectedObject == null || !selectedObject.isCustomPath()) {
            e.getPresentation().setEnabled(false);
        }
    }

    private void writeSettings(MutableFlatDataModel<TwigPath> model) {
        List<TwigNamespaceSetting> twigPaths = new ArrayList<>();

        for (TwigPath twigPath : model) {
            // Skip absolute paths - only relative paths are supported
            if (twigPath.isCustomPath() && FileUtil.isAbsolute(twigPath.getPath())) {
                continue;
            }

            String relativePath = twigPath.getRelativePath(myProject);

            // only custom and disabled path need to save
            if ((!twigPath.isEnabled() && relativePath != null) || twigPath.isCustomPath()) {
                twigPaths.add(
                    new TwigNamespaceSetting(
                        twigPath.getNamespace(),
                        relativePath,
                        twigPath.isEnabled(),
                        twigPath.getNamespaceType(),
                        twigPath.isCustomPath()
                    )
                );
            }
        }

        mySettings.twigNamespaces = twigPaths;
    }

    @RequiredUIAccess
    private void resetList() {
        MutableFlatDataModel<TwigPath> model = myModel;
        if (model != null) {
            myLoading = true;
            try {
                model.removeAll();
            }
            finally {
                myLoading = false;
            }
        }
    }

    @RequiredUIAccess
    private void openTwigPathDialog(@Nullable TwigPath twigPath) {
        TwigNamespaceDialog.show(myProject, twigPath, newTwigPath -> {
            MutableFlatDataModel<TwigPath> model = myModel;
            Table<TwigPath> table = myTable;
            if (model == null || table == null) {
                return;
            }

            // re-add old item to not use public setter wor twigpaths
            if (twigPath != null) {
                UiSettingsUtil.replaceItem(model, twigPath, newTwigPath);
            }
            else {
                model.add(newTwigPath);
            }
            table.select(newTwigPath);
        });
    }
}
