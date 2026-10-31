package lab.devices;

public class InvalidDeviceReadingException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String deviceId;
    private final int invalidValue;

    public InvalidDeviceReadingException(String deviceId, int invalidValue, String what) {
        super(what + " không hợp lệ (" + invalidValue + ") cho thiết bị " + deviceId + ", phải trong khoảng 0-100");
        this.deviceId = deviceId;
        this.invalidValue = invalidValue;
    }

    public String getDeviceId() { return deviceId; }
    public int getInvalidValue() { return invalidValue; }
}
