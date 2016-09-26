package hang.bh1sfz.ft817_assistant;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;


public class DeviceListActivity extends AppCompatActivity {
    private static final String TAG = "FT817";

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private class DeviceAdapter extends BaseAdapter{
        public ArrayList<BluetoothDevice> devices;
        private Context mContext;

        public DeviceAdapter(Context context) {
            this.mContext = context;
            this.devices = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {

            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = LayoutInflater.from(this.mContext).inflate(R.layout.item_device_name,parent,false);
            TextView name = (TextView) itemView.findViewById(R.id.item_tv_device_name);
            TextView address = (TextView) itemView.findViewById(R.id.item_tv_device_address);

            BluetoothDevice dev = devices.get(position);
            name.setText(dev.getName());
            address.setText(dev.getAddress());

            return itemView;
        }
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = (BluetoothDevice)btDeviceAdapter.getItem(position);
            Log.i(TAG,"select BT device: "+device.getName());

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());

            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };


    private BluetoothAdapter btAdapter;
    private DeviceAdapter btDeviceAdapter;
    private Button btnScan;
    private ListView lvDevices;
    private ProgressDialog progress;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        setTitle(R.string.select_device);
        btnScan = (Button) findViewById(R.id.btn_scan);
        // 搜索设备
        btnScan.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                progress = ProgressDialog.show(DeviceListActivity.this, "",  getResources().getString(R.string.scaning_device),true);

                // 搜索设备
                doDiscovery();
            }
        });

        btDeviceAdapter = new DeviceAdapter(this);

        lvDevices = (ListView) findViewById(R.id.lv_devices);
        lvDevices.setAdapter(btDeviceAdapter);
        lvDevices.setOnItemClickListener(mDeviceClickListener);

        // 检测设备
        this.registerReceiver(btDeviceReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        // 完成设备检测
        this.registerReceiver(btDeviceReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // 已配对的设备
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                btDeviceAdapter.devices.add(device);
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (btAdapter != null) {
            // 结束设备发现
            btAdapter.cancelDiscovery();
        }

        this.unregisterReceiver(btDeviceReceiver);
        this.finish();
    }

    // 开始发现设备
    private void doDiscovery() {
        this.btDeviceAdapter.devices.clear();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                btDeviceAdapter.devices.add(device);
            }
        }

        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }

        btAdapter.startDiscovery();
    }

    private final BroadcastReceiver btDeviceReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    btDeviceAdapter.devices.add(device);
                }

                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progress.dismiss();

                Toast.makeText(context, R.string.scan_completed, Toast.LENGTH_SHORT).show();
            }
        }
    };

}