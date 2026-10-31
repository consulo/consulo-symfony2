package fr.adrienbrault.idea.symfony2plugin.profiler.renderer;

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDispatchedEvent;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerEventDispatcher;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerEventListener;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerEvents;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerEventsConsumer;
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile;

import java.util.regex.Pattern;

/**
 * Renders dispatcher/event headings with called listener lists as CSV in source order.
 */
public final class SymfonyProfilerEventsDetailRenderer implements ProfilerDetailRenderer {
    public static final SymfonyProfilerEventsDetailRenderer INSTANCE = new SymfonyProfilerEventsDetailRenderer();

    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("[\\u0000-\\u001F\\u007F]+");

    private SymfonyProfilerEventsDetailRenderer() {
    }

    @Override
    public String getName() {
        return "events";
    }

    @Override
    public int getOverviewWeight() {
        return 50;
    }

    @Override
    public String renderOverview(SymfonyProfilerProfile profile) {
        return formatOverview(SymfonyProfilerEventsConsumer.INSTANCE.read(profile));
    }

    @Override
    public String renderDetails(SymfonyProfilerProfile profile, int page) {
        return formatDetails(SymfonyProfilerEventsConsumer.INSTANCE.read(profile));
    }

    public String formatOverview(SymfonyProfilerEvents events) {
        StringBuilder builder = new StringBuilder();
        appendSummary(builder, events);
        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    public String formatDetails(SymfonyProfilerEvents events) {
        StringBuilder builder = new StringBuilder();
        appendSummary(builder, events);

        if (events.getDispatchers().isEmpty()) {
            builder.append('\n');
            builder.append("No event dispatchers recorded.").append('\n');
            return ProfilerDetailRendererKt.trimEnd(builder);
        }

        for (SymfonyProfilerEventDispatcher dispatcher : events.getDispatchers()) {
            builder.append('\n');
            builder.append("### Dispatcher: ").append(plainText(dispatcher.getName())).append('\n');

            if (dispatcher.getEvents().isEmpty()) {
                builder.append('\n');
                builder.append("No dispatched events recorded.").append('\n');
                continue;
            }

            for (SymfonyProfilerDispatchedEvent event : dispatcher.getEvents()) {
                builder.append('\n');
                builder.append("#### ").append(plainText(event.getName()))
                    .append(" (").append(formatListenerCount(event.getListeners().size())).append(")").append('\n');
                builder.append('\n');
                builder.append("priority,listener").append('\n');
                for (SymfonyProfilerEventListener listener : event.getListeners()) {
                    appendListener(builder, listener);
                }
            }
        }

        return ProfilerDetailRendererKt.trimEnd(builder);
    }

    private static void appendSummary(StringBuilder builder, SymfonyProfilerEvents events) {
        builder.append("## Collector: events").append('\n');
        builder.append('\n');
        builder.append("- Dispatchers: ").append(events.getDispatchers().size()).append('\n');
        builder.append("- Dispatched events: ").append(events.getDispatchedEventCount()).append('\n');
        builder.append("- Called listeners: ").append(events.getCalledListenerCount()).append('\n');
    }

    private static void appendListener(StringBuilder builder, SymfonyProfilerEventListener listener) {
        Integer priority = listener.getPriority();
        builder.append(
            ProfilerTextRenderer.INSTANCE.csvRow(
                priority != null ? priority.toString() : "",
                listener.getListener()
            )
        ).append('\n');
    }

    private static String formatListenerCount(int count) {
        return count + " " + (count == 1 ? "listener" : "listeners");
    }

    private static String plainText(String value) {
        return CONTROL_CHARACTERS.matcher(value).replaceAll(" ")
            .replace("|", "\\|");
    }
}
