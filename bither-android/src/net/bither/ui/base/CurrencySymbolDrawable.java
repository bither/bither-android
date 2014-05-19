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

package net.bither.ui.base;

import javax.annotation.Nonnull;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public final class CurrencySymbolDrawable extends Drawable {
	private final Paint paint = new Paint();
	private final String symbol;
	private final float y;

	public CurrencySymbolDrawable(@Nonnull final String symbol,
			final float textSize, final int color, final float y) {
		paint.setColor(color);
		paint.setAntiAlias(true);
		paint.setTextSize(textSize);
		this.symbol = symbol + " ";
		this.y = y;
	}

	@Override
	public void draw(final Canvas canvas) {
		canvas.drawText(symbol, 0, y, paint);
	}

	@Override
	public int getIntrinsicWidth() {
		return (int) paint.measureText(symbol);
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public void setAlpha(final int alpha) {
	}

	@Override
	public void setColorFilter(final ColorFilter cf) {
	}
}
