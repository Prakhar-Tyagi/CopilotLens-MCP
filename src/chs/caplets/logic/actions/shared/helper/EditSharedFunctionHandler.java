/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.helper;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IPinList;
import chs.ctf.caf.utils.PinProxy;
import chs.ctf.caf.utils.PortProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditSharedFunctionHandler extends EditSharedPinlistHandler
{
	
	public EditSharedFunctionHandler(@NotNull EditSharedPinListModel esplModel, @NotNull ILogicDesign design,
			@Nullable IPinList cpl, @NotNull IEditSharedPinlistAdapter adapter)
	{
		super(esplModel, design, cpl, adapter);
	}

	@NotNull @Override protected PinProxy createPinProxy(IAbstractPin pin)
	{
		return new PortProxy(pin.getName(), false);
	}
}
