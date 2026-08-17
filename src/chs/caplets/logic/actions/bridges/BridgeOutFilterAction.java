/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.bridges;

import chs.bridges.BridgesIntegrationServices;
import chs.bridges.exporter.BridgeOutFilterDlg;
import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.bridges.BridgeAction;
import chs.caf.cafmain.actions.bridges.BridgeCAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.common.IDesignContainer;
import chs.common.IReleaseLevel;

import java.awt.event.ActionEvent;

public class BridgeOutFilterAction extends BridgeAction
{

	public BridgeOutFilterAction(ICapletController controller)
	{
		super(controller, true);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		IActionEnum oRC = IActionEnum.eCanceled;

		// Process current design
		IDesign design = (IDesign) BridgeCAFUtils.getDesign();
		if (design != null) {
			BridgeOutFilterDlg filter =
					new BridgeOutFilterDlg(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), design);
		}

		return oRC;
	}

	public boolean enabledInReadOnly()
	{
		return true;
	}

	public boolean isActionAllowedForDesign(IDesignContainer design)
	{
		return true;
	}

	public boolean isEnabled()
	{
		boolean enabled = super.isEnabled();
		if (enabled) {
			enabled = false;
			IDesign design = (IDesign) BridgeCAFUtils.getDesign();
			boolean bridgeAllowed = BridgesIntegrationServices.bridgesAvailable(this);
			if (bridgeAllowed) {
				IReleaseLevel relLevel = design.getReleaseLevel();
				if (relLevel != null) {
					enabled = relLevel.isBridgeAllowed();
				}
			}
		}
		return enabled;
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
		}
		return false; // no need to save
	}

	protected boolean isComponentDeletionOnImportAllowed()
	{
		return false;
	}
}
