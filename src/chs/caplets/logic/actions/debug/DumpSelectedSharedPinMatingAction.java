/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.debug;

import chs.caf.caplet.ICapletController;
import chs.capitalmanager.appserver.UserSessionException;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.common.IUIDObject;
import chs.utilities.WrappingRuntimeException;

import java.util.Map;

public class DumpSelectedSharedPinMatingAction extends DumpSelectedAction
{

	public DumpSelectedSharedPinMatingAction(ICapletController controller)
	{
		super(controller);
	}

	protected void dumpObject(IUIDObject obj)
	{
		if (obj instanceof IRepresentedObject) {
			dumpObject(((IRepresentedObject) obj).getRawConnectivity());
		}
		else if (obj instanceof IPinList) {
			IPinList pinList = (IPinList) obj;
			ISharedPinList sharedPinList = pinList.getSharedPinList();

			if (sharedPinList != null) {
				printMsg("[Shared pin list]: " + sharedPinList.getFullName());

				// Retrieve mated pins
				try {
					Map<ISharedPin, ISharedPin> matedSharedPins =
							SharedPinListHelper.getSharedPinsMating(sharedPinList);
					for (ISharedPin pin : matedSharedPins.keySet()) {
						ISharedPin matedPin = matedSharedPins.get(pin);
						printMsg("&nbsp;&nbsp;[Shared Pin]: " + pin.getName()
								+ " [Mated Pin]: " + matedPin.getName()
								+ " [Owner]: " + matedPin.getOwner().getName());
					}
				}
				catch (UserSessionException e1) {
					throw new WrappingRuntimeException(e1);
				}
			}
		}
	}

	public String getActionUIClass()
	{
		return DumpSelectedSharedPinMatingActionUI.class.getName();
	}
}
