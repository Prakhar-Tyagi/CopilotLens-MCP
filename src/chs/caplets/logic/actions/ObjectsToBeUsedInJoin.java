/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2021-2025 Siemens
 */

package chs.caplets.logic.actions;

import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.cofUtils.parameterized.PinSideCalculator;
import chs.common.IExtent;
import chs.common.Side;
import chs.system.FactoryMgr;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.ReverseMap;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.TransformHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resultant object to be used for join
 */
abstract class ObjectsToBeUsedInJoin
{

	protected ReverseMap<IAbstractSchemPin, IAbstractSchemPin> m_connectedPins;
	protected int gridSpacing = 0;
	protected String m_errorMessage = "";
	protected Map<IPinList, Side> m_sideOfAttachedPinlist;
	protected List<Pair<IPinList, IPinList>> m_matedPairs;
	private Boolean isVerticalPinPlacement;
	private Boolean isHorizontalPinPlacement;

	protected boolean addSchemInstances(@Nullable IPinList currentSchem, @Nullable IPinList mateSchem)
	{
		if (currentSchem != null && currentSchem.getParameterized() == null) {
			m_errorMessage =
					ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.NonParameterized",
							JoinPinlistsHelper.getType(currentSchem));
			return false;
		}

		if (mateSchem != null && mateSchem.getParameterized() == null) {
			m_errorMessage =
					ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.NonParameterized",
							JoinPinlistsHelper.getType(mateSchem));
			return false;
		}
		return true;
	}

	@Nullable abstract IPinList getAnchorObject();

	abstract IPinList getInstanceOfAnchorObject(Pair<IPinList, IPinList> pairOfPinlists);

	protected boolean areAttachedPinlistsHandledInResize()
	{
		return false;
	}

	String getErrorMessage()
	{
		return m_errorMessage;
	}

	Collection<IPinList> getAnchorSchemInstances()
	{
		Collection<IPinList> anchorPinlistInstances = new HashSet<IPinList>(m_matedPairs.size());
		for (Pair<IPinList, IPinList> pair : m_matedPairs) {
			anchorPinlistInstances.add(getInstanceOfAnchorObject(pair));
		}
		return anchorPinlistInstances;
	}

	List<Pair<IPinList, IPinList>> sortPinlistInstancesOnX()
	{
		List<Pair<IPinList, IPinList>> sortedOnX = new ArrayList<Pair<IPinList, IPinList>>(m_matedPairs);

		Collections.sort(sortedOnX, new Comparator<Pair<IPinList, IPinList>>()
		{

			@Override public int compare(Pair<IPinList, IPinList> o1, Pair<IPinList, IPinList> o2)
			{
				int o1Y = ExtentHelper.getAbsExtent(getInstanceOfAnchorObject(o1)).getX();
				int o2Y = ExtentHelper.getAbsExtent(getInstanceOfAnchorObject(o2)).getX();
				return o1Y - o2Y;
			}
		});
		return sortedOnX;
	}

	List<Pair<IPinList, IPinList>> sortPinlistInstancesOnY()
	{
		List<Pair<IPinList, IPinList>> sortedOnY = new ArrayList<Pair<IPinList, IPinList>>(m_matedPairs);

		Collections.sort(sortedOnY, new Comparator<Pair<IPinList, IPinList>>()
		{

			@Override public int compare(Pair<IPinList, IPinList> o1, Pair<IPinList, IPinList> o2)
			{
				int o1Y = ExtentHelper.getAbsExtent(getInstanceOfAnchorObject(o1)).getY();
				int o2Y = ExtentHelper.getAbsExtent(getInstanceOfAnchorObject(o2)).getY();
				return o1Y - o2Y;
			}
		});
		return sortedOnY;
	}

	private void initializePlacementOfPins(@Nullable IPinList pinlist)
	{
		Collection<Side> sideOfPins = getPinSides(pinlist);
		isVerticalPinPlacement = false;
		isHorizontalPinPlacement = false;
		if (sideOfPins.contains(Side.LEFT) || sideOfPins.contains(Side.RIGHT)) {
			isVerticalPinPlacement = true;
		}
		if (sideOfPins.contains(Side.TOP) || sideOfPins.contains(Side.BOTTOM)) {
			isHorizontalPinPlacement = true;
		}
		if (!(isVerticalPinPlacement ^ isHorizontalPinPlacement)) {
			isVerticalPinPlacement = false;
			isHorizontalPinPlacement = false;
			TransformHelper transInfo = null;

			transInfo = TransformHelper.getTransformInfo(Objects.requireNonNull(pinlist).getTransform());
			if (transInfo.getRotation() == 90 || transInfo.getRotation() == 270) {
				isHorizontalPinPlacement = true;
			}
			else {
				isVerticalPinPlacement = true;
			}
		}
	}

	boolean isVerticalPlacementOfPins(IPinList pinlist)
	{
		if (isVerticalPinPlacement == null) {
			initializePlacementOfPins(pinlist);
		}

		return isVerticalPinPlacement;
	}

	boolean isHorizontalPlacementOfPins(@Nullable IPinList pinlist)
	{
		if (isHorizontalPinPlacement == null) {
			initializePlacementOfPins(pinlist);
		}

		return isHorizontalPinPlacement;
	}

	@Nullable IExtent determineExtentOfNewStitchedObject()
	{
		IPinList anchorObject = getAnchorObject();
		boolean verticalPinPlacement = isVerticalPlacementOfPins(getAnchorObject());
		boolean horizontalPinPlacement = isHorizontalPlacementOfPins(getAnchorObject());

		if (!verticalPinPlacement && !horizontalPinPlacement) {
			m_errorMessage =
					"There are pins on the both sides of pinlist which is the first selection made for join";
			return null;
		}
		if (verticalPinPlacement) {
			List<Pair<IPinList, IPinList>> sortedAnchorSchemInstances = sortPinlistInstancesOnY();
			if (sortedAnchorSchemInstances == null) {
				return null;
			}

			return determineExtentForVerticalPlacementUsingUnionApproach(anchorObject,
					sortedAnchorSchemInstances);
		}
		List<Pair<IPinList, IPinList>> sortedAnchorSchemInstances = sortPinlistInstancesOnX();
		if (sortedAnchorSchemInstances == null) {
			return null;
		}

		return determineExtentForHorizontalPlacementUsingUnionApproach(anchorObject,
				sortedAnchorSchemInstances);
	}

	protected boolean addMissingMatedPairs()
	{
		return true;
	}

	public boolean verifyPinlistPairs()
	{
		for (Pair<IPinList, IPinList> pinListPair : m_matedPairs) {
			IPinList anchorInstance = getInstanceOfAnchorObject(pinListPair);
			if (anchorInstance.getParameterized() == null) {
				m_errorMessage =
						ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.NonParameterized",
								JoinPinlistsHelper.getType(anchorInstance));
				return false;
			}
			IPinList attachedPinList;
			if (anchorInstance == pinListPair.getFirst()) {
				attachedPinList = pinListPair.getSecond();
			}
			else {
				attachedPinList = pinListPair.getFirst();
			}
			if (attachedPinList == null) {
				continue;
			}
			if (attachedPinList.getParameterized() == null) {
				m_errorMessage =
						ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.NonParameterized",
								JoinPinlistsHelper.getType(attachedPinList));
				return false;
			}
		}
		return true;
	}

	boolean validatePinAndAttachedPinlist(IAbstractSchemPin pin, IPinList attachedPinlist)
	{
		if (m_connectedPins == null) {
			return false;
		}
		if (isBackshellTermination(pin)) {
			//Backshell termination will not have a connected pin
			return true;
		}
		IAbstractSchemPin attachedPin = m_connectedPins.get(pin);
		if (attachedPin == null) {
			attachedPin = m_connectedPins.getKey(pin);
		}
		if (attachedPin == null) {

			return false;
		}
		return attachedPin.getParent() == attachedPinlist;
	}

	protected boolean isBackshellTermination(@NotNull IAbstractSchemPin schemPin)
	{
		return schemPin instanceof IGenericSchemPin &&
				((IConnectivityRef) schemPin).getConnectivity() instanceof IBackshellTermination;
	}

	protected void createConnectedPinPairs(@Nullable IPinList currentPinlist, @Nullable IPinList matePinlist)
	{
		if (m_connectedPins == null) {
			m_connectedPins = new ReverseMap<IAbstractSchemPin, IAbstractSchemPin>();
		}

		ConnectionHelper connectionHelper = ConnectionHelper.getConnectionHelper(currentPinlist, matePinlist);
		if (connectionHelper != null) {
			Side sideOfAttachedPinlist = null;
			PinSideCalculator sideCalculator = PinSideCalculator.createAbsolute(currentPinlist);

			for (IAbstractSchemPin pin : currentPinlist.getAllPins(false)) {
				IAbstractSchemPin schemConnectorPin =
						connectionHelper.getConnectedPin(pin, IAbstractSchemPin.class);
				if (schemConnectorPin != null) {
					m_connectedPins.put(pin, schemConnectorPin);
					if (sideOfAttachedPinlist == null) {
						//side of attached pinlist is same as side of the pin.
						sideOfAttachedPinlist = sideCalculator.getSide(pin);
						if (!(currentPinlist.getConnectivity() instanceof IBaseDevice)) {
							sideOfAttachedPinlist = sideOfAttachedPinlist.getOpposite();
						}
					}
				}
			}

			if (m_sideOfAttachedPinlist == null) {
				m_sideOfAttachedPinlist = new HashMap<IPinList, Side>();
			}
			if (sideOfAttachedPinlist != null) {
				m_sideOfAttachedPinlist.put(matePinlist, sideOfAttachedPinlist);
				m_sideOfAttachedPinlist.put(currentPinlist, sideOfAttachedPinlist.getOpposite());
			}
			else{
				Side side = ExtentHelper.getSide(ExtentHelper.getAbsNonTextExtent(currentPinlist), ExtentHelper.getAbsNonTextExtent(matePinlist));
				m_sideOfAttachedPinlist.put(matePinlist, side);
				m_sideOfAttachedPinlist.put(currentPinlist, side.getOpposite());
			}
		}
	}

	public Side getSideOfAttachedPinlist(IPinList attachedPinListInstance)
	{
		return m_sideOfAttachedPinlist.get(attachedPinListInstance);
	}

	protected boolean areAttachedPinlistsOfCorrectSize(
			Map<IPinList, JoinPinlistsHelper.LocationAndExtentOfPinlistOnMergedPinlist> attachedPinlistLocation)
	{
		for (Pair<IPinList, IPinList> pinListPair : m_matedPairs) {
			IPinList anchorInstance = getInstanceOfAnchorObject(pinListPair);
			IPinList attachedPinList;
			if (anchorInstance == pinListPair.getFirst()) {
				attachedPinList = pinListPair.getSecond();
			}
			else {
				attachedPinList = pinListPair.getFirst();
			}
			if (attachedPinList == null) {
				continue;
			}

			IExtent attachedPinlistExtent = JoinPinlistsHelper.getOriginalExtent(attachedPinList, gridSpacing);
			IExtent anchorInstanceExtent = JoinPinlistsHelper.getOriginalExtent(anchorInstance, gridSpacing);
			JoinPinlistsHelper.LocationOnMergedPinlist locationOnMergedPinlist =
					attachedPinlistLocation.get(attachedPinList);
			if (locationOnMergedPinlist == null) {
				continue;
			}
			Side sideOfAttachedPinlist = locationOnMergedPinlist.getSide();
			boolean valid = false;
			if (sideOfAttachedPinlist == Side.LEFT || sideOfAttachedPinlist == Side.RIGHT) {
				if (attachedPinlistExtent != null && anchorInstanceExtent != null &&
						attachedPinlistExtent.getY() >= anchorInstanceExtent.getY() &&
						attachedPinlistExtent.getTop() <= anchorInstanceExtent.getTop()) {
					valid = true;
				}
			}
			else if (sideOfAttachedPinlist == Side.TOP || sideOfAttachedPinlist == Side.BOTTOM) {
				if (attachedPinlistExtent != null && anchorInstanceExtent != null &&
						attachedPinlistExtent.getX() >= anchorInstanceExtent.getX() &&
						attachedPinlistExtent.getRight() <= anchorInstanceExtent.getRight()) {
					valid = true;
				}
			}

			if (!valid) {
				m_errorMessage =
						ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.LongerOrWider",
								JoinPinlistsHelper.getType(anchorInstance),
								JoinPinlistsHelper.getType(attachedPinList));
				return false;
			}
		}
		return true;
	}

	@NotNull private Collection<Side> getPinSides(@Nullable IPinList pinList)
	{
		Collection<Side> sideOfPins = new HashSet<Side>();
		PinSideCalculator pinSideCalculator = PinSideCalculator.createAbsolute(pinList);

		for (IAbstractSchemPin aPin : Objects.requireNonNull(pinList).getAllPins(false)) {

			Side side = pinSideCalculator.getSide(aPin);
			sideOfPins.add(side);
		}

		return sideOfPins;
	}

	private IExtent determineExtentForVerticalPlacementUsingUnionApproach(IPinList anchorObject,
			List<Pair<IPinList, IPinList>> anchorSchemInstances)
	{
		IExtent extent = FactoryMgr.getCommonFactory().createExtent();
		extent.invalidate();
		IExtent extentOfAnchorObject = JoinPinlistsHelper.getOriginalExtent(anchorObject, gridSpacing);

		if (extentOfAnchorObject != null) {
			int maxWidth = extentOfAnchorObject.getWidth();
			int lowerY = extentOfAnchorObject.getY();
			int upperY = extentOfAnchorObject.getY() + extentOfAnchorObject.getHeight();

			for (Pair<IPinList, IPinList> matedPair : anchorSchemInstances) {
				IPinList instanceOfAnchorObject = getInstanceOfAnchorObject(matedPair);
				if (instanceOfAnchorObject == anchorObject) {
					continue;
				}
				IExtent extentOfAnInstance = JoinPinlistsHelper.getOriginalExtent(instanceOfAnchorObject, gridSpacing);

				Objects.requireNonNull(extentOfAnInstance);

				if (extentOfAnInstance.getWidth() > maxWidth) {
					maxWidth = extentOfAnInstance.getWidth();
				}
				if (upperY < extentOfAnInstance.getY() + extentOfAnInstance.getHeight()) {
					upperY = extentOfAnInstance.getY() + extentOfAnInstance.getHeight();
				}
				if (lowerY > extentOfAnInstance.getY()) {
					lowerY = extentOfAnInstance.getY();
				}
			}
			extent.setY(lowerY);
			if (getAnchorsVerticalFixedSide() == Side.RIGHT) {
				extent.setX(extentOfAnchorObject.getRight() - maxWidth);
			}
			else {
				extent.setX(extentOfAnchorObject.getX());
			}

			extent.setHeight(upperY - lowerY);
			extent.setWidth(maxWidth);
		}

		return extent;
	}

	@Nullable private IExtent determineExtentForHorizontalPlacementUsingUnionApproach(IPinList anchorObject,
			List<Pair<IPinList, IPinList>> sortedAnchorSchemInstances)
	{
		IExtent extent = FactoryMgr.getCommonFactory().createExtent();
		extent.invalidate();
		IExtent extentOfAnchorObject = JoinPinlistsHelper.getOriginalExtent(anchorObject, gridSpacing);
		if (extentOfAnchorObject == null) {
			return null;
		}
		int maxHeight = extentOfAnchorObject.getHeight();
		int leftX = extentOfAnchorObject.getX();
		int rightX = extentOfAnchorObject.getX() + extentOfAnchorObject.getWidth();

		for (Pair<IPinList, IPinList> matedPair : sortedAnchorSchemInstances) {
			IPinList instanceOfAnchorObject = getInstanceOfAnchorObject(matedPair);
			if (instanceOfAnchorObject == anchorObject) {
				continue;
			}
			IExtent extentOfAnInstance = JoinPinlistsHelper.getOriginalExtent(instanceOfAnchorObject, gridSpacing);

			Objects.requireNonNull(extentOfAnInstance);

			if (extentOfAnInstance.getHeight() > maxHeight) {
				maxHeight = extentOfAnInstance.getHeight();
			}
			if (leftX > extentOfAnInstance.getX()) {
				leftX = extentOfAnInstance.getX();
			}
			if (rightX < extentOfAnInstance.getX() + extentOfAnInstance.getWidth()) {
				rightX = extentOfAnInstance.getX() + extentOfAnInstance.getWidth();
			}
		}
		extent.setX(leftX);

		if (getAnchorsHorizontalFixedSide() == Side.TOP) {
			extent.setY(extentOfAnchorObject.getTop() - maxHeight);
		}
		else {
			extent.setY(extentOfAnchorObject.getY());
		}

		extent.setWidth(rightX - leftX);
		extent.setHeight(maxHeight);

		return extent;
	}

	Collection<Pair<IPinList, IPinList>> getAttachedSchemInstances()
	{
		return m_matedPairs;
	}

	void preMovePin(IAbstractSchemPin pin)
	{

	}

	boolean arePinsOnBothSidesOfAnchor()
	{
		return false;
	}

	protected String verifyAnchorCanBeCreatedOnTheExtent(IExtent newExtent)
	{
		IPinList anchor = getAnchorObject();
		IPinList sameInstanceInJoin = getSameInstanceInJoin();

		if (sameInstanceInJoin != null) {
			Side side = m_sideOfAttachedPinlist.get(anchor);

			if (side == Side.LEFT || side == Side.RIGHT) {
				int xOffset = 0;
				if (side == Side.LEFT) {
					xOffset = -newExtent.getWidth();
				}

				for (IAbstractSchemPin aPin : sameInstanceInJoin.getAllPins(false)) {
					int pinY = aPin.getAbsLocation().getY();
					int pinX = aPin.getAbsLocation().getX() + xOffset;
					if (pinY > newExtent.getY() && newExtent.getTop() > pinY) {
						if (sameInstanceInJoin.getConnectivity() instanceof IBaseDevice) {
							if (pinX != newExtent.getX()) {
								continue;
							}
						}

						if (!m_connectedPins.keySet().contains(aPin) && !m_connectedPins.values().contains(aPin)) {
							return ResourceMgr.getString(JoinPinlistsAction.class,
									"JoinPinlistAction.DiagramObjectInBetween", JoinPinlistsHelper.getType(anchor));
						}
					}
				}
			}
			if (side == Side.TOP || side == Side.BOTTOM) {
				int yOffset = 0;
				if (side == Side.BOTTOM) {
					yOffset = -newExtent.getHeight();
				}

				for (IAbstractSchemPin aPin : sameInstanceInJoin.getAllPins(false)) {
					int pinX = aPin.getAbsLocation().getX();
					int pinY = aPin.getAbsLocation().getY() + yOffset;
					if (pinX > newExtent.getX() && newExtent.getRight() > pinX) {
						if (sameInstanceInJoin.getConnectivity() instanceof IBaseDevice) {
							if (pinY != newExtent.getY()) {
								continue;
							}
						}
						if (!m_connectedPins.keySet().contains(aPin)) {
							return ResourceMgr.getString(JoinPinlistsAction.class,
									"JoinPinlistAction.DiagramObjectInBetween", JoinPinlistsHelper.getType(anchor));
						}
					}
				}
			}
		}
		return "";
	}

	@Nullable protected IPinList getSameInstanceInJoin()
	{
		return null;
	}

	protected Side getAnchorsHorizontalFixedSide()
	{

		IPinList anchor = getAnchorObject();
		IPinList sameInstanceInJoin = getSameInstanceInJoin();
		if (sameInstanceInJoin != null) {
			return m_sideOfAttachedPinlist.get(anchor).getOpposite();
		}
		return Side.TOP;
	}

	protected Side getAnchorsVerticalFixedSide()
	{
		IPinList anchor = getAnchorObject();
		IPinList sameInstanceInJoin = getSameInstanceInJoin();
		if (sameInstanceInJoin != null) {
			return m_sideOfAttachedPinlist.get(anchor).getOpposite();
		}
		return Side.RIGHT;
	}
}
