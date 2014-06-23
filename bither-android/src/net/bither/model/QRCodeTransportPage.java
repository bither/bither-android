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

package net.bither.model;

import java.util.List;

import net.bither.util.StringUtil;

public class QRCodeTransportPage {
	private int mCurrentPage;
	private int mSumPage;
	private String mContent;

	public int getCurrentPage() {
		return mCurrentPage;
	}

	public void setCurrentPage(int mCurrentPage) {
		this.mCurrentPage = mCurrentPage;
	}

	public int getSumPage() {
		return mSumPage;
	}

	public void setSumPage(int mSumPage) {
		this.mSumPage = mSumPage;
	}

	public boolean hasNextPage() {
		return this.mCurrentPage+1 < this.mSumPage;
	}

	public String getContent() {
		return mContent;
	}

	public void setContent(String mContent) {
		this.mContent = mContent;
	}

	public static String formatQRCodeTran(
            List<QRCodeTransportPage> qrCodeTransportPages) {
		String transportString = "";
		for (QRCodeTransportPage qCodetTransportPage : qrCodeTransportPages) {
			if (!StringUtil.isEmpty(qCodetTransportPage.getContent())) {
				transportString = transportString
						+ qCodetTransportPage.getContent();
			}
		}
		return StringUtil.decodeQrCodeString(transportString);
	}

	public static QRCodeTransportPage formatQrCodeString(String text) {
		if (!StringUtil.verifyQrcodeTransport(text)) {
			return null;
		}
		QRCodeTransportPage qrCodetTransportPage = new QRCodeTransportPage();
		String[] strArray = text.split(StringUtil.QR_CODE_SPLIT);
		if (StringUtil.isInteger(strArray[0])) {
			int length = strArray[0].length() + strArray[1].length() + 2;
			qrCodetTransportPage.setSumPage(Integer.valueOf(strArray[0]) + 1);
			qrCodetTransportPage.setCurrentPage(Integer.valueOf(strArray[1]));
			qrCodetTransportPage.setContent(text.substring(length));
		} else {
			qrCodetTransportPage.setContent(text);
		}
		return qrCodetTransportPage;
	}
}
