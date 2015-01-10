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

package net.bither.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import net.bither.BitherApplication;
import net.bither.R;
import net.bither.ui.base.DropdownMessage;

public class UIUtil {
    public static final int SwitchToAbcActionId = 18203;
    public static final int SwitchTo123ActionId = 18204;

    public static final int dip2pix(float dip) {
        final float scale = BitherApplication.mContext.getResources()
                .getDisplayMetrics().density;
        return (int) (dip * scale + 0.5f);
    }

    public static int getScreenWidth() {
        return BitherApplication.mContext.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return BitherApplication.mContext.getResources().getDisplayMetrics().heightPixels;
    }

    public static void gotoBrower(Activity activity, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            activity.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            DropdownMessage.showDropdownMessage(activity, R.string.find_browser_error);
        }

    }
}
