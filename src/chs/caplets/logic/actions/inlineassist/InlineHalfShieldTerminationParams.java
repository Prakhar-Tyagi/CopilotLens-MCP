/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025-2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caplets.logic.actions.PlaceInlineBackshellTerminationEnum;
import chs.caplets.logic.actions.PlaceInlineShieldTerminationEnum;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.common.ILocation;
import chs.common.Location;
import chs.utility.helpers.PinListHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;

/**
 * This class contains parameters for inline half shield termination.
 */
public class InlineHalfShieldTerminationParams
{

	@NotNull private final IPinList mSchemInlineHalf;
	@NotNull private final ShieldTerminationInfo mShieldTerminationInfo;
	@NotNull private final IShieldConductor mInlineHalfShield;
	@NotNull private IPin mSchemInlineHalfPin;
	@NotNull private IAbstractPin mInlineHalfPin;
	private final ShieldTerminationPinTypeMapper mShieldTerminationPinTypeMapper;

	public InlineHalfShieldTerminationParams(@NotNull IShieldConductor inlineHalfShield,
			@NotNull ShieldTerminationInfo shieldTerminationInfo, @NotNull IPinList schemInlineHalf)
	{
		mInlineHalfShield = inlineHalfShield;
		mShieldTerminationInfo = shieldTerminationInfo;
		mSchemInlineHalf = schemInlineHalf;
		mShieldTerminationPinTypeMapper = new ShieldTerminationPinTypeMapper();
	}

	@NotNull
	public IShieldConductor getInlineHalfShield()
	{
		return mInlineHalfShield;
	}

	@NotNull
	public ILocation getInlineHalfPinAbsLocation()
	{
		return new Location(mShieldTerminationInfo.getTerminationLocation());
	}

	@NotNull
	public Point getInlineHalfPinRelativePosition()
	{
		return PinListHelper.getRelativeToPinList(mSchemInlineHalf, mShieldTerminationInfo.getTerminationLocation());
	}

	@NotNull public IPinList getSchemInlineHalf()
	{
		return mSchemInlineHalf;
	}

	public void setSchemInlineHalfPin(@NotNull IPin schemInlineHalfPin)
	{
		mSchemInlineHalfPin = schemInlineHalfPin;
	}

	@NotNull public IPin getSchemInlineHalfPin()
	{
		return mSchemInlineHalfPin;
	}

	public void setInlineHalfPin(@NotNull IAbstractPin inlineHalfPin)
	{
		mInlineHalfPin = inlineHalfPin;
	}

	@NotNull public IAbstractPin getInlineHalfPin()
	{
		return mInlineHalfPin;
	}

	@NotNull public PlaceInlineShieldTerminationEnum getShieldTerminationType()
	{
		return mShieldTerminationInfo.getShieldTerminationType();
	}

	@NotNull
	public PlaceInlineBackshellTerminationEnum getShieldTerminationPinType()
	{
		return mShieldTerminationInfo.getShieldTerminationPinType();
	}

	@NotNull
	public ShieldTerminationPinType getPinType()
	{
		return mShieldTerminationPinTypeMapper.getShieldTerminationPinType(getShieldTerminationPinType());
	}
}
