package fr.adrienbrault.idea.symfony2plugin.translation.form;

import consulo.application.ReadAction;
import consulo.disposer.Disposable;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.FormBuilder;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.action.comparator.PsiWeightListComparator;
import fr.adrienbrault.idea.symfony2plugin.action.dict.TranslationFileModel;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslatorKeyExtractorDialog extends DialogDescriptor {
    private final Project myProject;
    private final PsiFile myFileContext;
    private final Collection<String> myDomains;
    private final @Nullable String myDefaultKey;
    private final String myDefaultDomain;

    private final MutableFlatDataModel<TranslationFileModel> myListTableModel = FlatDataModel.of(List.of());
    private final TextBox myTextTranslationKey = TextBox.create();
    private final CheckBox myCheckNavigateTo = CheckBox.create(LocalizeValue.localizeTODO("Navigate to created key (first file only)"));
    private @Nullable ComboBox<String> myComboBox;

    @RequiredUIAccess
    private TranslatorKeyExtractorDialog(
        Project project,
        PsiFile fileContext,
        Collection<String> domains,
        @Nullable String defaultKey,
        String defaultDomain
    ) {
        super(LocalizeValue.localizeTODO("Symfony: Extract Translation Key"));
        myProject = project;
        myFileContext = fileContext;
        myDomains = domains;
        myDefaultKey = defaultKey;
        myDefaultDomain = defaultDomain;
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofPixel(600, 300);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        ComboBox<String> comboBox = ComboBox.create(new ArrayList<>(myDomains));
        myComboBox = comboBox;

        if (myDefaultKey != null) {
            myTextTranslationKey.setValue(myDefaultKey);
        }

        Table<TranslationFileModel> tableView = Table.create(myListTableModel);
        tableView.addColumn(LocalizeValue.empty(), TranslatorKeyExtractorDialog::getFileIcon)
            .setWidth(32)
            .setResizable(false)
            .setRenderer((presentation, item) -> presentation.withIcon(item.getValue()));
        tableView.addColumn(LocalizeValue.localizeTODO("Path"), TranslatorKeyExtractorDialog::getPathName);
        tableView.addColumn(LocalizeValue.localizeTODO("Name"), domainModel -> domainModel.getPsiFile().getName())
            .setWidth(190);
        tableView.addColumn(LocalizeValue.localizeTODO("Create"), TranslationFileModel::isEnabled)
            .setWidth(50)
            .setResizable(false)
            .setRenderer(ComponentItemRenderer.reusable(
                () -> CheckBox.create(LocalizeValue.empty()),
                (checkBox, item) -> checkBox.setValue(Boolean.TRUE.equals(item.getValue()), false)
            ))
            .setEditor(new TableItemEditor<>() {
                @RequiredUIAccess
                @Override
                public ValueComponent<Boolean> createComponent(TranslationFileModel domainModel) {
                    return CheckBox.create(LocalizeValue.empty(), domainModel.isEnabled());
                }

                @RequiredUIAccess
                @Override
                public void commit(TranslationFileModel domainModel, @Nullable Boolean value) {
                    domainModel.setEnabled(Boolean.TRUE.equals(value));
                    myListTableModel.update(domainModel);
                }
            });

        comboBox.setValue(myDefaultDomain);
        filterList(myDefaultDomain);

        comboBox.addValueListener(event -> {
            String domain = event.getValue();
            if (domain != null) {
                filterList(domain);
            }
        });

        FormBuilder builder = FormBuilder.create();
        builder.addLabeled(LocalizeValue.localizeTODO("Translation Key"), myTextTranslationKey);
        builder.addLabeled(LocalizeValue.localizeTODO("Domain"), comboBox);

        DockLayout layout = DockLayout.create();
        layout.top(builder.build());
        layout.center(ScrollableLayout.create(tableView));
        layout.bottom(myCheckNavigateTo);
        return layout;
    }

    @RequiredUIAccess
    @Override
    public Component getPreferredFocusedComponent() {
        return myTextTranslationKey;
    }

    @RequiredUIAccess
    private void filterList(String domainName) {
        // we only support yaml files right now
        // filter on PsiFile instance
        Collection<PsiFile> domainPsiFilesYaml = TranslationUtil.getDomainPsiFiles(myProject, domainName).stream()
            .filter(domainPsiFile -> domainPsiFile instanceof YAMLFile || TranslationUtil.isSupportedXlfFile(domainPsiFile))
            .collect(Collectors.toCollection(ArrayList::new));

        List<TranslationFileModel> items = getFormattedFileModelList(domainPsiFilesYaml);

        // only one domain; fine preselect it
        if (items.size() == 1) {
            items.get(0).setEnabled(true);
        }

        myListTableModel.replaceAll(items);
    }

    private static @Nullable Image getFileIcon(TranslationFileModel modelParameter) {
        if (modelParameter.isBoldness()) {
            return Symfony2Icons.BUNDLE;
        }

        return ReadAction.compute(() -> IconDescriptorUpdaters.getIcon(modelParameter.getPsiFile(), 0));
    }

    private static String getPathName(TranslationFileModel domainModel) {
        SymfonyBundle symfonyBundle = domainModel.getSymfonyBundle();
        if (symfonyBundle != null) {
            return symfonyBundle.getName();
        }

        String relative = domainModel.getRelativePath();
        if (relative != null) {
            return relative;
        }

        return domainModel.getPsiFile().getName();
    }

    private List<TranslationFileModel> getFormattedFileModelList(Collection<PsiFile> psiFiles) {
        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(myProject);
        final SymfonyBundle symfonyBundle = symfonyBundleUtil.getContainingBundle(myFileContext);

        List<TranslationFileModel> psiFilesSorted = new ArrayList<>();
        for (PsiFile psiFile : psiFiles) {
            TranslationFileModel psiWeightList = new TranslationFileModel(psiFile);

            if (symfonyBundle != null && symfonyBundle.isInBundle(psiFile)) {
                psiWeightList.setSymfonyBundle(symfonyBundle);
                psiWeightList.setBoldness(true);
                psiWeightList.addWeight(2);
            }
            else {
                psiWeightList.setSymfonyBundle(symfonyBundleUtil.getContainingBundle(psiFile));
            }

            String relativePath = psiWeightList.getRelativePath();
            if (relativePath != null && (relativePath.startsWith("src") || relativePath.startsWith("app"))) {
                psiWeightList.addWeight(1);
            }

            psiFilesSorted.add(psiWeightList);
        }

        psiFilesSorted.sort(new PsiWeightListComparator());

        return psiFilesSorted;
    }

    @RequiredUIAccess
    private void onOK(OnOkCallback okCallback) {
        String text = myTextTranslationKey.getValue();
        if (StringUtils.isBlank(text)) {
            return;
        }

        List<TranslationFileModel> psiFiles = new ArrayList<>();
        for (TranslationFileModel translationFileModel : myListTableModel) {
            if (translationFileModel.isEnabled()) {
                psiFiles.add(translationFileModel);
            }
        }

        ComboBox<String> comboBox = myComboBox;
        String domain = comboBox != null ? comboBox.getValue() : null;
        if (!psiFiles.isEmpty()) {
            okCallback.onClick(psiFiles, text, domain != null ? domain : myDefaultDomain, myCheckNavigateTo.getValueOrError());
        }
    }

    public interface OnOkCallback {
        void onClick(List<TranslationFileModel> files, String keyName, String domain, boolean navigateTo);
    }

    @RequiredUIAccess
    public static void show(
        Project project,
        PsiFile fileContext,
        Collection<String> domains,
        @Nullable String defaultKey,
        String defaultDomain,
        OnOkCallback okCallback
    ) {
        TranslatorKeyExtractorDialog descriptor = new TranslatorKeyExtractorDialog(project, fileContext, domains, defaultKey, defaultDomain);

        UIAccess uiAccess = UIAccess.current();
        project.getApplication().getInstance(DialogService.class)
            .build(project, descriptor)
            .showAsync()
            .whenCompleteAsync((value, error) -> {
                if (error == null && value != null) {
                    descriptor.onOK(okCallback);
                }
            }, uiAccess);
    }
}
