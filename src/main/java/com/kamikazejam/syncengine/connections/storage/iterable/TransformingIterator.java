package com.kamikazejam.syncengine.connections.storage.iterable;

import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * The transformer is slightly more complicated because we allow the transformer to return a null value.
 * When a null value is supplied, the iterable skips that element and continues to the next one.
 * As a result, we pre-compute the next element as needed in order to maintain a valid state.
 */
public class TransformingIterator<S, T> implements Iterator<T> {
    private final Iterator<S> sourceIterator;
    private final Function<S, T> transformer;

    private @Nullable T nextElement;
    private boolean hasNextElement;

    public TransformingIterator(Iterator<S> sourceIterator, Function<S, @Nullable T> transformer) {
        this.sourceIterator = sourceIterator;
        this.transformer = transformer;
        advanceToNext(); // Initialize the first valid element
    }

    private void advanceToNext() {
        while (sourceIterator.hasNext()) {
            S sourceElement = sourceIterator.next();
            nextElement = transformer.apply(sourceElement);
            if (nextElement != null) {
                hasNextElement = true;
                return;
            }
        }
        // No more valid elements
        hasNextElement = false;
    }

    @Override
    public boolean hasNext() {
        return hasNextElement;
    }

    @Override
    public T next() {
        if (!hasNextElement) {
            throw new NoSuchElementException();
        }
        T result = nextElement;
        advanceToNext(); // Prepare for the next call
        return result;
    }
}

