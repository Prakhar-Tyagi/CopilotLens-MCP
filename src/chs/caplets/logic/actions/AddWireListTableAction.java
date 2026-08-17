package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.graphics.AbstractTableAction;
import chs.cof.drawplus.table.ITableData;
import chs.cof.logical.schem.ILogicWireTableData;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import chs.common.table.ITableNames;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;

import java.awt.event.ActionEvent;

public class AddWireListTableAction extends AbstractTableAction
{

	public AddWireListTableAction(ICapletController controller)
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
		return ResourceMgr.getString(this, "AddWireListTableAction.statusbar.text");
	}

	private ILogicWireTableData createTableData()
	{
		ISchemDiagram diagram = getModel().getDiagram();

		IUID uid = FactoryMgr.getCommonFactory().createUID();
		ILogicWireTableData wireTableData =
				FactoryMgr.getLogicalFactory().createLogicWireTableData(uid, getModel().getDesign(), diagram);
		return wireTableData;
	}

	protected ITableData getTableData()
	{
		ISchemDiagram diagram = getModel().getDiagram();
		ILogicWireTableData tableData = (ILogicWireTableData) diagram.getTableData(ILogicWireTableData.class);

		if (tableData == null) {
			tableData = createTableData();
			diagram.addTableData(tableData);
		}

		return tableData;
	}

	protected String getTableName()
	{
		return ITableNames.LOGIC_WIRE_TABLE;
	}
}
