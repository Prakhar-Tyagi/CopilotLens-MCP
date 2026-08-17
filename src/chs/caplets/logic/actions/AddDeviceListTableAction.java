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
import chs.cof.logical.schem.ILogicDeviceTableData;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import chs.common.table.ITableNames;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;

import java.awt.event.ActionEvent;

public class AddDeviceListTableAction extends AbstractTableAction
{

	public AddDeviceListTableAction(ICapletController controller)
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
		return AddDeviceListTableActionUI.class.getName();
	}

	/**
	 * Set the status text for this action
	 */
	public String getStatusbarText()
	{
		return ResourceMgr.getString(this, "AddDeviceListTableAction.statusbar.text");
	}

	private ILogicDeviceTableData createTableData()
	{
		ISchemDiagram diagram = getModel().getDiagram();

		IUID uid = FactoryMgr.getCommonFactory().createUID();
		ILogicDeviceTableData deviceTableData =
				FactoryMgr.getLogicalFactory().createLogicDeviceTableData(uid, diagram.getDesign(), diagram);
		return deviceTableData;
	}

	protected ITableData getTableData()
	{
		ISchemDiagram diagram = getModel().getDiagram();
		ILogicDeviceTableData tableData = (ILogicDeviceTableData) diagram.getTableData(ILogicDeviceTableData.class);

		if (tableData == null) {
			tableData = createTableData();
			diagram.addTableData(tableData);
		}

		return tableData;
	}

	protected String getTableName()
	{
		return ITableNames.LOGIC_DEVICES_TABLE;
	}
}
