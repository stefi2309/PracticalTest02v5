package ro.pub.cs.systems.eim.practicaltest02v5.network;


import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import ro.pub.cs.systems.eim.practicaltest02v5.general.Constants;
import ro.pub.cs.systems.eim.practicaltest02v5.general.Utilities;

public class CommunicationThread extends Thread {

    private final ServerThread serverThread;
    private final Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }

        try {
            BufferedReader br = Utilities.getReader(socket);
            PrintWriter pw = Utilities.getWriter(socket);

            String operation = br.readLine();
            String key = br.readLine();
            String value = br.readLine();

            if (operation == null || key == null || operation.trim().isEmpty() || key.trim().isEmpty()) {
                pw.println("ERROR: operation/key missing");
                pw.flush();
                return;
            }

            operation = operation.trim().toLowerCase();
            key = key.trim();

            if (Constants.OP_PUT.equals(operation)) {
                serverThread.put(key, value == null ? Constants.EMPTY_STRING : value);
                pw.println("OK (stored key=" + key + ")");
                pw.flush();
                return;
            }

            if (Constants.OP_GET.equals(operation)) {
                String cached = serverThread.get(key);
                if (cached != null) {
                    pw.println("CACHE HIT");
                    pw.println(cached);
                    pw.flush();
                    return;
                }

                String fetched = fetchFromTimeNow(key);
                if (fetched == null) {
                    pw.println("ERROR: could not fetch timezone for key=" + key);
                } else {
                    // opțional: cache rezultat
                    serverThread.put(key, fetched);
                    pw.println("CACHE MISS -> FETCHED");
                    pw.println(fetched);
                }
                pw.flush();
                return;
            }

            pw.println("ERROR: unknown operation (use get / put)");
            pw.flush();

        } catch (Exception e) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private String fetchFromTimeNow(String key) {
        HttpsURLConnection conn = null;
        try {

            String safePath = buildSafeTimezonePath(key);

            URL url = new URL(Constants.TIME_NOW_BASE + safePath);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int code = conn.getResponseCode();
            if (code != 200) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader r =
                         new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }

            JSONObject obj = new JSONObject(sb.toString());


            String timezone = obj.optString("timezone", key);
            String datetime = obj.optString("datetime", "");
            String utcOffset = obj.optString("utc_offset", "");
            String dst = obj.optString("dst", "");

            return "timezone=" + timezone + "\n" +
                    "datetime=" + datetime + "\n" +
                    "utc_offset=" + utcOffset + "\n" +
                    "dst=" + dst;

        } catch (Exception e) {
            Log.e(Constants.TAG, "[FETCH] " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String buildSafeTimezonePath(String key) throws Exception {
        String[] parts = key.split("/");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            if (out.length() > 0) out.append("/");
            out.append(URLEncoder.encode(parts[i], "UTF-8"));
        }
        return out.toString();
    }
}

