package chs.caplets.shared;

import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;

public class ReversePinOrder implements IPinLocationAdjustment
{

	@Override public void adjust(@NotNull Point[] locations, boolean isVertical)
	{
		Collections.reverse(Arrays.asList(locations));
	}
}
