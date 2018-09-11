package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;

public class HomeActivity extends AppCompatActivity {

    ArrayList<Device> combinedDeviceList;
    ArrayList<Device> wifiDevices;
    ArrayList<Device> bluetoothDevices;
    ListView deviceListView;
    DeviceListAdapter deviceListAdapter;
    PeerDiscoveryController peerDiscoveryController;
    WDUDPSender udpSender;
    ConnectedSocketManager connectedSocketManager;
//    SocketConnector socketConnector;
    Handler BTDiscoverableHandler;
    boolean willUpdateDeviceList;
    boolean willRecordRSSI;
    int experimentNo;
    long RTTs[];
    long udpThroughputRTTs[];
    String distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        willUpdateDeviceList = true;
        willRecordRSSI = false;
        setUpPermissions();
        BTDiscoverableHandler = new Handler();
        BTDiscoverableHandler.post(makeBluetoothDiscoverable);
        setUpBluetoothDataTransfer();
        startDiscovery();
    }

    public void setUpPermissions() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.REQUEST_CODE_LOCATION);
    }

    private Runnable makeBluetoothDiscoverable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, Constants.BT_DISCOVERABLE_LENGTH);
            startActivity(intent);
            BTDiscoverableHandler.postDelayed(this, Constants.BT_DISCOVERABLE_LENGTH*1000);
        }
    };

    private void setUpBluetoothDataTransfer() {
//        socketConnector = new SocketConnector();
        BluetoothConnectionListener bluetoothConnectionListener = new BluetoothConnectionListener(this);
        bluetoothConnectionListener.start();
    }

    //configures the bluetooth and wifi discovery options and starts the background process for discovery
    public void startDiscovery(){
        configureDeviceListView();
        peerDiscoveryController = new PeerDiscoveryController(this, this);
    }

    //setting up the device list view adapter and item click events
    public void configureDeviceListView(){
        deviceListView = findViewById(R.id.device_listView);
        combinedDeviceList = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(this, combinedDeviceList);
        deviceListView.setAdapter(deviceListAdapter);
    }

    public void recordRSSI(View view) {
        EditText distanceText = (EditText)findViewById(R.id.distance_editText);
        if (textboxIsEmpty(distanceText))
            distanceText.setError("");
        else {
            distance = distanceText.getText().toString().trim();
            if (willRecordRSSI)
                willRecordRSSI = false;
            else
                willRecordRSSI = true;
        }

    }

    public void connectButton(View view) {
        int tag = (int)view.getTag();
        Device currentDevice = combinedDeviceList.get(tag);
        if (currentDevice.deviceType == Constants.WIFI_DEVICE)
            peerDiscoveryController.connectWiFiDirectDevice(combinedDeviceList.get(tag));
    }

    public void rttButton(View view) {
        int tag = (int)view.getTag();
        Device currentDevice = combinedDeviceList.get(tag);
        EditText distanceText = findViewById(R.id.distance_editText);
        if (textboxIsEmpty(distanceText)) {
            distanceText.setError("enter distance");
            return;
        }
        EditText pktSizeText = findViewById(R.id.pkt_size_editText);
        if (textboxIsEmpty(pktSizeText)) {
            pktSizeText.setError("enter packet size");
            return;
        }
        experimentNo = 0;
        RTTs = new long[Constants.NO_OF_EXPS];
        String pktSizeStr = pktSizeText.getText().toString().trim();
        int pktSize = Integer.parseInt(pktSizeStr);
        if (currentDevice.deviceType == Constants.WIFI_DEVICE) {
            if (currentDevice.IPAddress == null) {
                showToast("ip mac not synced");
                return;
            }
            currentDevice.rttPkt = PacketManager.createRTTPacket(Constants.RTT, Constants.hostWifiAddress, currentDevice.wifiDevice.deviceAddress, pktSize);
            udpSender = null;
            udpSender = new WDUDPSender();
            udpSender.createPkt(currentDevice.rttPkt, currentDevice.IPAddress);
            udpSender.setRunLoop(false);
            currentDevice.rttStartTime = Calendar.getInstance().getTimeInMillis();
            udpSender.start();
        }
        else {
            SocketConnector socketConnector = new SocketConnector();
            socketConnector.setDevice(currentDevice);
            BluetoothSocket connectedSocket = socketConnector.createSocket();
            connectedSocketManager = null;
            if (connectedSocket!=null) {
                connectedSocketManager = new ConnectedSocketManager(connectedSocket);
                connectedSocketManager.start();
            }
            String packet = PacketManager.createRTTPacket(Constants.RTT, Constants.hostBluetoothAddress, currentDevice.bluetoothDevice.getAddress(), pktSize);
            connectedSocketManager.sendPkt(packet, Constants.RTT);
        }
    }

    public void pktLossButton(View view) {
        int tag = (int)view.getTag();
        Device currentDevice = combinedDeviceList.get(tag);
        EditText distanceText = findViewById(R.id.distance_editText);
        if (textboxIsEmpty(distanceText)) {
            distanceText.setError("enter distance");
            return;
        }
        if (currentDevice.IPAddress == null) {
            showToast("ip mac not synced");
            return;
        }
        udpSender = null;
        udpSender = new WDUDPSender();
        String lossRatioPkt = PacketManager.createLossRatioPacket(Constants.PKT_LOSS, Constants.hostWifiAddress, currentDevice.wifiDevice.deviceAddress);
        udpSender.createPkt(lossRatioPkt, currentDevice.IPAddress);
        udpSender.setRunLoop(true);
        udpSender.setNoOfPktsToSend(Constants.MAX_LOSS_RATIO_PKTS);
        udpSender.start();
    }

    public void UDPThroughputButton(View view) {
        int tag = (int)view.getTag();
        Device currentDevice = combinedDeviceList.get(tag);
        EditText distanceText = findViewById(R.id.distance_editText);
        if (textboxIsEmpty(distanceText)) {
            distanceText.setError("enter distance");
            return;
        }
        if (currentDevice.IPAddress == null) {
            showToast("ip mac not synced");
            return;
        }
        experimentNo = 0;
        udpThroughputRTTs = new long[Constants.NO_OF_EXPS +1];
    }

    public void TCPThroughputButton(View view) {
        int tag = (int)view.getTag();
    }

    //callback method from peer discovery controller after finishing a cycle of wifi and bluetooth discovery
    public void discoveryFinished(ArrayList<Device> wifiDevices, ArrayList<Device> bluetoothDevices) {
        wifiDevices = cleanUpDeviceList(wifiDevices, Constants.WIFI_DEVICE);
        bluetoothDevices = cleanUpDeviceList(bluetoothDevices, Constants.BLUETOOTH_DEVICE);
        if (willUpdateDeviceList) {
            this.wifiDevices = wifiDevices;
            this.bluetoothDevices = bluetoothDevices;
            if (combinedDeviceList.size() > 0)
                combinedDeviceList.clear();
            combinedDeviceList.addAll(this.wifiDevices);
            combinedDeviceList.addAll(this.bluetoothDevices);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    deviceListAdapter.notifyDataSetChanged();
                }
            });
            if (willRecordRSSI) {
                long currentTime = Calendar.getInstance().getTimeInMillis();
                String timestamp = String.valueOf(currentTime);
                FileWriter.writeRSSIResult(distance, timestamp, bluetoothDevices);
                showToast("rssi recorded");
            }
        }
    }

    public ArrayList<Device> cleanUpDeviceList(ArrayList<Device> devices, int deviceType) {
        ArrayList<Device> cleanedList = new ArrayList<>();
        if (deviceType == Constants.WIFI_DEVICE) {
            for (Device newDevice: devices
                 ) {
                String deviceName = newDevice.wifiDevice.deviceName;
                if (deviceName!=null && newDevice.wifiDevice.deviceName.contains("NWSL")) {
                    boolean newDeviceFlag = true;
                    for (Device oldDevice: cleanedList
                            ) {
                        if (oldDevice.wifiDevice.deviceAddress.equals(newDevice.wifiDevice.deviceAddress)) {
                            newDeviceFlag = false;
                            break;
                        }
                    }
                    if (newDeviceFlag)
                        cleanedList.add(newDevice);
                }
            }
        }
        else {
            for (Device newDevice: devices
                    ) {
                String deviceName = newDevice.bluetoothDevice.getName();
                if (deviceName!=null && deviceName.contains("NWSL")) {
                    boolean newDeviceFlag = true;
                    for (Device oldDevice: cleanedList
                            ) {
                        if (oldDevice.bluetoothDevice.getAddress().equals(newDevice.bluetoothDevice.getAddress())) {
                            newDeviceFlag = false;
                            break;
                        }
                    }
                    if (newDeviceFlag)
                        cleanedList.add(newDevice);
                }
            }
        }
        return cleanedList;
    }

    //shows the wifi p2p state
    public void wifiP2PState(int state) {
        if (state == 0)
            showAlert("WiFi direct disabled");
    }

    public void connectionEstablished(int connectionType, BluetoothSocket connectedSocket) {
        if (connectionType == Constants.WIFI_DIRECT_CONNECTION) {
            showToast("wifi direct connection established");
            WDUDPListener udpListener = new WDUDPListener(this);
            udpListener.start();
            if (!Constants.isGroupOwner)
                ipMacSync();
        }
        else {
            showToast("bluetooth connection established");
            connectedSocketManager = new ConnectedSocketManager(connectedSocket);
            connectedSocketManager.start();
        }
    }

    public void ipMacSync() {
        String pkt = PacketManager.createIpMacSyncPkt(Constants.IP_MAC_SYNC_REC, Constants.hostWifiAddress);
        udpSender = null;
        udpSender = new WDUDPSender();
        udpSender.createPkt(pkt, Constants.groupOwnerAddress);
        udpSender.setRunLoop(false);
        udpSender.start();
    }

    public void matchIPToMac(InetAddress ipAddr, String macAddr) {
        for (Device device:combinedDeviceList
                ) {
            if (device.deviceType == Constants.WIFI_DEVICE) {
                if (device.wifiDevice.deviceAddress.equals(macAddr)){
                    device.IPAddress = ipAddr;
                    device.lossRatioPktsReceived = 0;
                    willUpdateDeviceList = false;
                    showToast("ip mac synced");
                    break;
                }
            }
        }
    }

    public void processReceivedWiFiPkt(InetAddress srcAddr, long receivingTime, final String receivedPkt) {
        String splited[] = receivedPkt.split("#");
        int pktType = Integer.parseInt(splited[0]);
        if (pktType == Constants.IP_MAC_SYNC_REC) {
            String pkt = PacketManager.createIpMacSyncPkt(Constants.IP_MAC_SYNC_RET, Constants.hostWifiAddress);
            udpSender = null;
            udpSender = new WDUDPSender();
            udpSender.createPkt(pkt, srcAddr);
            udpSender.setRunLoop(false);
            udpSender.start();
            matchIPToMac(srcAddr, splited[1]);
        }
        else if (pktType == Constants.IP_MAC_SYNC_RET)
            matchIPToMac(srcAddr, splited[1]);
        else if (pktType == Constants.RTT) {
            int pktSize = Integer.parseInt(splited[3]);
            String pkt = PacketManager.createRTTPacket(Constants.RTT_RET, Constants.hostWifiAddress, splited[1], pktSize);
            udpSender = null;
            udpSender = new WDUDPSender();
            udpSender.createPkt(pkt, srcAddr);
            udpSender.setRunLoop(false);
            udpSender.start();
        }
        else if (pktType == Constants.RTT_RET) {
            for (Device device:combinedDeviceList
                 ) {
                if (device.deviceType == Constants.WIFI_DEVICE) {
                    if (device.wifiDevice.deviceAddress.equals(splited[1])) {
                        device.roundTripTime = receivingTime - device.rttStartTime;
                        RTTs[experimentNo] = device.roundTripTime;
                        experimentNo++;
                        if (experimentNo < Constants.NO_OF_EXPS) {
                            udpSender = null;
                            udpSender = new WDUDPSender();
                            udpSender.createPkt(device.rttPkt, srcAddr);
                            udpSender.setRunLoop(false);
                            device.rttStartTime = Calendar.getInstance().getTimeInMillis();
                            udpSender.start();
                        }
                        else {
                            writeResult(device.wifiDevice.deviceName, Constants.RTT);
                        }
                        break;
                    }
                }
                else {
                    //bluetooth rtt
                }
            }
        }
        else if (pktType == Constants.PKT_LOSS) {
            for (final Device device:combinedDeviceList
                 ) {
                if (device.deviceType == Constants.WIFI_DEVICE) {
                    if (device.wifiDevice.deviceAddress.equals(splited[1])) {
                        if (device.lossRatioPktsReceived == 0) {
                            device.lossRatioPktsReceived++;
                            Log.d("pkt loss", "first pkt");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            double pktLoss = ((Constants.MAX_LOSS_RATIO_PKTS - device.lossRatioPktsReceived)/Constants.MAX_LOSS_RATIO_PKTS) * 100.00;
                                            Log.d("pkt loss after 1 minute", String.valueOf(pktLoss));
                                        }
                                    }, 60 * 1000);
                                }
                            });
                        }
                        else
                            device.lossRatioPktsReceived++;
                    }
                }
            }
        }
    }

    public void writeResult(String deviceName, int measurementType) {
        EditText distanceText = findViewById(R.id.distance_editText);
        String distance = distanceText.getText().toString().trim();

        EditText pktSizeText = findViewById(R.id.pkt_size_editText);
        String pktSize = pktSizeText.getText().toString().trim();

        if (measurementType == Constants.RTT) {
            boolean retVal = FileWriter.writeRTTResult(deviceName, pktSize, distance, RTTs);
            if (retVal)
                showToast("rtt written successfully");
            else
                showToast("rtt write not successful");
        }
    }

    //function to show an alert message
    public void showAlert(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                builder.setMessage(message);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    public void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    //function to check empty text field
    public boolean textboxIsEmpty(EditText editText) {
        return editText.getText().toString().trim().length() == 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}