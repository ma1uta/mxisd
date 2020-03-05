package io.kamax.mxisd.hash.rotation;

import io.kamax.mxisd.hash.engine.Engine;

public interface HashRotationStrategy {

    void register(Engine engine);

    Engine getHashEngine();

    void newRequest();

    default void trigger() {
        getHashEngine().updateHashes();
    }
}
