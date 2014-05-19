package net.bither.charts.event;

public interface ITouchEventResponse {

	public void notifyTouchPointMove(int x, int y);

	public void notifyTouchContentChange(Object[] objs);

	public void clearTounchPoint();

}
