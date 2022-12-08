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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import net.bither.BitherApplication;
import net.bither.BitherSetting;

public class SystemUtil {
    private static PackageInfo packageInfo;

    public synchronized static PackageInfo packageInfo() {
        if (packageInfo == null) {
            try {
                packageInfo = BitherApplication.mContext.getPackageManager()
                        .getPackageInfo(
                                BitherApplication.mContext.getPackageName(), 0);
            } catch (final NameNotFoundException x) {
                throw new RuntimeException(x);
            }
        }
        return packageInfo;
    }

    public static String getIMEI() {
        return ((TelephonyManager) BitherApplication.mContext
                .getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
    }

    public static int getAppVersionCode() {
        try {
            // ---get the package info---
            PackageManager pm = BitherApplication.mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(
                    BitherApplication.mContext.getPackageName(), 0);
            return pi.versionCode;
        } catch (Exception e) {
            Log.e("VersionInfo", "Exception", e);
            return 0;
        }

    }

    public static void nmNotifyOfWallet(NotificationManager nm, Context context,
                                        int notifyId, Intent intent, String title,
                                        String contentText,
                                        int iconId, int rawId) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context);
        builder.setSmallIcon(iconId);
        builder.setContentText(contentText);
        builder.setContentTitle(title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_CANCEL_CURRENT));
        } else {
            builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        }

        builder.setWhen(System.currentTimeMillis());
        Notification notification = null;
        if (ServiceUtil.isNoPrompt(System.currentTimeMillis())) {
            notification = builder.build();
            notification.flags = Notification.FLAG_AUTO_CANCEL
                    | Notification.FLAG_ONLY_ALERT_ONCE;
            notification.sound = null;
        } else {
            builder.setSound(Uri.parse("android.resource://"
                    + context.getPackageName() + "/" + rawId));
            notification = builder.build();
            notification.flags = Notification.FLAG_AUTO_CANCEL
                    | Notification.FLAG_ONLY_ALERT_ONCE;
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            notification.ledARGB = 0xFF84E4FA;
            notification.ledOnMS = 3000;
            notification.ledOffMS = 2000;
        }

        nm.notify(notifyId, notification);

    }

    public static void nmNotifyDefault(NotificationManager nm, Context context,
                                       int notifyId, Intent intent, String title,
                                       String contentText,
                                       int iconId) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context);
        builder.setSmallIcon(iconId);
        builder.setContentText(contentText);
        builder.setContentTitle(title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_CANCEL_CURRENT));
        } else {
            builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        }

        builder.setWhen(System.currentTimeMillis());
        Notification notification = null;

        notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        notification.flags = Notification.FLAG_AUTO_CANCEL
                | Notification.FLAG_ONLY_ALERT_ONCE;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.ledARGB = 0xFF84E4FA;
        notification.ledOnMS = 3000;
        notification.ledOffMS = 2000;
        nm.notify(notifyId, notification);

    }

    public static void gotoWirelessSetting(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT > 10) {
            activity.startActivityForResult(new Intent(
                            android.provider.Settings.ACTION_SETTINGS),
                    BitherSetting.INTENT_REF.WIRELESS_SETTINGS_CODE
            );
        } else {
            activity.startActivityForResult(new Intent(
                            android.provider.Settings.ACTION_WIRELESS_SETTINGS),
                    BitherSetting.INTENT_REF.WIRELESS_SETTINGS_CODE
            );
        }
    }

}
