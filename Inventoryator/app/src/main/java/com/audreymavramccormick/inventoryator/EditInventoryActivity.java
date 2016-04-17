package com.audreymavramccormick.inventoryator;


import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class EditInventoryActivity extends AppCompatActivity {
    //Similar to NewInventoryActivity, but fields need to be populated
    private static final String TAG = "EditInventoryActivity";

    private ProgressDialog progressDialog;
    private Button mEditButton;
    private EditText mInventoryName;
    private String mName;
    private String mDescription;
    private EditText mInventoryDescription;
    private Inventory mUpdatedInventory;
    private LocalBroadcastManager mLBM;

    private BroadcastReceiver mUpdatedMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            progressDialog.cancel();
            if (mUpdatedInventory == null) {
                Toast.makeText(context, "Inventory update failed.", Toast.LENGTH_SHORT).show();
            } else {
                //load inventory activity with this inventory as the thingy
                EditInventoryActivity.this.finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_inventory);
        mLBM = LocalBroadcastManager.getInstance(this);
        mLBM.registerReceiver(mUpdatedMessageReceiver,
                new IntentFilter("inventory-updated"));

        mInventoryName = (EditText) findViewById(R.id.edit_inventory_name);
        mInventoryName.setText(Inventory.getInventory().getName());
        mInventoryDescription = (EditText) findViewById(R.id.edit_inventory_description);
        mInventoryDescription.setText(Inventory.getInventory().getDescription());

        mEditButton = (Button) findViewById(R.id.edit_inventory_button);
        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mName = mInventoryName.getText().toString();
                mDescription = mInventoryDescription.getText().toString();
                new EditInventoryTask().execute();

            }
        });
    }



    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, EditInventoryActivity.class);
    }


    private class EditInventoryTask extends AsyncTask<String, Void, Inventory> {

        @Override
        protected Inventory doInBackground(String... params) {
            return Inventory.editInventory(mName, mDescription);
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(
                    EditInventoryActivity.this);
            progressDialog.setMessage("Updating " + mName + "...");
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

        @Override
        protected void onPostExecute(Inventory inventory) {
            mUpdatedInventory = inventory;
            Log.i(TAG, "Updating inventory: " + mUpdatedInventory.getName());
            Log.i(TAG, "Fetched contents: " + inventory.getName());
            Intent intent = new Intent("inventory-updated");
            mLBM.sendBroadcast(intent);
        }
    }
}
