/*
 * Copyright 2006-2008 Mentor Graphics Corporation
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
import chs.caf.caplet.helpers.graphics.AbstractTableAction;
import chs.cof.drawplus.table.ITableData;
import chs.cof.logical.IDesign;
import chs.cof.logical.schem.ILogicDiagramTableData;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import chs.common.table.ITableNames;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;

import java.awt.event.ActionEvent;

public class AddDiagramListTableAction extends AbstractTableAction
{

	public AddDiagramListTableAction(ICapletController controller)
	{
		super(controller);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		return super.onActivate(e);
	}

	protected boolean onTerminate(boolean successful)
	{
		return super.onTerminate(successful);
	}

	public String getActionUIClass()
	{
		return AddDiagramListTableActionUI.class.getName();
	}

	/**
	 * Set the status text for this action
	 */
	public String getStatusbarText()
	{
		return ResourceMgr.getString(this, "AddDiagramListTableAction.statusbar.text");
	}

	private ILogicDiagramTableData createTableData()
	{
		IDesign design = getModel().getDesign();

		IUID uid = FactoryMgr.getCommonFactory().createUID();
		ILogicDiagramTableData tableData = FactoryMgr.getLogicalFactory().createLogicDiagramTableData(uid, design, getModel().getDiagram());
		return tableData;
	}

	protected ITableData getTableData()
	{
		ISchemDiagram diagram = getModel().getDiagram();
		ILogicDiagramTableData tableData = (ILogicDiagramTableData) diagram.getTableData(ILogicDiagramTableData.class);

		if (tableData == null) {
			tableData = createTableData();
			diagram.addTableData(tableData);
		}

		return tableData;
	}

	protected String getTableName()
	{
		return ITableNames.LOGIC_DIAGRAM_TABLE;
	}
}
