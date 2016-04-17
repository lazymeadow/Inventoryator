package com.audreymavramccormick.inventoryator;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class ViewInventoryActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new InventoryItemsListFragment();
    }

    public static Intent newIntent(Context packageContext) {
        Intent intent = new Intent(packageContext, ViewInventoryActivity.class);
        return intent;
    }

}
