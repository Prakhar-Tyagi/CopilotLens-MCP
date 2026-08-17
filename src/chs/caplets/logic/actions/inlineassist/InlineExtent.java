/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025-2026 Siemens
 */
package chs.caplets.logic.actions.inlineassist;

import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IConductor;
import chs.services.dynamicgfx.ISmartPoint;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps a list of three Points, where the first is first corner, the second is the center of the first wire to split
 * and the last is the last corner (diagonal offset from the first)
 */
public class InlineExtent
{

	//
	//             .---------|---------. SecondPoint
	//             |         |         |
	//    ---------|         |         |---------
	//             |         |         |
	//    ---------|  FirstWi.rePoint  |---------
	//             |         |         |
	//  FirstPoint .---------|---------.
	//
	// For vertical orientation the first and second points are top left and bottom right, but we convert them in this
	// class to be consistent
	//
	// This is first corner of the inline
	@NotNull private final Point mFirstPoint;
	// This is a point that is on the first wire to be split, it will be in the middle of first and second point on
	// either the x or y axis, depending on the orientation
	@NotNull private final Point mFirstWirePoint;
	// This is the second corner of the inline
	@NotNull private final Point mSecondPoint;

	// The original ISmartPoints the object was constructed with
	@NotNull private final List<ISmartPoint> mPoints;
	// The positions of additional pins to add (needed for shared inline only)
	@NotNull private final List<Point> mAdditionalPinPositions;
	@NotNull private Map<IShieldConductor, ShieldPositionData> mShieldTerminationInfo =
			new LinkedHashMap<>();

	@NotNull private Collection<IConductor> mAddedConductors = new HashSet<>();

	InlineExtent(@NotNull List<ISmartPoint> points, @NotNull List<Point> additionalPinPositions,
			@NotNull Collection<IConductor> addedConductors)
	{
		if (points.size() != 3) {
			throw new IllegalArgumentException("Expected points to be 3 in size");
		}
		mPoints = new ArrayList<ISmartPoint>(points);
		mAdditionalPinPositions = new ArrayList<>(additionalPinPositions);
		mFirstPoint = points.get(0).getAbsoluteLocation();
		mFirstWirePoint = points.get(1).getAbsoluteLocation();
		mSecondPoint = points.get(2).getAbsoluteLocation();

		if (mFirstPoint.y > mSecondPoint.y) {
			// Must be vertical case where first point is top left and second point is bottom right, convert to same
			// as horizontal case
			int firstPointY = mFirstPoint.y;
			mFirstPoint.y = mSecondPoint.y;
			mSecondPoint.y = firstPointY;
		}

		if (mFirstPoint.x > mSecondPoint.x || mFirstPoint.y > mSecondPoint.y ||
				mFirstWirePoint.x < mFirstPoint.x || mFirstWirePoint.x > mSecondPoint.x ||
				mFirstWirePoint.y < mFirstPoint.y || mFirstWirePoint.y > mSecondPoint.y) {
			throw new IllegalArgumentException("Unexpected geometry of points");
		}
		mAddedConductors = new HashSet<>(addedConductors);
	}

	@NotNull public Point getFirstPoint()
	{
		return mFirstPoint;
	}

	@NotNull public Point getFirstWirePoint()
	{
		return mFirstWirePoint;
	}

	@NotNull public Point getSecondPoint()
	{
		return mSecondPoint;
	}

	@NotNull public List<ISmartPoint> getPoints()
	{
		return mPoints;
	}

	@NotNull public List<Point> getAdditionalPinPositions()
	{
		return mAdditionalPinPositions;
	}

	public int getNumberOfPins()
	{
		// mFirstWirePoint represents the first pin
		return getAdditionalPinPositions().size() + 1;
	}

	@NotNull public Point getCenter()
	{
		Rectangle rectangle = new Rectangle();
		rectangle.add(getFirstPoint());
		rectangle.add(getSecondPoint());
		//noinspection NumericCastThatLosesPrecision
		return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY());
	}

	@NotNull
	public Map<IShieldConductor, ShieldPositionData> getShieldTerminationInfos()
	{
		return mShieldTerminationInfo;
	}

	@NotNull
	public Collection<IConductor> getAddedConductors()
	{
		return mAddedConductors;
	}

	public void addShieldTerminationPosition(@NotNull IShieldConductor shield,
			@NotNull ShieldPositionData shieldPositionData)
	{
		mShieldTerminationInfo.put(shield, shieldPositionData);
	}
}
