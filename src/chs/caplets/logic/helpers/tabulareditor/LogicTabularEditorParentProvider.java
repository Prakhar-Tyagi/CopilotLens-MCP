/*
* Copyright 2017 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.
*/

package chs.caplets.logic.helpers.tabulareditor;

import chs.caplets.logic.helpers.ILogicObjectParentProvider;
import chs.cof.logical.IInternalPositionBase;
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
public class LogicTabularEditorParentProvider implements ILogicObjectParentProvider
{

	@Nullable @Override public IUIDObject getParent(@NotNull IAbstractPin pin)
	{
		return pin.getOwner();
	}

	@Nullable @Override public IUIDObject getParent(@NotNull IConductor conductor)
	{
		return conductor.getMulticore();
	}

	@Nullable @Override public IUIDObject getParent(@NotNull IMulticore multicore)
	{
		return multicore.getParent();
	}

	@Nullable @Override public IUIDObject getParent(@NotNull IDeviceConnector deviceConnector)
	{
		return deviceConnector.getOwner();
	}

	@Nullable @Override public IUIDObject getParent(@NotNull IBackshell backshell)
	{
		return backshell.getOwner();
	}

	@Nullable @Override public IUIDObject getParent(@NotNull IShieldBody shieldBody)
	{
		return shieldBody.getMulticore();
	}

	@Nullable @Override public IUIDObject getParent(@NotNull IConnectorBase positionedObject)
	{
		IInternalPositionBase position = positionedObject.getOccupiedPosition();
		if (position != null) {
			return position.getInternalPositionContainer();
		}
		return null;
	}
}
