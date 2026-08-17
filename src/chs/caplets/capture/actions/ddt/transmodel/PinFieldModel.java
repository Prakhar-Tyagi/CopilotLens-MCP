/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.capture.actions.ddt.transmodel;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IDevice;
import chs.cof.project.ddtrans.IDDTType;
import chs.common.IProperty;
import chs.utilities.ResourceMgr;

import javax.swing.table.AbstractTableModel;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Model that represnents the field/values for the ddt for a given pin. This transient model is initialized from the
 * device's properties and, likewise, has its data "applied" to the persistent device when the "ok" button is pressed.
 */
public class PinFieldModel extends AbstractTableModel
{

	/**
	 * A list of rows in the pin table.  Each row is a potential pin, name, and correponding set of field VALUES
	 */
	private List m_pinRows = new LinkedList();

	/**
	 * This is the DDT type that holds the fields
	 */
	private IDDTType m_ddtType;

	public PinFieldModel(IDevice dev, IDDTType ddtType)
	{
		m_ddtType = ddtType;
		List pinFields = ddtType.getPinFields().getAllFields();
		for (IAbstractPinIterator pinIter = dev.getPins(); pinIter.hasNext();) {
			IAbstractPin curPin = pinIter.getNext();

			Map fieldValueMap = new LinkedHashMap();
			PinTableRow ptr = new PinTableRow(curPin.getName(), fieldValueMap);
			ptr.setPin(curPin);
			m_pinRows.add(ptr);
			for (Iterator itr = pinFields.iterator(); itr.hasNext();) {
				String field = (String) itr.next();

				IProperty prop = curPin.findPropertyByName(field);
				if (prop != null) {
					fieldValueMap.put(field, new String(prop.getString()));
				}
				else {
					fieldValueMap.put(field, new String(""));
				}
			}
		}
	}

	public void addRow(String pinName)
	{
		PinTableRow ptr = new PinTableRow(pinName, new LinkedHashMap());
		m_pinRows.add(ptr);
		fireTableDataChanged();
	}

	public int getColumnCount()
	{
		List pinFields = m_ddtType.getPinFields().getAllFields();
		return pinFields.size() + 1;
	}

	public int getRowCount()
	{
		return m_pinRows.size();
	}

	public boolean isCellEditable(int row, int column)
	{
		if (column == 0) {
			return false;
		}
		return true;
	}

	/**
	 * Sets the value of the cell. Column 0 is never set, column 1 is a pin "rename" and the rest are field value
	 * modifications
	 */
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		List pinFields = m_ddtType.getPinFields().getAllFields();
		PinTableRow row = (PinTableRow) m_pinRows.get(rowIndex);
		if (columnIndex == 0) { // Set the name
			row.setName((String) aValue);
		}
		else if (columnIndex > 0) { // Set a pin's field value
			String pinField = (String) pinFields.get(columnIndex - 1);
			row.getFieldValues().put(pinField, aValue);
		}
	}

	/**
	 * Retrievs the value of a cell.  Column 0 may have an IAbstractPin object or not. If so then that means that this row
	 * represents a pin that existed on the design previously.  Column 1 is the pins name and the rest of the columns are
	 * the pin field values.
	 *
	 * @return The value of the cell. MAY BE NULL for column 0, which means that the row does not represent a persistent
	 *         pin.
	 */
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		List pinFields = m_ddtType.getPinFields().getAllFields();
		PinTableRow row = (PinTableRow) m_pinRows.get(rowIndex);
		if (columnIndex == 0) { // Get the name
			return row.getName();
		}
		else {
			String pinField = (String) pinFields.get(columnIndex - 1);
			return row.getFieldValues().get(pinField);
		}
	}

	public String getColumnName(int column)
	{
		if (column == 0) {
			return ResourceMgr.getString(DeviceFieldModel.class, "PinFieldModel.fieldNameText");
		}
		else {
			List pinFields = m_ddtType.getPinFields().getAllFields();

			return (String) pinFields.get(column - 1);
		}
	}

	public List getPinRows()
	{
		return m_pinRows;
	}

	public void deleteRows(int[] selRows)
	{
		Collection toBeRemoved = new LinkedList();
		for (int i = 0; i < selRows.length; i++) {
			PinTableRow row = (PinTableRow) m_pinRows.get(selRows[i]);
			toBeRemoved.add(row);
		}
		m_pinRows.removeAll(toBeRemoved);
		fireTableDataChanged();
	}

	/**
	 * Is this row representing a real IAbstractPin object?
	 *
	 * @param row The row in question
	 *
	 * @return True if the row represents a real IAbstractPin
	 */
	public boolean isPinRow(int row)
	{
		PinTableRow ptr = (PinTableRow) m_pinRows.get(row);
		if (ptr.getExistingPin() != null) {
			return true;
		}
		return false;
	}
}
