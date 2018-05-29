package tausif.androidprojects.files;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;



public class FileReceiver implements Runnable{

    private final Socket clientSocket;
    private final String TAG = FileReceiver.class.getName();
    private final OnTransferFinishListener onTransferFinishListener;

    public FileReceiver(OnTransferFinishListener onTransferFinishListener, Socket clientSocket){
        this.clientSocket = clientSocket;
        this.onTransferFinishListener = onTransferFinishListener;
    }

    @Override
    public void run() {
        while (clientSocket != null && clientSocket.isConnected()){
            DataInputStream dataInputStream = null;
            try {
                dataInputStream = new DataInputStream(clientSocket.getInputStream());
//                String fileName = null;
//                byte[] buffer = new byte[4096];
//                int contentLength = 0;
//                dataInputStream.readFully(buffer,0,2);
//                contentLength = buffer[0] * 256 + buffer[1];
//                dataInputStream.readFully(buffer,0,contentLength);
//                fileName = new String(buffer,0,contentLength);
//                dataInputStream.readFully(buffer,0,2);
//                contentLength = buffer[0] * 256 + buffer[1];
//                int read = 0;
//                int totalRead = 0;
//                File file = new File(Environment.getExternalStorageDirectory(),fileName);
//                FileOutputStream fileOutputStream = new FileOutputStream(file);
//                while (totalRead < contentLength && clientSocket.isConnected()){
//                    read = dataInputStream.read(buffer,0,buffer.length);
//                    fileOutputStream.write(buffer,0,read);
//                    totalRead += read;
//                }
//                fileOutputStream.close();
                byte readBytes[] = new byte[1024];
                InputStream inputStream = clientSocket.getInputStream();
                int numBytes = inputStream.read(readBytes);
                String receivedPacket = new String(readBytes);
                if(onTransferFinishListener != null){
                    onTransferFinishListener.onReceiveSuccess(/*fileName*/receivedPacket);
                }
                Log.e(TAG,"File received successfully");
            }
            catch (EOFException ex){
                ex.printStackTrace();
                break;
            }
            catch (Exception e) {
                e.printStackTrace();
                if(onTransferFinishListener != null){
                    onTransferFinishListener.onError(e.toString());
                }
            }
        }

    }
}
