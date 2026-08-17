/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IText;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.shared.PinListTypeEnum;

/**
 * This class exists for typing only.
 */
public class AddSharedInterconnectConnectorAction extends AddSharedPlugConnectorAction
{

	public AddSharedInterconnectConnectorAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller, sharedSelectMgr);
	}

	protected PinListTypeEnum getType()
	{
		return INTERCONNECT_CONNECTOR;
	}

	public boolean onTerminate(boolean successful)
	{
		boolean state = super.onTerminate(successful) && successful;
		if (state && getCreatedPinList() != null) {
			for (IAbstractSchemPin spin : getCreatedPinList().getAllPins()) {
				//
				// REMOVE text from interconnect pins.
				//
				// FEAT00013786: interconnect pins are not yet allowed in stack pins. assert it here.
				assert spin instanceof IPin;
				for (IGfxObjectIterator titr = spin.getObjects(true); titr.hasNext(); ) {
					IGfxObject gobj = titr.getNext();
					if (gobj instanceof IText) {
						spin.removeObject(gobj);
					}
				}
			}
		}
		return state;
	}

	public String getActionUIClass()
	{
		return AddSharedInterconnectConnectorActionUI.class.getName();
	}
}
