package lab.subscribers;

import lab.devices.AlertSeverity;
import lab.devices.DeviceAlertEventArgs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MaintenanceService {
    private final List<String> requests = new ArrayList<>();

    public List<String> getRequests() { return Collections.unmodifiableList(requests); }

    public void handleCriticalAlert(Object sender, DeviceAlertEventArgs e) {
        if (e.severity() != AlertSeverity.CRITICAL) {
            return; 
        }
        requests.add("Maintenance request for " + e.deviceId() + ": " + e.message());
    }
}
