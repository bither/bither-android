package net.bither.activity.hot;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.bither.R;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.core.PeerManager;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.utils.DnsDiscovery;
import net.bither.bitherj.utils.Utils;
import net.bither.preference.AppSharedPreference;
import net.bither.runnable.ThreadNeedService;
import net.bither.service.BlockchainService;
import net.bither.ui.base.BaseFragmentActivity;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.dialog.DialogConfirmTask;
import net.bither.ui.base.dialog.DialogWithActions;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NetworkCustomPeerActivity extends BaseFragmentActivity {

    private ImageButton ibtnBack;
    private TextView tvCurrentCustomPeerTitle;
    private TextView tvCurrentCustomPeer;
    private EditText etDnsOrIp;
    private EditText etPort;
    private ImageView ivOptionLine;
    private ImageButton ibtnOption;
    private Button btnConfirm;
    private InputMethodManager imm;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_custom_peer);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        ibtnBack = findViewById(R.id.ibtn_back);
        ibtnBack.setOnClickListener(new IBackClickListener());
        etDnsOrIp = findViewById(R.id.et_dns_or_ip);
        etPort = findViewById(R.id.et_port);
        etPort.setHint(getString(R.string.network_custom_peer_port_hint, BitherjSettings.port));
        btnConfirm = findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(confirmClick);
        String customDnsOrIp = AppSharedPreference.getInstance().getNetworkCustomPeerDnsOrIp();
        if (Utils.isEmpty(customDnsOrIp)) {
            return;
        }
        tvCurrentCustomPeerTitle = findViewById(R.id.tv_current_custom_peer_title);
        tvCurrentCustomPeer = findViewById(R.id.tv_current_custom_peer);
        tvCurrentCustomPeer.setText(customDnsOrIp + ":" + AppSharedPreference.getInstance().getNetworkCustomPeerPort());
        ivOptionLine = findViewById(R.id.iv_option_line);
        ibtnOption = findViewById(R.id.ibtn_option);
        findViewById(R.id.ibtn_option).setOnClickListener(optionClick);
        showCurrentCustomPeer(true);
    }

    private void showCurrentCustomPeer(boolean isShow) {
        int visibility = isShow ? View.VISIBLE : View.GONE;
        tvCurrentCustomPeerTitle.setVisibility(visibility);
        tvCurrentCustomPeer.setVisibility(visibility);
        ivOptionLine.setVisibility(visibility);
        ibtnOption.setVisibility(visibility);
    }

    private View.OnClickListener confirmClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String dnsOrIp = etDnsOrIp.getText().toString();
            if (Utils.isEmpty(dnsOrIp)) {
                DropdownMessage.showDropdownMessage(NetworkCustomPeerActivity.this, R.string.network_custom_peer_dns_or_ip_empty);
                return;
            }
            int port;
            String portStr = etPort.getText().toString();
            if (Utils.isEmpty(portStr)) {
                port = BitherjSettings.port;
            } else {
                try {
                    port = Integer.valueOf(portStr);
                } catch (Exception exception) {
                    DropdownMessage.showDropdownMessage(NetworkCustomPeerActivity.this, R.string.network_custom_peer_port_invalid);
                    return;
                }
            }
            imm.hideSoftInputFromWindow(etDnsOrIp.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            DnsDiscovery.instance().getPeers(dnsOrIp, port, 5, TimeUnit.SECONDS);
            String getPeersErrorMsg = DnsDiscovery.instance().getPeersErrorMsg();
            if (!Utils.isEmpty(getPeersErrorMsg)) {
                DropdownMessage.showDropdownMessage(NetworkCustomPeerActivity.this, getPeersErrorMsg);
                return;
            }
            AppSharedPreference.getInstance().setNetworkCustomPeer(dnsOrIp, port);
            new ThreadNeedService(null, NetworkCustomPeerActivity.this) {
                @Override
                public void runWithService(BlockchainService service) {
                    if (service != null) {
                        service.stopAndUnregister();
                    }
                    AbstractDb.peerProvider.recreate();
                    if (service != null) {
                        service.startAndRegister();
                    }
                    ThreadUtil.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            DialogConfirmTask dialogConfirmTask = new DialogConfirmTask(NetworkCustomPeerActivity.this, getString(R.string.network_custom_peer_success), new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            }, false);
                            dialogConfirmTask.setCanceledOnTouchOutside(false);
                            dialogConfirmTask.show();
                        }
                    });
                }
            }.start();
        }
    };

    private View.OnClickListener optionClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            new DialogWithActions(v.getContext()) {

                @Override
                protected List<Action> getActions() {
                    ArrayList<Action> actions = new ArrayList<Action>();
                    actions.add(new Action(R.string.network_custom_peer_clear, new Runnable() {
                        @Override
                        public void run() {
                            AppSharedPreference.getInstance().removeNetworkCustomPeer();
                            PeerManager.instance().setCustomPeer(null, BitherjSettings.port);
                            new ThreadNeedService(null, NetworkCustomPeerActivity.this) {

                                @Override
                                public void runWithService(BlockchainService service) {
                                    if (service != null) {
                                        service.stopAndUnregister();
                                    }
                                    AbstractDb.peerProvider.recreate();
                                    if (service != null) {
                                        service.startAndRegister();
                                    }
                                }
                            }.start();
                            showCurrentCustomPeer(false);
                        }
                    }));
                    return actions;
                }
            }.show();
        }
    };

}
