package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayEntry;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBoolean;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpFloat;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeResult;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValueKt;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reads Symfony time and memory metrics and orders visible events by descending duration. */
public final class SymfonyProfilerTimeConsumer {
    public static final SymfonyProfilerTimeConsumer INSTANCE = new SymfonyProfilerTimeConsumer();

    private static final String TIME_COLLECTOR =
        "Symfony\\Component\\HttpKernel\\DataCollector\\TimeDataCollector";
    private static final String MEMORY_COLLECTOR =
        "Symfony\\Component\\HttpKernel\\DataCollector\\MemoryDataCollector";
    private static final String STOPWATCH_EVENT = "Symfony\\Component\\Stopwatch\\StopwatchEvent";
    private static final String STOPWATCH_PERIOD = "Symfony\\Component\\Stopwatch\\StopwatchPeriod";
    private static final String SECTION_EVENT = "__section__";

    private SymfonyProfilerTimeConsumer() {
    }

    public SymfonyProfilerTime read(SymfonyProfilerProfile profile) {
        PhpObject collector = profile.collector("time");
        if (collector == null) {
            throw new IllegalStateException("Symfony profile does not contain the time collector");
        }
        if (!TIME_COLLECTOR.equals(collector.getClassName().utf8StringOrNull())) {
            throw new IllegalStateException("Symfony time collector has an unsupported class");
        }

        if (!(SymfonyProfilerProfileKt.resolve(collector.get("data"), profile.getResult()) instanceof PhpArray data)) {
            throw new IllegalStateException("Symfony time collector does not contain its data array");
        }
        if (!(SymfonyProfilerProfileKt.resolve(data.get("events"), profile.getResult()) instanceof PhpArray eventsData)) {
            throw new IllegalStateException("Symfony time collector does not contain its events array");
        }
        List<ParsedEvent> parsedEvents = new ArrayList<>(eventsData.getEntries().size());
        for (PhpArrayEntry entry : eventsData.getEntries()) {
            String name = entry.getKey() instanceof PhpStringKey stringKey ? stringKey.getBytes().utf8StringOrNull() : null;
            if (name == null) {
                throw new IllegalStateException("Symfony time event name is not UTF-8 text");
            }
            parsedEvents.add(readEvent(name, entry.getValue(), profile.getResult()));
        }
        ParsedEvent section = null;
        for (ParsedEvent parsedEvent : parsedEvents) {
            if (parsedEvent.event().getName().equals(SECTION_EVENT)) {
                section = parsedEvent;
                break;
            }
        }
        double startTime = finiteNumberOrZero(SymfonyProfilerProfileKt.resolve(data.get("start_time"), profile.getResult()), "start time");

        List<SymfonyProfilerTimeEvent> events = new ArrayList<>();
        for (ParsedEvent parsedEvent : parsedEvents) {
            if (!parsedEvent.event().getName().equals(SECTION_EVENT)) {
                events.add(parsedEvent.event());
            }
        }
        events.sort(Comparator.comparingDouble(SymfonyProfilerTimeEvent::getDurationMs).reversed());

        return new SymfonyProfilerTime(
            roundToTwoDecimals(
                section != null ? section.originMs() + section.rawDurationMs() - startTime : 0.0
            ),
            roundToTwoDecimals(section != null ? section.originMs() - startTime : 0.0),
            readMemory(profile),
            SymfonyProfilerProfileKt.resolve(data.get("stopwatch_installed"), profile.getResult()) instanceof PhpBoolean stopwatchInstalled
                ? stopwatchInstalled.getValue()
                : !parsedEvents.isEmpty(),
            events
        );
    }

    private @Nullable SymfonyProfilerMemory readMemory(SymfonyProfilerProfile profile) {
        PhpObject collector = profile.collector("memory");
        if (collector == null) {
            return null;
        }
        if (!MEMORY_COLLECTOR.equals(collector.getClassName().utf8StringOrNull())) {
            throw new IllegalStateException("Symfony memory collector has an unsupported class");
        }

        if (!(SymfonyProfilerProfileKt.resolve(collector.get("data"), profile.getResult()) instanceof PhpArray data)) {
            throw new IllegalStateException("Symfony memory collector does not contain its data array");
        }

        Long peakBytes = byteCountOrNull(SymfonyProfilerProfileKt.resolve(data.get("memory"), profile.getResult()));
        if (peakBytes == null) {
            throw new IllegalStateException("Symfony memory collector does not contain its peak usage");
        }
        Long limitBytes = byteCountOrNull(SymfonyProfilerProfileKt.resolve(data.get("memory_limit"), profile.getResult()));
        if (limitBytes == null) {
            throw new IllegalStateException("Symfony memory collector does not contain its PHP memory limit");
        }

        return new SymfonyProfilerMemory(Math.max(peakBytes, 0L), limitBytes);
    }

