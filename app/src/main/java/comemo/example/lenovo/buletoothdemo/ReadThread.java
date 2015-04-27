package comemo.example.lenovo.buletoothdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by lenovo on 2015/4/22.
 */
public class ReadThread extends Thread {


    private BluetoothSocket socket;
    private Handler handler;

    public ReadThread(BluetoothSocket socket, Handler handler) {
        this.socket = socket;

        this.handler = handler;
    }



    @Override
    public void run() {
        BluetoothDevice device = socket.getRemoteDevice();
        String utf;
        DataInputStream stream = null;
        try {
            stream = new DataInputStream(socket.getInputStream());
            while ((utf = stream.readUTF()) != null) {
                Log.d("read", device.getName() + "--" + utf);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
