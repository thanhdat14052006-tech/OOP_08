package lab.subscribers;

import lab.devices.DeviceAlertEventArgs;

import java.io.PrintStream;

public class ConsoleNotificationService {
    private final PrintStream out;

    public ConsoleNotificationService() { this(System.out); }
    public ConsoleNotificationService(PrintStream out) { this.out = out; }

    public void handleAlert(Object sender, DeviceAlertEventArgs e) {
        out.println("[" + e.severity() + "] " + e.deviceId() + ": " + e.message());
    }
}
