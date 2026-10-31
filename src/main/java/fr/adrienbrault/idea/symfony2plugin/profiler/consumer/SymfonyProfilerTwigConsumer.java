package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArray;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpArrayEntry;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpCustomObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpFloat;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpInteger;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpObject;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpString;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeLimits;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeResult;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializer;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValueKt;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/** Reads Twig render metrics, unique templates, and the complete rendering profile tree. */
public final class SymfonyProfilerTwigConsumer {
    public static final SymfonyProfilerTwigConsumer INSTANCE = new SymfonyProfilerTwigConsumer();

    private static final Set<String> TWIG_COLLECTORS = Set.of(
        "Symfony\\Bridge\\Twig\\DataCollector\\TwigDataCollector",
        "Symfony\\Bundle\\TwigBundle\\DataCollector\\TwigDataCollector"
    );
    private static final String PROFILE_TYPE_TEMPLATE = "template";
    private static final String PROFILE_TYPE_BLOCK = "block";
    private static final String PROFILE_TYPE_MACRO = "macro";
    private static final String PROFILE_TYPE_ROOT = "ROOT";

    private SymfonyProfilerTwigConsumer() {
    }

    public SymfonyProfilerTwig read(SymfonyProfilerProfile profile) {
        PhpObject collector = profile.collector("twig");
        if (collector == null) {
            throw new IllegalStateException("Symfony profile does not contain the twig collector");
        }
        String className = collector.getClassName().utf8StringOrNull();
        if (className == null || !TWIG_COLLECTORS.contains(className)) {
            throw new IllegalStateException("Symfony twig collector has an unsupported class");
        }

        if (!(SymfonyProfilerProfileKt.resolve(collector.get("data"), profile.getResult()) instanceof PhpArray data)) {
            throw new IllegalStateException("Symfony twig collector does not contain its data array");
        }
        Map<String, String> templatePaths = readTemplatePaths(data.get("template_paths"), profile.getResult());
        SymfonyProfilerTwigProfile root = new ProfileDecoder().decode(data.get("profile"), profile.getResult());
        if (!PROFILE_TYPE_ROOT.equals(root.getType())) {
            throw new IllegalStateException("Symfony twig profile does not have a root node");
        }
        List<SymfonyProfilerTwigProfile> allProfiles = new ArrayList<>();
        depthFirst(root, allProfiles);
        List<SymfonyProfilerTwigProfile> profiles = allProfiles.subList(1, allProfiles.size());
        Map<String, SymfonyProfilerTwigTemplate> renderedTemplates = new LinkedHashMap<>();

        int templateCallCount = 0;
        int blockCallCount = 0;
        int macroCallCount = 0;
        for (SymfonyProfilerTwigProfile rendered : profiles) {
            if (PROFILE_TYPE_TEMPLATE.equals(rendered.getType())) {
                templateCallCount++;

                SymfonyProfilerTwigTemplate existing = renderedTemplates.get(rendered.getTemplate());
                renderedTemplates.put(rendered.getTemplate(), new SymfonyProfilerTwigTemplate(
                    rendered.getTemplate(),
                    templatePaths.get(rendered.getTemplate()),
                    (existing != null ? existing.getRenderCount() : 0) + 1
                ));
            }
            else if (PROFILE_TYPE_BLOCK.equals(rendered.getType())) {
                blockCallCount++;
            }
            else if (PROFILE_TYPE_MACRO.equals(rendered.getType())) {
                macroCallCount++;
            }
        }

        return new SymfonyProfilerTwig(
            root.getDurationMs(),
            templateCallCount,
            blockCallCount,
            macroCallCount,
            new ArrayList<>(renderedTemplates.values()),
            root
        );
    }

    private Map<String, String> readTemplatePaths(@Nullable PhpValue value, PhpUnserializeResult result) {
        if (!(SymfonyProfilerProfileKt.resolve(value, result) instanceof PhpArray paths)) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new LinkedHashMap<>();
        for (PhpArrayEntry entry : paths.getEntries()) {
            String template = entry.getKey() instanceof PhpStringKey stringKey ? stringKey.getBytes().utf8StringOrNull() : null;
            if (template == null) {
                continue;
            }
            String path = utf8StringOrNull(SymfonyProfilerProfileKt.resolve(entry.getValue(), result));
            if (path == null) {
                continue;
            }
            map.put(template, path);
        }
        return map;
    }

    private static void depthFirst(SymfonyProfilerTwigProfile profile, List<SymfonyProfilerTwigProfile> result) {
        result.add(profile);
        for (SymfonyProfilerTwigProfile child : profile.getChildren()) {
            depthFirst(child, result);
        }
    }

    private static @Nullable String utf8StringOrNull(@Nullable PhpValue value) {
        return value != null ? PhpValueKt.utf8StringOrNull(value) : null;
    }

    private static final class ProfileDecoder {
        private static final Set<String> TWIG_PROFILE_CLASSES = Set.of("Twig\\Profiler\\Profile", "Twig_Profiler_Profile");

        private final PhpUnserializeLimits myLimits = new PhpUnserializeLimits();
        private final Set<PhpValue> myActiveProfiles = Collections.newSetFromMap(new IdentityHashMap<>());
        private int myProfileCount = 0;
        private int myNestedPayloadBytes = 0;

        SymfonyProfilerTwigProfile decode(@Nullable PhpValue value, PhpUnserializeResult result) {
            PhpValue resolved = SymfonyProfilerProfileKt.resolve(value, result);
            if (resolved == null) {
                throw new IllegalStateException("Symfony twig collector does not contain its serialized profile");
            }
            if (resolved instanceof PhpString string) {
                PhpUnserializeResult nestedResult = PhpUnserializer.unserialize(string.getBytes().toByteArray(), myLimits);
                return decodeProfile(nestedResult.getRoot(), nestedResult, 0);
            }

            return decodeProfile(resolved, result, 0);
        }

