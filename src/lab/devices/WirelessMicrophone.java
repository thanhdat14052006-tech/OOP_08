// Nguyen Thanh Dat - 202419040
package lab.devices;

public class WirelessMicrophone extends TeachingDevice {
    private static final int BATTERY_WARN = 30;
    private static final int SIGNAL_WARN = 40;
    private int batteryLevel = 100;
    private int signalQuality = 100;

    public WirelessMicrophone(String id, String name) { super(id, name); }

    public int getBatteryLevel() { return batteryLevel; }
    public int getSignalQuality() { return signalQuality; }

    public void updateBatteryLevel(int newLevel) {
        validatePercent(newLevel, "Mức pin");
        int oldZone = zone(batteryLevel, BATTERY_WARN);
        int newZone = zone(newLevel, BATTERY_WARN);
        batteryLevel = newLevel;
        if (newZone > oldZone) {
            if (newZone == 2) raiseAlert("Micro hết pin (0%)", AlertSeverity.CRITICAL);
            else raiseAlert("Pin micro thấp: " + newLevel + "%", AlertSeverity.WARNING);
        }
    }

    public void updateSignalQuality(int newQuality) {
        validatePercent(newQuality, "Chất lượng tín hiệu");
        int oldZone = zone(signalQuality, SIGNAL_WARN);
        int newZone = zone(newQuality, SIGNAL_WARN);
        signalQuality = newQuality;
        if (newZone > oldZone) {
            if (newZone == 2) raiseAlert("Micro mất tín hiệu (0%)", AlertSeverity.CRITICAL);
            else raiseAlert("Tín hiệu micro yếu: " + newQuality + "%", AlertSeverity.WARNING);
        }
    }

    /** 0 = an toàn, 1 = dưới ngưỡng, 2 = bằng 0. */
    private static int zone(int value, int threshold) {
        if (value == 0) return 2;
        return value < threshold ? 1 : 0;
    }
}
