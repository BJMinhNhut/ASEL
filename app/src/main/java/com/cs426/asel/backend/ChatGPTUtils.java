package com.cs426.asel.backend;


import android.content.Context;
import android.util.Log;

import com.cs426.asel.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ChatGPTUtils {
    private static final String  API_URL = "https://api.openai.com/v1/completions";
    private static final String MODEL = "gpt-3.5-turbo";

    public static String getResponse(String prompt, Context context) {
        StringBuilder response = new StringBuilder();
        String request = "{"
                + "\"model\": \"" + MODEL + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + prompt + "\"}],"
                + "\"temperature\": 0.1"
                + "}";

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(API_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Authorization", "Bearer " + getAPIKey(context));
                    connection.setDoOutput(true);

                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = request.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    } catch (Exception e) {
                        Log.e("ChatGPTUtils", "Error in writing request");
                        e.printStackTrace();
                    }

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        try (BufferedReader br = new BufferedReader(
                                new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                        }
                    } else {
                        System.err.println("Request failed with response code: " + responseCode);
                    }

                } catch (Exception e) {
                    Log.e("ChatGPTUtils", "Error in API request");
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.e("ChatGPTUtils", "Thread interrupted");
            e.printStackTrace();
        }

        return response.toString();
    }

    public static String getMailSummary(Mail mail, Context context) {
        String content = mail.getContent();
        String prompt = "Summarize the following email: " + content;

        return getResponse(prompt, context);
    }

    public static JSONObject createEventFromMail(Mail mail) {
        JSONObject result = new JSONObject();

        return result;
    }

    private static String getAPIKey(Context context) {
        return context.getString(R.string.api_key).toString();
    }
}
