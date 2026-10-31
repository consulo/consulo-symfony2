package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEntry;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue;
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.SymfonyVarDumperDataReader;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Reads the summary and message rows shown by Symfony's translation profiler panel. */
public final class SymfonyProfilerTranslationConsumer {
    public static final SymfonyProfilerTranslationConsumer INSTANCE = new SymfonyProfilerTranslationConsumer();

    private static final Set<String> TRANSLATION_COLLECTORS = Set.of(
        "Symfony\\Component\\Translation\\DataCollector\\TranslationDataCollector",
        "Symfony\\Bundle\\FrameworkBundle\\DataCollector\\TranslationDataCollector"
    );

    private SymfonyProfilerTranslationConsumer() {
    }

    public SymfonyProfilerTranslation read(SymfonyProfilerProfile profile) {
        PhpObject collector = profile.collector("translation");
        if (collector == null) {
            throw new IllegalStateException("Symfony profile does not contain the translation collector");
        }
        String className = collector.getClassName().utf8StringOrNull();
        if (className == null || !TRANSLATION_COLLECTORS.contains(className)) {
            throw new IllegalStateException("Symfony translation collector has an unsupported class");
        }

        PhpValue rawData = collector.get("data");
        if (rawData == null) {
            throw new IllegalStateException("Symfony translation collector does not contain its data value");
        }
        if (!(new SymfonyVarDumperDataReader(profile.getResult()).read(rawData) instanceof ProfilerArray data)) {
            throw new IllegalStateException("Symfony translation collector data is not an array");
        }
        List<SymfonyProfilerTranslationMessage> messages = readMessages(data.get("messages"));

        return new SymfonyProfilerTranslation(
            text(data, "locale"),
            readStringList(data.get("fallback_locales")),
            stateCount(data, SymfonyProfilerTranslationState.DEFINED, messages),
            stateCount(data, SymfonyProfilerTranslationState.MISSING, messages),
            stateCount(data, SymfonyProfilerTranslationState.FALLBACK, messages),
            messages
        );
    }

    private List<SymfonyProfilerTranslationMessage> readMessages(@Nullable ProfilerValue value) {
        if (!(value instanceof ProfilerArray messages)) {
            return Collections.emptyList();
        }

        List<SymfonyProfilerTranslationMessage> result = new ArrayList<>();
        for (ProfilerEntry entry : messages.getEntries()) {
            if (!(entry.getValue() instanceof ProfilerArray message)) {
                continue;
            }

            String fallbackLocale = text(message, "fallbackLocale");
            if (fallbackLocale == null) {
                fallbackLocale = text(message, "fallback_locale");
            }

            result.add(new SymfonyProfilerTranslationMessage(
                text(message, "locale"),
                fallbackLocale,
                text(message, "domain"),
                text(message, "id"),
                text(message, "translation"),
                SymfonyProfilerTranslationState.Companion.from(message.get("state")),
                message.get("count") instanceof ProfilerInteger count ? Math.max(count.getValue(), 1L) : 1L
            ));
        }
        return result;
    }

    private List<String> readStringList(@Nullable ProfilerValue value) {
        if (!(value instanceof ProfilerArray array)) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (ProfilerEntry entry : array.getEntries()) {
            if (entry.getValue() instanceof ProfilerString string) {
                String text = string.utf8StringOrNull();
                if (text != null) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    private int stateCount(
        ProfilerArray array,
        SymfonyProfilerTranslationState state,
        List<SymfonyProfilerTranslationMessage> messages
    ) {
        ProfilerInteger count = null;
        for (ProfilerEntry entry : array.getEntries()) {
            if (entry.getKey() instanceof PhpIntegerKey integerKey && integerKey.getValue() == (long) state.getCode()) {
                count = entry.getValue() instanceof ProfilerInteger integer ? integer : null;
                break;
            }
        }

        if (count != null) {
            return (int) Math.min(Math.max(count.getValue(), 0L), (long) Integer.MAX_VALUE);
        }

        int messageCount = 0;
        for (SymfonyProfilerTranslationMessage message : messages) {
            if (message.getState() == state) {
                messageCount++;
            }
        }
        return messageCount;
    }

    private static @Nullable String text(ProfilerArray array, String key) {
        return array.get(key) instanceof ProfilerString string ? string.utf8StringOrNull() : null;
    }
}
