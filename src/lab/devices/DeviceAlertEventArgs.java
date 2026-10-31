package lab.devices;

import java.time.LocalDateTime;

public record DeviceAlertEventArgs(
        String deviceId,
        String message,
        AlertSeverity severity,
        LocalDateTime occurredAt) {
}
