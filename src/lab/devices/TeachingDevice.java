package lab.devices;

import lab.events.Event;
import lab.events.EventPublisher;

import java.time.LocalDateTime;

public abstract class TeachingDevice {
    private final String id;
    private final String name;
    private DeviceStatus status;

    private final EventPublisher<DeviceStatusChangedEventArgs> statusChanged = new EventPublisher<>();
    private final EventPublisher<DeviceAlertEventArgs> alertRaised = new EventPublisher<>();

    protected TeachingDevice(String id, String name) {
        this.id = id;
        this.name = name;
        this.status = DeviceStatus.AVAILABLE;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public DeviceStatus getStatus() { return status; }

    public Event<DeviceStatusChangedEventArgs> statusChanged() { return statusChanged.event(); }
    public Event<DeviceAlertEventArgs> alertRaised() { return alertRaised.event(); }

    public void changeStatus(DeviceStatus newStatus) {
        if (newStatus == status) {
            return; 
        }
        DeviceStatus oldStatus = status;
        status = newStatus;
        onStatusChanged(new DeviceStatusChangedEventArgs(id, oldStatus, newStatus, LocalDateTime.now()));
    }

    protected void raiseAlert(String message, AlertSeverity severity) {
        onAlertRaised(new DeviceAlertEventArgs(id, message, severity, LocalDateTime.now()));
    }

    protected void onStatusChanged(DeviceStatusChangedEventArgs e) {
        statusChanged.raise(this, e);
    }

    protected void onAlertRaised(DeviceAlertEventArgs e) {
        alertRaised.raise(this, e);
    }

    protected void validatePercent(int value, String what) {
        if (value < 0 || value > 100) {
            throw new InvalidDeviceReadingException(id, value, what);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + ", " + name + ", " + status + "]";
    }
}
