/*
* Copyright 2017 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.
*/

package chs.caplets.logic.icd;

import chs.cof.icd.IDeviceICD;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.common.ILocation;
import chs.utility.IDeviceICDSignalsContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author pbhawsar on 04-04-2017
 */
public class NullICDSignalSourceSchemPinList implements IICDSignalSourceSchemPinlist
{

	@NotNull private IPinList mSchemPinList;

	public NullICDSignalSourceSchemPinList(@NotNull IPinList schemPinList)
	{
		mSchemPinList = schemPinList;
	}

	@Nullable public IPin getConnectedSchemHarnConnectorPin(@Nullable IPin pin)
	{
		return pin;
	}

	@Nullable @Override public IPin getSignalMatchingDevicePin(@Nullable String pinName)
	{
		return null;
	}

	@Nullable public IDevice getCableDevice()
	{
		return null;
	}

	@NotNull @Override public Collection<? extends IDeviceICDSignalsContainer> getICDSignalContainers(@NotNull
			IDeviceICD icd)
	{
		return Collections.emptySet();
	}

	@Nullable @Override public ILocation getPinLocation(@Nullable String pinName)
	{
		return null;
	}

	@NotNull @Override public IPinList getSchemPinlist()
	{
		return mSchemPinList;
	}

	@Nullable @Override public IPinList getSchemDevice()
	{
		return null;
	}

	@Nullable @Override public IPin getEquivalentICDMatchingSignalPin(@Nullable IPin pin)
	{
		return pin;
	}
}
