package com.audreymavramccormick.inventoryator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class NewUserActivity extends AppCompatActivity {

    private Button mLoadInventoryButton;
    private Button mNewInventoryButton;
    private EditText mShareCodeEditText;
    private LocalBroadcastManager mLBM;

    private BroadcastReceiver mAddedInventoryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            if (intent.getBooleanExtra("result", false)) {
                Intent i = LoadInventoryActivity.newIntent(context);
                startActivity(i);
                NewUserActivity.this.finish();
            }
            else {
                Toast.makeText(context, "Failed to add inventory.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user);

        mLBM = LocalBroadcastManager.getInstance(this);
        mLBM.registerReceiver(mAddedInventoryReceiver,
                new IntentFilter("inventory added"));

        mLoadInventoryButton = (Button) findViewById(R.id.add_inventory_button);
        mLoadInventoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_add_inventory, null);
                mShareCodeEditText = (EditText) dialogView.findViewById(R.id.edit_text_share_code);

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setView(dialogView)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String code = mShareCodeEditText.getText().toString();
                                Log.i("NewUserActivity", code);
                                new AddInventoryTask().execute(code);
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        mNewInventoryButton = (Button) findViewById(R.id.new_inventory_button);
        mNewInventoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = NewInventoryActivity.newIntent(v.getContext());
                startActivity(intent);
            }
        });
    }


    public static Intent newIntent(Context packageContext) {
        Intent intent = new Intent(packageContext, NewUserActivity.class);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        //return true;
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private class AddInventoryTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            return Inventory.addInventory(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.i("NewUserActivity", "Add inventory:" + result);
            Intent intent = new Intent("inventory added");
            intent.putExtra("result", result);
            mLBM.sendBroadcast(intent);
        }
    }
}
