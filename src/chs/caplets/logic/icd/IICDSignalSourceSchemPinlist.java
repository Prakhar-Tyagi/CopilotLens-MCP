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

/**
 * @author pbhawsar on 04-04-2017
 */
public interface IICDSignalSourceSchemPinlist
{

	@NotNull Collection<? extends IDeviceICDSignalsContainer> getICDSignalContainers(@NotNull IDeviceICD icd);

	@Nullable ILocation getPinLocation(@Nullable String pinName);

	@NotNull IPinList getSchemPinlist();

	@Nullable IPinList getSchemDevice();

	@Nullable IPin getEquivalentICDMatchingSignalPin(@Nullable IPin pin);

	@Nullable IPin getConnectedSchemHarnConnectorPin(@Nullable IPin pin);

	@Nullable IPin getSignalMatchingDevicePin(@Nullable String pinName);

	@Nullable IDevice getCableDevice();
}
