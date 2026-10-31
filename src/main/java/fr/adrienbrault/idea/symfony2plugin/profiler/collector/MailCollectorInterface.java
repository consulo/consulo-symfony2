package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailMessage;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface MailCollectorInterface {
    Collection<MailMessage> getMessages();
}
