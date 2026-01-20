package ro.pub.cs.systems.eim.practicaltest02v5.network;


import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import ro.pub.cs.systems.eim.practicaltest02v5.general.Constants;

public class ServerThread extends Thread {

    private ServerSocket serverSocket;
    private final HashMap<String, String> data = new HashMap<>();

    public ServerThread(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            Log.e(Constants.TAG, "[SERVER THREAD] " + e.getMessage());
        }
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public synchronized void put(String key, String value) {
        data.put(key, value);
    }

    public synchronized String get(String key) {
        return data.get(key);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Log.i(Constants.TAG, "[SERVER THREAD] Waiting for client...");
                Socket socket = serverSocket.accept();
                new CommunicationThread(this, socket).start();
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "[SERVER THREAD] " + e.getMessage());
        }
    }

    public void stopThread() {
        interrupt();
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
    }
}
