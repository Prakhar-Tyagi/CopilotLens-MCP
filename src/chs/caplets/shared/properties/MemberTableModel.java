/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.shared.properties;

import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.IInterconnectMember;
import chs.cof.logical.cable.IInterconnectMemberIterator;
import chs.utilities.ResourceMgr;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class MemberTableModel extends AbstractTableModel
{

	private List m_proxies;
	private List columnNames;

	MemberTableModel(IInterconnectConductor cond)
	{
		super();
		m_proxies = new ArrayList();
		columnNames = new ArrayList();
		columnNames.add(ResourceMgr.getString(InterconnectMemberControl.class,
				"InterconnectMemberControl.Table.PartNum.Text"));
		columnNames.add(ResourceMgr.getString(InterconnectMemberControl.class,
				"InterconnectMemberControl.Table.Type.Text"));
		columnNames.add(ResourceMgr.getString(InterconnectMemberControl.class,
				"InterconnectMemberControl.Table.Count.Text"));
		//
		Set squishes = new HashSet();
		for (IInterconnectMemberIterator imitr = cond.getMembers(); imitr.hasNext();) {
			IInterconnectMember im = imitr.getNext();
			MemberProxy mp = MemberProxy.create(im);
			squishes.add(mp);
		}
		for (Iterator itr = squishes.iterator(); itr.hasNext();) {
			addMemberProxy((MemberProxy) itr.next());
		}
	}

	public void addMemberProxy(MemberProxy im)
	{
		m_proxies.add(im);
		fireTableDataChanged();
	}

	public void removeMemberProxy(int row)
	{
		MemberProxy mp = (MemberProxy) m_proxies.remove(row);
		MemberProxy.remove(mp);
		fireTableDataChanged();
	}

	public List getProxies()
	{
		return m_proxies;
	}

	public int getColumnCount()
	{
		return 3;
	}

	public int getRowCount()
	{
		return m_proxies.size();
	}

	public Object getValueAt(int rowIndex, int columnIndex)
	{
		MemberProxy mp = (MemberProxy) m_proxies.get(rowIndex);
		switch (columnIndex) {
			case 0:
				return mp.getPartNumber();
			case 1:
				return InterconnectMemberControl.typeMap.get("" + mp.getPartClass());
			case 2:
				return new Integer(mp.getRealObjects().size());
		}
		return null;
	}

	public String getColumnName(int column)
	{
		return (String) columnNames.get(column);
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		if (columnIndex != 2) {
			return;
		}
		try {
			int ival = Integer.parseInt((String) aValue);
			if (ival < 0) {
				return;
			}
			if (ival == 0) {
				removeMemberProxy(rowIndex);
			}
			else {
				MemberProxy mp = (MemberProxy) m_proxies.get(rowIndex);
				mp.resizeTo(ival);
			}
		}
		catch (NumberFormatException nfe) {
		}
	}
}
