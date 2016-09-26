package hang.bh1sfz.catspp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by BH1SFZ.hang on 16-8-20.
 */
public class FT817 {
    private static final String LOG_TAG = "FT817-SPP";

    private static final UUID UUID_OTHER_DEVICE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public final BluetoothDevice mmDevice;
    private final BluetoothSocket mmSocket;
    private InputStream mmInput;
    private OutputStream mmOutput;


    // mode<string> <<->> mode<hex>
    private static final Map<Byte, String> ByteMode;
    private static final Map<String, Byte> ModeByte;

    static {
        ByteMode = new HashMap<>();
        ByteMode.put((byte) 0x00, "LSB");
        ByteMode.put((byte) 0x02, "CW");
        ByteMode.put((byte) 0x04, "AM");
        ByteMode.put((byte) 0x06, "WFM");
        ByteMode.put((byte) 0x0A, "DIG");
        ByteMode.put((byte) 0x01, "USB");
        ByteMode.put((byte) 0x03, "CWR");
        ByteMode.put((byte) 0x08, "FM");
        ByteMode.put((byte) 0xFC, "PKT");

        ModeByte = new HashMap<>();
        ModeByte.put("LSB", (byte) 0x00);
        ModeByte.put("CW", (byte) 0x02);
        ModeByte.put("AM", (byte) 0x04);
        ModeByte.put("WFM", (byte) 0x06);
        ModeByte.put("DIG", (byte) 0x0A);
        ModeByte.put("USB", (byte) 0x01);
        ModeByte.put("CWR", (byte) 0x03);
        ModeByte.put("FM", (byte) 0x08);
        ModeByte.put("PKT", (byte) 0xFC);
    }


    public interface OnRecivedRefreshStatusListener {
        void onRecivedRefreshStatus(int cmd, int refreshType, FT817.Status status);
    }

    public static final int RefreshTypeInterrupt = 0;
    public static final int RefreshTypeFinished = 1;
    public static final int RefreshTypeContinue = 2;
    private OnRecivedRefreshStatusListener recivedFreshStatusListener = null;


    public static final int STATUS_CMD_READ_FREQ_MODE_STATUS = 1;
    public static final int STATUS_CMD_READ_RX_STATUS = 2;
    public static final int STATUS_CMD_CMD_READ_ROM_57 = 3;
    public static final int STATUS_CMD_CMD_READ_ROM_55 = 4;
    public static final int STATUS_CMD_CMD_READ_ROM_79 = 5;
    public static final int STATUS_CMD_CMD_READ_ROM_7A = 6;


    // 静噪开(无信号) squelch on (no signal)
    private static final int RX_SQL_ON = 0b10000000;
    // CTCSS/DCS编码不符合 CTCSS/DCS Code is Un-Matched
    private static final int RX_TON_UNMATCH = 0b01000000;
    // 鉴频器不位于中间 Discriminator is OFF-Center
    private static final int RX_DSCM_OFF_CENTER = 0b00100000;

    // PTT off
    private static final int TX_PTT_OFF = 0b11111111;
    // 高驻波状态 HI SWR on
    private static final int TX_HISWR_ON = 0b01000000;
    // 异频状态 SPLIT on
    private static final int TX_SPLIT_OFF = 0b00100000;

    // 面板解锁 lock off
    private static final int ROM_LOCK_OFF = 0b01000000;


    // 记忆频道模式 mem channel
    public static final int ROM_CH_MEM = 0b10000000;

    public static final int ROM_CH_MTUNE = 0b00100000;


    // MTQMB记忆调谐频道 MTQMB channel
    public static final int ROM_CH_QMB = 0b00000100;
    // HOME频道 HOME channel
    public static final int ROM_CH_HOME = 0b00010000;
    // QMB频道 QMB channel
    public static final int ROM_CH_MTQMB = 0b00000010;

    // VFOa频道 VFOa channel
    public static final int ROM_CH_VFOA = 0b00000001;
    // VFOb频道 VFOb channel
    public static final int ROM_CH_VFOB = 0b00000000;


    // 噪音抑制 noise blanker
    private static final int ROM_NB_ON = 0b00100000;

    // 自动增益 AGC auto
    public static final int ROM_AGC_AUTO = 0b00000000;
    // 自动增益 AGC fast
    public static final int ROM_AGC_FAST = 0b00000001;
    // 自动增益 AGC slow
    public static final int ROM_AGC_SLOW = 0b00000010;
    // 自动增益 AGC off
    public static final int ROM_AGC_OFF = 0b00000011;

    // 发射功率5w tx power
    public static final int ROM_TX_PWR_5W = 0b00000000;
    // 发射功率2.5w tx power
    public static final int ROM_TX_PWR_25W = 0b00000001;
    // 发射功率1w tx power
    public static final int ROM_TX_PWR_1W = 0b00000010;
    // 发射功率0.5w tx power
    public static final int ROM_TX_PWR_05W = 0b00000011;

