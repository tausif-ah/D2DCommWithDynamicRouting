package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.bluetooth.BluetoothDevice;
import android.net.wifi.p2p.WifiP2pDevice;


public class Device {
    public int deviceType;
    public WifiP2pDevice wifiDevice;
    public BluetoothDevice bluetoothDevice;
    public int rssi;
    public double roundTripTime;
    public double packetLossRatio;

    public Device(int deviceType, WifiP2pDevice wifiDevice, BluetoothDevice bluetoothDevice, int rssi) {
        this.deviceType = deviceType;
        this.wifiDevice = wifiDevice;
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
        this.roundTripTime = 0.0;
        this.packetLossRatio = 0.0;
    }
}
