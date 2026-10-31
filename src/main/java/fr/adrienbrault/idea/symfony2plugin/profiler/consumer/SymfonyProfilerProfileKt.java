package fr.adrienbrault.idea.symfony2plugin.profiler.consumer;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpReference;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpUnserializeResult;
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpValue;
import org.jspecify.annotations.Nullable;

public final class SymfonyProfilerProfileKt {
    private SymfonyProfilerProfileKt() {
    }

    public static @Nullable PhpValue resolve(@Nullable PhpValue value, PhpUnserializeResult result) {
        return value instanceof PhpReference reference ? result.resolve(reference) : value;
    }
}
