package fr.adrienbrault.idea.symfony2plugin.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.util.SimpleModificationTracker;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.ex.awt.util.Alarm;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Watches Symfony cache-related paths and exposes per-scope invalidation trackers.
 * <p>
 * Initialized on project startup and refreshed on Symfony settings changes.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class SymfonyVarDirectoryWatcher implements Disposable {

    /**
     * Cache invalidation scopes for compiled Symfony artifacts.
     */
    public enum Scope {
        CONTAINER,
        TRANSLATIONS,
        ROUTES,
    }

    private final Project myProject;

    /**
     * Stable trackers that consumers can keep as dependencies.
     */
    private final Map<Scope, SimpleModificationTracker> myTrackers;

    /**
     * Active VFS listener connection while the plugin is enabled for the project.
     */
    private volatile @Nullable MessageBusConnection myListenerConnection;

    private final Alarm myDebounceAlarm;
    private final Set<Scope> myPendingScopes = Collections.synchronizedSet(EnumSet.noneOf(Scope.class));

    @Inject
    public SymfonyVarDirectoryWatcher(Project project) {
        myProject = project;

        Map<Scope, SimpleModificationTracker> trackers = new EnumMap<>(Scope.class);
        trackers.put(Scope.CONTAINER, new SimpleModificationTracker());
        trackers.put(Scope.TRANSLATIONS, new SimpleModificationTracker());
        trackers.put(Scope.ROUTES, new SimpleModificationTracker());
        myTrackers = trackers;

        myDebounceAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);

        refreshSubscription();
    }

    @Override
    public void dispose() {
    }

    /**
     * Returns the tracker for the given invalidation scope.
     */
    public SimpleModificationTracker getModificationTracker(Scope scope) {
        return getTracker(scope);
    }

    /**
     * Rebuilds the watcher subscription from current settings and invalidates all scopes.
     */
    public void reloadConfiguration() {
        refreshSubscription();
        for (Scope scope : myTrackers.keySet()) {
            invalidate(scope);
        }
    }

    /**
     * Subscribes a VFS listener for the provided scope snapshot.
     */
    private MessageBusConnection subscribe(List<SymfonyVarDirectoryWatchScopeUtil.ScopeEntry> scopes) {
        MessageBusConnection connection = myProject.getMessageBus().connect(this);
        connection.subscribe(BulkFileListener.class, new BulkFileListener() {
            @Override
            public void after(List<? extends VFileEvent> events) {
                Set<Scope> toInvalidate = new LinkedHashSet<>();

                for (VFileEvent event : events) {
                    // Rename events need both old and new names to catch moves in either direction.
                    List<String> rawPaths = new ArrayList<>();
                    if (event instanceof VFilePropertyChangeEvent propertyChangeEvent && VirtualFile.PROP_NAME.equals(propertyChangeEvent.getPropertyName())) {
                        VirtualFile parentFile = propertyChangeEvent.getFile().getParent();
                        String parent = parentFile != null ? parentFile.getPath() : "";
                        rawPaths.add(event.getPath());
                        rawPaths.add(parent + "/" + propertyChangeEvent.getOldValue());
                    }
                    else {
                        rawPaths.add(event.getPath());
                    }

                    List<String> paths = new ArrayList<>();
                    for (String rawPath : rawPaths) {
                        String normalizedPath = SymfonyVarDirectoryWatchScopeUtil.INSTANCE.normalizePath(rawPath);
                        if (SymfonyVarDirectoryWatchScopeUtil.INSTANCE.isCachePath(normalizedPath)) {
                            paths.add(normalizedPath);
                        }
                    }

                    if (paths.isEmpty()) {
                        continue;
                    }

                    Set<Scope> matchedScopes = SymfonyVarDirectoryWatchScopeUtil.INSTANCE.collectScopesForPaths(scopes, paths);
                    toInvalidate.addAll(matchedScopes);
                }

                // Debounce: accumulate matched scopes and reschedule the flush so rapid
                // bursts of VFS events (e.g. a Symfony cache warm-up) produce a single invalidation.
                if (!toInvalidate.isEmpty()) {
                    myPendingScopes.addAll(toInvalidate);
                    myDebounceAlarm.cancelAllRequests();
                    myDebounceAlarm.addRequest(SymfonyVarDirectoryWatcher.this::flushPendingInvalidations, 300);
                }
            }
        });

        return connection;
    }

    /**
     * Drains {@link #myPendingScopes} and fires one {@link #invalidate} call per scope.
     */
    private void flushPendingInvalidations() {
        Set<Scope> scopes;
        synchronized (myPendingScopes) {
            if (myPendingScopes.isEmpty()) {
                return;
            }
            scopes = EnumSet.copyOf(myPendingScopes);
            myPendingScopes.clear();
        }
        for (Scope scope : scopes) {
            invalidate(scope);
        }
    }

    private void refreshSubscription() {
        myDebounceAlarm.cancelAllRequests();
        MessageBusConnection listenerConnection = myListenerConnection;
        if (listenerConnection != null) {
            listenerConnection.disconnect();
        }
        myListenerConnection = null;

        if (!Symfony2ProjectComponent.isEnabled(myProject)) {
            return;
        }

        myListenerConnection = subscribe(detectRoots());
    }

    private List<SymfonyVarDirectoryWatchScopeUtil.ScopeEntry> detectRoots() {
        VirtualFile projectDir = ProjectUtil.getProjectDir(myProject);
        if (projectDir == null) {
            return Collections.emptyList();
        }
        return SymfonyVarDirectoryWatchScopeUtil.INSTANCE.createScopes(projectDir.getPath(), Settings.getInstance(myProject));
    }

    private void invalidate(Scope scope) {
        getTracker(scope).incModificationCount();
    }

    private SimpleModificationTracker getTracker(Scope scope) {
        SimpleModificationTracker tracker = myTrackers.get(scope);
        if (tracker == null) {
            throw new java.util.NoSuchElementException("Key " + scope + " is missing in the map.");
        }
        return tracker;
    }
}
