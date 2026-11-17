package service;

import model.TelemetryData;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Procesador de telemetría IoT encargado de validar el formato de las tramas,
 * interpretar las consultas de estado, almacenar el último estado de los sensores
 * y aplicar las reglas de negocio del protocolo.
 */
public class TelemetryProcessor {

    private final Map<String, TelemetryData> lastReadings = new ConcurrentHashMap<>();

    /**
     * Procesa una trama recibida por UDP y retorna la respuesta correspondiente.
     *
     * @param message Trama de texto recibida codificada en UTF-8.
     * @return Respuesta correspondiente según el protocolo.
     */
    public String process(String message) {
        // TODO Paso 1.1: Validación de formato inicial (nulo o vacío)
        if (message == null || message.trim().isEmpty()) {
            return "ERROR;INVALID_FORMAT";
        }

        String[] parts = message.split(";");

        // TODO Paso 1.2: Procesamiento de consultas de estado (STATUS;DEVICE_ID)
        if (parts[0].equals("STATUS")) {
            if (parts.length != 2) {
                return "ERROR;INVALID_FORMAT";
            }
            String deviceId = parts[1].trim();
            if (deviceId.isEmpty()) {
                return "ERROR;INVALID_FORMAT";
            }

            TelemetryData data = lastReadings.get(deviceId);
            if (data != null) {
                return "STATUS_OK;" + data.getDeviceId() + ";" + data.getSensorType() + ";" + data.getRawValue();
            } else {
                return "ERROR;DEVICE_NOT_FOUND";
            }
        }

        // TODO Paso 1.3: Validación de campos de la trama de registro (DEVICE_ID;SENSOR_TYPE;VALUE)
        if (parts.length != 3) {
            return "ERROR;INVALID_FORMAT";
        }

        String deviceId = parts[0].trim();
        String sensorType = parts[1].trim();
        String valueStr = parts[2].trim();

        if (deviceId.isEmpty() || sensorType.isEmpty() || valueStr.isEmpty()) {
            return "ERROR;INVALID_FORMAT";
        }

        double value;
        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return "ERROR;INVALID_FORMAT";
        }

        // TODO Paso 1.4: Validación de tipo de sensor soportado
        if (!sensorType.equals("TEMP") && !sensorType.equals("HUMIDITY") && !sensorType.equals("BATTERY")) {
            return "ERROR;UNKNOWN_SENSOR_TYPE";
        }

        // TODO Paso 1.5: Almacenamiento de la última lectura válida
        TelemetryData telemetryData = new TelemetryData(deviceId, sensorType, value, valueStr, System.currentTimeMillis());
        lastReadings.put(deviceId, telemetryData);

        // TODO Paso 1.6: Evaluación de reglas de negocio y construcción de respuesta
        switch (sensorType) {
            case "TEMP":
                if (value > 40.0) {
                    return "ALERT;HIGH_TEMPERATURE;" + valueStr;
                } else if (value < 0.0) {
                    return "ALERT;FREEZING_TEMPERATURE;" + valueStr;
                } else {
                    return "OK;TEMP_RECORDED;" + valueStr;
                }

            case "HUMIDITY":
                if (value > 90.0) {
                    return "ALERT;HIGH_HUMIDITY;" + valueStr;
                } else if (value < 20.0) {
                    return "ALERT;LOW_HUMIDITY;" + valueStr;
                } else {
                    return "OK;HUMIDITY_RECORDED;" + valueStr;
                }

            case "BATTERY":
                if (value < 20.0) {
                    return "ALERT;LOW_BATTERY;" + valueStr;
                } else {
                    return "OK;BATTERY_OK;" + valueStr;
                }

            default:
                return "ERROR;UNKNOWN_SENSOR_TYPE";
        }
    }

    /**
     * Retorna el mapa inmodificable con las últimas lecturas almacenadas.
     */
    public Map<String, TelemetryData> getLastReadings() {
        return Collections.unmodifiableMap(lastReadings);
    }

    /**
     * Retorna la última lectura de un dispositivo en específico.
     */
    public TelemetryData getLastReading(String deviceId) {
        return lastReadings.get(deviceId);
    }

    /**
     * Limpia los registros acumulados.
     */
    public void clear() {
        lastReadings.clear();
    }
}
