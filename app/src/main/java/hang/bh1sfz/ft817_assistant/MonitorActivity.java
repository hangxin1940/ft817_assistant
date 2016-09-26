package hang.bh1sfz.ft817_assistant;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import hang.bh1sfz.catspp.FT817;

public class MonitorActivity extends AppCompatActivity implements FT817.OnRecivedRefreshStatusListener {
    private static final String LOG_TAG = "FT817-Monitor";

    private static final String FreqFormat = "%1$08d";

    private FT817 ft817;

    private boolean connecting = false;


    private TextView tvModeLSB;
    private TextView tvModeUSB;
    private TextView tvModeCW;
    private TextView tvModeCWR;
    private TextView tvModeAM;
    private TextView tvModeWFM;
    private TextView tvModeFM;
    private TextView tvModeDIG;
    private TextView tvModePKT;

    private TextView tvChannel;
    private TextView tvTxpwr;
    private TextView tvFreq1;
    private TextView tvFreq2;
    private TextView tvFreq3;

    private ProgressBar pbSMeter;

    private TextView tvSQL;

    private ImageView imgvLockon;

    private ImageButton ibtnBtDevices;

    private static final int Delay = 300;
    private static final int InterruptDelay = 200;

    private int modeColorDisabled;
    private int modeColorEnabled;
    private int rxColorOn;
    private int rxColorOff;
    private int btConnected;
    private int btDisconnected;


    Handler handlerRefreshFT817 = new Handler();

    // 定时刷新电台信息
    private Runnable runnableRefreshStatus = new Runnable() {
        @Override
        public void run() {

            if (null != ft817) {
                  ft817.readStatus();
            }
        }
    };

    Handler handerUpdateUI = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            int refreshType = msg.arg1;



            if (refreshType == FT817.RefreshTypeFinished) {
                handlerRefreshFT817.postDelayed(runnableRefreshStatus, Delay);
                return;
            } else if (refreshType == FT817.RefreshTypeInterrupt) {
                handlerRefreshFT817.postDelayed(runnableRefreshStatus, InterruptDelay);
                return;
            }

