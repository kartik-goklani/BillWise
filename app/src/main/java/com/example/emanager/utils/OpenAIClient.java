package com.example.emanager.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenAIClient {

    private static final String API_KEY = "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"; // Replace with your key
    private static final String TAG = "OpenAIClient";

    public interface ResponseCallback {
        void onResponse(String result);
        void onError(String error);
    }

    public static void sendToOpenAI(String ocrText, final ResponseCallback callback) {

        OkHttpClient client = new OkHttpClient();

        String json = "{\n" +
                "  \"model\": \"gpt-3.5-turbo\",\n" +
                "  \"messages\": [\n" +
                "    {\"role\": \"system\", \"content\": \"Extract structured data from this receipt in JSON format with the following fields: 'date', 'vendor_name', 'items' (as a list of objects with 'name', 'quantity', 'price'), and the last field must be 'total_expense'. Ensure 'total_expense' is the last key in the JSON. Respond with JSON only, no explanation.\"},\n" +
                "    {\"role\": \"user\", \"content\": " + JSONObject.quote(ocrText) + "}\n" +
                "  ]\n" +
                "}";

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
                Log.e(TAG, "API Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Response failed: " + response.message());
                    return;
                }

                String responseBody = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    JSONArray choices = jsonObject.getJSONArray("choices");
                    String result = choices.getJSONObject(0).getJSONObject("message").getString("content");

                    callback.onResponse(result);

                } catch (JSONException e) {
                    callback.onError("JSON parsing error: " + e.getMessage());
                }
            }
        });
    }
}