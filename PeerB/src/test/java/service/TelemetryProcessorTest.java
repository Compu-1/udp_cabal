package service;

import model.TelemetryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryProcessorTest {

    private TelemetryProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TelemetryProcessor();
    }

    @Test
    @DisplayName("CP-01: Registro de Temperatura - Normal, Alta y Bajo Cero")
    void testCP01_TemperatureRecording_NormalAndAlerts() {
        // Temperatura Normal (0.0 <= TEMP <= 40.0)
        assertEquals("OK;TEMP_RECORDED;25.0", processor.process("sensor-01;TEMP;25.0"));
        assertEquals("OK;TEMP_RECORDED;0.0", processor.process("sensor-01;TEMP;0.0"));
        assertEquals("OK;TEMP_RECORDED;40.0", processor.process("sensor-01;TEMP;40.0"));

        // Alerta Temperatura Alta (TEMP > 40.0)
        assertEquals("ALERT;HIGH_TEMPERATURE;42.5", processor.process("sensor-01;TEMP;42.5"));
        assertEquals("ALERT;HIGH_TEMPERATURE;40.1", processor.process("sensor-01;TEMP;40.1"));

        // Alerta Temperatura Bajo Cero (TEMP < 0.0)
        assertEquals("ALERT;FREEZING_TEMPERATURE;-5.0", processor.process("sensor-01;TEMP;-5.0"));
        assertEquals("ALERT;FREEZING_TEMPERATURE;-0.1", processor.process("sensor-01;TEMP;-0.1"));

        // Verificación de almacenamiento
        TelemetryData stored = processor.getLastReading("sensor-01");
        assertNotNull(stored);
        assertEquals("sensor-01", stored.getDeviceId());
        assertEquals("TEMP", stored.getSensorType());
        assertEquals(-0.1, stored.getValue(), 0.0001);
    }

    @Test
    @DisplayName("CP-02: Registro de Humedad - Normal, Crítica Alta y Crítica Baja")
    void testCP02_HumidityRecording_NormalAndAlerts() {
        // Humedad Normal (20.0 <= HUMIDITY <= 90.0)
        assertEquals("OK;HUMIDITY_RECORDED;60.0", processor.process("sensor-02;HUMIDITY;60.0"));
        assertEquals("OK;HUMIDITY_RECORDED;20.0", processor.process("sensor-02;HUMIDITY;20.0"));
        assertEquals("OK;HUMIDITY_RECORDED;90.0", processor.process("sensor-02;HUMIDITY;90.0"));

        // Humedad Crítica Alta (HUMIDITY > 90.0)
        assertEquals("ALERT;HIGH_HUMIDITY;93.0", processor.process("sensor-02;HUMIDITY;93.0"));
        assertEquals("ALERT;HIGH_HUMIDITY;90.1", processor.process("sensor-02;HUMIDITY;90.1"));

        // Humedad Crítica Baja (HUMIDITY < 20.0)
        assertEquals("ALERT;LOW_HUMIDITY;18.0", processor.process("sensor-02;HUMIDITY;18.0"));
        assertEquals("ALERT;LOW_HUMIDITY;19.9", processor.process("sensor-02;HUMIDITY;19.9"));
    }

    @Test
    @DisplayName("CP-03: Registro de Batería - Normal y Crítica")
    void testCP03_BatteryRecording_NormalAndAlerts() {
        // Batería Normal (BATTERY >= 20.0)
        assertEquals("OK;BATTERY_OK;80.0", processor.process("sensor-03;BATTERY;80.0"));
        assertEquals("OK;BATTERY_OK;20.0", processor.process("sensor-03;BATTERY;20.0"));

        // Batería Crítica (BATTERY < 20.0)
        assertEquals("ALERT;LOW_BATTERY;15.0", processor.process("sensor-03;BATTERY;15.0"));
        assertEquals("ALERT;LOW_BATTERY;19.9", processor.process("sensor-03;BATTERY;19.9"));
    }

    @Test
    @DisplayName("CP-04: Consulta de Estado Exitosa (STATUS_OK)")
    void testCP04_StatusQuery_DeviceFound() {
        processor.process("sensor-01;TEMP;25.5");
        assertEquals("STATUS_OK;sensor-01;TEMP;25.5", processor.process("STATUS;sensor-01"));

        // Actualizar con otra telemetría y consultar de nuevo
        processor.process("sensor-01;BATTERY;95.0");
        assertEquals("STATUS_OK;sensor-01;BATTERY;95.0", processor.process("STATUS;sensor-01"));
    }

    @Test
    @DisplayName("CP-05: Consulta de Estado para Dispositivo no Encontrado")
    void testCP05_StatusQuery_DeviceNotFound() {
        assertEquals("ERROR;DEVICE_NOT_FOUND", processor.process("STATUS;sensor-99"));
        assertEquals("ERROR;DEVICE_NOT_FOUND", processor.process("STATUS;desconocido"));
    }

    @Test
    @DisplayName("CP-06: Sensor no Soportado (UNKNOWN_SENSOR_TYPE)")
    void testCP06_UnsupportedSensorType() {
        assertEquals("ERROR;UNKNOWN_SENSOR_TYPE", processor.process("sensor-01;PRESSURE;1013.25"));
        assertEquals("ERROR;UNKNOWN_SENSOR_TYPE", processor.process("sensor-01;LIGHT;500.0"));
        assertEquals("ERROR;UNKNOWN_SENSOR_TYPE", processor.process("sensor-01;CO2;400.0"));
    }

    @Test
    @DisplayName("CP-07: Formato Inválido - Nulo, Vacío o Campos Faltantes")
    void testCP07_InvalidFormat_EmptyOrMissingFields() {
        assertEquals("ERROR;INVALID_FORMAT", processor.process(null));
        assertEquals("ERROR;INVALID_FORMAT", processor.process(""));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("   "));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("sensor-01"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("sensor-01;TEMP"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("sensor-01;TEMP;25.0;EXTRA"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("STATUS"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("STATUS;"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process(";TEMP;25.0"));
    }

    @Test
    @DisplayName("CP-08: Formato Inválido - Valor no Numérico")
    void testCP08_InvalidFormat_NonNumericValue() {
        assertEquals("ERROR;INVALID_FORMAT", processor.process("sensor-01;TEMP;abc"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("sensor-01;BATTERY;veinte"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("sensor-01;HUMIDITY;--"));
        assertEquals("ERROR;INVALID_FORMAT", processor.process("sensor-01;TEMP;25,5"));
    }
}
