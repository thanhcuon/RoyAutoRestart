package com.github.athanh.royAutoRestart.utils;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {
    private final String url;

    public DiscordWebhook(String url) {
        this.url = url;
    }

    public void sendMessage(String content) {
        try {
            URL url = new URL(this.url);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String json = String.format("{\"content\": \"%s\"}", content);
            byte[] out = json.getBytes(StandardCharsets.UTF_8);

            OutputStream stream = connection.getOutputStream();
            stream.write(out);
            stream.flush();
            stream.close();

            connection.getInputStream().close();
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}