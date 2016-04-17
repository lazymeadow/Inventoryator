package com.audreymavramccormick.inventoryator;


import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class LoadInventoryListFragment extends Fragment implements ActionMode.Callback {
    private static final String TAG = "LoadInvenListFragment";
    private ProgressDialog progressDialog;

    private RecyclerView mInventoryRecyclerView;
    private List<Inventory> mInventories = new ArrayList<>();
    private InventoryAdapter mAdapter;
    private EditText mShareCodeEditText;
    private LocalBroadcastManager mLBM;
    private ActionMode mActionMode;

    private BroadcastReceiver mAddedInventoryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            if (intent.getBooleanExtra("result", false)) {
                new FetchInventoriesTask().execute();
                updateUI();
            } else {
                Toast.makeText(context, "Failed to add inventory.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private BroadcastReceiver mInventoryLoadedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) {
                // Get extra data included in the Intent
                String message = intent.getStringExtra("message");
                Log.i(TAG, "Message received:" + message);
                int activity = intent.getIntExtra("activity", -1);
                if (activity == 0) {
                    //view
                    Intent i = ViewInventoryActivity.newIntent(context);
                    startActivity(i);
                } else if (activity == 1) {
                    //edit
                    Intent i = EditInventoryActivity.newIntent(context);
                    startActivity(i);
                }
            }
        }
    };


    private BroadcastReceiver mRemovalMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) {
                // Get extra data included in the Intent
                String message = intent.getStringExtra("message");
                Log.d("receiver", "Got message: " + message);
                if (progressDialog != null) {
                    progressDialog.cancel();
                }
                if (Inventory.getInventory() != null) {
                    Toast.makeText(context, "Inventory removal failed.", Toast.LENGTH_SHORT).show();
                } else {
                    Intent i = LoadInventoryActivity.newIntent(context);
                    startActivity(i);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mLBM = LocalBroadcastManager.getInstance(getContext());
        mLBM.registerReceiver(mInventoryLoadedReceiver,
                new IntentFilter("inventory-loaded"));
        mLBM.registerReceiver(mAddedInventoryReceiver,
                new IntentFilter("inventory added"));
        mLBM.registerReceiver(mRemovalMessageReceiver,
                new IntentFilter("inventory-removal"));

    }

    @Override
    public void onResume() {
        Log.i(TAG, "ON RESUME");
        super.onResume();
        new FetchInventoriesTask().execute();
        updateUI();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_load_inventory_list, container, false);

        mInventoryRecyclerView = (RecyclerView) view.findViewById(R.id.load_inventory_recycler_view);
        mInventoryRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mInventoryRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        new FetchInventoriesTask().execute();
        setupAdapter();
        updateUI();
        return view;
    }

    private void setupAdapter() {
        mInventoryRecyclerView.setAdapter(new InventoryAdapter(mInventories));
    }

    public void updateUI() {
        if (mAdapter == null) {
            setupAdapter();
        } else {
            mAdapter.setInventories(mInventories);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_load_inventory_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentManager fragmentManager = this.getActivity().getSupportFragmentManager();
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_item_new_inventory:
                intent = NewInventoryActivity.newIntent(this.getContext());
                startActivity(intent);
                return true;
            case R.id.menu_item_add_inventory:
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_add_inventory, null);
                mShareCodeEditText = (EditText) dialogView.findViewById(R.id.edit_text_share_code);

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
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
                return true;
            case R.id.menu_item_logout:
                User user = User.findById(User.class, 1);
                user.password = null;
                user.save();
                intent = UserAuthActivity.newIntent(this.getContext());
                startActivity(intent);
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // Called when the action mode is created; startActionMode() was called
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.load_inventory_context_menu, menu);
        return true;
    }

    // Called each time the action mode is shown. Always called after onCreateActionMode, but
    // may be called multiple times if the mode is invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false; // Return false if nothing is done
    }

    Inventory mSelected;

    // Called when the user selects a contextual menu item
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_item_edit_inventory:
                new FetchInventoryTask().execute(mSelected.getId(), 1);
                mode.finish(); // Action picked, so close the CAB
                return true;
            case R.id.menu_item_remove_inventory:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.remove_inventory_prompt)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                new RemoveInventoryTask().execute();
                            }
                        })
                        .setNegativeButton(R.string.nevermind, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                // Create the AlertDialog object and return it
                Dialog dialog = builder.create();
                dialog.show();
                mode.finish(); // Action picked, so close the CAB
                return true;
            default:
                return false;
        }
    }

    // Called when the user exits the action mode
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
    }


    private class InventoryHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private Inventory mInventory;
        private TextView mNameTextView;
        private TextView mDescriptionTextView;

        public InventoryHolder(View inventoryView) {
            super(inventoryView);

            mNameTextView = (TextView) inventoryView.findViewById(R.id.list_item_inventory_name);
            mDescriptionTextView = (TextView) inventoryView.findViewById(R.id.list_item_inventory_description);
            inventoryView.setOnClickListener(this);
            inventoryView.setLongClickable(true);
            inventoryView.setOnLongClickListener(this);
        }

        public void bindInventory(Inventory inventory) { //change this to use the inventory whenever its setup
            mInventory = inventory;
            Log.i(TAG, String.format("bindInventory: %s, %s", inventory.getName(), inventory.getDescription()));
            mNameTextView.setText(mInventory.getName());
            mDescriptionTextView.setText(mInventory.getDescription());
        }

        @Override
        public void onClick(View v) {
            if (mActionMode == null) {
                new FetchInventoryTask().execute(mInventory.getId(), 0);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            //new FetchInventoryTask().execute(mInventory.getId(), 1);
            if (mActionMode != null) {
                return false;
            }

            // Start the CAB using the ActionMode.Callback defined above
            mActionMode = getActivity().startActionMode(LoadInventoryListFragment.this);
            v.setSelected(true);
            mSelected = mInventory;

            return true;
        }
    }

    private class InventoryAdapter extends RecyclerView.Adapter<InventoryHolder> {
        private List<Inventory> mInventories;

        public InventoryAdapter(List<Inventory> inventories) {
            mInventories = inventories;
        }

        @Override
        public InventoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_inventory, parent, false);
            return new InventoryHolder(view);
        }

        @Override
        public void onBindViewHolder(InventoryHolder holder, int position) {
            Inventory inventory = mInventories.get(position);
            holder.bindInventory(inventory);
        }

        @Override
        public int getItemCount() {
            return mInventories.size();
        }

        public void setInventories(List<Inventory> inventories) {
            mInventories = inventories;
        }
    }


    private class FetchInventoryTask extends AsyncTask<Integer, Void, Inventory> {
        private int activity;

        @Override
        protected Inventory doInBackground(Integer... params) {
            activity = params[1];
            return Inventory.loadInventory(params[0]);
        }

        @Override
        protected void onPostExecute(Inventory inventory) {
            Intent intent = new Intent("inventory-loaded");
            intent.putExtra("activity", activity);
            mLBM.sendBroadcast(intent);
        }
    }

    private class FetchInventoriesTask extends AsyncTask<Void, Void, List<Inventory>> {
        @Override
        protected List<Inventory> doInBackground(Void... params) {
            return Inventory.getInventories(User.findById(User.class, 1));
        }

        @Override
        protected void onPostExecute(List<Inventory> inventories) {
            if (isAdded()) {
                if (inventories.isEmpty()) {
                    Intent intent = NewUserActivity.newIntent(getContext());
                    startActivity(intent);
                }
                mInventories = inventories;
                setupAdapter();
            }
        }
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

    private class RemoveInventoryTask extends AsyncTask<Void, Void, Object> {

        @Override
        protected Void doInBackground(Void... params) {
            Inventory.removeInventory(mSelected.getId());
            return null;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(
                    getActivity());
            progressDialog.setMessage("Removing " + mSelected.getName() + "...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Object result) {
            Log.i(TAG, "Inventory removed.");
            Intent intent = new Intent("inventory-removal");
            mLBM.sendBroadcast(intent);
        }
    }
}
