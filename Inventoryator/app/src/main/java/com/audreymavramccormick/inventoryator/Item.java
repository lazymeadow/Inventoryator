package com.audreymavramccormick.inventoryator;

import android.content.pm.PackageManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.ConnectException;

public class Item {
    private static final String TAG = "Item";
    private static Item sItem = null;
    private static DatabaseTalker sDBTalker = new DatabaseTalker();

    private String name;
    private int id, number;

    public static void setItem(Item item) {
        sItem = item;
    }

    public static Item getItem() {
        return sItem;
    }

    /**
     * Adds an item to the current active inventory.
     * Does a POST request to the API to save the item in the database.
     * Sets the current active item to the new item before returning.
     *
     * @param name the name of the item
     * @param number the number of items
     * @return the Item object
     */
    public static Item newItem(String name, int number) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("number", number);
        JsonElement json = sDBTalker.doPost(DatabaseTalker.INVENTORY_ITEM(Inventory.getInventory().getId(), User.findById(User.class, 1)), body.toString());

        if (json != null) {
            sItem = new Gson().fromJson(json.toString(), Item.class);
        }
        else {
            sItem = null;
        }

        return sItem;
    }

    public static boolean removeItem(int id) {
        try {
            JsonElement json = sDBTalker.doDelete(DatabaseTalker.ITEM(id));
            if (json != null) {
                //do stuff
                Log.i(TAG, "Item deleted");
                return true;
            }
        }
        catch (ConnectException ce) {
            Log.e(TAG, "Connection failed", ce);
        }
        return false;

    }

    public static Item editItem(String name, int number) {
        JsonObject body = new JsonObject();
        if (name != null && !name.equals("")) {
            body.addProperty("name", name);
        }
        if (number >= 0) {
            body.addProperty("number", number);
        }
        JsonElement json = sDBTalker.doPut(DatabaseTalker.ITEM(sItem.getId()), body.toString());
        if (json != null) {
            sItem = new Gson().fromJson(json, Item.class);
        }
        else {
            sItem = null;
        }
        Inventory.reloadInventory();
        return sItem;
    }

    public String getName() {
        return this.name;
    }

    public int getNumber() {
        return this.number;
    }

    public int getId() {
        return this.id;
    }
}
