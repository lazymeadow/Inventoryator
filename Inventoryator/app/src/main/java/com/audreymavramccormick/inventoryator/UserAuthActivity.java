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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class UserAuthActivity extends AppCompatActivity {

    public static final String TAG = "UserAuthActivity";

    private ProgressDialog progressDialog;
    private Button mLoginButton, mRegisterButton;
    private LocalBroadcastManager mLBM;
    private EditText mUsernameEditText, mPasswordEditText;
    private User mUser;

    private BroadcastReceiver mAuthorizedUserReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            if (progressDialog != null) {
                progressDialog.cancel();
            }
            if (intent.getBooleanExtra("result", false)) {
                Intent newIntent = LoadInventoryActivity.newIntent(context);
                startActivity(newIntent);
                UserAuthActivity.this.finish();
            }
            else {
                Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_auth);


        mLBM = LocalBroadcastManager.getInstance(this);
        mLBM.registerReceiver(mAuthorizedUserReceived,
                new IntentFilter("checked user"));

        mUsernameEditText = (EditText) findViewById(R.id.username_edit_text);
        mPasswordEditText = (EditText) findViewById(R.id.password_edit_text);

        mUser = User.findById(User.class, 1);
        if (mUser != null) {
            Log.i(TAG, "user found");
            mUsernameEditText.setText(mUser.username);
        } else {
            Log.i(TAG, "no table");
            mUser = new User();
        }

        mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUser.username = mUsernameEditText.getText().toString();
                mUser.password = PasswordHasher.bin2hex(PasswordHasher.getHash(mPasswordEditText.getText().toString()));
                Log.i(TAG, mUser.password);
                mUser.save();
                new CheckCredentialsTask().execute();
            }
        });

        mRegisterButton = (Button) findViewById(R.id.register_button);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = RegisterUserActivity.newIntent(v.getContext());
                startActivity(intent);
            }
        });

    }

    public static Intent newIntent(Context packageContext) {
        Intent intent = new Intent(packageContext, UserAuthActivity.class);
        return intent;
    }

    private class CheckCredentialsTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.i(TAG, mUser.toString());
            return User.checkUser(mUser);
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(
                    UserAuthActivity.this);
            progressDialog.setMessage("Logging in...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.i(TAG, result.toString());
            Intent intent = new Intent("checked user");
            intent.putExtra("result", result);
            mLBM.sendBroadcast(intent);
        }
    }
}
