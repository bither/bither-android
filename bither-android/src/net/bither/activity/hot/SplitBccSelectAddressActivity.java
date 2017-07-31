package net.bither.activity.hot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import net.bither.R;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.listener.IBackClickListener;

/**
 * Created by ltq on 2017/7/26.
 */

public class SplitBccSelectAddressActivity extends SwipeRightFragmentActivity {
    public static final int SPLIT_BCC_HDACCOUNT_REQUEST_CODE = 777;
    HotAddressFragment hotAddressFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_split_bcc_address);
        initView();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(
                new IBackClickListener(0, R.anim.slide_out_right));
        FragmentManager manager = getSupportFragmentManager();
        hotAddressFragment = (HotAddressFragment) manager.findFragmentById(R.id.fragment_split_address);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_right);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (SplitBccSelectAddressActivity.
                SPLIT_BCC_HDACCOUNT_REQUEST_CODE == requestCode) {
            hotAddressFragment.doRefresh();
        }
    }
}
