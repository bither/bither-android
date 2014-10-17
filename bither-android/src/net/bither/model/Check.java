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

import net.bither.BitherApplication;
import net.bither.bitherj.utils.Utils;
import net.bither.util.ThreadUtil;

public class Check {

	public interface ICheckAction {
		boolean check();
	}

	public interface CheckListener {
		public void onCheckBegin(Check check);

		public void onCheckEnd(Check check, boolean success);
	}

	public interface CheckOperation {
		public void operate();
	}

	private String mTitle;
	private String mTitleFailed;
	private String mTitleChecking;

	private ICheckAction mAction;
	private CheckListener listener;
	private CheckOperation operation;

	public Check(String title, ICheckAction action) {
		setTitle(title);
		setCheckAction(action);
	}

	public Check(int titleResouce, ICheckAction action) {
		setTitle(titleResouce);
		setCheckAction(action);
	}

	public Check(int titleResource, int failedTitleResource, ICheckAction action) {
		setTitle(titleResource);
		setTitleFailed(failedTitleResource);
		setCheckAction(action);
	}

	public Check(String title, String failedTitle, ICheckAction action) {
		setTitle(title);
		setTitleFailed(failedTitle);
		setCheckAction(action);
	}

	public Check(String title, String failedTitle, String checkingTitle,
			ICheckAction action) {
		setTitle(title);
		setTitleFailed(failedTitle);
		setTitleChecking(checkingTitle);
		setCheckAction(action);
	}

	public Check(int title, int failedTitle, int checkingTitle,
			ICheckAction action) {
		setTitle(title);
		setTitleFailed(failedTitle);
		setTitleChecking(checkingTitle);
		setCheckAction(action);
	}

	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String mTitle) {
		this.mTitle = mTitle;
	}

	public void setTitle(int resouceId) {
		this.mTitle = BitherApplication.mContext.getString(resouceId);
	}

	public String getTitleFailed() {
		return mTitleFailed;
	}

	public void setTitleFailed(int resourceId) {
		this.mTitleFailed = BitherApplication.mContext.getString(resourceId);
	}

	public void setTitleFailed(String mTitleFailed) {
		this.mTitleFailed = mTitleFailed;
	}

	public String getTitleChecking() {
		return mTitleChecking;
	}

	public void setTitleChecking(String titleChecking) {
		this.mTitleChecking = titleChecking;
	}

	public void setTitleChecking(int resourceId) {
		this.mTitleChecking = BitherApplication.mContext.getString(resourceId);
	}

	public void setCheckAction(ICheckAction action) {
		this.mAction = action;
	}

	public ICheckAction getCheckAction() {
		return mAction;
	}

	public Check setCheckListener(CheckListener listener) {
		this.listener = listener;
		return this;
	}

	public void setCheckOperation(CheckOperation operation) {
		this.operation = operation;
	}

	public boolean check() {
		if (listener != null) {
			ThreadUtil.runOnMainThread(new Runnable() {
				@Override
				public void run() {
					listener.onCheckBegin(Check.this);
				}
			});
		}
		boolean result = true;
		if (mAction != null) {
			result = mAction.check();
		}
		if (listener != null) {
			ThreadUtil.runOnMainThread(new CheckEndRunnable(result));
		}
		return result;
	}

	public void operate() {
		if (operation != null) {
			operation.operate();
		}
	}

	private class CheckEndRunnable implements Runnable {
		private boolean result;

		public CheckEndRunnable(boolean result) {
			this.result = result;
		}

		@Override
		public void run() {
			if (listener != null) {
				listener.onCheckEnd(Check.this, result);
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Check) {
			Check check = (Check) o;
			return Utils.compareString(getTitle(), check.getTitle());
		}
		return false;
	}
}
