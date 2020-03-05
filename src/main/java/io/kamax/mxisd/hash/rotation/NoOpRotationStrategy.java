package io.kamax.mxisd.hash.rotation;

import io.kamax.mxisd.hash.engine.Engine;

public class NoOpRotationStrategy implements HashRotationStrategy {

    private Engine engine;

    @Override
    public void register(Engine engine) {
        this.engine = engine;
    }

    @Override
    public Engine getHashEngine() {
        return engine;
    }

    @Override
    public void newRequest() {
        // nothing to do
    }
}
