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

import java.util.ArrayList;
import java.util.List;

import net.bither.http.BitherUrl;
import net.bither.http.HttpPostResponse;
import net.bither.http.HttpSetting.HttpType;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

public class GetCookieApi extends HttpPostResponse<String> {

	private static final String TIME_STRING = "ts";

	public GetCookieApi() {
		setUrl(BitherUrl.BITHER_GET_COOKIE_URL);
		setHttpType(HttpType.GetBitherCookie);
	}

	@Override
	public void setResult(String response) throws Exception {
		this.result = response;

	}

	@Override
	public HttpEntity getHttpEntity() throws Exception {
		long time = System.currentTimeMillis();
		time = time / 1000 * 1000 + 215;
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(TIME_STRING, Long.toString(time)));
		return new UrlEncodedFormEntity(params, HTTP.UTF_8);

	}

}
