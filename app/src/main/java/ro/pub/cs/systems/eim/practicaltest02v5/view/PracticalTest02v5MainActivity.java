package ro.pub.cs.systems.eim.practicaltest02v5.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ro.pub.cs.systems.eim.practicaltest02v5.R;
import ro.pub.cs.systems.eim.practicaltest02v5.general.Constants;
import ro.pub.cs.systems.eim.practicaltest02v5.network.ClientThread;
import ro.pub.cs.systems.eim.practicaltest02v5.network.ServerThread;

public class PracticalTest02v5MainActivity extends AppCompatActivity {

    private EditText serverPortEditText;
    private EditText clientAddressEditText;
    private EditText clientPortEditText;

    private EditText requestEditText;
    private TextView responseTextView;

    private ServerThread serverThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(Constants.TAG, "[MAIN ACTIVITY] onCreate()");
        setContentView(R.layout.activity_practical_test02v5_main);

        serverPortEditText = findViewById(R.id.server_port_edit_text);
        clientAddressEditText = findViewById(R.id.client_address_edit_text);
        clientPortEditText = findViewById(R.id.client_port_edit_text);

        requestEditText = findViewById(R.id.request_edit_text);
        responseTextView = findViewById(R.id.response_text_view);

        Button connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(v -> startServer());

        Button sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(v -> sendRequest());
    }

    private void startServer() {
        String serverPortStr = serverPortEditText.getText().toString().trim();
        if (serverPortStr.isEmpty()) {
            Toast.makeText(this, "Server port required", Toast.LENGTH_SHORT).show();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(serverPortStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Server port must be number", Toast.LENGTH_SHORT).show();
            return;
        }

        serverThread = new ServerThread(port);
        if (serverThread.getServerSocket() == null) {
            Toast.makeText(this, "Could not start server", Toast.LENGTH_SHORT).show();
            return;
        }
        serverThread.start();
        Toast.makeText(this, "Server started on " + port, Toast.LENGTH_SHORT).show();
    }

    private void sendRequest() {
        String clientAddress = clientAddressEditText.getText().toString().trim();
        String clientPortStr = clientPortEditText.getText().toString().trim();

        if (clientAddress.isEmpty() || clientPortStr.isEmpty()) {
            Toast.makeText(this, "Client address/port required", Toast.LENGTH_SHORT).show();
            return;
        }

        int clientPort;
        try {
            clientPort = Integer.parseInt(clientPortStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Client port must be number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (serverThread == null || !serverThread.isAlive()) {
            Toast.makeText(this, "No server running", Toast.LENGTH_SHORT).show();
            return;
        }

        String raw = requestEditText.getText().toString();
        String[] lines = raw.split("\\r?\\n");

        if (lines.length < 2) {
            Toast.makeText(this, "Format:\nget\\nKEY\nor\nput\\nKEY\\nVALUE", Toast.LENGTH_LONG).show();
            return;
        }

        String op = lines[0].trim().toLowerCase();
        String key = lines[1].trim();
        String value = (lines.length >= 3) ? lines[2] : Constants.EMPTY_STRING;

        if (op.isEmpty() || key.isEmpty()) {
            Toast.makeText(this, "Operation and key required", Toast.LENGTH_SHORT).show();
            return;
        }

        responseTextView.setText(Constants.EMPTY_STRING);

        ClientThread clientThread = new ClientThread(
                clientAddress, clientPort, op, key, value, responseTextView
        );
        clientThread.start();
    }

    @Override
    protected void onDestroy() {
        Log.i(Constants.TAG, "[MAIN ACTIVITY] onDestroy()");
        if (serverThread != null) {
            serverThread.stopThread();
        }
        super.onDestroy();
    }
}
