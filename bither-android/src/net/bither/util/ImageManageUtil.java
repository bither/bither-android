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

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.Window;

public class ImageManageUtil {

	public static Bitmap getBitmapFromView(View v, int width, int height) {
		v.measure(width, height);
		v.layout(0, 0, width, height);
		Bitmap bmp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		v.draw(new Canvas(bmp));
		return bmp;
	}

	public static Bitmap getBitmapFromView(View v) {
		Bitmap bmp = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
				Config.ARGB_8888);
		v.draw(new Canvas(bmp));
		return bmp;
	}

	public static final int getStatusBarHeight(Window window) {
		Rect frame = new Rect();
		window.getDecorView().getWindowVisibleDisplayFrame(frame);
		return frame.top;
	}

}
