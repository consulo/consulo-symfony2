package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEntry;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.SymfonyVarDumperDataReader;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reads dispatched events while preserving dispatcher, event-list, and listener order. */
public final class SymfonyProfilerEventsConsumer {
    public static final SymfonyProfilerEventsConsumer INSTANCE = new SymfonyProfilerEventsConsumer();

    private static final String EVENT_COLLECTOR =
        "Symfony\\Component\\HttpKernel\\DataCollector\\EventDataCollector";
    private static final String DEFAULT_DISPATCHER = "event_dispatcher";

    private SymfonyProfilerEventsConsumer() {
    }

    public SymfonyProfilerEvents read(SymfonyProfilerProfile profile) {
        PhpObject collector = profile.collector("events");
        if (collector == null) {
            throw new IllegalStateException("Symfony profile does not contain the events collector");
        }
        if (!EVENT_COLLECTOR.equals(collector.getClassName().utf8StringOrNull())) {
            throw new IllegalStateException("Symfony events collector has an unsupported class");
        }

        PhpValue rawData = collector.get("data");
        if (rawData == null) {
            throw new IllegalStateException("Symfony events collector does not contain its data value");
        }
        if (!(new SymfonyVarDumperDataReader(profile.getResult()).read(rawData) instanceof ProfilerArray data)) {
            throw new IllegalStateException("Symfony events collector data is not an array");
        }

        // Symfony <= 6.2 stores listener lists directly; Symfony >= 6.3 groups them by dispatcher.
        List<SymfonyProfilerEventDispatcher> dispatchers;
        if (data.get("called_listeners") != null) {
            dispatchers = Collections.singletonList(readDispatcher(DEFAULT_DISPATCHER, data));
        }
        else {
            dispatchers = new ArrayList<>(data.getEntries().size());
            for (ProfilerEntry entry : data.getEntries()) {
                String name = entry.getKey() instanceof PhpStringKey stringKey ? stringKey.getBytes().utf8StringOrNull() : null;
                if (name == null) {
                    throw new IllegalStateException("Symfony event dispatcher name is not UTF-8 text");
                }
                if (!(entry.getValue() instanceof ProfilerArray dispatcher)) {
                    throw new IllegalStateException("Symfony event dispatcher data is not an array");
                }
                dispatchers.add(readDispatcher(name, dispatcher));
            }
        }

        return new SymfonyProfilerEvents(dispatchers);
    }

    private SymfonyProfilerEventDispatcher readDispatcher(String name, ProfilerArray data) {
        if (!(data.get("called_listeners") instanceof ProfilerArray calledListeners)) {
            return new SymfonyProfilerEventDispatcher(name, Collections.emptyList());
        }
        List<MutableEvent> events = new ArrayList<>();

        for (ProfilerEntry entry : calledListeners.getEntries()) {
            if (!(entry.getValue() instanceof ProfilerArray listenerData)) {
                throw new IllegalStateException("Symfony called event listener data is not an array");
            }
            String eventName = text(listenerData, "event");
            if (eventName == null) {
                throw new IllegalStateException("Symfony called event listener does not contain its event name");
            }

            Integer priority = null;
            if (listenerData.get("priority") instanceof ProfilerInteger priorityValue
                && priorityValue.getValue() >= Integer.MIN_VALUE && priorityValue.getValue() <= Integer.MAX_VALUE) {
                priority = (int) priorityValue.getValue();
            }

            String listenerName = text(listenerData, "stub");
            if (listenerName == null) {
                String pretty = text(listenerData, "pretty");
                if (pretty != null) {
                    listenerName = callableName(pretty);
                }
            }
            if (listenerName == null) {
                throw new IllegalStateException("Symfony called event listener does not contain its callable");
            }

            SymfonyProfilerEventListener listener = new SymfonyProfilerEventListener(priority, listenerName);

            MutableEvent event = events.isEmpty() ? null : events.get(events.size() - 1);
            if (event == null || !event.myName.equals(eventName)) {
                event = new MutableEvent(eventName);
                events.add(event);
            }
            event.myListeners.add(listener);
        }

        List<SymfonyProfilerDispatchedEvent> dispatchedEvents = new ArrayList<>(events.size());
        for (MutableEvent event : events) {
            dispatchedEvents.add(new SymfonyProfilerDispatchedEvent(event.myName, new ArrayList<>(event.myListeners)));
        }

        return new SymfonyProfilerEventDispatcher(name, dispatchedEvents);
    }

    private static String callableName(String value) {
        return value.endsWith(")") ? value : value + "()";
    }

    private static @Nullable String text(ProfilerArray array, String key) {
        return array.get(key) instanceof ProfilerString string ? string.utf8StringOrNull() : null;
    }

    private static final class MutableEvent {
        private final String myName;
        private final List<SymfonyProfilerEventListener> myListeners = new ArrayList<>();

        private MutableEvent(String name) {
            myName = name;
        }
    }
}
