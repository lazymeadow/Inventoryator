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

public class RegisterUserActivity extends AppCompatActivity {

    //like login but with additional password field and cancel button

    public static final String TAG = "RegisterUserActivity";

    private ProgressDialog progressDialog;
    private Button mRegisterButton;
    private Button mCancelButton;
    private String mUsername;
    private String mPassword;
    private LocalBroadcastManager mLBM;
    private EditText mUsernameEditText, mPassword1EditText, mPassword2EditText;

    private BroadcastReceiver mUserCreatedReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            progressDialog.cancel();
            if (intent.getBooleanExtra("result", false)) {
                User user = User.findById(User.class, 1);
                user.username = mUsername;
                user.password = mPassword;
                user.save();
                Intent newIntent = LoadInventoryActivity.newIntent(context);
                startActivity(newIntent);
                RegisterUserActivity.this.finish();
            } else {
                Toast.makeText(context, "User registration failed", Toast.LENGTH_SHORT).show();
            }
        }
    };

    //TODO SET USER ID 1 TO THE NEW REGISTERED USER, SAVE IT, SO THAT IT GOES BACK TO AUTH AND IS RIGHT

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        mLBM = LocalBroadcastManager.getInstance(this);
        mLBM.registerReceiver(mUserCreatedReceived,
                new IntentFilter("user created"));

        mUsernameEditText = (EditText) findViewById(R.id.register_username);
        mPassword1EditText = (EditText) findViewById(R.id.register_password1);
        mPassword2EditText = (EditText) findViewById(R.id.register_password2);


        mRegisterButton = (Button) findViewById(R.id.register_confirm_button);
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUsername = mUsernameEditText.getText().toString();
                String password1 = PasswordHasher.bin2hex(PasswordHasher.getHash(mPassword1EditText.getText().toString()));
                Log.i(TAG, password1);
                String password2 = PasswordHasher.bin2hex(PasswordHasher.getHash(mPassword2EditText.getText().toString()));
                Log.i(TAG, password2);
                mPassword = password1;
                new RegisterUserTask().execute(mUsername, password1, password2);
            }
        });

        mCancelButton = (Button) findViewById(R.id.register_cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterUserActivity.this.finish();
            }
        });

    }


    public static Intent newIntent(Context packageContext) {
        Intent intent = new Intent(packageContext, RegisterUserActivity.class);
        return intent;
    }

    private class RegisterUserTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            String username = params[0];
            String password1 = params[1];
            String password2 = params[2];
            Log.i(TAG, "Registering user " + username);
            return User.registerUser(username, password1, password2);
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(
                    RegisterUserActivity.this);
            progressDialog.setMessage("Logging in...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.i(TAG, result.toString());
            Intent intent;
            Log.i(TAG, "Credentials Authorized.");
            intent = new Intent("user created");
            intent.putExtra("result", result);
            mLBM.sendBroadcast(intent);
        }
    }
}
