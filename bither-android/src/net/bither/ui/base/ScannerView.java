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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;

import net.bither.R;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;

public class ScannerView extends View {
	private static final long LASER_ANIMATION_DELAY_MS = 100l;
	private static final int DOT_OPACITY = 0xa0;
	private static final int DOT_SIZE = 8;
	private static final int DOT_TTL_MS = 500;

	private final Paint maskPaint;
	private final Paint laserPaint;
	private final Paint dotPaint;
	private Bitmap resultBitmap;
	private final int maskColor;
	private final int resultColor;
	private final Map<ResultPoint, Long> dots = new HashMap<ResultPoint, Long>(
			16);
	private Rect frame, framePreview;

	public ScannerView(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		final Resources res = getResources();
		maskColor = res.getColor(R.color.scan_mask);
		resultColor = res.getColor(R.color.scan_result_view);
		final int laserColor = res.getColor(R.color.scan_laser);
		final int dotColor = res.getColor(R.color.scan_dot);

		maskPaint = new Paint();
		maskPaint.setStyle(Style.FILL);

		laserPaint = new Paint();
		laserPaint.setColor(laserColor);
		laserPaint.setStrokeWidth(DOT_SIZE);
		laserPaint.setStyle(Style.STROKE);

		dotPaint = new Paint();
		dotPaint.setColor(dotColor);
		dotPaint.setAlpha(DOT_OPACITY);
		dotPaint.setStyle(Style.STROKE);
		dotPaint.setStrokeWidth(DOT_SIZE);
		dotPaint.setAntiAlias(true);
	}

	public void setFraming(@Nonnull final Rect frame,
			@Nonnull final Rect framePreview) {
		this.frame = frame;
		this.framePreview = framePreview;

		invalidate();
	}

	public void drawResultBitmap(@Nonnull final Bitmap bitmap) {
		resultBitmap = bitmap;

		invalidate();
	}

	public void addDot(@Nonnull final ResultPoint dot) {
		dots.put(dot, System.currentTimeMillis());

		invalidate();
	}

	@Override
	public void onDraw(final Canvas canvas) {
		if (frame == null)
			return;

		final long now = System.currentTimeMillis();

		final int width = canvas.getWidth();
		final int height = canvas.getHeight();

		// draw mask darkened
		maskPaint.setColor(resultBitmap != null ? resultColor : maskColor);
		canvas.drawRect(0, 0, width, frame.top, maskPaint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, maskPaint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
				maskPaint);
		canvas.drawRect(0, frame.bottom + 1, width, height, maskPaint);

		if (resultBitmap != null) {
			canvas.drawBitmap(resultBitmap, null, frame, maskPaint);
		} else {
			// draw red "laser scanner" to show decoding is active
			final boolean laserPhase = (now / 600) % 2 == 0;
			laserPaint.setAlpha(laserPhase ? 160 : 255);
			canvas.drawRect(frame, laserPaint);

			// draw points
			final int frameLeft = frame.left;
			final int frameTop = frame.top;
			final float scaleX = frame.width() / (float) framePreview.width();
			final float scaleY = frame.height() / (float) framePreview.height();

			for (final Iterator<Map.Entry<ResultPoint, Long>> i = dots
					.entrySet().iterator(); i.hasNext();) {
				final Map.Entry<ResultPoint, Long> entry = i.next();
				final long age = now - entry.getValue();
				if (age < DOT_TTL_MS) {
					dotPaint.setAlpha((int) ((DOT_TTL_MS - age) * 256 / DOT_TTL_MS));

					final ResultPoint point = entry.getKey();
					canvas.drawPoint(frameLeft + (int) (point.getX() * scaleX),
							frameTop + (int) (point.getY() * scaleY), dotPaint);
				} else {
					i.remove();
				}
			}

			// schedule redraw
			postInvalidateDelayed(LASER_ANIMATION_DELAY_MS);
		}
	}
}
