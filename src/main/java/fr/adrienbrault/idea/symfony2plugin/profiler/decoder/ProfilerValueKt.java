package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBytes;

import java.nio.charset.StandardCharsets;

public final class ProfilerValueKt {
    private ProfilerValueKt() {
    }

    public static ProfilerString profilerString(String value) {
        return new ProfilerString(new PhpBytes(value.getBytes(StandardCharsets.UTF_8)));
    }
}
