package cz.gymjs.robot;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class MotorController {
    UsbSerialPort port;
    MotorController(UsbManager manager) throws IOException {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            throw new IOException("No device found");
        }
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            throw new IOException("Could not connect");
        }
        port = driver.getPorts().get(0);
        port.open(connection);
        port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        port.close();
    }

    public int rotate(int speed1, int speed2) throws IOException {
        byte[] message = encodeRotation(new int[]{speed1, -speed2});
        return port.write(message, 100);
    }

    private byte[] encodeRotation(int[] speed) {
        byte[] result = new byte[2 + 2 * speed.length];
        result[0] = (byte) result.length;
        result[1] = 1;
        for (int i = 0; i < speed.length; i++) {
            result[2 * i + 2] = (byte) speed[i];
            result[2 * i + 3] = (byte) (speed[i] >> 8);
        }
        return result;
    }

}
