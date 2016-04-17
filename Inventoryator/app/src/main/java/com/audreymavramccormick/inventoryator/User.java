package com.audreymavramccormick.inventoryator;


import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

public class User extends SugarRecord {
    String username;
    String password;

    @Ignore
    private static DatabaseTalker sDBTalker = new DatabaseTalker();

    public static boolean checkUser(User user) {
        Log.i("User", "checkUser");
        Gson gson = new Gson();
        return (sDBTalker.doPut(DatabaseTalker.LOGIN, gson.toJson(user)) != null);
    }

    public static boolean registerUser(String username, String password1, String password2) {
        Log.i("User", "registerUser");
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("password1", password1);
        json.addProperty("password2", password2);
        return (sDBTalker.doPost(DatabaseTalker.NEW_USER, json.toString()) != null);
    }

    public String toString() {
        return String.format("%s, %s", username, password);
    }
}
