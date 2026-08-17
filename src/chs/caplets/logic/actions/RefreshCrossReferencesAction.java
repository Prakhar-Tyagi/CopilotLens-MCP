/*
 * Copyright 2003-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.IDesignSharedUsageMgr;
import chs.common.DesignUtils;
import chs.common.IUID;
import chs.common.IPrivilegedDesignMgr;

import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Oct 20, 2003 Time: 4:45:42 PM To change this template use Options |
 * File Templates.
 */
public class RefreshCrossReferencesAction extends ControllerActionRT
{

	public RefreshCrossReferencesAction(ICapletController controller)
	{
		super(controller);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		CAFUtils.getInstance().getCurrentProject().getCrossReferenceMonitor().generateCrossReferences();
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		return true;
	}

	public boolean isEnabled()
	{
		return hasLocalSharedUsages() && super.isEnabled();
	}

	public String getActionUIClass()
	{
		return RefreshCrossReferencesActionUI.class.getName();
	}

	private boolean hasLocalSharedUsages()
	{
		for(IUID designId : CAFUtils.getInstance().getCurrentProject().getDesignMgr().getLoadedDesigns()) {
			ILogicDesign loadedDesign = DesignUtils.getLoadedDesign(designId, ILogicDesign.class);
			if (loadedDesign != null && loadedDesign.isLocked()) {
				IDesignSharedUsageMgr dsum = loadedDesign.getSharedUsageMgr();
				if (dsum.getSharedConductorUsageCount() > 0 || dsum.getSharedPinListUsageCount() > 0) {
					return true;
				}
			}
		}
		return false;
	}
}
