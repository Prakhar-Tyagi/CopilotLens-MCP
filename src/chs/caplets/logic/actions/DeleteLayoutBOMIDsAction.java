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
import chs.common.IUIDObject;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utility.harness.HarnessProcessingUtils;

import java.awt.Cursor;
import java.util.Set;

/**
 * @author chandras on 02-02-2020.
 */
public class DeleteLayoutBOMIDsAction extends AbstractGenerateLayoutBOMIDsAction
{

	public DeleteLayoutBOMIDsAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return DeleteLayoutBOMIDsActionUI.class.getName();
	}

	/**
	 * Set the status text for this action
	 */
	public String getStatusbarText()
	{
		return ResourceMgr.getString(this, "DeleteLayoutBOMIDsAction.statusbar.text");
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean status = false;
		ILayoutLogicDesign layoutDesign = CommonUtils.cast(getDesign(), ILayoutLogicDesign.class);
		if (successful && layoutDesign != null) {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			// Get all the BOM IDable objects in the design.
			Set<IUIDObject> designObjects = HarnessProcessingUtils.getAllBOMidCapableDesignObjects(layoutDesign);
			HarnessProcessingUtils.removeBOMidValues(layoutDesign, designObjects);
			postBOMIdsUpdate();
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			status = true;
		}
		return status;
	}
}