package net.bither.charts.view;

import static net.bither.charts.utils.Utils.formatDoubleToString;

import java.util.List;

import net.bither.charts.R;
import net.bither.charts.entity.DateValueEntity;
import net.bither.charts.entity.LineEntity;
import net.bither.charts.entity.MarketDepthEntity;
import net.bither.charts.event.ITouchEventResponse;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;

public class MarketDepthChart extends SlipLineChart {

	private final int buyOrderColor = getContext().getResources().getColor(
			R.drawable.buy_order);
	private final int sellOrderColor = getContext().getResources().getColor(
			R.drawable.sell_order);
	private int splitIndex;

	public MarketDepthChart(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public MarketDepthChart(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MarketDepthChart(Context context) {
		super(context);
	}

	public void setMareketDepthEntity(MarketDepthEntity marketDepthEntity) {
		this.linesData = marketDepthEntity.getDateValueEntities();
		this.splitIndex = marketDepthEntity.getSplitIndex();

	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		drawAreas(canvas);
	}

	@Override
	protected void drawAxisXgraduate(Canvas canvas, float clickPostX) {
		float prepcentage = getAxisXPrecentage(clickPostX);
		List<DateValueEntity> dataValueEntities = linesData.get(0)
				.getLineData();
		int index = (int) (dataValueEntities.size() * prepcentage);

		float value = dataValueEntities.get(index).getValue();
		// calculate Y
		float valueY = (float) ((1f - (value - minValue)
				/ (maxValue - minValue)) * getDataQuadrantPaddingHeight())
				+ getDataQuadrantPaddingStartY();
		Paint paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setAlpha(70);
		paint.setAntiAlias(true);
		canvas.drawCircle(clickPostX, valueY, 8, paint);
		int moveToY = (int) (((value - minValue) / (maxValue - minValue)) * getDataQuadrantPaddingHeight());
		ITouchEventResponse touchEventResponse = getTouchEventResponse();
		if (touchEventResponse != null) {
			touchEventResponse.notifyTouchPointMove((int) clickPostX, moveToY);
			boolean isBuyOrder = index < splitIndex;
			String price = dataValueEntities.get(index).getTitle();
			Object[] objs = new Object[] { isBuyOrder, price,
					formatDoubleToString(value) };
			touchEventResponse.notifyTouchContentChange(objs);

		}
	}

	protected void drawAreas(Canvas canvas) {
		if (null == linesData) {
			return;
		}
		// distance between two points
		float lineLength = getDataQuadrantPaddingWidth() / displayNumber - 1;
		// start point‘s X
		float startX;

		// draw lines
		for (int i = 0; i < linesData.size(); i++) {
			LineEntity<DateValueEntity> line = (LineEntity<DateValueEntity>) linesData
					.get(i);
			if (line == null) {
				continue;
			}
			if (line.isDisplay() == false) {
				continue;
			}
			List<DateValueEntity> lineData = line.getLineData();
			if (lineData == null) {
				continue;
			}

			Paint buyOrderPaint = new Paint();
			buyOrderPaint.setColor(buyOrderColor);
			buyOrderPaint.setAlpha(70);
			buyOrderPaint.setAntiAlias(true);

			Paint sellOrderPaint = new Paint();
			sellOrderPaint.setColor(sellOrderColor);
			sellOrderPaint.setAlpha(70);
			sellOrderPaint.setAntiAlias(true);

			// set start point’s X
			startX = getDataQuadrantPaddingStartX() + lineLength / 2f;
			Path linePath = new Path();
			for (int j = displayFrom; j < displayFrom + displayNumber; j++) {
				float value = lineData.get(j).getValue();
				// calculate Y
				float valueY = (float) ((1f - (value - minValue)
						/ (maxValue - minValue)) * getDataQuadrantPaddingHeight())
						+ getDataQuadrantPaddingStartY();

				// if is not last point connect to previous point
				if (j == displayFrom) {
					linePath.moveTo(startX, getDataQuadrantPaddingEndY());
					linePath.lineTo(startX, valueY);
				} else if (j == displayFrom + displayNumber - 1) {
					linePath.lineTo(startX, valueY);
					linePath.lineTo(startX, getDataQuadrantPaddingEndY());
				} else {
					linePath.lineTo(startX, valueY);
				}
				startX = startX + 1 + lineLength;
				if (j == splitIndex) {
					linePath.close();
					canvas.drawPath(linePath, buyOrderPaint);
					linePath = new Path();
					linePath.moveTo(startX, getDataQuadrantPaddingEndY());
				}
			}
			linePath.close();
			canvas.drawPath(linePath, sellOrderPaint);
		}
	}

	@Override
	protected void drawLines(Canvas canvas) {
		if (null == this.linesData) {
			return;
		}
		// distance between two points
		float lineLength = getDataQuadrantPaddingWidth() / displayNumber - 1;
		// start point‘s X
		float startX;

		// draw lines
		for (int i = 0; i < linesData.size(); i++) {
			LineEntity<DateValueEntity> line = (LineEntity<DateValueEntity>) linesData
					.get(i);
			if (line == null) {
				continue;
			}
			if (line.isDisplay() == false) {
				continue;
			}
			List<DateValueEntity> lineData = line.getLineData();
			if (lineData == null) {
				continue;
			}
			Paint buyOrderPaint = new Paint();
			buyOrderPaint.setColor(buyOrderColor);
			buyOrderPaint.setAntiAlias(true);

			Paint sellOrderPaint = new Paint();
			sellOrderPaint.setColor(sellOrderColor);
			sellOrderPaint.setAntiAlias(true);
			// set start point’s X
			startX = getDataQuadrantPaddingStartX() + lineLength / 2;
			// start point
			PointF ptFirst = null;

			for (int j = displayFrom; j < displayFrom + displayNumber; j++) {
				float value = lineData.get(j).getValue();
				// calculate Y
				float valueY = (float) ((1f - (value - minValue)
						/ (maxValue - minValue)) * getDataQuadrantPaddingHeight())
						+ getDataQuadrantPaddingStartY();

				// if is not last point connect to previous point
				if (j > displayFrom) {
					if (j > splitIndex) {
						canvas.drawLine(ptFirst.x, ptFirst.y, startX, valueY,
								sellOrderPaint);
					} else {
						canvas.drawLine(ptFirst.x, ptFirst.y, startX, valueY,
								buyOrderPaint);
					}
				}
				// reset
				ptFirst = new PointF(startX, valueY);
				startX = startX + 1 + lineLength;
			}
		}
	}

}
