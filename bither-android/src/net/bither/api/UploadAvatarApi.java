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


package net.bither.api;


import net.bither.http.BitherUrl;
import net.bither.http.HttpPostResponse;
import net.bither.http.HttpSetting;

import org.apache.http.HttpEntity;
import org.apache.http.entity.FileEntity;

import java.io.File;

public class UploadAvatarApi extends HttpPostResponse<String> {
    private File mFile;

    public UploadAvatarApi(File file) {
        this.mFile = file;
        setUrl(BitherUrl.BITHER_UPLOAD_AVATAR);
    }

    @Override
    public HttpEntity getHttpEntity() throws Exception {
        FileEntity entity = new FileEntity(this.mFile, "binary/octet-stream");
        return entity;
    }

    @Override
    public void setResult(String response) throws Exception {
        this.result = response;
    }
}
