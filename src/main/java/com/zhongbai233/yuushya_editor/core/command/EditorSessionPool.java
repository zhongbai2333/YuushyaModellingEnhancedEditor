package com.zhongbai233.yuushya_editor.core.command;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.LongSupplier;

/** Fixed-size, expiring LRU pool for inactive editor histories. */
public final class EditorSessionPool<K, S> {
    private final int maximumSessions;
    private final int maximumCommands;
    private final long ttlNanos;
    private final LongSupplier ticker;
    private final LinkedHashMap<K, Entry<S>> entries = new LinkedHashMap<>(16, 0.75F, true);

    public EditorSessionPool(int maximumSessions, int maximumCommands, long ttlNanos) {
        this(maximumSessions, maximumCommands, ttlNanos, System::nanoTime);
    }

    EditorSessionPool(int maximumSessions, int maximumCommands, long ttlNanos, LongSupplier ticker) {
        if (maximumSessions <= 0 || maximumCommands <= 0 || ttlNanos <= 0L) {
            throw new IllegalArgumentException("session pool limits must be positive");
        }
        this.maximumSessions = maximumSessions;
        this.maximumCommands = maximumCommands;
        this.ttlNanos = ttlNanos;
        this.ticker = Objects.requireNonNull(ticker, "ticker");
    }

    public synchronized Optional<Session<S>> take(K key, S currentDocument,
            BiPredicate<? super S, ? super S> matches) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(currentDocument, "currentDocument");
        Objects.requireNonNull(matches, "matches");
        pruneExpired(ticker.getAsLong());
        Entry<S> entry = entries.remove(key);
        if (entry == null || !matches.test(entry.document(), currentDocument)) return Optional.empty();
        return Optional.of(new Session<>(entry.document(), entry.history()));
    }

    public synchronized void put(K key, S document, CommandStack<S> history) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(history, "history");
        long now = ticker.getAsLong();
        pruneExpired(now);
        entries.remove(key);
        if (history.size() == 0) return;
        entries.put(key, new Entry<>(document, history, now));
        enforceLimits();
    }

    public synchronized void discard(K key) {
        entries.remove(Objects.requireNonNull(key, "key"));
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized int sessionCount() {
        pruneExpired(ticker.getAsLong());
        return entries.size();
    }

    public synchronized int commandCount() {
        pruneExpired(ticker.getAsLong());
        return totalCommands();
    }

    private void pruneExpired(long now) {
        entries.entrySet().removeIf(entry -> now - entry.getValue().lastAccessNanos() >= ttlNanos);
    }

    private void enforceLimits() {
        Iterator<Map.Entry<K, Entry<S>>> iterator = entries.entrySet().iterator();
        while ((entries.size() > maximumSessions || totalCommands() > maximumCommands)
                && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private int totalCommands() {
        int result = 0;
        for (Entry<S> entry : entries.values()) result += entry.history().size();
        return result;
    }

    public record Session<S>(S document, CommandStack<S> history) {
        public Session {
            Objects.requireNonNull(document, "document");
            Objects.requireNonNull(history, "history");
        }
    }

    private record Entry<S>(S document, CommandStack<S> history, long lastAccessNanos) { }
}
