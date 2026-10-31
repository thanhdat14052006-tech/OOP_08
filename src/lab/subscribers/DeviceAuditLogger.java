package lab.subscribers;

import lab.devices.DeviceStatusChangedEventArgs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeviceAuditLogger {
    private final List<String> entries = new ArrayList<>();

    public List<String> getEntries() { return Collections.unmodifiableList(entries); }

    public void handleStatusChanged(Object sender, DeviceStatusChangedEventArgs e) {
        entries.add(e.changedAt() + ": " + e.deviceId() + ", " + e.oldStatus() + " -> " + e.newStatus());
    }
}
