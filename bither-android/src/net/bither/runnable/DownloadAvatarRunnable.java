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


package net.bither.runnable;

import net.bither.bitherj.api.DownloadFile;
import net.bither.bitherj.utils.Utils;
import net.bither.bitherj.http.BitherUrl;
import net.bither.preference.AppSharedPreference;
import net.bither.util.ImageFileUtil;

import java.io.File;

public class DownloadAvatarRunnable extends BaseRunnable {
    @Override
    public void run() {
        String avatar = AppSharedPreference.getInstance().getUserAvatar();
        if (!Utils.isEmpty(avatar)) {
            try {
                File file = ImageFileUtil.getSmallAvatarFile(avatar);
                if (!file.exists()) {
                    DownloadFile downloadFile = new DownloadFile();
                    downloadFile.downloadFile(BitherUrl.BITHER_DOWNLOAD_AVATAR, file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
