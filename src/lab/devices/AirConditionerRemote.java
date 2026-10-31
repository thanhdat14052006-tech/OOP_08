// Nguyen Thanh Dat - 202419040
package lab.devices;

public class AirConditionerRemote extends TeachingDevice {
    private final String assignedRoom;
    private boolean responsive = true;

    public AirConditionerRemote(String id, String name, String assignedRoom) {
        super(id, name);
        this.assignedRoom = assignedRoom;
    }

    public String getAssignedRoom() { return assignedRoom; }
    public boolean isResponsive() { return responsive; }

    public void updateResponseState(boolean isResponsive) {
        boolean wasResponsive = responsive;
        responsive = isResponsive;
        if (wasResponsive && !isResponsive) {
            raiseAlert("Điều hòa phòng " + assignedRoom + " không phản hồi", AlertSeverity.CRITICAL);
        }
    }
}
