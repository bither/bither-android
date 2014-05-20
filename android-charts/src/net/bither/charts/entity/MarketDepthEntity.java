package net.bither.charts.entity;

import java.util.List;

public class MarketDepthEntity {
	private List<LineEntity<DateValueEntity>> dateValueEntities;
	private int splitIndex;

	public MarketDepthEntity(List<LineEntity<DateValueEntity>> dataEntities,
			int splitIndex) {
		this.dateValueEntities = dataEntities;
		this.splitIndex = splitIndex;

	}

	public List<LineEntity<DateValueEntity>> getDateValueEntities() {
		return dateValueEntities;
	}

	public void setDateValueEntities(
			List<LineEntity<DateValueEntity>> dateValueEntities) {
		this.dateValueEntities = dateValueEntities;
	}

	public int getSplitIndex() {
		return splitIndex;
	}

	public void setSplitIndex(int splitIndex) {
		this.splitIndex = splitIndex;
	}

}
