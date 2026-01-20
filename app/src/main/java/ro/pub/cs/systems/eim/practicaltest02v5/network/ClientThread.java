package ro.pub.cs.systems.eim.practicaltest02v5.network;

import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import ro.pub.cs.systems.eim.practicaltest02v5.general.Constants;
import ro.pub.cs.systems.eim.practicaltest02v5.general.Utilities;

public class ClientThread extends Thread {

    private final String address;
    private final int port;
    private final String operation;
    private final String key;
    private final String value;
    private final TextView responseTextView;

    private Socket socket;

    public ClientThread(String address, int port, String operation, String key, String value, TextView responseTextView) {
        this.address = address;
        this.port = port;
        this.operation = operation;
        this.key = key;
        this.value = value;
        this.responseTextView = responseTextView;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(address, port);
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);

            printWriter.println(operation);
            printWriter.println(key);
            printWriter.println(value == null ? Constants.EMPTY_STRING : value);
            printWriter.flush();

            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            final String response = sb.toString().trim();
            responseTextView.post(() -> responseTextView.setText(response));

        } catch (IOException e) {
            Log.e(Constants.TAG, "[CLIENT THREAD] " + e.getMessage());
            responseTextView.post(() -> responseTextView.setText("Client error: " + e.getMessage()));
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}

