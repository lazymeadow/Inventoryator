package com.audreymavramccormick.inventoryator;


import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

public class LoadInventoryActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new LoadInventoryListFragment();
    }

    public static Intent newIntent(Context packageContext) {
        Intent intent = new Intent(packageContext, LoadInventoryActivity.class);
        return intent;
    }

}
