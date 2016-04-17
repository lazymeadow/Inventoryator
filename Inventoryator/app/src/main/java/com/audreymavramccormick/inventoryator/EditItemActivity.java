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

public class EditItemActivity extends AppCompatActivity {
    private static final String TAG = "NewItemActivity";

    private ProgressDialog progressDialog;
    private Button mEditButton;
    private EditText mItemName;
    private String mName;
    private int mNumber;
    private EditText mItemNumber;
    private Item mSavedItem;
    private LocalBroadcastManager mLBM;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            progressDialog.cancel();
            if (mSavedItem == null) {
                Toast.makeText(context, "Item update failed.", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(context, String.format("Item %s created.", mSavedItem.getName()), Toast.LENGTH_SHORT).show();
                EditItemActivity.this.finish(); //is this the right function to stop the activity and go to the previous??
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_item);
        mLBM = LocalBroadcastManager.getInstance(this);
        mLBM.registerReceiver(mMessageReceiver,
                new IntentFilter("item-updated"));

        mItemName = (EditText) findViewById(R.id.edit_item_name);
        mItemName.setText(Item.getItem().getName());
        mItemNumber = (EditText) findViewById(R.id.edit_item_number);
        mItemNumber.setText(String.valueOf(Item.getItem().getNumber()));

        mEditButton = (Button) findViewById(R.id.update_item_button);
        mEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mName = mItemName.getText().toString();
                String number = mItemNumber.getText().toString();
                if (number.equals("")) {
                    mNumber = 0;
                } else {
                    mNumber = Integer.parseInt(number);
                }
                new EditItemTask().execute();
            }
        });
    }

    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, EditItemActivity.class);
    }


    private class EditItemTask extends AsyncTask<Void, Void, Item> {

        @Override
        protected Item doInBackground(Void... params) {
            return Item.editItem(mName, mNumber);
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(
                    EditItemActivity.this);
            progressDialog.setMessage("Updating " + mName + "...");
            progressDialog.setCancelable(false);
            progressDialog.show();

        }

        @Override
        protected void onPostExecute(Item item) {
            mSavedItem = item;
            Log.i(TAG, "Update item: " + mSavedItem.getName());
            Log.i(TAG, "Fetched contents: " + item.getName());
            Intent intent = new Intent("item-updated");
            mLBM.sendBroadcast(intent);
        }
    }
}
