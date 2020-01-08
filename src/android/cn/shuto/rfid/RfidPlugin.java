package cn.shuto.rfid;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import com.szfore.api.SZForeElectricAPI;
import com.szfore.listener.CallbackListener;
import com.szfore.listener.RFIDListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RfidPlugin extends CordovaPlugin {

    //SDK
    private SZForeElectricAPI mAPI;
    //蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter = null;
    //设备蓝牙开关定义code
    private static final int REQUEST_ENABLE_BT = 1001;
    //蓝牙权限code
    private static final int REQUEST_PERMISSION_ACCESS_LOCATION = 1002;
    private CallbackContext mCallbackContext;

    //权限数组
    private String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

    //定义操作方法
    //打开设备蓝牙
    private static final String OPEN_BLUETOOTH = "open_bluetooth";
    //获取蓝牙列表
    private static final String GET_BLUETOOTH_LIST = "get_bluetooth_list";
    //链接某一设备
    private static final String CONNECT_BLUETOOTH = "connect_bluetooth";
    //读EPC
    private static final String READ = "read";
    //写EPC
    private static final String WRITE = "write";

    //蓝牙设备列表
    private JSONArray mBlueToothArray;
    //搜索出的蓝牙设备列表，其中有重复的,去重
    private List<String> mBlueToothAddressList;

    //判断设备是否已连接蓝牙
    private Boolean isConnect = false;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        //初始化sdk
        mAPI = SZForeElectricAPI.getInstance(cordova.getContext().getApplicationContext());
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mAPI.setRFIDListener(mRFIDListener);
    }

    /**
     * @param action          操作action
     * @param args            参数
     * @param callbackContext 回调
     * @return true / false
     * @throws JSONException
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.mCallbackContext = callbackContext;
        switch (action) {
            case OPEN_BLUETOOTH:
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    this.cordova.startActivityForResult(this, enableIntent, REQUEST_ENABLE_BT);
                } else {
                    this.mCallbackContext.success("蓝牙已开启");
                }
                break;
            case GET_BLUETOOTH_LIST:
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    this.cordova.startActivityForResult(this, enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (!hasPermission()) {
                        PermissionHelper.requestPermissions(this, REQUEST_PERMISSION_ACCESS_LOCATION, permissions);
                    } else {
                        doDiscovery();
                    }
                }
                break;
            case CONNECT_BLUETOOTH:
                if (isConnect) {
                    mCallbackContext.success("模块打开成功");
                } else {
                    JSONObject obj = args.getJSONObject(0);
                    String address = obj.getString("address");
                    if ("".equals(address)) {
                        this.mCallbackContext.error("蓝牙地址为空");
                    } else {
                        mAPI.onStart(address, new CallbackListener() {
                            @Override
                            public void callback(final boolean b, final String s) {
                                cordova.getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        isConnect = b;
                                        //主动向页面发送消息
                                        JSONObject obj = new JSONObject();
                                        try {
                                            if (b) {
                                                obj.put("code", 1);
                                            } else {
                                                obj.put("code", 0);
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        webView.loadUrl("javascript:sendDataToJs(" + obj.toString() + ");");
                                        //页面主动调用是返回值
                                        if (b) {
                                            mCallbackContext.success(s);
                                        } else {
                                            mCallbackContext.error(s);
                                        }
                                    }
                                });
                            }
                        });
                    }
                }
                break;
            case READ:
                if (!isConnect) {
                    this.mCallbackContext.error("蓝牙设备已断开,请检测是否连接");
                } else {
                    if (mAPI != null) {
                        mAPI.sendCmdOnReadTag();
                    } else {
                        this.mCallbackContext.error("插件初始化失败");
                    }
                }
                break;
            case WRITE:
                if (!isConnect) {
                    this.mCallbackContext.error("蓝牙设备已断开,请检测是否连接");
                } else {
                    if (mAPI != null) {
                        //获取客户端传来的epc值与编码方式。编码方式如果不传则默认0C
                        JSONObject obj1 = args.getJSONObject(0);
                        String number = obj1.getString("epc_number");
                        String epclen = "0c";
                        if (obj1.has("epc_len")) {
                            epclen = obj1.getString("epc_len");
                            if ("".equals(epclen)) {
                                epclen = "0c";
                            }
                        }
                        int value;
                        //将16进制0C转换为十进制数 12
                        value = Integer.parseInt(epclen, 16);
                        if (value % 2 != 0) {
                            mCallbackContext.error("epclen输入字节长度不能为奇数");
                        } else {
                            if ("".equals(number)) {
                                mCallbackContext.error("EPC值不能为空");
                            } else if (value * 2 != number.length()) {
                                mCallbackContext.error("输入内容长度与设置的字节长度不一致");
                            } else {
                                mAPI.sendCmdOnEditEPC(number);
                            }
                        }
                    } else {
                        this.mCallbackContext.error("插件初始化失败");
                    }
                }
                break;
            default:
                return false;
        }
        return true;
    }

    private RFIDListener mRFIDListener = new RFIDListener() {
        @Override
        public void onResult(final int i, final String s, final List<String> list) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (0 == i) {
                        if ("success".equals(s)) {
                            //读取成功的返回值
                            JSONObject obj = new JSONObject();
                            try {
                                obj.put("message", "读取成功");
                                obj.put("data", list);
                            } catch (JSONException e) {
                                mCallbackContext.error(e.getMessage());
                            }
                            mCallbackContext.success(obj);
                        } else if ("指令执行成功".equals(s)) {
                            mCallbackContext.success("写入成功");
                        }
                    } else {
                        mCallbackContext.error(s);
                    }

                }
            });
        }
    };

    private boolean hasPermission() {
        for (String p : permissions) {
            if (!PermissionHelper.hasPermission(this, p)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (requestCode == REQUEST_PERMISSION_ACCESS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doDiscovery();
            } else {
                this.mCallbackContext.error("请允许使用蓝牙权限");
            }
        }
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    /**
     * 搜索蓝牙设备列表
     */
    private void doDiscovery() {
        if (!mBluetoothAdapter.isDiscovering()) {
            // 注册蓝牙开始搜索广播
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            this.cordova.getActivity().registerReceiver(mReceiver, filter);

            // 注册蓝牙搜索完毕广播
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            this.cordova.getActivity().registerReceiver(mReceiver, filter);
            mBlueToothArray = new JSONArray();
            mBlueToothAddressList = new ArrayList<>();
            //开始搜索
            mBluetoothAdapter.startDiscovery();
        }

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mBlueToothAddressList.indexOf(device.getAddress()) == -1) {
                    mBlueToothAddressList.add(device.getAddress());
                    JSONObject object = new JSONObject();
                    try {
                        object.put("name", device.getName() == null ? "" : device.getName());
                        object.put("address", device.getAddress());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mBlueToothArray.put(object);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("message", "蓝牙搜索完毕");
                    obj.put("data", mBlueToothArray);
                } catch (JSONException e) {
                    mCallbackContext.error(e.getMessage());
                }
                mCallbackContext.success(obj);
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                this.mCallbackContext.success("蓝牙开启成功");
            } else {
                this.mCallbackContext.error("蓝牙开启失败");
            }
        }
    }

    /**
     * 断开蓝牙
     */
    private void tryDisconnect() {
        try {
            if (null != mAPI) {
                mAPI.onStop();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        tryDisconnect();
    }

}
