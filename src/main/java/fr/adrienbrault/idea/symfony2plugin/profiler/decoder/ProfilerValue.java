package fr.adrienbrault.idea.symfony2plugin.profiler.decoder;

/** A collector-neutral value tree produced after PHP-specific structures have been decoded. */
public sealed interface ProfilerValue permits ProfilerNull, ProfilerBoolean, ProfilerInteger, ProfilerFloat, ProfilerString, ProfilerArray,
    ProfilerEnum, ProfilerReference, ProfilerOpaque {
}
