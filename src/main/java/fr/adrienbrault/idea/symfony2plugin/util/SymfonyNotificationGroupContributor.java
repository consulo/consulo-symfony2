package fr.adrienbrault.idea.symfony2plugin.util;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;

import java.util.function.Consumer;

/**
 * Replacement for the plugin.xml {@code <notificationGroup id="Symfony Notifications" displayType="STICKY_BALLOON"/>}
 */
@ExtensionImpl
public class SymfonyNotificationGroupContributor implements NotificationGroupContributor {
    public static final NotificationGroup GROUP = new NotificationGroup(
        "Symfony Notifications",
        LocalizeValue.localizeTODO("Symfony Notifications"),
        NotificationDisplayType.STICKY_BALLOON,
        true
    );

    @Override
    public void contribute(Consumer<NotificationGroup> registrator) {
        registrator.accept(GROUP);
    }
}
