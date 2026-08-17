/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.ISplice;
import chs.common.IDesignContainer;
import chs.utility.logic.DesignHelper;
import chs.utility.logic.PinUtils;

import java.awt.event.ActionEvent;

/**
 * Sub action to create a parameterized splice instance based on an existing connectivity object
 * <p/>
 * This is a "sub action" designed to be constructed and used by another action, rather than called via CAF
 */
public class AddParameterizedSpliceAction extends CreateSpliceAction
{

	private ISplice splice;

	public AddParameterizedSpliceAction(ICapletController controller, ISplice splice)
	{
		super(controller);
		assert splice != null;
		this.splice = splice;
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		if (splice != null) {
			IDesignContainer designContainer = DesignHelper.getDesign(splice);
			if (designContainer != null) {
				for (IAbstractPin sPin : splice.getPins()) {
					if (!PinUtils.isPinPlaceableInDesign(sPin, designContainer)) {
						return IActionEnum.eCanceled;
					}
				}
			}
		}
		return super.onActivate(e);
	}

	// Overridden here to set the cable splice field to the existing splice passed to the sub action ctor
	@Override public boolean onTerminate(boolean successful)
	{
		if (successful) {
			assert splice != null;
			m_cableSplice = splice;
		}
		return super.onTerminate(successful);
	}

	@Override protected ISplice getCableSplice()
	{
		return splice;
	}
}