        private SymfonyProfilerTwigProfile decodeProfile(
            PhpValue value,
            PhpUnserializeResult result,
            int depth
        ) {
            if (depth > myLimits.getMaxNestingDepth()) {
                throw new IllegalStateException("Symfony twig profile nesting is too deep");
            }
            if (++myProfileCount > myLimits.getMaxContainerEntries()) {
                throw new IllegalStateException("Symfony twig profile contains too many nodes");
            }

            PhpValue resolved = SymfonyProfilerProfileKt.resolve(value, result);
            if (resolved == null) {
                throw new IllegalStateException("Symfony twig profile node is missing");
            }
            if (!myActiveProfiles.add(resolved)) {
                throw new IllegalStateException("Symfony twig profile contains a reference cycle");
            }
            try {
                if (resolved instanceof PhpObject object) {
                    return decodeObject(object, result, depth);
                }
                if (resolved instanceof PhpCustomObject customObject) {
                    return decodeCustomObject(customObject, depth);
                }
                throw new IllegalStateException("Symfony twig profile node has an unsupported value");
            }
            finally {
                myActiveProfiles.remove(resolved);
            }
        }

        private SymfonyProfilerTwigProfile decodeObject(
            PhpObject profile,
            PhpUnserializeResult result,
            int depth
        ) {
            String className = profile.getClassName().utf8StringOrNull();
            if (className == null || !TWIG_PROFILE_CLASSES.contains(className)) {
                throw new IllegalStateException("Symfony twig profile has an unsupported class");
            }
            return decodeFields(
                (index, name) -> {
                    PhpValue indexed = profile.get((long) index);
                    return indexed != null ? indexed : profile.get(name);
                },
                result,
                depth
            );
        }

        private SymfonyProfilerTwigProfile decodeCustomObject(
            PhpCustomObject profile,
            int depth
        ) {
            String className = profile.getClassName().utf8StringOrNull();
            if (className == null || !TWIG_PROFILE_CLASSES.contains(className)) {
                throw new IllegalStateException("Symfony twig profile has an unsupported class");
            }
            myNestedPayloadBytes += profile.getPayload().getSize();
            if (myNestedPayloadBytes > myLimits.getMaxInputBytes()) {
                throw new IllegalStateException("Symfony twig profile nested payload exceeds the parse limit");
            }

            PhpUnserializeResult payloadResult = PhpUnserializer.unserialize(profile.getPayload().toByteArray(), myLimits);
            if (!(payloadResult.getRoot() instanceof PhpArray fields)) {
                throw new IllegalStateException("Symfony twig legacy profile payload is not an array");
            }
            return decodeFields(
                (index, name) -> fields.get((long) index),
                payloadResult,
                depth
            );
        }

        private SymfonyProfilerTwigProfile decodeFields(
            BiFunction<Integer, String, @Nullable PhpValue> field,
            PhpUnserializeResult result,
            int depth
        ) {
            String template = requireText(SymfonyProfilerProfileKt.resolve(field.apply(0, "template"), result), "template");
            String name = requireText(SymfonyProfilerProfileKt.resolve(field.apply(1, "name"), result), "name");
            String type = requireText(SymfonyProfilerProfileKt.resolve(field.apply(2, "type"), result), "type");
            PhpArray starts = SymfonyProfilerProfileKt.resolve(field.apply(3, "starts"), result) instanceof PhpArray array ? array : null;
            PhpArray ends = SymfonyProfilerProfileKt.resolve(field.apply(4, "ends"), result) instanceof PhpArray array ? array : null;
            PhpArray childrenData = SymfonyProfilerProfileKt.resolve(field.apply(5, "profiles"), result) instanceof PhpArray array
                ? array
                : null;
            List<SymfonyProfilerTwigProfile> children = new ArrayList<>();
            if (childrenData != null) {
                for (PhpArrayEntry child : childrenData.getEntries()) {
                    children.add(decodeProfile(child.getValue(), result, depth + 1));
                }
            }

            Double endsWallTime = ends != null ? number(ends, "wt", result) : null;
            Double startsWallTime = starts != null ? number(starts, "wt", result) : null;
            double measuredDurationMs = ((endsWallTime != null ? endsWallTime : 0.0) -
                (startsWallTime != null ? startsWallTime : 0.0)) * 1000.0;
            double durationMs;
            if (PROFILE_TYPE_ROOT.equals(type) && !children.isEmpty()) {
                durationMs = 0.0;
                for (SymfonyProfilerTwigProfile child : children) {
                    durationMs += child.getDurationMs();
                }
            }
            else {
                durationMs = measuredDurationMs < 0.0 ? 0.0 : measuredDurationMs;
            }
            if (!Double.isFinite(durationMs)) {
                throw new IllegalStateException("Symfony twig profile contains a non-finite duration");
            }

            return new SymfonyProfilerTwigProfile(
                template,
                type,
                name,
                durationMs,
                children
            );
        }

        private static @Nullable Double number(PhpArray array, String key, PhpUnserializeResult result) {
            PhpValue value = SymfonyProfilerProfileKt.resolve(array.get(key), result);
            if (value instanceof PhpFloat floatValue) {
                return floatValue.getValue();
            }
            if (value instanceof PhpInteger integer) {
                return (double) integer.getValue();
            }
            return null;
        }

        private static String requireText(@Nullable PhpValue value, String field) {
            String text = utf8StringOrNull(value);
            if (text == null) {
                throw new IllegalStateException("Symfony twig profile " + field + " is not UTF-8 text");
            }
            return text;
        }
    }
}