    private ParsedEvent readEvent(
        String name,
        PhpValue value,
        PhpUnserializeResult result
    ) {
        if (!(SymfonyProfilerProfileKt.resolve(value, result) instanceof PhpObject event)) {
            throw new IllegalStateException("Symfony time event is not a StopwatchEvent object");
        }
        if (!STOPWATCH_EVENT.equals(event.getClassName().utf8StringOrNull())) {
            throw new IllegalStateException("Symfony time event has an unsupported class");
        }

        if (!(SymfonyProfilerProfileKt.resolve(event.get("periods"), result) instanceof PhpArray periodsData)) {
            throw new IllegalStateException("Symfony time event does not contain its periods array");
        }
        List<StopwatchPeriod> periods = new ArrayList<>(periodsData.getEntries().size());
        for (PhpArrayEntry entry : periodsData.getEntries()) {
            if (!(SymfonyProfilerProfileKt.resolve(entry.getValue(), result) instanceof PhpObject period)) {
                throw new IllegalStateException("Symfony time period is not a StopwatchPeriod object");
            }
            if (!STOPWATCH_PERIOD.equals(period.getClassName().utf8StringOrNull())) {
                throw new IllegalStateException("Symfony time period has an unsupported class");
            }

            periods.add(new StopwatchPeriod(
                finiteNumberOrZero(SymfonyProfilerProfileKt.resolve(period.get("start"), result), "period start"),
                finiteNumberOrZero(SymfonyProfilerProfileKt.resolve(period.get("end"), result), "period end"),
                SymfonyProfilerProfileKt.resolve(period.get("memory"), result) instanceof PhpInteger memory
                    ? Math.max(memory.getValue(), 0L)
                    : 0L
            ));
        }

        double rawDuration = 0.0;
        long memoryBytes = 0L;
        for (StopwatchPeriod period : periods) {
            rawDuration += period.endMs() - period.startMs();
        }
        if (!periods.isEmpty()) {
            memoryBytes = Long.MIN_VALUE;
            for (StopwatchPeriod period : periods) {
                memoryBytes = Math.max(memoryBytes, period.memoryBytes());
            }
        }

        String category = utf8StringOrNull(SymfonyProfilerProfileKt.resolve(event.get("category"), result));
        if (category == null) {
            category = "default";
        }

        return new ParsedEvent(
            new SymfonyProfilerTimeEvent(
                name,
                category,
                periods.isEmpty() ? 0.0 : periods.get(0).startMs(),
                periods.isEmpty() ? 0.0 : periods.get(periods.size() - 1).endMs(),
                rawDuration,
                memoryBytes
            ),
            finiteNumberOrZero(SymfonyProfilerProfileKt.resolve(event.get("origin"), result), "event origin"),
            rawDuration
        );
    }

    private static double finiteNumberOrZero(@Nullable PhpValue phpValue, String field) {
        double value;
        if (phpValue instanceof PhpFloat floatValue) {
            value = floatValue.getValue();
        }
        else if (phpValue instanceof PhpInteger integer) {
            value = (double) integer.getValue();
        }
        else {
            value = 0.0;
        }
        if (!Double.isFinite(value)) {
            throw new IllegalStateException("Symfony time collector contains a non-finite " + field);
        }
        return value;
    }

    private static @Nullable Long byteCountOrNull(@Nullable PhpValue value) {
        if (value instanceof PhpInteger integer) {
            return integer.getValue();
        }
        if (value instanceof PhpFloat floatValue) {
            return Double.isFinite(floatValue.getValue()) ? (long) floatValue.getValue() : null;
        }
        return null;
    }

    private static @Nullable String utf8StringOrNull(@Nullable PhpValue value) {
        return value != null ? PhpValueKt.utf8StringOrNull(value) : null;
    }

    private static double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private record ParsedEvent(
        SymfonyProfilerTimeEvent event,
        double originMs,
        double rawDurationMs
    ) {
    }

    private record StopwatchPeriod(
        double startMs,
        double endMs,
        long memoryBytes
    ) {
    }
}
