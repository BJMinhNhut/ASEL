package com.cs426.asel.backend;


import android.content.Context;
import android.util.Log;

import com.cs426.asel.BuildConfig;
import com.cs426.asel.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class ChatGPTUtils {
    private static final String MODEL = "gemini-1.5-flash";
    private static final GenerationConfig CONFIG;

    static {
        GenerationConfig.Builder builder = new GenerationConfig.Builder();
        builder.temperature = 0.1f;
        CONFIG = builder.build();
    }

    public static ListenableFuture<GenerateContentResponse> getResponse(String prompt) {
        GenerativeModel gm = new GenerativeModel(MODEL, BuildConfig.API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder().addText(prompt).build();

        return model.generateContent(content);
    }

    public static JSONObject createEventFromMail(Mail mail) {
        JSONObject result = new JSONObject();

        return result;
    }

    private static String getAPIKey() {
        return BuildConfig.API_KEY;
    }

    public interface ResponseCallback {
        void onResponse(String response);
        void onError(String errorMessage);
    }
}
