package net.bither.activity.hot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.widget.TextView;

import net.bither.R;
import net.bither.fragment.hot.HotAddressFragment;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.listener.IBackClickListener;

/**
 * Created by ltq on 2017/7/26.
 */

public class SplitBccSelectAddressActivity extends SwipeRightFragmentActivity {
    public static final int SPLIT_BCC_HDACCOUNT_REQUEST_CODE = 777;
    public static final String DETECT_BCC_ASSETS = "DETECT_BCC_ASSETS";
    public boolean isDetectBcc = false;
    HotAddressFragment hotAddressFragment;
    private TextView tvTitle;

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
        tvTitle = (TextView)findViewById(R.id.tv_title);
        isDetectBcc = (boolean) getIntent().getSerializableExtra(DETECT_BCC_ASSETS);
        FragmentManager manager = getSupportFragmentManager();
        hotAddressFragment = (HotAddressFragment) manager.findFragmentById(R.id.fragment_split_address);
        if (!isDetectBcc) {
            tvTitle.setText(R.string.obtain_BCC_select_address);
        } else {
            tvTitle.setText(R.string.detect_another_BCC_assets_select_address);
        }
    }

    public boolean isDetectBcc() {
        isDetectBcc = (boolean) getIntent().getSerializableExtra(DETECT_BCC_ASSETS);
        return isDetectBcc;
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
