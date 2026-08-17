package chs.caplets.shared;

import org.jetbrains.annotations.NotNull;

import java.awt.Point;

public class ReverseDirection implements IPinLocationAdjustment
{

	@Override public void adjust(@NotNull Point[] locations, boolean isVertical)
	{
		for (Point location : locations) {
			location.x = -location.x;
			location.y = -location.y;
		}
	}
}
