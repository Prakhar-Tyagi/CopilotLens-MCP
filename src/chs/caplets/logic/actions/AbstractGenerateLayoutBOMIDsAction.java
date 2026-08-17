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
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ChangedObjectsHolder;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.IChangedObjectsInfo;
import chs.cof.drawplus.table.ITableData;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ILayoutBOMTableData;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.CommonUtils;
import chs.utility.harness.HarnessProcessingUtils;
import chs.utility.logic.ILogicModel;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.Collections;

/**
 * @author chandras on 02-02-2020.
 */

public abstract class AbstractGenerateLayoutBOMIDsAction extends ControllerActionRT
{

	protected AbstractGenerateLayoutBOMIDsAction(ICapletController controller)
	{
		super(controller);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		/**
		 * If a Composite Table already exists, dont allow another one
		 */
		return IActionEnum.eCompleted;
	}

	@Override public boolean isEnabled()
	{
		ILayoutLogicDesign layoutLogicDesign = CommonUtils.cast(getDesign(), ILayoutLogicDesign.class);
		return layoutLogicDesign != null && super.isEnabled();
	}

	protected void generateBOMidValues(ILayoutLogicDesign layoutDesign, boolean removeExisting)
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		HarnessProcessingUtils.generateBOMidValues(layoutDesign, removeExisting);
		postBOMIdsUpdate();
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	protected void postBOMIdsUpdate()
	{
		for (ISchemDiagram schemDiagram : getDesign().getDiagrams()) {
			if (schemDiagram.isFullyLoaded()) {
				ITableData layoutBOMTableData = schemDiagram.getTableData(ILayoutBOMTableData.class);
				if (layoutBOMTableData != null) {
					layoutBOMTableData.fireTableDataChanged();
				}
				IChangedObjectsInfo changedObjectsInfo = new ChangedObjectsHolder(Collections.emptyList(),
						Collections.emptyList(), Collections.emptyList());
				schemDiagram.updateAffectedDecorations(changedObjectsInfo);
			}
		}
	}

	protected ILogicDesign getDesign()
	{
		return ((ILogicModel) getController().getCapletModel()).getDesign();
	}
}