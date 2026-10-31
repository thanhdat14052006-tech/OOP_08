package lab.subscribers;

import lab.devices.DeviceAlertEventArgs;
import lab.devices.DeviceStatusChangedEventArgs;
import lab.devices.TeachingDevice;
import lab.events.EventHandler;

import java.util.ArrayList;
import java.util.List;

public class DeviceMonitoringCoordinator implements AutoCloseable {
    private final List<TeachingDevice> devices = new ArrayList<>();
    private final EventHandler<DeviceAlertEventArgs> notificationHandler;
    private final EventHandler<DeviceStatusChangedEventArgs> auditHandler;
    private final EventHandler<DeviceAlertEventArgs> maintenanceHandler;
    private boolean subscribed;

    public DeviceMonitoringCoordinator(Iterable<? extends TeachingDevice> devices,
                                       ConsoleNotificationService notificationService,
                                       DeviceAuditLogger auditLogger,
                                       MaintenanceService maintenanceService) {
        devices.forEach(this.devices::add);
        this.notificationHandler = notificationService::handleAlert;
        this.auditHandler = auditLogger::handleStatusChanged;
        this.maintenanceHandler = maintenanceService::handleCriticalAlert;
    }

    public void subscribe() {
        if (subscribed) return; // không đăng ký trùng
        for (TeachingDevice d : devices) {
            d.alertRaised().add(notificationHandler);
            d.alertRaised().add(maintenanceHandler);
            d.statusChanged().add(auditHandler);
        }
        subscribed = true;
    }

    public void unsubscribe() {
        if (!subscribed) return;
        for (TeachingDevice d : devices) {
            d.alertRaised().remove(notificationHandler);
            d.alertRaised().remove(maintenanceHandler);
            d.statusChanged().remove(auditHandler);
        }
        subscribed = false;
    }

    public boolean isSubscribed() { return subscribed; }

    public void dispose() { unsubscribe(); }

    @Override
    public void close() { dispose(); }
}