    // 异频模式 SPL on
    private static final int ROM_SPL_ON = 0b10000000;

    // 中继差转
    public static final int ROM_RPT_SIMPLEX = 0b00000000;
    // 中继差转
    public static final int ROM_RPT_MINUS = 0b01000000;
    // 中继差转
    public static final int ROM_RPT_PLUS = 0b10000000;
    // 中继差转
    public static final int ROM_RPT_NONSTANDARD = 0b11000000;


    // 读取频率和工作模式 Read Frequency & Mode Status
    private static final byte[] CMD_READ_FREQ_MODE_STATUS = new byte[]{0x00, 0x00, 0x00, 0x00, 0x03};

    // 读取接收状态信息 Read RX Status
    private static final byte[] CMD_READ_RX_STATUS = new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0xE7};

    // 读取发射状态信息 Read TX Status
    private static final byte[] CMD_READ_TX_STATUS = new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0xF7};

    // 加锁 lock on
    private static final byte[] CMD_LOCK_ON = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00};
    // 解锁 lock off
    private static final byte[] CMD_LOCK_OFF = new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0x80};
    // PTT按下 PTT on
    private static final byte[] CMD_PTT_ON = new byte[]{0x00, 0x00, 0x00, 0x00, 0x08};
    // PTT松开 PTT off
    private static final byte[] CMD_PTT_OFF = new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0x88};
    // 频率微调开 clar on
    private static final byte[] CMD_CLAR_ON = new byte[]{0x00, 0x00, 0x00, 0x00, 0x05};
    // 频率微调关 clar off
    private static final byte[] CMD_CLAR_OFF = new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0x85};
    // VFO-A/B
    private static final byte[] CMD_VFO_A_B = new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0x81};
    // 异频开 split on
    private static final byte[] CMD_SPLIT_ON = new byte[]{0x00, 0x00, 0x00, 0x00, 0x02};
    // 异频关 split off
    private static final byte[] CMD_SPLIT_OFF = new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0x82};
    // 电源开 power on
    private static final byte[] CMD_POWER_ON = new byte[]{0x00, 0x00, 0x00, 0x00, 0x0F};
    // 电源关 split off
    private static final byte[] CMD_POWER_OFF = new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0x8F};
    // 电源引导标志 dummy data
    private static final byte[] CMD_POWER_DUMMY = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00};


    // 读取rom eeprom
    private static final byte[] CMD_READ_ROM = new byte[]{0x00, 0x00, 0x00, 0x00, (byte) 0xBB};


    public FT817(BluetoothDevice device) throws IOException {
        mmDevice = device;
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID_OTHER_DEVICE);
    }

    public boolean isConnect() {
        return mmSocket.isConnected();
    }

    public void connect() throws IOException {
        mmSocket.connect();
        mmInput = mmSocket.getInputStream();
        mmOutput = mmSocket.getOutputStream();
    }

    public void disconnect() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    /**
     * recive refresh status
     *
     * @param listener
     */
    public void setRecivedRefreshStatusListener(OnRecivedRefreshStatusListener listener) {
        recivedFreshStatusListener = listener;
    }


    public class Status {
        public long freq;
        public String mode;
        public boolean sqlon;
        public int smeter;
        //
        public boolean tonUnmatch;
        // 鉴频器
        public boolean dscmOffcenter;
        // 面板锁
        public boolean lockoff;
        // 噪音抑制
        public boolean nbOn;
        // 自动增益
        public int agc;
        // 频道
        public int channel;
        // 发射功率模式
        public int txPower;
        // 异频开启
        public boolean splOn;
        // 差转
        public int rpt;
    }

    synchronized public void readStatus() {
        if (null != recivedFreshStatusListener) {
            Status status = new Status();
            try {
                //----- cmd READ_FREQ_MODE_STATUS
                byte[] datas = write(CMD_READ_FREQ_MODE_STATUS, 5);
                byte[] ftmp = datas;
                byte[] freqs = Arrays.copyOfRange(ftmp, 0, 4);
                String hexStr = Util.bytesToHex(freqs);
                status.freq = Long.parseLong(hexStr, 10);

                if (status.freq > 50000000 || ((status.freq % 30000000) == 1)) {
                    recivedFreshStatusListener.onRecivedRefreshStatus(0, RefreshTypeInterrupt, null);
                    return;
                }
                String mode = ByteMode.get(ftmp[4]);
                if (null == mode) {
                    recivedFreshStatusListener.onRecivedRefreshStatus(0, RefreshTypeInterrupt, null);
                    return;
                }

                status.mode = mode;

                recivedFreshStatusListener.onRecivedRefreshStatus(STATUS_CMD_READ_FREQ_MODE_STATUS, RefreshTypeContinue, status);

                //----- cmd READ_RX_STATUS
                datas = write(CMD_READ_RX_STATUS, 1);
                int rxdata = datas[0];

                // to unsigned byte
                rxdata = rxdata & 0xFF;

                status.smeter = ((rxdata << 4) & 0xFF) >> 4;
                if (status.smeter > 9) {
                    recivedFreshStatusListener.onRecivedRefreshStatus(0, RefreshTypeInterrupt, null);
                    return;
                }

                status.sqlon = (RX_SQL_ON & rxdata) == RX_SQL_ON;
                status.tonUnmatch = (RX_TON_UNMATCH & rxdata) == RX_TON_UNMATCH;
                status.dscmOffcenter = (RX_DSCM_OFF_CENTER & rxdata) == RX_DSCM_OFF_CENTER;

                recivedFreshStatusListener.onRecivedRefreshStatus(STATUS_CMD_READ_RX_STATUS, RefreshTypeContinue, status);


                byte[] cmd = CMD_READ_ROM;
//                //----- cmd 57
//                cmd[1] = 0x57;
//                datas = write(cmd, 1);
//                int data57 = datas[0];
//                data57 &= 0xFF;
//                // read lock status
//                status.lockoff = (data57 & ROM_LOCK_OFF) == ROM_LOCK_OFF;
//                // noise blanker
//                status.nbOn = (((data57 << 2)&0xFF >> 7) << 5) == ROM_NB_ON;
//                // AGC mode
//                status.agc = (data57 << 6)&0xFF >> 6;
//
//                recivedFreshStatusListener.onRecivedRefreshStatus(STATUS_CMD_CMD_READ_ROM_57, RefreshTypeContinue, status);

                //----- cmd 55
//                cmd[1] = 0x55;
//                datas = write(cmd, 1);
//                int channelByte = datas[0];
//                // read channel
//                status.channel = unpackChannel(channelByte & 0xFF);
//
//                recivedFreshStatusListener.onRecivedRefreshStatus(STATUS_CMD_CMD_READ_ROM_55, RefreshTypeContinue, status);

//                //----- cmd 79
//                cmd[1] = 0x79;
//                datas = write(cmd, 1);
//                int data79 = datas[0];
//                data79 &= 0xFF;
//                // TX power
//                status.txPower = (data79 << 6)&0xFF >> 6;
//
//                recivedFreshStatusListener.onRecivedRefreshStatus(STATUS_CMD_CMD_READ_ROM_79, RefreshTypeContinue, status);

//                //----- cmd 7A
//                cmd[1] = 0x7A;
//                datas = write(cmd, 1);
//                int data7A = datas[0];
//                data7A &= 0xFF;
//                // SPL on
//                status.splOn = ((data7A >> 7) << 7) == ROM_SPL_ON;
//
//                recivedFreshStatusListener.onRecivedRefreshStatus(STATUS_CMD_CMD_READ_ROM_7A, RefreshTypeContinue, status);

                recivedFreshStatusListener.onRecivedRefreshStatus(0, RefreshTypeFinished, null);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, e.toString());
                recivedFreshStatusListener.onRecivedRefreshStatus(0, RefreshTypeInterrupt, null);
            }
        }

    }

    synchronized private byte[] write(byte[] cmd, int readLen) throws Exception {
        byte[] data;
        int alen = 0;
        do {
            data = new byte[readLen];
            mmOutput.write(cmd);
            mmOutput.flush();

            alen = mmInput.available();
            if (alen > 0) {
                if (alen != readLen) {
                    mmInput.skip(alen);
                } else {
                    mmInput.read(data);
                }
            }

        } while (alen != readLen);
        return data;
    }


    /**
     * unpack channel
     *
     * @param data
     * @return
     */
    private int unpackChannel(int data) {

        if (((data >> 7) << 7) == ROM_CH_MEM) {
            if ((((data << 5)&0xFF >> 7) << 2) == ROM_CH_QMB) {
                return ROM_CH_QMB;
            } else if ((((data << 3)&0xFF >> 7) << 4) == ROM_CH_HOME) {
                return ROM_CH_HOME;
            } else if ((((data << 6)&0xFF >> 7) << 1) == ROM_CH_MTQMB) {
                return ROM_CH_MTQMB;
            } else if ((((data << 2)&0xFF >> 7) << 5) == ROM_CH_MTUNE) {
                return ROM_CH_MTUNE;
            } else {
                return ROM_CH_MEM;
            }
        } else {
            if (((data << 7)&0xFF >> 7) == ROM_CH_VFOA) {
                return ROM_CH_VFOA;
            } else {
                return ROM_CH_VFOB;
            }
        }
    }

}
