package io.kamax.mxisd.hash.rotation;

import io.kamax.mxisd.hash.engine.Engine;

import java.util.concurrent.atomic.AtomicInteger;

public class RotationPerRequests implements HashRotationStrategy {

    private Engine engine;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final int barrier;

    public RotationPerRequests(int barrier) {
        this.barrier = barrier;
    }

    @Override
    public void register(Engine engine) {
        this.engine = engine;
        trigger();
    }

    @Override
    public Engine getHashEngine() {
        return engine;
    }

    @Override
    public synchronized void newRequest() {
        int newValue = counter.incrementAndGet();
        if (newValue >= barrier) {
            counter.set(0);
            trigger();
        }
    }
}
