package com.audreymavramccormick.inventoryator;


import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NewItemActivity extends AppCompatActivity {

    private static final String TAG = "NewItemActivity";

    private ProgressDialog progressDialog;
    private Button mCreateButton;
    private EditText mItemName;
    private String mName;
    private int mNumber;
    private EditText mItemNumber;
    private Item mNewItem;
    private LocalBroadcastManager mLBM;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            progressDialog.cancel();
            if (mNewItem == null) {
                Toast.makeText(context, "Item creation failed.", Toast.LENGTH_SHORT).show();
            } else {
                Inventory.getInventory().addItem(mNewItem);
                //Toast.makeText(context, String.format("Item %s created.", mNewItem.getName()), Toast.LENGTH_SHORT).show();
                NewItemActivity.this.finish(); //is this the right function to stop the activity and go to the previous??
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_item);
        mLBM = LocalBroadcastManager.getInstance(this);
        mLBM.registerReceiver(mMessageReceiver,
                new IntentFilter("item-created"));

        mItemName = (EditText) findViewById(R.id.new_item_name);
        mItemNumber = (EditText) findViewById(R.id.new_item_number);

        mCreateButton = (Button) findViewById(R.id.create_new_item_button);
        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mName = mItemName.getText().toString();
                String number = mItemNumber.getText().toString();
                if (number.equals("")) {
                    mNumber = 0;
                } else {
                    mNumber = Integer.parseInt(number);
                }
                new NewItemTask().execute();
            }
        });
    }

    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, NewItemActivity.class);
    }


    private class NewItemTask extends AsyncTask<Void, Void, Item> {

        @Override
        protected Item doInBackground(Void... params) {
            return Item.newItem(mName, mNumber);
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(
                    NewItemActivity.this);
            progressDialog.setMessage("Creating " + mName + "...");
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

        @Override
        protected void onPostExecute(Item item) {
            mNewItem = item;
            Log.i(TAG, "New item: " + mNewItem.getName());
            Log.i(TAG, "Fetched contents: " + item.getName());
            Intent intent = new Intent("item-created");
            mLBM.sendBroadcast(intent);
        }
    }
}
