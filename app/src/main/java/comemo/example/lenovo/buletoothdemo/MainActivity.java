package comemo.example.lenovo.buletoothdemo;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {
    private BluetoothAdapter bluetoothAdapter;
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private Device device;
    private String UUIDS = "4e3a500b-1ba9-4c3f-a5fe-76cb46608b5f";
    private BluetoothServerSocket service;
    private Map<BluetoothDevice, BluetoothSocket> socketMap = new ArrayMap<BluetoothDevice, BluetoothSocket>();
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    BluetoothDevice device = msg.getData().getParcelable(BluetoothDevice.EXTRA_DEVICE);
                    adapter.add(device);
                    break;
                case 1:
                    Toast.makeText(MainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = ((RecyclerView) findViewById(R.id.re));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));//这一行是什么意思？


        //获取蓝牙设备
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        adapter = new DeviceAdapter(new ArrayList<BluetoothDevice>(), this);
        recyclerView.setAdapter(adapter);
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "本设备没有蓝牙模块", Toast.LENGTH_SHORT).show();
//           System.exit(0);
            finish();
        }
        //检测蓝牙设备是否开启
        if (!bluetoothAdapter.isEnabled()) {
            //bluetoothAdapter.enable();//自动开启蓝牙设备
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 0);
        } else {
            //获取已经匹配的列表
            adapter.addAll(bluetoothAdapter.getBondedDevices());
            //获取到搜索到的列表名单
            discovery();
        }

    }

    //扫描蓝牙设备
    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    private void discovery() {
        bluetoothAdapter.startDiscovery();//通过广播通知找到的蓝牙设备
        device = new Device(handler);
        //找到蓝牙设备
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //注册广播
        registerReceiver(device, filter);
        try {
            service = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("", UUID.fromString(UUIDS));
            //不能再主线程中去接受请求
            new Thread() {
                @Override
                public void run() {
                    BluetoothSocket socket;
                    try {
                        while ((socket = service.accept()) != null) {
                            BluetoothDevice device = socket.getRemoteDevice();//发送者的信息

                            Bundle bundle = new Bundle();
                            bundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
                            Message message = handler.obtainMessage(0);
                            message.setData(bundle);
                            message.sendToTarget();
                            //等待线程
                            new ReadThread(socket, handler).start();//可以同时接受多个人的信息传递

//                            DataInputStream stream = new DataInputStream(socket.getInputStream());//接收到的数据
//                            Log.d("BluetoothSocket", stream.readUTF());
//   Log.d("BluetoothSocket", device.getName());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //解注册
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recyclerView != null) {
            unregisterReceiver(device);
        }
        if(service!=null){
            try {
                service.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Set<Map.Entry<BluetoothDevice,BluetoothSocket>> entries=socketMap.entrySet();
            for(Map.Entry<BluetoothDevice,BluetoothSocket> entry:entries){

            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            Toast.makeText(this, "开启", Toast.LENGTH_SHORT).show();
            adapter.addAll(bluetoothAdapter.getBondedDevices());
            discovery();
        } else {
            Toast.makeText(this, "失败", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onClick(View view) {

        int position = recyclerView.getChildPosition(view);
        final BluetoothDevice item = adapter.getItem(position);
        //版本十五以下的没有
//        ParcelUuid[] uuids = item.getUuids();
//        if (uuids != null) {
//            for (ParcelUuid uuid : uuids) {
//                Log.d("ss", uuid.toString());
//            }
//        } else {
//            Log.d("ss", "没有搜索到蓝牙装置");
//        }
        new Thread() {
            @Override
            public void run() {
                try {
                    BluetoothSocket socket = socketMap.get(item);
                    if (socket == null) {
                        socket = item.createRfcommSocketToServiceRecord(UUID.fromString(UUIDS));
                        //发起一个连接
                        socket.connect();
                        new ReadThread(socket, handler).start();
                        socketMap.put(item, socket);
                    }
                    //当连接成功的时候
                    DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                    stream.writeUTF("你好我是谢杰的粉丝");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
}
