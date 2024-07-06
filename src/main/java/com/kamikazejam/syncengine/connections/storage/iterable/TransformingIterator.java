package com.kamikazejam.syncengine.connections.storage.iterable;

import java.util.Iterator;
import java.util.function.Function;

public class TransformingIterator<S, T> implements Iterator<T> {
    private final Iterator<S> sourceIterator;
    private final Function<S, T> transformer;

    public TransformingIterator(Iterator<S> sourceIterator, Function<S, T> transformer) {
        this.sourceIterator = sourceIterator;
        this.transformer = transformer;
    }

    @Override
    public boolean hasNext() {
        return sourceIterator.hasNext();
    }

    @Override
    public T next() {
        S sourceElement = sourceIterator.next();
        return transformer.apply(sourceElement);
    }
}

