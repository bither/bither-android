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

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;

public abstract class AbstractMultiThread {

	public interface MultiThreadListener {
		public void prepare();

		public void success(Object object);

		public void error(int errorCode, Object object);

		public void nextStep();

	}

	private Handler mHandler;
	private List<Thread> threads;
	private boolean mGetResult = false;
	private byte[] lock = new byte[0];
	private int mErrorCount;
	private boolean mIsRunning = false;

	public abstract String getName();

	private void initHandler(final MultiThreadListener multiRunnableListener) {
		mHandler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
				case HandlerMessage.MSG_PREPARE:
					if (multiRunnableListener != null) {
						multiRunnableListener.prepare();
					}
					break;
				case HandlerMessage.MSG_SUCCESS:
					stopThread();
					if (mGetResult) {
						return;
					}
					synchronized (lock) {
						if (mGetResult) {
							return;
						}
						mGetResult = true;

					}
					if (multiRunnableListener != null) {
						multiRunnableListener.success(msg.obj);
					}
					mIsRunning = false;
					if (multiRunnableListener != null) {
						multiRunnableListener.nextStep();
					}
					break;
				case HandlerMessage.MSG_FAILURE:
					mErrorCount--;
					if (mErrorCount == 0) {
						if (multiRunnableListener != null) {
							multiRunnableListener.error(
									HandlerMessage.MSG_FAILURE, msg.obj);

						}
						mIsRunning = false;
					}
					break;

				case HandlerMessage.MSG_ADDRESS_NOT_MONITOR:
					mIsRunning = false;
					if (multiRunnableListener != null) {
						multiRunnableListener
								.error(HandlerMessage.MSG_ADDRESS_NOT_MONITOR,
										msg.obj);
						multiRunnableListener.nextStep();

					}

					break;
				default:
					mErrorCount--;
					if (mErrorCount == 0) {
						if (multiRunnableListener != null) {
							multiRunnableListener.error(
									HandlerMessage.MSG_FAILURE, msg.obj);
						}
						mIsRunning = false;
					}
					break;
				}
			}
		};

	}

	public void run(List<BaseRunnable> runnables,
			MultiThreadListener multiRunnableListener) {
		threads = new ArrayList<Thread>();
		initHandler(multiRunnableListener);
		this.mGetResult = false;
		this.mIsRunning = true;
		this.mErrorCount = runnables.size();

		for (BaseRunnable runnable : runnables) {
			runnable.setHandler(mHandler);
			Thread t = new Thread(runnable);
			t.start();
			threads.add(t);
		}

	}

	public boolean isRunning() {
		return mIsRunning;

	}

	private void stopThread() {
		for (Thread t : threads) {
			if (t.isAlive()) {
				t.interrupt();
			}
		}
	}

}
