package model;

import java.util.Objects;

/**
 * Modelo inmutable para almacenar la información de telemetría de un sensor.
 */
public class TelemetryData {
    private final String deviceId;
    private final String sensorType;
    private final double value;
    private final String rawValue;
    private final long timestamp;

    public TelemetryData(String deviceId, String sensorType, double value) {
        this(deviceId, sensorType, value, String.valueOf(value), System.currentTimeMillis());
    }

    public TelemetryData(String deviceId, String sensorType, double value, String rawValue, long timestamp) {
        this.deviceId = deviceId;
        this.sensorType = sensorType;
        this.value = value;
        this.rawValue = rawValue != null ? rawValue : String.valueOf(value);
        this.timestamp = timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSensorType() {
        return sensorType;
    }

    public double getValue() {
        return value;
    }

    public String getRawValue() {
        return rawValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TelemetryData)) return false;
        TelemetryData that = (TelemetryData) o;
        return Double.compare(that.value, value) == 0 &&
                Objects.equals(deviceId, that.deviceId) &&
                Objects.equals(sensorType, that.sensorType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, sensorType, value);
    }

    @Override
    public String toString() {
        return "TelemetryData{" +
                "deviceId='" + deviceId + '\'' +
                ", sensorType='" + sensorType + '\'' +
                ", value=" + value +
                ", rawValue='" + rawValue + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
