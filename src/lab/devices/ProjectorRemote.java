package lab.devices;

public class ProjectorRemote extends TeachingDevice {
    private static final int LOW_THRESHOLD = 20;
    private int batteryLevel = 100;

    public ProjectorRemote(String id, String name) { super(id, name); }

    public int getBatteryLevel() { return batteryLevel; }

    public void updateBatteryLevel(int newLevel) {
        validatePercent(newLevel, "Mức pin");   // sai thì ném exception, KHÔNG đổi trạng thái
        int oldZone = zone(batteryLevel);
        int newZone = zone(newLevel);
        batteryLevel = newLevel;

        // Chỉ cảnh báo khi RƠI sang vùng xấu hơn, không lặp lại khi vẫn cùng vùng.
        if (newZone > oldZone) {
            if (newZone == 2) {
                raiseAlert("Pin remote máy chiếu đã hết (0%)", AlertSeverity.CRITICAL);
            } else {
                raiseAlert("Pin remote máy chiếu thấp: " + newLevel + "%", AlertSeverity.WARNING);
            }
        }
    }

    /** 0 = an toàn, 1 = thấp (<20), 2 = hết pin. */
    private static int zone(int level) {
        if (level == 0) return 2;
        return level < LOW_THRESHOLD ? 1 : 0;
    }
}
