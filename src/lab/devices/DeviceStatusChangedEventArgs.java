// Nguyen Thanh Dat - 202419040
package lab.devices;

import java.time.LocalDateTime;

public record DeviceStatusChangedEventArgs(
        String deviceId,
        DeviceStatus oldStatus,
        DeviceStatus newStatus,
        LocalDateTime changedAt) {
}
