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

public class NewInventoryActivity extends AppCompatActivity {

    private static final String TAG = "NewInventoryActivity";

    private ProgressDialog progressDialog;
    private Button mCreateButton;
    private EditText mInventoryName;
    private String mName;
    private String mDescription;
    private EditText mInventoryDescription;
    private Inventory mNewInventory;
    private LocalBroadcastManager mLBM;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            progressDialog.cancel();
            if (mNewInventory == null) {
                Toast.makeText(context, "Inventory creation failed.", Toast.LENGTH_SHORT).show();
            } else {
                //load inventory activity with this inventory as the thingy
                Toast.makeText(context, String.format("Inventory %s created.", mNewInventory.getName()), Toast.LENGTH_SHORT).show();
                Intent newIntent = ViewInventoryActivity.newIntent(context);
                startActivity(newIntent);
                NewInventoryActivity.this.finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_inventory);
        mLBM = LocalBroadcastManager.getInstance(this);
        mLBM.registerReceiver(mMessageReceiver,
                new IntentFilter("inventory-created"));

        mInventoryName = (EditText) findViewById(R.id.new_inventory_name);
        mInventoryDescription = (EditText) findViewById(R.id.new_inventory_description);

        mCreateButton = (Button) findViewById(R.id.create_new_inventory_button);
        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mName = mInventoryName.getText().toString();
                mDescription = mInventoryDescription.getText().toString();
                new NewInventoryTask().execute();

            }
        });
    }

    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, NewInventoryActivity.class);
    }


    private class NewInventoryTask extends AsyncTask<String, Void, Inventory> {

        @Override
        protected Inventory doInBackground(String... params) {
            return Inventory.newInventory(mName, mDescription);
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(
                    NewInventoryActivity.this);
            progressDialog.setMessage("Creating " + mName + "...");
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

        @Override
        protected void onPostExecute(Inventory inventory) {
            mNewInventory = inventory;
            Intent intent = new Intent("inventory-created");
            mLBM.sendBroadcast(intent);
        }
    }
}
