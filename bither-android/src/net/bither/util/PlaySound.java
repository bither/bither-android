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

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.SparseArray;

import net.bither.BitherApplication;

public class PlaySound {
	public interface PlaySoundCompletionListener {
		public void onCompletion();
	}

	@SuppressLint("UseSparseArrays")
	private static SparseArray<Integer> sounds = new SparseArray<Integer>();
	private static SoundPool soundPool = new SoundPool(3,
			AudioManager.STREAM_MUSIC, 100);

	public static void loadSound(int soundId) {
		if (sounds.get(soundId) != null) {
			return;
		}
		synchronized (sounds) {
			if (sounds.get(soundId) != null) {
				return;
			}
			int soundIDOfPool = soundPool.load(BitherApplication.mContext,
					soundId, 1);
			LogUtil.d("record", "load:" + soundId);
			sounds.put(soundId, soundIDOfPool);
		}
	}

	public static void play(final int soundId,
			final PlaySoundCompletionListener playSoundCompletionListener) {
		if (sounds.get(soundId) != null) {
			int soundID1 = sounds.get(soundId);
			beginPlay(soundID1, playSoundCompletionListener);
		} else {
			synchronized (sounds) {
				final int soundIDOfPool = soundPool.load(
						BitherApplication.mContext, soundId, 1);
				soundPool
						.setOnLoadCompleteListener(new OnLoadCompleteListener() {
							@Override
							public void onLoadComplete(SoundPool arg0,
									int arg1, int arg2) {
								if (arg1 == soundIDOfPool) {
									beginPlay(soundIDOfPool,
											playSoundCompletionListener);
								}
							}
						});
				LogUtil.d("record", "load:" + soundId);
				sounds.put(soundId, soundIDOfPool);
			}
		}
	}

	private static void beginPlay(int soundId,
			PlaySoundCompletionListener playSoundCompletionListener) {
		if (soundId == 0) {
			if (playSoundCompletionListener != null) {
				playSoundCompletionListener.onCompletion();
			}
		} else {
			AudioManager mgr = (AudioManager) BitherApplication.mContext
					.getSystemService(Context.AUDIO_SERVICE);
			final float volume = mgr
					.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			soundPool.play(soundId, volume, volume, 1, 0, 1.0f);
			if (playSoundCompletionListener != null) {
				playSoundCompletionListener.onCompletion();
			}
		}

	}

	public static void unload(int resId) {
		if (sounds.get(resId) != null) {
			soundPool.unload(sounds.get(resId));
		}

	}
}
