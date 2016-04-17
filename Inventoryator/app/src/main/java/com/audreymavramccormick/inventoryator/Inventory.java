package com.audreymavramccormick.inventoryator;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;


public class Inventory {
    private static final String TAG = "Inventory";
    private static Inventory sInventory = null;
    private static DatabaseTalker sDBTalker = new DatabaseTalker();

    private int id;
    private String name, description;
    private List<Item> items;

    public static Inventory getInventory() {
        return sInventory;
    }

    public static List<Inventory> getInventories(User user) {
        Gson gson = new Gson();
        List<Inventory> inventories = new ArrayList<>();

        try {
            JsonArray json = sDBTalker.doGet(DatabaseTalker.USER_INVENTORY(user)).getAsJsonArray();
            Log.i(TAG, json.toString());

            for (JsonElement i : json.getAsJsonArray()) {
                Inventory inv = gson.fromJson(i, Inventory.class);
                Log.i(TAG, inv.toString());
                inventories.add(inv);
            }

        } catch (ConnectException ce) {
            Log.e(TAG, "Connection failed", ce);
        }
        return inventories;
    }


    public static List<Item> getItems() {
        Gson gson = new Gson();
        List<Item> items = new ArrayList<>();
        try {
            JsonElement json = sDBTalker.doGet(DatabaseTalker.INVENTORY_ITEM(sInventory.getId(), User.findById(User.class, 1)));

            Log.i(TAG, json.toString());

            if (json.isJsonArray()) {
                for (JsonElement i : json.getAsJsonArray()) {
                    Item item = gson.fromJson(i, Item.class);
                    Log.i(TAG, item.toString());
                    items.add(item);
                }
            }
        } catch (ConnectException ce) {
            Log.e(TAG, "Connection failed", ce);
        }
        sInventory.items = items;
        return sInventory.items;
    }

    public static void removeInventory(int id) {
        try {
            JsonElement json = sDBTalker.doDelete(DatabaseTalker.USER_INVENTORY_ID(id, User.findById(User.class, 1)));
            if (json != null) {
                Log.i(TAG, json.toString());
                if (sInventory != null && sInventory.id == id) {
                    sInventory = null;
                }
            }
        } catch (ConnectException ce) {
            Log.e(TAG, "Connection failed", ce);
        }
    }

    public static void reloadInventory() {
        int id = sInventory.id;
        sInventory = loadInventory(id);
        sInventory.items = getItems();
    }

    public static Inventory loadInventory(int id) {
        Gson gson = new Gson();

        try {
            JsonElement json = sDBTalker.doGet(DatabaseTalker.INVENTORY_ID(id, User.findById(User.class, 1)));
            if (json != null) {
                sInventory = gson.fromJson(json, Inventory.class);
            } else {
                sInventory = null;
            }
            return sInventory;
        } catch (ConnectException ce) {
            Log.e(TAG, "Connection failed", ce);
        }
        return null;
    }

    /**
     * Creates new Inventory object.
     * Calls DatabaseTalker to save new inventory in database.
     * The singleton inventory is set to the new inventory.
     *
     * @param name        the name of the new inventory
     * @param description the descripton of the new inventory
     * @return whatever the singleton inventory is set to now
     */
    public static Inventory newInventory(String name, String description) {
        Gson gson = new Gson();
        Inventory i = new Inventory();
        if (!name.equals("")) {
            i.name = name;
        }
        i.description = description;
        JsonElement response = sDBTalker.doPost(DatabaseTalker.INVENTORY(User.findById(User.class, 1)), gson.toJson(i));
        if (response != null) {
            i.id = response.getAsJsonObject().get("id").getAsInt();
        } else {
            i.id = 0;
        }
        Log.i(TAG, "ID: " + String.valueOf(i.id));
        if ((i.id) > 0) {
            sInventory = i;
        } else {
            sInventory = null;
        }
        return sInventory;
    }

    public static Inventory editInventory(String name, String description) {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("description", description);
        JsonElement data = sDBTalker.doPut(DatabaseTalker.INVENTORY_ID(sInventory.getId(), User.findById(User.class, 1)), json.toString());
        sInventory = new Gson().fromJson(data.toString(), Inventory.class);
        return sInventory;
    }

    public static String getShareCode() {
        try {
            JsonElement data = sDBTalker.doGet(DatabaseTalker.SHARE_INVENTORY(sInventory.getId(), User.findById(User.class, 1)));
            if (data.isJsonObject()) {
                return data.getAsJsonObject().get("share_code").getAsString();
            }
        } catch (ConnectException ce) {
            Log.e(TAG, "Connection failed", ce);
        }
        return null;
    }

    public static boolean addInventory(String share_code) {
        JsonObject json = new JsonObject();
        JsonElement data = sDBTalker.doPut(DatabaseTalker.ADD_SHARED_INVENTORY(share_code, User.findById(User.class, 1)), null);
        return (data != null);
    }

    private Inventory() {
        this.name = "My Inventory";
        this.items = new ArrayList<>();
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public int getId() {
        return this.id;
    }

    public void addItem(Item i) {
        this.items.add(i);
    }

    public String toString() {
        return String.format("<Inventory>\n\tID:%d;\n\tName:%s;\n\tDescription:%s", this.id, this.name, this.description);
    }
}
