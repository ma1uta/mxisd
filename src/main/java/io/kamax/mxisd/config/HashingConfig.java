package io.kamax.mxisd.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HashingConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(HashingConfig.class);

    private boolean enabled = false;
    private int pepperLength = 20;
    private RotationPolicyEnum rotationPolicy;
    private HashStorageEnum hashStorageType;
    private String delay = "10s";
    private transient long delayInSeconds = 10;
    private int requests = 10;
    private List<Algorithm> algorithms = new ArrayList<>();

    public void build(MatrixConfig matrixConfig) {
        if (isEnabled()) {
            LOGGER.info("--- Hash configuration ---");
            LOGGER.info("   Pepper length: {}", getPepperLength());
            LOGGER.info("   Rotation policy: {}", getRotationPolicy());
            LOGGER.info("   Hash storage type: {}", getHashStorageType());
            if (RotationPolicyEnum.per_seconds == getRotationPolicy()) {
                setDelayInSeconds(new DurationDeserializer().deserialize(getDelay()));
                LOGGER.info("   Rotation delay: {}", getDelay());
                LOGGER.info("   Rotation delay in seconds: {}", getDelayInSeconds());
            }
            if (RotationPolicyEnum.per_requests == getRotationPolicy()) {
                LOGGER.info("   Rotation after requests: {}", getRequests());
            }
            LOGGER.info("   Algorithms: {}", getAlgorithms());
        } else {
            if (matrixConfig.isV2()) {
                LOGGER.warn("V2 enabled without the hash configuration.");
            }
            LOGGER.info("Hash configuration disabled, used only `none` pepper.");
        }
    }

    public enum Algorithm {
        none,
        sha256
    }

    public enum RotationPolicyEnum {
        per_requests,
        per_seconds
    }

    public enum HashStorageEnum {
        in_memory,
        sql
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPepperLength() {
        return pepperLength;
    }

    public void setPepperLength(int pepperLength) {
        this.pepperLength = pepperLength;
    }

    public RotationPolicyEnum getRotationPolicy() {
        return rotationPolicy;
    }

    public void setRotationPolicy(RotationPolicyEnum rotationPolicy) {
        this.rotationPolicy = rotationPolicy;
    }

    public HashStorageEnum getHashStorageType() {
        return hashStorageType;
    }

    public void setHashStorageType(HashStorageEnum hashStorageType) {
        this.hashStorageType = hashStorageType;
    }

    public String getDelay() {
        return delay;
    }

    public void setDelay(String delay) {
        this.delay = delay;
    }

    public long getDelayInSeconds() {
        return delayInSeconds;
    }

    public void setDelayInSeconds(long delayInSeconds) {
        this.delayInSeconds = delayInSeconds;
    }

    public int getRequests() {
        return requests;
    }

    public void setRequests(int requests) {
        this.requests = requests;
    }

    public List<Algorithm> getAlgorithms() {
        return algorithms;
    }

    public void setAlgorithms(List<Algorithm> algorithms) {
        this.algorithms = algorithms;
    }
}
