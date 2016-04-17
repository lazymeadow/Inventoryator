package com.audreymavramccormick.inventoryator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class InventoryItemsListFragment extends Fragment {

    private static final String TAG = "InvenItemsListFragment";

    private RecyclerView mInventoryItemsRecyclerView;
    private List<Item> mItems = new ArrayList<>();
    private InventoryItemsAdapter mAdapter;
    private LocalBroadcastManager mLBM;

    private BroadcastReceiver mShareCodeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            String code = intent.getStringExtra("code");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(code)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mLBM = LocalBroadcastManager.getInstance(getContext());
        mLBM.registerReceiver(mShareCodeReceiver,
                new IntentFilter("share_code"));
    }

    @Override
    public void onResume() {
        Log.i(TAG, "RESUMING");
        super.onResume();
        new FetchItemsTask().execute();
        updateUI();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory_items_list, container, false);
        getActivity().setTitle(Inventory.getInventory().getName());

        mInventoryItemsRecyclerView = (RecyclerView) view.findViewById(R.id.inventory_items_recycler_view);
        mInventoryItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mInventoryItemsRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        new FetchItemsTask().execute();
        setupAdapter();
        updateUI();
        return view;
    }

    private void setupAdapter() {
        mInventoryItemsRecyclerView.setAdapter(new InventoryItemsAdapter(mItems));
    }

    public void updateUI() {
        if (mAdapter == null) {
            setupAdapter();
        } else {
            mAdapter.setItems(mItems);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_inventory_items_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentManager fragmentManager = this.getActivity().getSupportFragmentManager();
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_item_edit_inventory:
                intent = EditInventoryActivity.newIntent(this.getContext());
                startActivity(intent);
                return true;
            case R.id.menu_item_add_item:
                intent = NewItemActivity.newIntent(this.getContext());
                startActivity(intent);
                return true;
            case R.id.menu_item_share:
                new ShareCodeTask().execute();
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


    private class InventoryItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private Item mItem;
        private TextView mNameTextView;
        private TextView mNumberTextView;

        public InventoryItemHolder(View itemView) {
            super(itemView);

            mNameTextView = (TextView) itemView.findViewById(R.id.list_item_item_name);
            mNumberTextView = (TextView) itemView.findViewById(R.id.list_item_item_number);
            itemView.setOnClickListener(this);
            itemView.setLongClickable(true);
            itemView.setOnLongClickListener(this);
        }

        public void bindItem(Item item) {
            mItem = item;
            mNameTextView.setText(mItem.getName());
            mNumberTextView.setText(String.valueOf(mItem.getNumber()));
        }

        @Override
        public void onClick(View v) {
            Log.i(TAG, "Long press DETECTED");
            Item.setItem(mItem);
            Context context = v.getContext();
            Intent intent = EditItemActivity.newIntent(context);
            startActivity(intent);
        }

        @Override
        public boolean onLongClick(View v) {
            Context context = v.getContext();
            int duration = Toast.LENGTH_SHORT;
            Item.setItem(mItem);

            Toast toast = Toast.makeText(context, "View " + mItem.getName() + "id:" + mItem.getId(), duration);
            toast.show();
            return true;
        }
    }

    private class InventoryItemsAdapter extends RecyclerView.Adapter<InventoryItemHolder> {
        private List<Item> mItems;

        public InventoryItemsAdapter(List<Item> items) {
            mItems = items;
        }

        @Override
        public InventoryItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_item, parent, false);
            return new InventoryItemHolder(view);
        }

        @Override
        public void onBindViewHolder(InventoryItemHolder holder, int position) {
            Item item = mItems.get(position);
            holder.bindItem(item);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        public void setItems(List<Item> items) {
            mItems = items;
        }
    }


    private class FetchItemsTask extends AsyncTask<Void, Void, List<Item>> {
        @Override
        protected List<Item> doInBackground(Void... params) {
            return Inventory.getItems();
        }

        @Override
        protected void onPostExecute(List<Item> items) {
            if (isAdded()) {
                mItems = items;
                setupAdapter();
            }
        }
    }

    private class ShareCodeTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            return Inventory.getShareCode();
        }

        @Override
        protected void onPostExecute(String code) {
            if (isAdded()) {
                Intent intent = new Intent("share_code");
                intent.putExtra("code", code);
                mLBM.sendBroadcast(intent);
            }
        }
    }
}
