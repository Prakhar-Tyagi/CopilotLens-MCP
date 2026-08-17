package chs.caplets.shared;

import org.jetbrains.annotations.NotNull;

import java.awt.Point;

public interface IPinLocationAdjustment
{
	default boolean isPinOrderReversed(@NotNull Point[] locations)
	{
		int length = locations.length;
		return locations.length > 1 && (locations[length - 1].x == 0 && locations[length - 1].y == 0);
	}

	void adjust(@NotNull Point[] locations, boolean isVertical);
}
