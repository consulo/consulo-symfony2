package fr.adrienbrault.idea.symfony2plugin.integrations.database;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Selects the found connections to add as data sources; all are selected at first
 */
public class AddFoundDatabaseConnectionDialogDescriptor extends DialogDescriptor {
    private final List<DatabaseConnectionConfig> myConnections;
    private final Consumer<List<DatabaseConnectionConfig>> myOnOk;
    private final Map<DatabaseConnectionConfig, CheckBox> myCheckBoxes = new LinkedHashMap<>();

    public AddFoundDatabaseConnectionDialogDescriptor(List<DatabaseConnectionConfig> connections,
                                                      Consumer<List<DatabaseConnectionConfig>> onOk) {
        super(LocalizeValue.localizeTODO("Add Symfony Database Connections"));
        myConnections = connections;
        myOnOk = onOk;
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        VerticalLayout connectionsLayout = VerticalLayout.create();
        for (DatabaseConnectionConfig config : myConnections) {
            CheckBox checkBox = CheckBox.create(formatConnectionLabel(config), true);
            myCheckBoxes.put(config, checkBox);
            connectionsLayout.add(checkBox);
        }

        VerticalLayout layout = VerticalLayout.create();
        layout.add(Label.create(LocalizeValue.localizeTODO("Select database connections to add from Symfony configuration:")));
        layout.add(ScrollableLayout.create(connectionsLayout));
        return layout;
    }

    @Override
    @RequiredUIAccess
    public void onHandleValue(AnAction action, @Nullable DialogValue value) {
        super.onHandleValue(action, value);

        if (value == DialogValue.OK_VALUE) {
            List<DatabaseConnectionConfig> selected = myCheckBoxes.entrySet().stream()
                .filter(entry -> entry.getValue().getValueOrError())
                .map(Map.Entry::getKey)
                .toList();
            if (!selected.isEmpty()) {
                myOnOk.accept(selected);
            }
        }
    }

    private static String formatConnectionLabel(DatabaseConnectionConfig config) {
        StringBuilder builder = new StringBuilder();
        builder.append(config.name()).append(" — ").append(config.driver()).append("://");

        if (config.host() != null) {
            builder.append(config.host());
            if (config.port() != null) {
                builder.append(":").append(config.port());
            }
        }

        if (config.database() != null) {
            builder.append("/").append(config.database());
        }
        if (config.username() != null) {
            builder.append(" (user: ").append(config.username()).append(")");
        }
        return builder.toString();
    }
}
