import java.util.List;
import java.util.Locale;

public class Main {

    private static int failures = 0;

    private static void check(String label, Object actual, boolean ok) {
        if (!ok) failures++;
        System.out.println(" -> " + label + ": " + actual
                + (ok ? " [PASSED]" : " [FAILED]"));
    }

    private static String pct(double v) {
        return String.format(Locale.US, "%.2f%%", v);
    }

    public static void main(String[] args) {
        String line = "============================================================";
        System.out.println(line);
        System.out.println(" OMNIHOME SMART CONTROLLER: SYSTEM STARTUP");
        System.out.println(line);


        LegacyBulb rawBulb = new LegacyBulb();
        LegacyThermostat rawThermostat = new LegacyThermostat();
        BulbAdapter bulbAdapter = new BulbAdapter(rawBulb);
        ThermostatAdapter thermostatAdapter = new ThermostatAdapter(rawThermostat);
        List<SmartDevice> deviceList = List.of(bulbAdapter, thermostatAdapter);
        ModernHub hub = new ModernHub(deviceList);



        System.out.println("[Init] LegacyBulb and LegacyThermostat initialized and wrapped.");
        System.out.println("[Hub] Registering " + deviceList.size()
                + " adapted devices into ModernHub...");

        // Step 5: activate
        System.out.println();
        System.out.println("--- OPERATION: ACTIVATE ALL DEVICES ---");
        System.out.println("[Action] ModernHub.activateAll() invoked.");
        hub.activateAll();
        System.out.println(" -> BulbAdapter: Brightness set to " + rawBulb.readBrightness() + ".");
        System.out.println(" -> ThermostatAdapter: Dial set to '" + rawThermostat.checkDial() + "'.");
        boolean allOn = bulbAdapter.isOn() && thermostatAdapter.isOn();
        System.out.println("[Status] All devices reported active: " + allOn);
        if (!allOn) failures++;


        double avg = hub.calculateAveragePowerUsage();
        System.out.println("[Power] Fleet Average Power Usage: " + pct(avg)
                + " (Bulb: " + bulbAdapter.getPowerPercent() + "%, Thermostat: "
                + thermostatAdapter.getPowerPercent() + "%)");
        if (avg != 66.5) failures++;


        System.out.println();
        System.out.println("--- CALIBRATION CHECK (K = 3) ---");
        LegacyBulb midBulb = new LegacyBulb();
        midBulb.setBrightness(128);
        int mid = new BulbAdapter(midBulb).getPowerPercent(); // 50 + 3
        check("raw 128 -> percent", mid + "%", mid == 53);


        System.out.println();
        System.out.println("--- AUDIT: HARDWARE FAULT INJECTION (STAGE 4) ---");
        System.out.println("[Fault 1] Filament physically severed on LegacyBulb...");
        rawBulb.breakFilament(); // readBrightness() still returns 255
        check("BulbAdapter.isOn()", bulbAdapter.isOn(), !bulbAdapter.isOn());
        check("BulbAdapter.getPowerPercent()", bulbAdapter.getPowerPercent() + "%",
                bulbAdapter.getPowerPercent() == 0);

        String[] badStates = {"STUCK", "OVERHEAT", "", null};
        int n = 2;
        for (String bad : badStates) {
            String shown = (bad == null) ? "null" : "'" + bad + "'";
            System.out.println("[Fault " + n++ + "] Dial encoder set to illegal "
                    + shown + " state on LegacyThermostat...");
            rawThermostat.rotateDial(bad);
            check("ThermostatAdapter.isOn()", thermostatAdapter.isOn(),
                    !thermostatAdapter.isOn());
            check("ThermostatAdapter.getPowerPercent()", thermostatAdapter.getPowerPercent(),
                    thermostatAdapter.getPowerPercent() == -1);
        }


        System.out.println();
        System.out.println("--- OPERATION: EMERGENCY SHUTDOWN ---");
        System.out.println("[Action] ModernHub.emergencyShutdown() invoked.");
        hub.emergencyShutdown();
        check("Bulb raw brightness", rawBulb.readBrightness(), rawBulb.readBrightness() == 0);
        check("Thermostat dial", rawThermostat.checkDial(),
                "IDLE".equals(rawThermostat.checkDial()));
        double after = hub.calculateAveragePowerUsage();
        System.out.println("[Power] Fleet Average Power Usage: " + pct(after));
        if (after != 0.0) failures++;

        System.out.println(line);
        if (failures == 0) {
            System.out.println(" ALL INTEGRATION TESTS PASSED (100/100)");
        } else {
            System.out.println(" TESTS FAILED: " + failures);
        }
        System.out.println(line);
    }
}