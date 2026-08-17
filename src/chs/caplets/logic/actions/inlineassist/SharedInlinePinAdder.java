/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.inlineassist;

import chs.caplets.logic.actions.AddPinActionHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adds pins to AddPinActionHelper in the correct location for inserting either a horizontal or vertical inline.
 */
public class SharedInlinePinAdder
{

	@NotNull private final AddPinActionHelper mAddPinActionHelper;
	@NotNull private final InlineExtent mInlineExtent;
	private final boolean mPinsVertical;
	private final boolean mReversedPinSide;

	SharedInlinePinAdder(@NotNull AddPinActionHelper addPinActionHelper, @NotNull InlineExtent inlineExtent,
			boolean pinsVertical, boolean reversedPinSide)
	{
		mAddPinActionHelper = addPinActionHelper;
		mInlineExtent = inlineExtent;
		mPinsVertical = pinsVertical;
		mReversedPinSide = reversedPinSide;
	}

	void invoke()
	{
		final List<Point> additionalPinPositions = mInlineExtent.getAdditionalPinPositions();
		List<Point> sortedPoints = new ArrayList<>(additionalPinPositions.size() + 1);
		// This is the first point of the longest segment wire - but it could be anywhere in the list if wires we are
		// going to split so we need to get all the positions then sort them so the pins are added in top to bottom or
		// left to right order
		sortedPoints.add(adjustPositionToSide(mInlineExtent.getFirstWirePoint()));
		additionalPinPositions.stream()
				.map(this::adjustPositionToSide)
				.collect(Collectors.toCollection(() -> sortedPoints));
		sortPoints(sortedPoints);
		// Register them with the action helper. If any of these positions is not on the correct side of the plug then
		// the action will crash in some weird and wonderful way, not checked here.
		sortedPoints.forEach(mAddPinActionHelper::addPinAtPosition);
	}

	@NotNull private Point adjustPositionToSide(@NotNull Point position)
	{
		final Point firstPoint = mInlineExtent.getFirstPoint();
		final Point secondPoint = mInlineExtent.getSecondPoint();
		Point pinLocation;
		if (mPinsVertical) {
			pinLocation = new Point(mReversedPinSide ? firstPoint.x : secondPoint.x, position.y);
		}
		else {
			pinLocation = new Point(position.x, mReversedPinSide ? firstPoint.y : secondPoint.y);
		}
		return pinLocation;
	}

	private void sortPoints(@NotNull List<Point> pinPositions)
	{
		final Comparator<Point> comparator = new Comparator<Point>()
		{

			@Override public int compare(Point o1, Point o2)
			{
				// We want the pins to be added left to right or top to bottom, requires sorting inverted for y-axis.
				int p1 = mPinsVertical ? o1.y : o1.x;
				int p2 = mPinsVertical ? o2.y : o2.x;
				if (p1 > p2) {
					return mPinsVertical ? -1 : 1;
				}
				if (p1 < p2) {
					return mPinsVertical ? 1 : -1;
				}
				return 0;
			}
		};
		pinPositions.sort(comparator);
	}
}
