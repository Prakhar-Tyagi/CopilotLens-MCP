package chs.caplets.shared;

import chs.utilities.CHSConstants;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.stream.IntStream;

public class DecrementSpace implements IPinLocationAdjustment
{

	@Override public void adjust(@NotNull Point[] locations, boolean isVertical)
	{
		int length = locations.length;
		if (length <= 1) {
			return;
		}

		if (!canSpaceBeDecremented(locations, isVertical)) {
			return;
		}

		boolean isPinOrderReversed = isPinOrderReversed(locations);

		for (int i = 0; i < length; i++) {
			int multiplier = isPinOrderReversed ? (length-1-i) : i;
			if (isVertical) {
				int value = locations[i].x;
				locations[i].x -= (Integer.signum(value) * multiplier * CHSConstants.PIN_SPACING);
			}
			else {
				int value = locations[i].y;
				locations[i].y -= (Integer.signum(value) * multiplier * CHSConstants.PIN_SPACING);
			}
		}
	}

	private boolean canSpaceBeDecremented(Point[] locations, boolean isVertical)
	{
		if (isVertical) {
			return IntStream.range(1, locations.length)
					.allMatch(index -> Math.abs(locations[index].x - locations[index - 1].x) > CHSConstants.PIN_SPACING);
		}
		else {
			return IntStream.range(1, locations.length)
					.allMatch(index -> Math.abs(locations[index].y - locations[index - 1].y) > CHSConstants.PIN_SPACING);
		}
	}
}
