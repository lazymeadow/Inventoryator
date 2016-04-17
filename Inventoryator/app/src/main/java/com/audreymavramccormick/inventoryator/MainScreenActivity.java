package com.audreymavramccormick.inventoryator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.List;


public class MainScreenActivity extends AppCompatActivity {

    public static final String TAG = "MainScreenActivity";

    private LocalBroadcastManager mLBM;
    private User mUser;

    private BroadcastReceiver mAuthorizedUserReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            new FetchInventoriesSizeTask().execute();
        }
    };

    private BroadcastReceiver mInvalidUserReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            Intent newIntent = UserAuthActivity.newIntent(context);
            startActivity(newIntent);
            MainScreenActivity.this.finish();
        }
    };

    private BroadcastReceiver mInventorySizeReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            Intent newIntent;
            if (intent.getIntExtra("size", 0) > 0) {
                newIntent = LoadInventoryActivity.newIntent(context);
            } else {
                newIntent = NewUserActivity.newIntent(context);
            }
            startActivity(newIntent);
            MainScreenActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        mLBM = LocalBroadcastManager.getInstance(this);
        mLBM.registerReceiver(mAuthorizedUserReceived,
                new IntentFilter("valid user"));
        mLBM.registerReceiver(mInvalidUserReceived,
                new IntentFilter("invalid user"));
        mLBM.registerReceiver(mInventorySizeReceived,
                new IntentFilter("inventory size"));

        mUser = User.findById(User.class, 1);
        if (mUser != null) {
            Log.i(TAG, "user found");
            new CheckCredentialsTask().execute();
        } else {
            Log.i(TAG, "no table");
            Intent intent = UserAuthActivity.newIntent(getApplicationContext());
            startActivity(intent);
            MainScreenActivity.this.finish();
        }
    }

    private class CheckCredentialsTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.i(TAG, mUser.toString());
            return User.checkUser(mUser);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.i(TAG, result.toString());
            if (result) {
                Log.i(TAG, "Credentials Authorized.");
                Intent intent = new Intent("valid user");
                mLBM.sendBroadcast(intent);
            } else {
                Log.i(TAG, "Credentials Invalid.");
                Intent intent = new Intent("invalid user");
                mLBM.sendBroadcast(intent);
            }
        }
    }

    private class FetchInventoriesSizeTask extends AsyncTask<Void, Void, List<Inventory>> {
        @Override
        protected List<Inventory> doInBackground(Void... params) {
            return Inventory.getInventories(User.findById(User.class, 1));
        }

        @Override
        protected void onPostExecute(List<Inventory> inventories) {
            Intent intent = new Intent("inventory size");
            intent.putExtra("size", inventories.size());
            mLBM.sendBroadcast(intent);
        }
    }
}
