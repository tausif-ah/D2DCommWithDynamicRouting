package tausif.androidprojects.files;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class TransferService implements OnTransferFinishListener {
    private static final int MAX_THREAD = 4;
    private final Context context;
    private ExecutorService executorService;
    private ServerSocket socket;
    private Socket clientSocket;
    private Handler handler;

    public TransferService(Context context){
        this.context = context;
        handler = new Handler();
        executorService = Executors.newFixedThreadPool(MAX_THREAD);
    }

    public void startServer(int bindPort){
        executorService.execute(new ServerConnector(bindPort));
    }

    public void establishConnection(String serverIP,int port){
        executorService.execute(new Connector(serverIP,port));
    }

    @Override
    public void onError(final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSendSuccess(final String name) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,"packet send successfully ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onReceiveSuccess(final String name) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,"packet received successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class Connector implements Runnable{
        private final String serverIP;
        private final int port;

        public Connector(String serverIP, int port){
            this.serverIP = serverIP;
            this.port = port;
        }
        public void run(){
            try {
                clientSocket = new Socket(serverIP,port);
                clientSocket.setKeepAlive(true);
                executorService.execute(new FileReceiver(TransferService.this,clientSocket));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,"Connected with server" + serverIP, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,"Failed to connect with server" + serverIP, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }
    }

    private class ServerConnector implements Runnable{


        private final int port;

        public ServerConnector(int port){
            this.port = port;
        }
        public void run(){
            try {
                socket = new ServerSocket(port);
                clientSocket = socket.accept(); //accepting client connection
                clientSocket.setKeepAlive(true);
                executorService.execute(new FileReceiver(TransferService.this,clientSocket));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,"Server connected with client " + clientSocket.getInetAddress().getHostAddress(), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,"Failed to create server ", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    public void  sendFile(/*String path,String name*/String packet){
        executorService.execute(new FileSender(this, clientSocket,/*path,name*/packet));
    }

    public void shutdown(){
        if(clientSocket != null){
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        executorService.shutdownNow();
    }
}
