package fr.adrienbrault.idea.symfony2plugin.profiler.collector;

import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerTwigComponent;

import java.util.Collection;

public interface TwigComponentDataCollectorInterface {
    Collection<ProfilerTwigComponent> getComponents();
}
