/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelection;
import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.shared.ISharedConductor;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Mar 3, 2004 Time: 1:19:45 PM To change this template use Options |
 * File Templates.
 */
public class AddLibraryInnercoreWireAction extends AbstractAddLibraryWireAction
{

	public AddLibraryInnercoreWireAction(ICapletController controller, ISpecialSelection libSelectMgr)
	{
		super(controller, libSelectMgr, IWireConductor.class);
	}

	public String getActionUIClass()
	{
		return AddLibraryInnercoreWireActionUI.class.getName();
	}

	@Override public boolean isEnabled()
	{
		//dts0100716240-FEAT15007: A shared muticore conductor should not be allowed to create as net in one design and as wire in another.
		boolean bEnabled = super.isEnabled();
		if (bEnabled) {
			ISharedConductor sharedCond = m_helper.getSharedConductor();
			if (sharedCond != null) {
				String type = sharedCond.getType();
				bEnabled = type == null || (type != null && type.compareToIgnoreCase("wire") == 0);
			}
		}
		return bEnabled;
	}

	@Override COFTypeEnum getObjectType()
	{
		return COFTypeEnum.Wire;
	}
}
