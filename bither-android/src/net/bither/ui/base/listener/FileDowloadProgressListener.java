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

package net.bither.ui.base.listener;

public interface FileDowloadProgressListener {
	public void onProgress(ProgressValue progressValue);

	public void onCancel();

	public enum DownloadType {
		PREPARE, BEGIN, DOWNLOING, END, CANCEL, ERROR
	}

	public class ProgressValue {
		private DownloadType mDownloadType;
		private int mValue;
		private int mSum;
		private String mKey;

		public ProgressValue(DownloadType downloadType, int sum, int value) {
			this.mDownloadType = downloadType;
			this.mValue = value;
			this.mSum = sum;

		}

		public ProgressValue(String key, DownloadType downloadType, int sum,
				int value) {
			this.mDownloadType = downloadType;
			this.mValue = value;
			this.setSum(sum);
			this.setKey(key);

		}

		public DownloadType getDownloadType() {
			return mDownloadType;
		}

		public void setDownloadType(DownloadType mDownloadType) {
			this.mDownloadType = mDownloadType;
		}

		public int getValue() {
			return mValue;
		}

		public void setValue(int mValue) {
			this.mValue = mValue;
		}

		public String getKey() {
			return mKey;
		}

		public void setKey(String mKey) {
			this.mKey = mKey;
		}

		public int getSum() {
			return mSum;
		}

		public void setSum(int mSum) {
			this.mSum = mSum;
		}
	}

}
