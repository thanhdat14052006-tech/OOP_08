package lab.app;

import lab.devices.*;
import lab.events.EventHandler;
import lab.subscribers.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            failed++;
            System.out.println("  [FAIL] " + name);
        }
    }

    private static void section(String title) {
        System.out.println("\n== " + title + " ==");
    }

    private static class FakeDevice extends TeachingDevice {
        int overrideCalls = 0;

        FakeDevice(String id) { super(id, "Fake"); }

        void fire(String msg, AlertSeverity s) { raiseAlert(msg, s); }

        @Override
        protected void onStatusChanged(DeviceStatusChangedEventArgs e) {
            overrideCalls++;
            super.onStatusChanged(e); 
        }
    }

    private static void testStatusChanged() {
        section("TeachingDevice.changeStatus");
        ProjectorRemote p = new ProjectorRemote("PRJ-01", "Remote máy chiếu");
        List<DeviceStatusChangedEventArgs> got = new ArrayList<>();
        List<DeviceStatus> statusSeenInsideHandler = new ArrayList<>();
        List<Object> senders = new ArrayList<>();
        p.statusChanged().add((sender, e) -> {
            senders.add(sender);
            got.add(e);
            statusSeenInsideHandler.add(((TeachingDevice) sender).getStatus());
        });

        check("Trạng thái ban đầu là AVAILABLE", p.getStatus() == DeviceStatus.AVAILABLE);

        p.changeStatus(DeviceStatus.AVAILABLE);
        check("Đổi sang cùng trạng thái -> không phát event", got.isEmpty());

        p.changeStatus(DeviceStatus.BORROWED);
        check("Đổi trạng thái -> phát đúng 1 event", got.size() == 1);
        check("Event chứa old/new/id đúng",
                got.get(0).oldStatus() == DeviceStatus.AVAILABLE
                        && got.get(0).newStatus() == DeviceStatus.BORROWED
                        && got.get(0).deviceId().equals("PRJ-01"));
        check("Trạng thái đã cập nhật TRƯỚC khi raise", statusSeenInsideHandler.get(0) == DeviceStatus.BORROWED);
        check("sender là chính thiết bị", senders.get(0) == p);
    }

    private static void testMultipleSubscribers() {
        section("Nhiều subscriber / thêm subscriber không sửa publisher");
        ProjectorRemote p = new ProjectorRemote("PRJ-02", "Remote");
        int[] counter = {0, 0};
        p.alertRaised().add((s, e) -> counter[0]++);
        p.alertRaised().add((s, e) -> counter[1] += 10);
        p.updateBatteryLevel(10);
        check("Cả 2 subscriber đều nhận event", counter[0] == 1 && counter[1] == 10);

        // Subscriber "tương lai" (vd. email) chỉ cần add, không đụng gì tới ProjectorRemote.
        List<String> fakeEmails = new ArrayList<>();
        p.alertRaised().add((s, e) -> fakeEmails.add("mail: " + e.message()));
        p.updateBatteryLevel(0);
        check("Subscriber mới nhận được event sau khi đăng ký", fakeEmails.size() == 1);
        check("Subscriber cũ vẫn hoạt động", counter[0] == 2);
    }

    private static void testProjector() {
        section("ProjectorRemote");
        ProjectorRemote p = new ProjectorRemote("PRJ-03", "Remote");
        List<DeviceAlertEventArgs> alerts = new ArrayList<>();
        p.alertRaised().add((s, e) -> alerts.add(e));

        p.updateBatteryLevel(50);
        check("100 -> 50: không cảnh báo", alerts.isEmpty());
        p.updateBatteryLevel(20);
        check("Pin = 20 chưa phải 'dưới 20' -> không cảnh báo", alerts.isEmpty());
        p.updateBatteryLevel(19);
        check("Pin 19 -> Warning", alerts.size() == 1 && alerts.get(0).severity() == AlertSeverity.WARNING);
        p.updateBatteryLevel(5);
        check("19 -> 5 cùng vùng thấp -> không lặp cảnh báo", alerts.size() == 1);
        p.updateBatteryLevel(0);
        check("Pin 0 -> Critical", alerts.size() == 2 && alerts.get(1).severity() == AlertSeverity.CRITICAL);
        p.updateBatteryLevel(0);
        check("0 -> 0: không lặp Critical", alerts.size() == 2);
        p.updateBatteryLevel(80);
        check("Sạc lên: không cảnh báo", alerts.size() == 2 && p.getBatteryLevel() == 80);
        p.updateBatteryLevel(15);
        check("Rơi lại vùng thấp sau khi sạc -> cảnh báo lại", alerts.size() == 3);

        ProjectorRemote p2 = new ProjectorRemote("PRJ-04", "Remote");
        List<DeviceAlertEventArgs> a2 = new ArrayList<>();
        p2.alertRaised().add((s, e) -> a2.add(e));
        p2.updateBatteryLevel(0);
        check("100 -> 0 trực tiếp -> chỉ 1 Critical (không kèm Warning)",
                a2.size() == 1 && a2.get(0).severity() == AlertSeverity.CRITICAL);
    }

    private static void testAirConditioner() {
        section("AirConditionerRemote");
        AirConditionerRemote ac = new AirConditionerRemote("AC-01", "Remote điều hòa", "P.301");
        List<DeviceAlertEventArgs> alerts = new ArrayList<>();
        ac.alertRaised().add((s, e) -> alerts.add(e));

        check("Ban đầu phản hồi", ac.isResponsive() && ac.getAssignedRoom().equals("P.301"));
        ac.updateResponseState(true);
        check("true -> true: không cảnh báo", alerts.isEmpty());
        ac.updateResponseState(false);
        check("true -> false: Critical", alerts.size() == 1 && alerts.get(0).severity() == AlertSeverity.CRITICAL);
        ac.updateResponseState(false);
        check("false -> false: không lặp", alerts.size() == 1);
        ac.updateResponseState(true);
        check("Hồi phục: không cảnh báo", alerts.size() == 1 && ac.isResponsive());
        ac.updateResponseState(false);
        check("Lại mất phản hồi -> cảnh báo lại", alerts.size() == 2);
    }

    private static void testMicrophone() {
        section("WirelessMicrophone");
        WirelessMicrophone m = new WirelessMicrophone("MIC-01", "Micro");
        List<DeviceAlertEventArgs> alerts = new ArrayList<>();
        m.alertRaised().add((s, e) -> alerts.add(e));

        m.updateBatteryLevel(30);
        check("Pin 30 chưa dưới 30 -> không cảnh báo", alerts.isEmpty());
        m.updateBatteryLevel(29);
        check("Pin 29 -> Warning", alerts.size() == 1 && alerts.get(0).severity() == AlertSeverity.WARNING);
        m.updateBatteryLevel(10);
        check("Pin vẫn thấp -> không lặp", alerts.size() == 1);
        m.updateBatteryLevel(0);
        check("Pin 0 -> Critical", alerts.size() == 2 && alerts.get(1).severity() == AlertSeverity.CRITICAL);

        m.updateSignalQuality(40);
        check("Tín hiệu 40 chưa dưới 40 -> không cảnh báo", alerts.size() == 2);
        m.updateSignalQuality(39);
        check("Tín hiệu 39 -> Warning", alerts.size() == 3 && alerts.get(2).severity() == AlertSeverity.WARNING);
        m.updateSignalQuality(0);
        check("Tín hiệu 0 -> Critical", alerts.size() == 4 && alerts.get(3).severity() == AlertSeverity.CRITICAL);
        m.updateSignalQuality(0);
        check("Tín hiệu 0 -> 0: không lặp", alerts.size() == 4);
    }

    private static void testInvalidReadings() {
        section("InvalidDeviceReadingException");
        ProjectorRemote p = new ProjectorRemote("PRJ-05", "Remote");
        WirelessMicrophone m = new WirelessMicrophone("MIC-02", "Micro");
        int[] alertCount = {0};
        p.alertRaised().add((s, e) -> alertCount[0]++);
        m.alertRaised().add((s, e) -> alertCount[0]++);

        p.updateBatteryLevel(60);
        try {
            p.updateBatteryLevel(-1);
            check("Pin -1 phải ném exception", false);
        } catch (InvalidDeviceReadingException ex) {
            check("Pin -1 ném exception đúng id/giá trị",
                    ex.getDeviceId().equals("PRJ-05") && ex.getInvalidValue() == -1);
        }
        try {
            p.updateBatteryLevel(101);
            check("Pin 101 phải ném exception", false);
        } catch (InvalidDeviceReadingException ex) {
            check("Pin 101 ném exception", ex.getInvalidValue() == 101);
        }
        check("Giá trị sai không làm đổi trạng thái pin", p.getBatteryLevel() == 60);

        try {
            m.updateSignalQuality(150);
            check("Tín hiệu 150 phải ném exception", false);
        } catch (InvalidDeviceReadingException ex) {
            check("Tín hiệu 150 ném exception, tín hiệu giữ nguyên", m.getSignalQuality() == 100);
        }
        try {
            m.updateBatteryLevel(-5);
            check("Pin micro -5 phải ném exception", false);
        } catch (InvalidDeviceReadingException ex) {
            check("Pin micro -5 ném exception", true);
        }
        check("Đọc sai không phát sinh cảnh báo nào", alertCount[0] == 0);
        p.updateBatteryLevel(0);
        p.updateBatteryLevel(100);
        check("Biên 0 và 100 hợp lệ", p.getBatteryLevel() == 100);
    }

    private static void testSubscribers() {
        section("Subscribers (Console / Audit / Maintenance)");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ConsoleNotificationService console = new ConsoleNotificationService(new PrintStream(buf, true));
        DeviceAuditLogger logger = new DeviceAuditLogger();
        MaintenanceService maintenance = new MaintenanceService();

        ProjectorRemote p = new ProjectorRemote("PRJ-06", "Remote");
        AirConditionerRemote ac = new AirConditionerRemote("AC-02", "Điều hòa", "P.402");
        List<TeachingDevice> devices = List.of(p, ac);

        DeviceMonitoringCoordinator c = new DeviceMonitoringCoordinator(devices, console, logger, maintenance);
        c.subscribe();

        p.updateBatteryLevel(10);   // Warning
        ac.updateResponseState(false); // Critical
        p.changeStatus(DeviceStatus.BORROWED);
        p.changeStatus(DeviceStatus.UNDER_MAINTENANCE);

        String out = buf.toString();
        check("Console in cả Warning lẫn Critical",
                out.contains("[WARNING] PRJ-06") && out.contains("[CRITICAL] AC-02"));
        check("Maintenance chỉ nhận Critical (1 yêu cầu)",
                maintenance.getRequests().size() == 1 && maintenance.getRequests().get(0).contains("AC-02"));
        check("Audit chỉ ghi thay đổi trạng thái (2 dòng)", logger.getEntries().size() == 2);
        check("Audit ghi đúng chiều thay đổi",
                logger.getEntries().get(1).contains("BORROWED -> UNDER_MAINTENANCE"));
        check("Entries là danh sách chỉ đọc", throwsUnsupported(() -> logger.getEntries().add("hack")));
        check("Requests là danh sách chỉ đọc", throwsUnsupported(() -> maintenance.getRequests().add("hack")));
    }

    private static boolean throwsUnsupported(Runnable r) {
        try {
            r.run();
            return false;
        } catch (UnsupportedOperationException ex) {
            return true;
        }
    }

    private static void testCoordinator() {
        section("DeviceMonitoringCoordinator");
        ConsoleNotificationService console = new ConsoleNotificationService(new PrintStream(new ByteArrayOutputStream()));
        DeviceAuditLogger logger = new DeviceAuditLogger();
        MaintenanceService maintenance = new MaintenanceService();
        ProjectorRemote p = new ProjectorRemote("PRJ-07", "Remote");

        DeviceMonitoringCoordinator c = new DeviceMonitoringCoordinator(List.of(p), console, logger, maintenance);
        check("Chưa subscribe: không có handler", p.alertRaised().handlerCount() == 0
                && p.statusChanged().handlerCount() == 0);

        c.subscribe();
        c.subscribe();
        c.subscribe();
        check("Subscribe nhiều lần không đăng ký trùng (alert=2, status=1)",
                p.alertRaised().handlerCount() == 2 && p.statusChanged().handlerCount() == 1);

        p.changeStatus(DeviceStatus.BORROWED);
        check("Gọi subscribe 3 lần nhưng chỉ ghi log 1 lần", logger.getEntries().size() == 1);

        c.unsubscribe();
        check("Unsubscribe gỡ sạch handler", p.alertRaised().handlerCount() == 0
                && p.statusChanged().handlerCount() == 0 && !c.isSubscribed());
        p.changeStatus(DeviceStatus.AVAILABLE);
        p.updateBatteryLevel(0);
        check("Sau unsubscribe subscriber không nhận thêm gì",
                logger.getEntries().size() == 1 && maintenance.getRequests().isEmpty());

        c.unsubscribe(); // gọi thừa không được lỗi
        check("Unsubscribe lặp lại an toàn", true);

        c.subscribe();
        check("Có thể subscribe lại sau unsubscribe", p.alertRaised().handlerCount() == 2);
        c.dispose();
        check("dispose() gọi unsubscribe", p.alertRaised().handlerCount() == 0 && !c.isSubscribed());

        try (DeviceMonitoringCoordinator c2 = new DeviceMonitoringCoordinator(List.of(p), console, logger, maintenance)) {
            c2.subscribe();
            check("Trong try-with-resources đã subscribe", p.statusChanged().handlerCount() == 1);
        }
        check("Ra khỏi try-with-resources tự dispose", p.statusChanged().handlerCount() == 0);
    }

    private static void testFilteredSubscriber() {
        section("Delegate AlertFilter + FilteredAlertSubscriber");
        ProjectorRemote p = new ProjectorRemote("PRJ-08", "Remote");
        List<String> criticalLines = new ArrayList<>();
        FilteredAlertSubscriber criticalOnly = new FilteredAlertSubscriber(
                alert -> alert.severity() == AlertSeverity.CRITICAL,
                alert -> criticalLines.add("CRITICAL: " + alert.deviceId() + " - " + alert.message()));
        p.alertRaised().add(criticalOnly::handleAlert);

        p.updateBatteryLevel(10);
        check("Warning bị bộ lọc chặn", criticalLines.isEmpty());
        p.updateBatteryLevel(0);
        check("Critical đi qua bộ lọc", criticalLines.size() == 1 && criticalLines.get(0).startsWith("CRITICAL: PRJ-08"));

        // Đổi chính sách lọc chỉ bằng cách truyền lambda khác
        List<String> byDevice = new ArrayList<>();
        FilteredAlertSubscriber byId = new FilteredAlertSubscriber(
                a -> a.deviceId().startsWith("AC"), a -> byDevice.add(a.message()));
        AirConditionerRemote ac = new AirConditionerRemote("AC-03", "Điều hòa", "P.101");
        ac.alertRaised().add(byId::handleAlert);
        p.alertRaised().add(byId::handleAlert);
        ac.updateResponseState(false);
        check("Lọc theo mã thiết bị bằng lambda khác", byDevice.size() == 1);
    }

    private static void testStability() {
        section("Tính ổn định của event");
        ProjectorRemote p = new ProjectorRemote("PRJ-09", "Remote");
        int[] good = {0};
        EventHandler<DeviceAlertEventArgs> faulty = (s, e) -> { throw new IllegalStateException("subscriber lỗi"); };
        PrintStream oldErr = System.err;
        System.setErr(new PrintStream(new ByteArrayOutputStream())); // tránh in nhiễu
        try {
            p.alertRaised().add(faulty);
            p.alertRaised().add((s, e) -> good[0]++);
            p.updateBatteryLevel(10);
            check("Handler lỗi không làm publisher ném exception", true);
            check("Handler lỗi không chặn handler phía sau", good[0] == 1);
            check("Trạng thái thiết bị vẫn đúng sau khi handler lỗi", p.getBatteryLevel() == 10);
        } catch (RuntimeException ex) {
            check("Publisher không bị ảnh hưởng bởi handler lỗi", false);
        } finally {
            System.setErr(oldErr);
        }

        // Tự hủy đăng ký ngay trong lúc event đang được gọi
        ProjectorRemote q = new ProjectorRemote("PRJ-10", "Remote");
        int[] once = {0};
        AtomicReference<EventHandler<DeviceAlertEventArgs>> self = new AtomicReference<>();
        self.set((s, e) -> { once[0]++; q.alertRaised().remove(self.get()); });
        q.alertRaised().add(self.get());
        q.updateBatteryLevel(10);
        q.updateBatteryLevel(0);
        check("Handler tự hủy đăng ký khi đang chạy: chỉ nhận 1 lần, không lỗi", once[0] == 1);

        // Đăng ký cùng handler 2 lần
        ProjectorRemote r = new ProjectorRemote("PRJ-11", "Remote");
        int[] n = {0};
        EventHandler<DeviceAlertEventArgs> h = (s, e) -> n[0]++;
        r.alertRaised().add(h);
        r.alertRaised().add(h);
        r.updateBatteryLevel(5);
        check("Cùng handler add 2 lần chỉ chạy 1 lần", n[0] == 1);
        r.alertRaised().remove(h);
        r.alertRaised().remove(h); // remove lần 2 không lỗi
        check("Remove handler đã gỡ không gây lỗi", r.alertRaised().handlerCount() == 0);
    }

    private static void testDerivedAndEncapsulation() {
        section("Lớp dẫn xuất & đóng gói event");
        FakeDevice f = new FakeDevice("FAKE-01");
        List<DeviceAlertEventArgs> alerts = new ArrayList<>();
        f.alertRaised().add((s, e) -> alerts.add(e));
        f.fire("hello", AlertSeverity.INFORMATION);
        check("Lớp dẫn xuất raise được qua raiseAlert()", alerts.size() == 1
                && alerts.get(0).severity() == AlertSeverity.INFORMATION);

        int[] statusEvents = {0};
        f.statusChanged().add((s, e) -> statusEvents[0]++);
        f.changeStatus(DeviceStatus.BORROWED);
        check("Override onStatusChanged() được gọi và vẫn phát event",
                f.overrideCalls == 1 && statusEvents[0] == 1);

        boolean hasRaise = false;
        for (var m : lab.events.Event.class.getMethods()) {
            if (m.getName().toLowerCase().contains("raise") || m.getName().toLowerCase().contains("invoke")) {
                hasRaise = true;
            }
        }
        check("Giao diện Event công khai không có raise/invoke", !hasRaise);
        check("Event trả ra không phải là EventPublisher (không ép kiểu để raise được)",
                !((Object) f.alertRaised() instanceof lab.events.EventPublisher));
    }

    private static void demo() {
        section("DEMO kịch bản thực tế");
        ConsoleNotificationService console = new ConsoleNotificationService();
        DeviceAuditLogger logger = new DeviceAuditLogger();
        MaintenanceService maintenance = new MaintenanceService();

        ProjectorRemote projector = new ProjectorRemote("PRJ-A1", "Remote máy chiếu A1");
        AirConditionerRemote ac = new AirConditionerRemote("AC-A1", "Remote điều hòa A1", "D3-201");
        WirelessMicrophone mic = new WirelessMicrophone("MIC-A1", "Micro không dây A1");

        try (DeviceMonitoringCoordinator coordinator =
                     new DeviceMonitoringCoordinator(List.of(projector, ac, mic), console, logger, maintenance)) {
            coordinator.subscribe();

            projector.changeStatus(DeviceStatus.BORROWED);
            projector.updateBatteryLevel(15);
            projector.updateBatteryLevel(0);
            ac.updateResponseState(false);
            mic.updateSignalQuality(35);
            mic.updateBatteryLevel(0);
            mic.changeStatus(DeviceStatus.UNDER_MAINTENANCE);
        }

        System.out.println("-- Audit log:");
        logger.getEntries().forEach(s -> System.out.println("   " + s));
        System.out.println("-- Maintenance requests:");
        maintenance.getRequests().forEach(s -> System.out.println("   " + s));
    }

    public static void main(String[] args) {
        testStatusChanged();
        testMultipleSubscribers();
        testProjector();
        testAirConditioner();
        testMicrophone();
        testInvalidReadings();
        testSubscribers();
        testCoordinator();
        testFilteredSubscriber();
        testStability();
        testDerivedAndEncapsulation();
        demo();

        System.out.println("\n==============================");
        System.out.println("Tổng kết: " + passed + " PASS, " + failed + " FAIL");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