            int cmd= msg.what;
            FT817.Status status = (FT817.Status) msg.obj;
            switch (cmd) {
                case FT817.STATUS_CMD_READ_FREQ_MODE_STATUS:
                    tvModeLSB.setTextColor(modeColorDisabled);
                    tvModeUSB.setTextColor(modeColorDisabled);
                    tvModeCW.setTextColor(modeColorDisabled);
                    tvModeCWR.setTextColor(modeColorDisabled);
                    tvModeAM.setTextColor(modeColorDisabled);
                    tvModeWFM.setTextColor(modeColorDisabled);
                    tvModeFM.setTextColor(modeColorDisabled);
                    tvModeDIG.setTextColor(modeColorDisabled);
                    tvModePKT.setTextColor(modeColorDisabled);

                    if (status.freq == 0) {
                        tvFreq1.setText(Html.fromHtml("<font color='#1E1E1E'>000</font>"), TextView.BufferType.SPANNABLE);
                        tvFreq2.setText(Html.fromHtml("<font color='#1E1E1E'>000</font>"), TextView.BufferType.SPANNABLE);
                        tvFreq3.setText(Html.fromHtml("<font color='#1E1E1E'>00</font>"), TextView.BufferType.SPANNABLE);

                    }

                    char[] frq = String.format(FreqFormat, status.freq).toCharArray();

                    String styleText1 = "";
                    boolean hasNonZero = false;
                    for (int i = 0; i < 3; i++) {

                        if (hasNonZero) {
                            styleText1 += frq[i];
                        } else {
                            if (frq[i] == 48) {
                                styleText1 += "<font color='#1E1E1E'>" + frq[i] + "</font>";
                            } else {
                                styleText1 += frq[i];
                                hasNonZero = true;
                            }
                        }
                    }
                    tvFreq1.setText(Html.fromHtml(styleText1), TextView.BufferType.SPANNABLE);

                    tvFreq2.setText(frq, 3, 3);
                    tvFreq3.setText(frq, 6, 2);

                    switch (status.mode) {
                        case "LSB":
                            tvModeLSB.setTextColor(modeColorEnabled);
                            break;
                        case "USB":
                            tvModeUSB.setTextColor(modeColorEnabled);
                            break;
                        case "CW":
                            tvModeCW.setTextColor(modeColorEnabled);
                            break;
                        case "CWR":
                            tvModeCWR.setTextColor(modeColorEnabled);
                            break;
                        case "AM":
                            tvModeAM.setTextColor(modeColorEnabled);
                            break;
                        case "WFM":
                            tvModeWFM.setTextColor(modeColorEnabled);
                            break;
                        case "FM":
                            tvModeFM.setTextColor(modeColorEnabled);
                            break;
                        case "DIG":
                            tvModeDIG.setTextColor(modeColorEnabled);
                            break;
                        case "PKT":
                            tvModePKT.setTextColor(modeColorEnabled);
                            break;

                    }
                    break;

                case FT817.STATUS_CMD_READ_RX_STATUS:
                    if (status.sqlon) {
                        tvSQL.setTextColor(rxColorOff);
                    } else {
                        tvSQL.setTextColor(rxColorOn);
                    }

                    int p = status.smeter * (100 / 9);
                    p = p > 0 ? p + 1 : p;
                    pbSMeter.setProgress(p);
                    break;

                case FT817.STATUS_CMD_CMD_READ_ROM_57:
                    if (status.lockoff){
                        imgvLockon.setVisibility(View.INVISIBLE);
                    } else {
                        imgvLockon.setVisibility(View.VISIBLE);
                    }
                    break;

                case FT817.STATUS_CMD_CMD_READ_ROM_55:
                    switch (status.channel){
                        case FT817.ROM_CH_HOME:
                            tvChannel.setText("HOME");
                            break;
                        case FT817.ROM_CH_MEM:
                            tvChannel.setText("MEM");
                            break;
                        case FT817.ROM_CH_MTUNE:
                            tvChannel.setText("MTUNE");
                            break;
                        case FT817.ROM_CH_MTQMB:
                            tvChannel.setText("MTWMB");
                            break;
                        case FT817.ROM_CH_QMB:
                            tvChannel.setText("QMB");
                            break;
                        case FT817.ROM_CH_VFOA:
                            tvChannel.setText("VFOa");
                            break;
                        case FT817.ROM_CH_VFOB:
                            tvChannel.setText("VFOb");
                            break;
                    }
                    break;

                case FT817.STATUS_CMD_CMD_READ_ROM_79:
                    switch (status.txPower) {
                        case FT817.ROM_TX_PWR_1W:
                            tvTxpwr.setText("1W");
                            break;
                        case FT817.ROM_TX_PWR_5W:
                            tvTxpwr.setText("5W");
                            break;
                        case FT817.ROM_TX_PWR_05W:
                            tvTxpwr.setText("0.5W");
                            break;
                        case FT817.ROM_TX_PWR_25W:
                            tvTxpwr.setText("2.5W");
                            break;
                    }
                    break;

            }

            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_monitor);

        modeColorDisabled = ContextCompat.getColor(this, R.color.mode_disabled);
        modeColorEnabled = ContextCompat.getColor(this, R.color.mode_enabled);
        rxColorOff = ContextCompat.getColor(this, R.color.rx_off);
        rxColorOn = ContextCompat.getColor(this, R.color.rx_on);
        btConnected = ContextCompat.getColor(this, R.color.bt_connected);
        btDisconnected = ContextCompat.getColor(this, R.color.bt_disconnected);

        tvModeLSB = (TextView) findViewById(R.id.tv_lsb);
        tvModeUSB = (TextView) findViewById(R.id.tv_usb);
        tvModeCW = (TextView) findViewById(R.id.tv_cw);
        tvModeCWR = (TextView) findViewById(R.id.tv_cwr);
        tvModeAM = (TextView) findViewById(R.id.tv_am);
        tvModeWFM = (TextView) findViewById(R.id.tv_wfm);
        tvModeFM = (TextView) findViewById(R.id.tv_fm);
        tvModeDIG = (TextView) findViewById(R.id.tv_dig);
        tvModePKT = (TextView) findViewById(R.id.tv_pkt);

        tvChannel = (TextView) findViewById(R.id.tv_channel);
        tvTxpwr = (TextView) findViewById(R.id.tv_txpwr);

        imgvLockon = (ImageView) findViewById(R.id.imgv_lockon);

        tvFreq1 = (TextView) findViewById(R.id.tv_freq1);
        tvFreq2 = (TextView) findViewById(R.id.tv_freq2);
        tvFreq3 = (TextView) findViewById(R.id.tv_freq3);

        tvFreq1.setText(Html.fromHtml("<font color='#1E1E1E'>000</font>"), TextView.BufferType.SPANNABLE);
        tvFreq2.setText(Html.fromHtml("<font color='#1E1E1E'>000</font>"), TextView.BufferType.SPANNABLE);
        tvFreq3.setText(Html.fromHtml("<font color='#1E1E1E'>00</font>"), TextView.BufferType.SPANNABLE);

        pbSMeter = (ProgressBar) findViewById(R.id.pb_smeter);

        tvSQL = (TextView) findViewById(R.id.tv_rx);

        ibtnBtDevices = (ImageButton) findViewById(R.id.ibtn_bt_devices);

        ibtnBtDevices.setBackgroundColor(btDisconnected);

        // 蓝牙设备按钮
        ibtnBtDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (null != ft817) {
            handlerRefreshFT817.removeCallbacks(runnableRefreshStatus);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (null != ft817) {
            if (!connecting) {
                handlerRefreshFT817.postDelayed(runnableRefreshStatus, Delay);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != ft817) {

            ft817.disconnect();
            handlerRefreshFT817.removeCallbacks(runnableRefreshStatus);

        }
    }


    Handler startHandler = new Handler() {
        public void handleMessage(Message msg) {
            String address = (String) msg.obj;

            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);


            try {

                ft817 = new FT817(device);

                ft817.setRecivedRefreshStatusListener(MonitorActivity.this);

                ft817.connect();
                Log.i(LOG_TAG, "connected device");
//                handlerRefreshFT817.post(runnableRefreshFreqMode);
//                handlerRefreshFT817.post(runnableRefreshRX);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            connecting = true;
            if (null != ft817) {
                ft817.disconnect();
                ft817 = null;
                ibtnBtDevices.setBackgroundColor(btDisconnected);
            }

            this.registerReceiver(btDeviceReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
            this.registerReceiver(btDeviceReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));

            Message msg = Message.obtain();
            msg.obj = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            startHandler.sendMessage(msg);
        }
    }


    @Override
    public void onRecivedRefreshStatus(int cmd, int refreshType, FT817.Status status) {
        Message msg = Message.obtain();
        msg.what = cmd;
        msg.arg1 = refreshType;
        msg.obj = status;

        handerUpdateUI.sendMessage(msg);
    }


    private final BroadcastReceiver btDeviceReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getAddress().equals(ft817.mmDevice.getAddress())) {
                    if (connecting) {
                        handlerRefreshFT817.postDelayed(runnableRefreshStatus, Delay);
                        ibtnBtDevices.setBackgroundColor(btConnected);

                        connecting = false;
                    }
                    Toast.makeText(MonitorActivity.this, R.string.device_connected, Toast.LENGTH_SHORT).show();

                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (null != ft817) {
                    if (device.getAddress().equals(ft817.mmDevice.getAddress())) {
                        initMonitor();
                        ibtnBtDevices.setBackgroundColor(btDisconnected);
                        ft817.disconnect();
                        ft817 = null;
                        Toast.makeText(MonitorActivity.this, R.string.device_disconnected, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    private void initMonitor() {
        tvModeLSB.setTextColor(modeColorDisabled);
        tvModeUSB.setTextColor(modeColorDisabled);
        tvModeCW.setTextColor(modeColorDisabled);
        tvModeCWR.setTextColor(modeColorDisabled);
        tvModeAM.setTextColor(modeColorDisabled);
        tvModeWFM.setTextColor(modeColorDisabled);
        tvModeFM.setTextColor(modeColorDisabled);
        tvModeDIG.setTextColor(modeColorDisabled);
        tvModePKT.setTextColor(modeColorDisabled);

        imgvLockon.setVisibility(View.INVISIBLE);

        tvFreq1.setText(Html.fromHtml("<font color='#1E1E1E'>000</font>"), TextView.BufferType.SPANNABLE);
        tvFreq2.setText(Html.fromHtml("<font color='#1E1E1E'>000</font>"), TextView.BufferType.SPANNABLE);
        tvFreq3.setText(Html.fromHtml("<font color='#1E1E1E'>00</font>"), TextView.BufferType.SPANNABLE);

        tvSQL.setTextColor(rxColorOff);

        tvTxpwr.setText("");
        tvChannel.setText("");

        pbSMeter.setProgress(0);

    }

}