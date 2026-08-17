/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.ILayoutLogicDesign;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;

/**
 * @author chandras on 02-02-2020.
 */
public class UpdateLayoutBOMIDsAction extends AbstractGenerateLayoutBOMIDsAction
{

	public UpdateLayoutBOMIDsAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return UpdateLayoutBOMIDsActionUI.class.getName();
	}

	/**
	 * Set the status text for this action
	 */
	public String getStatusbarText()
	{
		return ResourceMgr.getString(this, "UpdateLayoutBOMIDsAction.statusbar.text");
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean status = false;
		ILayoutLogicDesign layoutDesign = CommonUtils.cast(getDesign(), ILayoutLogicDesign.class);
		if (successful && layoutDesign != null) {
			generateBOMidValues(layoutDesign, false);
			status = true;
		}
		return status;
	}
}