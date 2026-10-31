package fr.adrienbrault.idea.symfony2plugin.templating.inlay.ui;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.twig.TwigFile;
import consulo.application.ApplicationManager;
import consulo.application.util.function.Computable;
import consulo.codeEditor.Editor;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.RelativePoint2D;
import consulo.ui.TextBox;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import fr.adrienbrault.idea.symfony2plugin.templating.inlay.TwigVariableInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyNotificationGroupContributor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Consulo: the unified tree has no per row action icon, so inserting and navigating are toolbar actions on the
 * selected row (double click navigates, like the click on a row upstream)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigVariablesTreePopup {
    /**
     * @param point where to show the popup, {@code null} shows it at the best position of the editor
     */
    @RequiredUIAccess
    public static void show(
        TwigFile file,
        Editor editor,
        Map<String, PsiVariable> variables,
        @Nullable RelativePoint2D point
    ) {
        List<VariableTreeEntry> variableEntries = new ArrayList<>();
        for (Map.Entry<String, PsiVariable> entry : variables.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, PsiVariable>, Integer>comparing(e -> e.getKey().equals("app") ? 1 : 0)
                    .thenComparing(Map.Entry.comparingByKey()))
                .toList()) {
            String varName = entry.getKey();
            PsiVariable psiVariable = entry.getValue();

            Set<String> resolvedTypes = resolveTypes(file.getProject(), psiVariable.getTypes());
            TwigVariableTreeNode varData = new TwigVariableTreeNode(varName, resolvedTypes, null);
            List<TwigVariableTreeNode> properties = TwigVariableTreeBuilder.buildProperties(file.getProject(), varName, resolvedTypes);
            variableEntries.add(new VariableTreeEntry(varData, properties));
        }
        Set<String> listVariables = new HashSet<>();
        Map<String, Set<String>> variableTypes = new HashMap<>();
        for (VariableTreeEntry entry : variableEntries) {
            variableTypes.put(entry.variableNode.name(), entry.variableNode.types());
            if (entry.variableNode.isList()) {
                listVariables.add(entry.variableNode.name());
            }
        }

        VariableTreeModel model = new VariableTreeModel();
        model.setRoot(buildFilteredRoot(variableEntries, ""));

        Tree<TwigVariableTreeNode> tree = Tree.create(model);

        TwigVariableInsertHandler insertHandler = new TwigVariableInsertHandler(editor, file);

        JBPopup[] popupRef = new JBPopup[1];

        TextBox searchField = TextBox.create();
        searchField.setPlaceholder(LocalizeValue.localizeTODO("Filter variables"));
        searchField.setToolTipText(LocalizeValue.localizeTODO("Filter variables"));

        Runnable insertSelected = () -> {
            TwigVariableTreeNode data = getSelectedValue(tree);
            if (data == null) {
                return;
            }

            if (!data.isPropertyNode()) {
                if (data.isList()) {
                    insertHandler.insertEmptyForeach(data.name());
                }
                else if (data.isBool()) {
                    insertHandler.insertIfStatement(data.name());
                }
                else {
                    insertHandler.insertPrintBlock(data.name());
                }
            }
            else {
                String parentVar = data.parentVariable();
                if (parentVar != null && listVariables.contains(parentVar)) {
                    insertHandler.insertForeachPropertyAccess(parentVar, data.name());
                }
                else if (data.isList() && parentVar != null) {
                    insertHandler.insertFilledForeach(parentVar, data.name());
                }
                else {
                    String expr = (parentVar != null ? parentVar + "." : "") + data.name();
                    insertHandler.insertPrintBlock(expr);
                }
            }

            if (popupRef[0] != null) {
                popupRef[0].cancel();
            }
        };

        Function<@Nullable TwigVariableTreeNode, Boolean> navigate = data -> {
            if (data == null) {
                return false;
            }

            Navigatable target = ApplicationManager.getApplication().runReadAction((Computable<Navigatable>) () -> findNavigationTarget(file.getProject(), data, variableTypes));
            if (target == null) {
                return false;
            }

            target.navigate(true);
            if (popupRef[0] != null) {
                popupRef[0].cancel();
            }
            return true;
        };

        tree.addDoubleClickListener(event -> navigate.apply(event.getValue().getValue()));

        DefaultActionGroup optionsGroup = new DefaultActionGroup(LocalizeValue.localizeTODO("Options"), true);
        optionsGroup.getTemplatePresentation().setIcon(PlatformIconGroup.generalGearplain());
        optionsGroup.add(new DumbAwareAction(LocalizeValue.localizeTODO("Disable inlay hints")) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                Project project = file.getProject();
                // TODO consulo-port: InlayHintsSettings.changeHintTypeStatus / SettingsKey missing; upstream:
                // InlayHintsSettings.instance().changeHintTypeStatus(new SettingsKey<>("symfony.twig.root.variables"), TwigLanguage.INSTANCE, false);
                DaemonCodeAnalyzer.getInstance(project).restart(file); // TODO consulo-port: restart(PsiFile, Object reason) missing, reason dropped

                Notification notification = NotificationService.getInstance()
                    .newInfo(SymfonyNotificationGroupContributor.GROUP)
                    .title(LocalizeValue.localizeTODO("Twig Variable Inlay Hints Disabled"))
                    .content(LocalizeValue.localizeTODO("Twig variable inlay hints were disabled. Re-enable them in Settings > Editor > Inlay Hints > Other > Twig > Twig Root Variables."))
                    .create();
                notification.notify(project);

                if (popupRef[0] != null) {
                    popupRef[0].cancel();
                }
            }
        });

        ActionGroup.Builder actions = ActionGroup.newImmutableBuilder();
        actions.add(new LegacyDumbAwareAction(LocalizeValue.localizeTODO("Insert"), LocalizeValue.empty(), PlatformIconGroup.generalAdd()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                insertSelected.run();
            }

            @RequiredUIAccess
            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(getSelectedValue(tree) != null);
            }
        });
        actions.add(new LegacyDumbAwareAction(LocalizeValue.localizeTODO("Navigate"), LocalizeValue.empty(), PlatformIconGroup.actionsEditsource()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                navigate.apply(getSelectedValue(tree));
            }

            @RequiredUIAccess
            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(getSelectedValue(tree) != null);
            }
        });
        actions.add(optionsGroup);

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("TwigVariablesTreePopup", actions.build(), true);
        toolbar.setTargetUIComponent(tree);

        DockLayout header = DockLayout.create();
        header.center(searchField);
        header.right(toolbar.getUIComponent());

        DockLayout content = DockLayout.create();
        content.top(header);
        content.center(ScrollableLayout.create(tree));

        searchField.addValueListener(event -> {
            String query = StringUtils.defaultString(searchField.getValue()).trim();
            model.setRoot(buildFilteredRoot(variableEntries, query));
            tree.refreshAll().whenComplete((o, throwable) -> {
                if (!query.isEmpty()) {
                    tree.expandAll();
                }
            });
        });

        popupRef[0] = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, searchField)
            .setRequestFocus(true)
            .setResizable(true)
            .createPopup();

        if (point != null) {
            popupRef[0].show(point);
        }
        else {
            editor.showPopupInBestPositionFor(popupRef[0]);
        }
    }

    private static @Nullable TwigVariableTreeNode getSelectedValue(Tree<TwigVariableTreeNode> tree) {
        TreeNode<TwigVariableTreeNode> node = tree.getSelectedNode();
        return node != null ? node.getValue() : null;
    }

    /**
     * Variables on the first level, their properties below; the root value is {@code null}
     */
    private static class VariableTreeModel implements TreeModel<TwigVariableTreeNode> {
        private Map<TwigVariableTreeNode, List<TwigVariableTreeNode>> myRoot = Map.of();

        void setRoot(Map<TwigVariableTreeNode, List<TwigVariableTreeNode>> root) {
            myRoot = root;
        }

        @Override
        public void buildChildren(Function<TwigVariableTreeNode, TreeNode<TwigVariableTreeNode>> nodeFactory, @Nullable TwigVariableTreeNode parentValue) {
            if (parentValue == null) {
                for (Map.Entry<TwigVariableTreeNode, List<TwigVariableTreeNode>> entry : myRoot.entrySet()) {
                    TreeNode<TwigVariableTreeNode> node = nodeFactory.apply(entry.getKey());
                    node.setRenderer((value, presentation) -> TwigVariableTreeCellRenderer.render(value, presentation));
                    node.setLeaf(entry.getValue().isEmpty());
                }
                return;
            }

            for (TwigVariableTreeNode property : myRoot.getOrDefault(parentValue, List.of())) {
                TreeNode<TwigVariableTreeNode> node = nodeFactory.apply(property);
                node.setRenderer((value, presentation) -> TwigVariableTreeCellRenderer.render(value, presentation));
                node.setLeaf(true);
            }
        }
    }

    private static Map<TwigVariableTreeNode, List<TwigVariableTreeNode>> buildFilteredRoot(List<VariableTreeEntry> variableEntries, String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        boolean hasQuery = !normalizedQuery.isBlank();
        Map<TwigVariableTreeNode, List<TwigVariableTreeNode>> root = new LinkedHashMap<>();

        for (VariableTreeEntry variableEntry : variableEntries) {
            TwigVariableTreeNode variableNodeData = variableEntry.variableNode;
            boolean variableMatches = !hasQuery || matchesQuery(variableNodeData, normalizedQuery);

            List<TwigVariableTreeNode> filteredProperties = new ArrayList<>();
            for (TwigVariableTreeNode propertyNodeData : variableEntry.propertyNodes) {
                if (!hasQuery || variableMatches || matchesQuery(propertyNodeData, normalizedQuery)) {
                    filteredProperties.add(propertyNodeData);
                }
            }

            if (!variableMatches && filteredProperties.isEmpty()) {
                continue;
            }

            root.put(variableNodeData, filteredProperties);
        }

        return root;
    }

    private static boolean matchesQuery(TwigVariableTreeNode node, String query) {
        if (query.isBlank()) {
            return true;
        }

        String name = node.name().toLowerCase(Locale.ROOT);
        if (name.contains(query)) {
            return true;
        }

        String type = node.getTypeDisplay().toLowerCase(Locale.ROOT);
        if (type.contains(query)) {
            return true;
        }

        String parent = node.parentVariable();
        return parent != null && (parent + "." + node.name()).toLowerCase(Locale.ROOT).contains(query);
    }

    private static @Nullable Navigatable findNavigationTarget(
        Project project,
        TwigVariableTreeNode node,
        Map<String, Set<String>> variableTypes
    ) {
        if (!node.isPropertyNode()) {
            return findClassNavigatable(project, node.types());
        }

        String parentVariable = node.parentVariable();
        if (parentVariable == null) {
            return null;
        }

        Set<String> parentTypes = variableTypes.get(parentVariable);
        if (parentTypes == null) {
            return null;
        }

        for (String type : parentTypes) {
            String baseType = type.replaceAll("\\[]$", "");
            if (baseType.isBlank() || !baseType.startsWith("\\")) {
                continue;
            }

            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, baseType);
            if (phpClass == null) {
                continue;
            }

            for (Method method : phpClass.getMethods()) {
                if (!TwigTypeResolveUtil.isTwigAccessibleMethod(method)) {
                    continue;
                }

                String propName = TwigTypeResolveUtil.getPropertyShortcutMethodName(method);
                if (node.name().equals(propName)) {
                    return (Navigatable) method;
                }
            }

            for (Field field : phpClass.getFields()) {
                if (!field.getModifier().isPublic() || field.getModifier().isStatic()) {
                    continue;
                }

                if (node.name().equals(field.getName())) {
                    return (Navigatable) field;
                }
            }
        }

        return null;
    }

    private static @Nullable Navigatable findClassNavigatable(Project project, Set<String> types) {
        for (String type : types) {
            String baseType = type.replaceAll("\\[\\]$", "");
            if (baseType.isBlank() || !baseType.startsWith("\\")) {
                continue;
            }

            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, baseType);
            if (phpClass != null) {
                return phpClass;
            }
        }

        return null;
    }

    private static Set<String> resolveTypes(Project project, Set<String> rawTypes) {
        PhpType resolved = PhpIndex.getInstance(project).completeType(
            project, PhpType.from(rawTypes.toArray(new String[0])), new HashSet<>()
        );
        Set<String> result = new LinkedHashSet<>();
        for (String t : resolved.getTypes()) {
            if (!t.isBlank() && !PhpType.isUnresolved(t)) {
                result.add(t);
            }
        }
        return result.isEmpty() ? rawTypes : result;
    }

    private record VariableTreeEntry(TwigVariableTreeNode variableNode, List<TwigVariableTreeNode> propertyNodes) {
    }
}
