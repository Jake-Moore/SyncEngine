package com.kamikazejam.syncengine.util;

import com.kamikazejam.syncengine.base.Cache;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Executes a List of caches in smart order from their dependencies, as parallel as possible.
 * @param <T> the type of Cache
 */
public class AsyncCachesExecutor<T extends Cache<?,?>> {
    public interface Execution<T extends Cache<?,?>> { void run(T cache); }


    private final Map<String, T> queue = new HashMap<>();    // Note: only remove from queue when T is completed
    private final List<CompletableFuture<String>> currentExecutions = new ArrayList<>();
    private final Execution<T> execution;
    private final Set<String> completed = new HashSet<>();
    private final long timeoutSec;

    public AsyncCachesExecutor(List<T> caches, Execution<T> execution, long timeoutSec) {
        List<T> sorted = caches.stream().sorted().toList();
        sorted.forEach(c -> queue.put(c.getName(), c));
        this.execution = execution;
        this.timeoutSec = timeoutSec;

        // Validation of dependencies
        Set<String> cacheNames = caches.stream().map(Cache::getName).collect(Collectors.toSet());
        this.queue.values().forEach(cache -> cache.getDependencyNames().forEach(dependency -> {
            if (!cacheNames.contains(dependency)) {
                throw new IllegalArgumentException("Cache " + cache.getName() + " has a dependency on " + dependency + " which does not exist!");
            }
        }));
    }

    private CompletableFuture<Void> future = new CompletableFuture<>();
    public CompletableFuture<Void> executeInOrder() {
        future = new CompletableFuture<>();
        // Bukkit.getLogger().severe("[AsyncCachesExecutor] FUTURE created");
        tryQueue();
        return future;
    }

    private final Set<String> executed = new HashSet<>();
    private void tryQueue() {
        // If there is nothing left in the queue, we are done
        if (queue.isEmpty()) {
            // Bukkit.getLogger().severe("[AsyncCachesExecutor] FUTURE completed");
            future.complete(null);
            return;
        }

        // Form a separate list to prevent concurrent modification
        (new ArrayList<>(queue.values())).forEach(c -> {
            // Happens in rare cases where quick swaps occur, it's not an error, the plr is no longer here
            if (c == null) { future.complete(null); return; }
            if (!completed.containsAll(c.getDependencyNames())) { return; } // Skip if dependencies aren't met
            if (executed.contains(c.getName())) { return; }                 // Skip if already running/ran

            // We have completed all required dependencies, so we can execute this cache
            CompletableFuture<T> f = CompletableFuture.supplyAsync(() -> {
                // Bukkit.getLogger().warning("[AsyncCachesExecutor] Running " + c.getName());
                execution.run(c);
                return c;
            }).orTimeout(timeoutSec, TimeUnit.SECONDS);
            executed.add(c.getName());

            f.whenComplete((cache, t) -> {
                try {
                    queue.remove(cache.getName());
                    completed.add(cache.getName());
                    // Bukkit.getLogger().warning("[AsyncCachesExecutor] " + cache.getName() + " completed, isDoneAlr:? " + future.isDone());
                    if (future.isDone()) { return; }

                    // If we run into an exception running the Execution, we should complete exceptionally
                    if (t != null) {
                        // Bukkit.getLogger().severe("[AsyncCachesExecutor] FUTURE completed EXCEPTIONALLY");

                        future.completeExceptionally(t);
                        currentExecutions.forEach(cf -> cf.cancel(true));
                        currentExecutions.clear();
                        return;
                    }
                    tryQueue();
                }catch (Throwable t2) {
                    t2.printStackTrace();
                }
            });
        });
    }
}
