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

import chs.cof.logical.cable.IDevice;
import chs.cof.project.ddtrans.IDDTType;
import chs.common.IProperty;
import chs.utilities.ResourceMgr;

import javax.swing.table.AbstractTableModel;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Model that represents the field/values for the ddt for a given device. This transient model is initialized from the
 * device's properties and, likewise, has its data "applied" to the persistent device when the "ok" button is pressed.
 */
public class DeviceFieldModel extends AbstractTableModel
{

	private Map m_fieldValues = new LinkedHashMap();
	private IDDTType m_ddtType;

	public DeviceFieldModel(IDevice dev, IDDTType ddtType)
	{
		m_ddtType = ddtType;
		List fields = ddtType.getAllFields();
		for (Iterator itr = fields.iterator(); itr.hasNext();) {
			String field = (String) itr.next();

			IProperty prop = dev.findPropertyByName(field);
			if (prop != null) {
				m_fieldValues.put(field, new String(prop.getString()));
			}
			else {
				m_fieldValues.put(field, new String(""));
			}
		}
	}

	public String getColumnName(int column)
	{
		if (column == 0) {
			return ResourceMgr.getString(DeviceFieldModel.class, "DeviceFieldModel.fieldNameText");
		}
		else {
			return ResourceMgr.getString(DeviceFieldModel.class, "DeviceFieldModel.fieldValueText");
		}
	}

	public int getColumnCount()
	{
		return 2;
	}

	public int getRowCount()
	{
		return m_fieldValues.entrySet().size();
	}

	public boolean isCellEditable(int row, int column)
	{
		if (column == 0) {
			return false;
		}
		return true;
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		List fieldList = m_ddtType.getAllFields();
		String field = (String) fieldList.get(rowIndex);
		m_fieldValues.put(field, aValue);
	}

	public Object getValueAt(int rowIndex, int columnIndex)
	{
		List fieldList = m_ddtType.getAllFields();
		String field = (String) fieldList.get(rowIndex);
		String fieldValue = (String) m_fieldValues.get(field);
		if (columnIndex == 0) {
			return field;
		}
		else {
			return fieldValue;
		}
	}

	/**
	 * Get all the field/value pairs in for the device.
	 *
	 * @return A name/value mapping of these fields
	 */
	public Map getFieldValuePairs()
	{
		return m_fieldValues;
	}
}
