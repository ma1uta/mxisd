package io.kamax.mxisd.hash.rotation;

import io.kamax.mxisd.hash.engine.Engine;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimeBasedRotation implements HashRotationStrategy {

    private final long delay;
    private Engine engine;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public TimeBasedRotation(long delay) {
        this.delay = delay;
    }

    @Override
    public void register(Engine engine) {
        this.engine = engine;
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));
        executorService.scheduleWithFixedDelay(this::trigger, 0, delay, TimeUnit.SECONDS);
    }

    @Override
    public Engine getHashEngine() {
        return engine;
    }

    @Override
    public void newRequest() {
    }
}
