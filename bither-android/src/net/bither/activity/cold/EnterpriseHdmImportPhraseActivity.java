/*
 *
 *  * Copyright 2014 http://Bither.net
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package net.bither.activity.cold;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.bitherj.core.EnterpriseHDMSeed;
import net.bither.bitherj.crypto.SecureCharSequence;
import net.bither.bitherj.crypto.mnemonic.MnemonicCode;
import net.bither.bitherj.crypto.mnemonic.MnemonicException;
import net.bither.bitherj.utils.Utils;
import net.bither.fragment.Refreshable;
import net.bither.ui.base.DropdownMessage;
import net.bither.ui.base.SwipeRightFragmentActivity;
import net.bither.ui.base.dialog.DialogHdmImportWordListReplace;
import net.bither.ui.base.dialog.DialogPassword;
import net.bither.ui.base.dialog.DialogProgress;
import net.bither.ui.base.dialog.DialogWithActions;
import net.bither.ui.base.listener.IBackClickListener;
import net.bither.ui.base.listener.IDialogPasswordListener;
import net.bither.util.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by songchenwen on 15/1/22.
 */
public class EnterpriseHdmImportPhraseActivity extends SwipeRightFragmentActivity implements
        TextView.OnEditorActionListener, DialogHdmImportWordListReplace
        .DialogHdmImportWordListReplaceListener {
    private static int WordCount = 24;

    private GridView gv;
    private TextView tvEmpty;
    private TextView tvTitle;
    private EditText etInput;
    private Button btnInput;
    private ImageButton btnSave;

    private MnemonicCode mnemonic = MnemonicCode.instance();

    private ArrayList<String> words = new ArrayList<String>();
    private DialogProgress dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in_right, 0);
        setContentView(R.layout.activity_hdm_import_word_list);
        initView();
        etInput.requestFocus();
    }

    private void initView() {
        findViewById(R.id.ibtn_back).setOnClickListener(new IBackClickListener());
        gv = (GridView) findViewById(R.id.gv);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        etInput = (EditText) findViewById(R.id.et_word);
        btnInput = (Button) findViewById(R.id.btn_input);
        tvEmpty.setText(R.string.enterprise_hdm_seed_import_from_phrase);
        tvTitle.setText(R.string.enterprise_hdm_seed_import_from_phrase);
        btnInput.setOnClickListener(inputClick);
        etInput.setOnEditorActionListener(this);
        gv.setAdapter(adapter);
        dp = new DialogProgress(this, R.string.please_wait);
        btnSave = (ImageButton) findViewById(R.id.btn_save);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                complete();
            }
        });
        refresh();
    }

    private void refresh() {
        if (words.size() > 0) {
            tvEmpty.setVisibility(View.GONE);
            gv.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
            gv.setVisibility(View.INVISIBLE);
        }
        adapter.notifyDataSetChanged();
        btnSave.setEnabled(words.size() % 3 == 0);
    }

    private View.OnClickListener inputClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (words.size() >= WordCount) {
                return;
            }
            String word = etInput.getText().toString().toLowerCase().trim();
            if (Utils.isEmpty(word)) {
                return;
            }
            if (!mnemonic.getWordList().contains(word)) {
                DropdownMessage.showDropdownMessage(EnterpriseHdmImportPhraseActivity.this, R
                        .string.hdm_import_word_list_wrong_word_warn);
                return;
            }
            words.add(word);
            etInput.setText("");
            refresh();
            gv.smoothScrollToPosition(words.size() - 1);
            if (words.size() >= WordCount) {
                complete();
            }
        }
    };

    private void complete() {

        new DialogPassword(this, new IDialogPasswordListener() {
            @Override
            public void onPasswordEntered(final SecureCharSequence password) {
                try {
                    final byte[] mnemonicCodeSeed = mnemonic.toEntropy(words);
                    dp.show();
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                EnterpriseHDMSeed seed = new EnterpriseHDMSeed(mnemonicCodeSeed,
                                            password);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dp.dismiss();
                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                });
                            } catch (MnemonicException.MnemonicLengthException e) {
                                e.printStackTrace();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dp.dismiss();
                                        DropdownMessage.showDropdownMessage(EnterpriseHdmImportPhraseActivity.this, R
                                                .string.enterprise_hdm_seed_import_format_error);
                                    }
                                });
                            }
                        }
                    }.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    DropdownMessage.showDropdownMessage(EnterpriseHdmImportPhraseActivity.this, R
                            .string.enterprise_hdm_seed_import_format_error);
                }
            }
        }).show();

    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        inputClick.onClick(btnInput);
        return true;
    }

    @Override
    public void replace(int index, String word) {
        words.set(index, word);
        refresh();
    }

    private class ItemClick extends DialogWithActions.DialogWithActionsClickListener {
        private int position;

        ItemClick(int position) {
            this.position = position;
        }

        @Override
        protected List<DialogWithActions.Action> getActions() {
            ArrayList<DialogWithActions.Action> actions = new ArrayList<DialogWithActions.Action>();
            actions.add(new DialogWithActions.Action(R.string.hdm_import_word_list_delete, new
                    Runnable() {
                @Override
                public void run() {
                    words.remove(position);
                    refresh();
                }
            }));
            actions.add(new DialogWithActions.Action(R.string.hdm_import_word_list_replace, new
                    Runnable() {
                @Override
                public void run() {
                    new DialogHdmImportWordListReplace(EnterpriseHdmImportPhraseActivity.this,
                            position, EnterpriseHdmImportPhraseActivity.this).show();
                }
            }));
            return actions;
        }
    }

    private BaseAdapter adapter = new BaseAdapter() {
        LayoutInflater inflater;

        @Override
        public int getCount() {
            return words.size();
        }

        @Override
        public String getItem(int position) {
            return words.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder h;
            if (convertView == null) {
                if (inflater == null) {
                    inflater = LayoutInflater.from(EnterpriseHdmImportPhraseActivity.this);
                }
                convertView = inflater.inflate(R.layout.list_item_hdm_import_word_list, parent,
                        false);
                h = new Holder(convertView);
                convertView.setTag(h);
            } else {
                h = (Holder) convertView.getTag();
            }
            h.tvIndex.setText((position + 1) + ".");
            h.tvWord.setText(getItem(position));
            convertView.setOnClickListener(new ItemClick(position));
            return convertView;
        }

        class Holder {
            View v;
            TextView tvIndex;
            TextView tvWord;

            Holder(View v) {
                this.v = v;
                tvIndex = (TextView) v.findViewById(R.id.tv_index);
                tvWord = (TextView) v.findViewById(R.id.tv_word);
            }
        }
    };
}
