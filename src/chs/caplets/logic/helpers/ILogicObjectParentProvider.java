/*
* Copyright 2017 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.
*/

package chs.caplets.logic.helpers;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectorBase;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldBody;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author pbhawsar on 25-05-2017
 */
public interface ILogicObjectParentProvider
{

	@Nullable IUIDObject getParent(@NotNull IAbstractPin pin);

	@Nullable IUIDObject getParent(@NotNull IConductor conductor);

	@Nullable IUIDObject getParent(@NotNull IMulticore multicore);

	@Nullable IUIDObject getParent(@NotNull IDeviceConnector deviceConnector);

	@Nullable IUIDObject getParent(@NotNull IBackshell backshell);

	@Nullable IUIDObject getParent(@NotNull IShieldBody shieldBody);

	@Nullable IUIDObject getParent(@NotNull IConnectorBase positionedObject);
}
