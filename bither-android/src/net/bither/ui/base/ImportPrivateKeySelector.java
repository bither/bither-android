/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.ui.base;


import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import net.bither.R;
import net.bither.BitherSetting;
import net.bither.ScanQRCodeTransportActivity;

public class ImportPrivateKeySelector implements SettingSelectorView.SettingSelector {

    private Fragment fragment;

    public ImportPrivateKeySelector(Fragment fragment) {
        this.fragment = fragment;

    }

    @Override
    public int getOptionCount() {
        return 2;
    }

    @Override
    public String getOptionName(int index) {
        switch (index) {
            case 0:
                return fragment.getString(R.string.import_private_key_qr_code);
            case 1:
                return fragment.getString(R.string.import_private_key_text);
            default:
                return "";
        }
    }

    @Override
    public String getOptionNote(int index) {
        return null;
    }

    @Override
    public Drawable getOptionDrawable(int index) {
        switch (index) {
            case 0:
                return fragment.getResources().getDrawable(R.drawable.scan_button_icon);
            case 1:
                return fragment.getResources().getDrawable(R.drawable.import_private_key_text_icon);
            default:
                return null;
        }
    }

    @Override
    public String getSettingName() {
        return fragment.getString(R.string.setting_name_import_private_key);
    }

    @Override
    public int getCurrentOptionIndex() {
        return -1;
    }

    @Override
    public void onOptionIndexSelected(int index) {
        switch (index) {
            case 0:
                importPrivateKeyFromQrCode();
                return;
            case 1:
                importPrivateKeyFromText();
                return;
            default:
                return;
        }
    }

    private void importPrivateKeyFromQrCode() {
        Intent intent = new Intent(fragment.getActivity(), ScanQRCodeTransportActivity.class);
        intent.putExtra(BitherSetting.INTENT_REF.TITLE_STRING,
                fragment.getString(R.string.import_private_key_qr_code_scan_title));
        fragment.startActivityForResult(intent, BitherSetting.INTENT_REF.IMPORT_PRIVATE_KEY_REQUEST_CODE);
    }

    private void importPrivateKeyFromText() {
        new DialogImportPrivateKeyText(fragment.getActivity()).show();
    }

}
