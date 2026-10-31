package lab.subscribers;

import lab.devices.DeviceAlertEventArgs;

@FunctionalInterface
public interface AlertFilter {
    boolean test(DeviceAlertEventArgs alert);
}
