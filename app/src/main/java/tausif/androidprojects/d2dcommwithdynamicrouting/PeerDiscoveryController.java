package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ctaus on 3/21/2018.
 * this class contains the methods to control the peer discovery process for bluetooth and wifi
 */

public class PeerDiscoveryController extends Activity{

    private Timer timer;
    private int timeSlotCount;
    private int timeInterval;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<Device> bluetoothDevices;
    private ArrayList<Device> combinedDeviceList;
    private HomeActivity homeActivity;

    public PeerDiscoveryController(HomeActivity homeActivity) {
        this.homeActivity = homeActivity;
        configureBluetoothDiscovery();
        timeSlotCount = 0;
        timeInterval = 30;
        timer = new Timer();
        timer.scheduleAtFixedRate(new StartStopDiscovery(), 0, timeInterval*1000);
    }

    //configure bluetooth device discovery options
    public void configureBluetoothDiscovery() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
        }
        if (!bluetoothAdapter.isEnabled()){
        }
    }

    class StartStopDiscovery extends TimerTask {
        @Override
        public void run() {
            if (timeSlotCount%2==0) {
                bluetoothDevices = new ArrayList<>();
                bluetoothAdapter.startDiscovery();
            }
            else {
                bluetoothAdapter.cancelDiscovery();
                mergeDeviceLists();
                homeActivity.peerDiscoveryFinished(combinedDeviceList);
            }
            timeSlotCount++;
        }
    }

    private void mergeDeviceLists() {
        combinedDeviceList = new ArrayList<>();
        combinedDeviceList.add(new Device("Bluetooth Devices", "", 0, null));
        combinedDeviceList.addAll(bluetoothDevices);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Device bluetoothDevice = new Device(device.getName(), device.getAddress(), 0, device);
                int flag = 0;
                for (Device item:bluetoothDevices) {
                    if (item.deviceAddress.equalsIgnoreCase(bluetoothDevice.deviceAddress)){
                        flag = 1;
                        break;
                    }
                }
                if (flag == 0){
                    bluetoothDevices.add(bluetoothDevice);
                }
            }
        }
    };
}
