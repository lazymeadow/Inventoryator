package com.audreymavramccormick.inventoryator;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;

public class DatabaseTalker {
    private static final String TAG = "DATABASE TALKER";
    private static final String API_INVENTORY_URI = "http://52.33.241.37:5000"; //"http://72.24.211.109:5000/";

    public static String INVENTORY(User user) {
        Log.i(TAG, user.password);
        return String.format("%s/inventory/?username=%s&password=%s", API_INVENTORY_URI, user.username, user.password);
    }

    public static String ITEM(int id) {
        return String.format("%s/item/%d/", API_INVENTORY_URI, id);
    }

    public static String INVENTORY_ID(int id, User user) {
        return String.format("%s/inventory/%d/?username=%s&password=%s", API_INVENTORY_URI, id, user.username, user.password);
    }

    public static String INVENTORY_ITEM(int id, User user) {
        return String.format("%s/inventory/%d/items/?username=%s&password=%s", API_INVENTORY_URI, id, user.username, user.password);
    }

    public static final String LOGIN = API_INVENTORY_URI + "/login/";

    public static final String NEW_USER = API_INVENTORY_URI + "/user/";

    public static String USER(String username) {
        return API_INVENTORY_URI + String.format("/user/%s/", username);
    }

    public static String SHARE_INVENTORY(int id, User user) {
        return String.format("%s/inventory/%d/share/?username=%s&password=%s", API_INVENTORY_URI, id, user.username, user.password);
    }

    public static String ADD_SHARED_INVENTORY(String code, User user) {
        return String.format("%s/user/%s/share/%s/?password=%s", API_INVENTORY_URI, user.username, code, user.password);
    }

    public static String USER_INVENTORY(User user) {
        return API_INVENTORY_URI + String.format("/user/%s/inventory/?password=%s", user.username, user.password);
    }

    public static String USER_INVENTORY_ID(int id, User user) {
        return API_INVENTORY_URI + String.format("/user/%s/inventory/%d/?password=%s", user.username, id, user.password);
    }

    private static final MediaType JSON = MediaType.parse("application/json");// charset=utf-8");

    private static OkHttpClient client = new OkHttpClient();

    private User user;

    public JsonElement doPost(String url, String json) {
        Log.i(TAG, "doPost");
        try {
            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            String data = response.body().string();
            APIResponse apiResponse = new Gson().fromJson(data, APIResponse.class);
            if (apiResponse.success) {
                return apiResponse.data;
            } else {
                Log.i(TAG, apiResponse.error);
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to post item(s)", ioe);
        }
        return null;
    }

    /**
     * Perform a GET request on the provided URL String
     *
     * @param url
     * @return "data" portion of the response json, or null on error
     */
    public JsonElement doGet(String url) throws ConnectException {
        Log.i(TAG, "doGet");
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            String data = response.body().string();
            Log.i(TAG, data);
            APIResponse apiResponse = new Gson().fromJson(data, APIResponse.class);
            Log.i(TAG, String.format("BOOLEAN RESPONSE %s", Boolean.toString(apiResponse.success)));
            if (apiResponse.success) {
                return apiResponse.data;
            } else {
                Log.i(TAG, apiResponse.error);
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to get item(s)", ioe);
        }
        return null;
    }

    public JsonElement doPut(String url, String json) {
        Log.i(TAG, "doPut");
        try {
            Request request;
            if (json != null) {
                RequestBody body = RequestBody.create(JSON, json);
                request = new Request.Builder()
                        .url(url)
                        .put(body)
                        .build();
            }
            else {
                request = new Request.Builder()
                        .url(url)
                        .put(null)
                        .build();
            }
            Response response = client.newCall(request).execute();
            String data = response.body().string();
            APIResponse apiResponse = new Gson().fromJson(data, APIResponse.class);
            Log.i(TAG, "Received:" + apiResponse.success + ", " + apiResponse.data);
            if (apiResponse.success) {
                return apiResponse.data;
            } else {
                Log.i(TAG, apiResponse.error);
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to put item(s)", ioe);
        }
        return null;
    }

    public JsonElement doDelete(String url) throws ConnectException {
        Log.i(TAG, "doDelete");
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .build();
            Response response = client.newCall(request).execute();
            String data = response.body().string();
            Log.i(TAG, data);
            APIResponse apiResponse = new Gson().fromJson(data, APIResponse.class);
            if (apiResponse.success) {
                return apiResponse.data;
            } else {
                Log.i(TAG, apiResponse.error);
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Failed perform delete", ioe);
        }
        return null;
    }


    private class APIResponse {
        JsonElement data;
        boolean success;
        String error;
    }

}
