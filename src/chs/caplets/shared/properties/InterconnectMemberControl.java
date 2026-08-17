/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.properties;

import chs.caf.CAFUtils;
import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.IPropertiesClientComponent;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.IInterconnectMember;
import chs.cof.logical.cable.IInterconnectMemberIterator;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.parts.ILibraryMulticore;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibraryWire;
import chs.cof.parts.Library;
import chs.cof.parts.LibraryCriteriaHelper;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.parts.partselector.ILibraryPartSelector;
import chs.utilities.ui.BasicUIFactory;
import chs.common.criteria.ICriteria;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.StripingTableCellRenderer;
import chs.utility.ui.IValidityListener;
import chs.utility.ui.table.TableSorterModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterconnectMemberControl implements IPropertiesClientComponent
{

	protected final static String ICXMEMBER_TAB_LABEL =
			ResourceMgr.getString(InterconnectMemberControl.class, "InterconnectMemberControl.Tab.Label");
	static Map typeMap = new HashMap();

	protected boolean m_modified = false;
	protected MemberTableModel m_tableModel;
	protected TableSorterModel m_sortedTableModel;
	protected JTable m_table = null;

	static {
		typeMap.put(Integer.toString(IInterconnectMember.TYPE_MULTICORE), ResourceMgr.getString(
				InterconnectMemberControl.class, "InterconnectMemberControl.Type.Multicore.Single"));
		typeMap.put(Integer.toString(IInterconnectMember.TYPE_OVERBRAID), ResourceMgr.getString(
				InterconnectMemberControl.class, "InterconnectMemberControl.Type.Overbraid.Single"));
		typeMap.put(Integer.toString(IInterconnectMember.TYPE_WIRE),
				ResourceMgr.getString(InterconnectMemberControl.class, "InterconnectMemberControl.Type.Wire.Single"));
	}

	public JPanel getWidget(IPropertiedSet propset)
	{
		MemberProxy.reset();
		if (CAFUtils.getInstance().getData() == null) {
			//
			// No DB Connection - no library...
			//
			JPanel jp = new JPanel();
			JLabel lbl = new JLabel("Library access is not available - disabled"); // N XLate - Dev only
			jp.add(lbl);
			//
			m_sortedTableModel = null;
			return jp;
		}
		m_modified = false;

		JPanel jp = new JPanel();
		jp.setBorder(BorderFactory.createTitledBorder(
				ResourceMgr.getString(InterconnectMemberControl.class, "InterconnectMemberControl.Panel.Title")));

		final IInterconnectConductor cond = getOperand(propset);
		m_tableModel = new MemberTableModel(cond);
		m_sortedTableModel = new TableSorterModel(m_tableModel);
		m_table = new JTable(m_sortedTableModel)
		{
			public boolean isCellEditable(int row, int column)
			{
				return column == 2;
			}
		};
		m_sortedTableModel.setupTableHeader(m_table);
		m_table.getTableHeader().setReorderingAllowed(false);

		if (m_table.getRowCount() > 0) {
			m_table.getSelectionModel().setSelectionInterval(0, 0);
		}
		TableCellRenderer renderer = new StripingTableCellRenderer();
		TableColumnModel tcm = m_table.getColumnModel();
		tcm.getColumn(0).setCellRenderer(renderer);
		tcm.getColumn(1).setCellRenderer(renderer);
		m_table.setName("IcxMemberTable");
		jp.setLayout(new BorderLayout());
		jp.add(new JScrollPane(m_table), BorderLayout.CENTER);

		JPanel controls = new JPanel();

		JPanel addSection = new JPanel();
		addSection.setLayout(new BorderLayout());
		addSection.setBorder(BorderFactory.createEtchedBorder());
		JButton addButton = BasicUIFactory.getInstance().createSiemensCustomJButton(
				ResourceMgr.getString(InterconnectMemberControl.class, "InterconnectMemberControl.Button.Add.Text"));
		addButton.setMnemonic(ResourceMgr.getMnemonic(InterconnectMemberControl.class,
				"InterconnectMemberControl.Button.Add.Mnemonic"));
		addSection.add(addButton, BorderLayout.WEST);
		final JComboBox addType = new JComboBox(new Object[]
				{ResourceMgr.getString(InterconnectMemberControl.class,
						"InterconnectMemberControl.Type.Multicore.Plural"),
						ResourceMgr.getString(InterconnectMemberControl.class,
								"InterconnectMemberControl.Type.Overbraid.Plural"),
						ResourceMgr.getString(InterconnectMemberControl.class,
								"InterconnectMemberControl.Type.Wire.Plural")}
		);
		addType.setName("ICXMemberType");
		addType.setSelectedIndex(2); // wires
		addSection.add(addType, BorderLayout.CENTER);
		controls.add(addSection);
		final JButton removeButton = BasicUIFactory.getInstance().createSiemensCustomJButton(
				ResourceMgr.getString(InterconnectMemberControl.class, "InterconnectMemberControl.Button.Remove.Text"));
		removeButton.setMnemonic(ResourceMgr.getMnemonic(InterconnectMemberControl.class,
				"InterconnectMemberControl.Button.Remove.Mnemonic"));
		controls.add(removeButton);
		removeButton.setEnabled(m_table.getSelectedRowCount() != 0);
		m_table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				removeButton.setEnabled(m_table.getRowCount() > 0 && m_table.getSelectedRowCount() != 0);
			}
		});
		m_table.getModel().addTableModelListener(new TableModelListener()
		{
			public void tableChanged(TableModelEvent e)
			{
				removeButton.setEnabled(m_table.getRowCount() > 0 && m_table.getSelectedRowCount() != 0);
			}
		});

		removeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int firstRow = 0;
				int[] rows = m_table.getSelectedRows();
				for (int i = rows.length - 1; i >= 0; i--) {
					firstRow = Math.min(firstRow, rows[i]);
					int srcRow = m_sortedTableModel.getModelRow(rows[i]);
					m_tableModel.removeMemberProxy(srcRow);
					m_tableModel.fireTableRowsDeleted(rows[i], rows[i]);
				}
				if (m_tableModel.getRowCount() > 0) {
					m_table.getSelectionModel().setSelectionInterval(firstRow, firstRow);
				}
				m_modified = true;
			}
		});

		addButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int type = addType.getSelectedIndex();
				int memberType = IInterconnectMember.TYPE_WIRE;
				List<ILibraryObject> selectedObjects = null;
				if (type == 0) {
					memberType = IInterconnectMember.TYPE_MULTICORE;
					ICriteria<ILibraryMulticore> criteria =
							LibraryCriteriaHelper.createCriteria(ILibraryMulticore.class);
					ILibraryPartSelector partSelector = Library.getInstance()
							.getLibraryPartSelector(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
					//@todo used library configuration context directly, needs confirmation - kjuthi
					ILibraryPartSelection m_libSelection =
							partSelector.selectPart(criteria, CAFUtils.getInstance().getCurrentProject(),
									null, ConfigurationTypeEnum.LOGICAL, CAFUtils.getInstance().getActiveDesignContainer());

					if (m_libSelection != null) {
						selectedObjects = new ArrayList<ILibraryObject>(m_libSelection.getSelectedObjects());
					}
				}
				else if (type == 1) {
					memberType = IInterconnectMember.TYPE_OVERBRAID;
					ICriteria<ILibraryWire> criteria = LibraryCriteriaHelper.createCriteria(ILibraryWire.class);
					ILibraryPartSelector partSelector = Library.getInstance()
							.getLibraryPartSelector(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
					//@todo used library configuration context directly, needs confirmation - kjuthi
					ILibraryPartSelection m_libSelection =
							partSelector.selectPart(criteria, CAFUtils.getInstance().getCurrentProject(),
									null, ConfigurationTypeEnum.LOGICAL,CAFUtils.getInstance().getActiveDesignContainer());

					if (m_libSelection != null) {
						selectedObjects = new ArrayList<ILibraryObject>(m_libSelection.getSelectedObjects());
					}
				}
				else if (type == 2) {
					memberType = IInterconnectMember.TYPE_WIRE;
					ICriteria<ILibraryWire> criteria = LibraryCriteriaHelper.createCriteria(ILibraryWire.class);
					ILibraryPartSelector partSelector = Library.getInstance()
							.getLibraryPartSelector(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
					//@todo used library configuration context directly, needs confirmation - kjuthi
					ILibraryPartSelection m_libSelection =
							partSelector.selectPart(criteria, CAFUtils.getInstance().getCurrentProject(),
									null, ConfigurationTypeEnum.LOGICAL,CAFUtils.getInstance().getActiveDesignContainer());

					if (m_libSelection != null) {
						selectedObjects = new ArrayList<ILibraryObject>(m_libSelection.getSelectedObjects());
					}
				}
				else {
					assert false;
				}
				//
				// Now we create the members.
				//
				if (selectedObjects != null) {
					for (Iterator itr = selectedObjects.iterator(); itr.hasNext();) {
						ILibraryObject lobj = (ILibraryObject) itr.next();
						String partNum = lobj.getPartNumber();
						MemberProxy mp = MemberProxy.create(lobj.getUID(), partNum, memberType);
						//
						// If there is only 1 object in the proxy, then it is new and needs to be added
						// to the model.
						//
						if (mp.getRealObjects().size() == 1) {
							m_tableModel.addMemberProxy(mp);
						}
					}
					m_modified = true;
				}
				m_tableModel.fireTableRowsUpdated(0, m_tableModel.getRowCount());
			}
		});

		jp.add(controls, BorderLayout.SOUTH);
		return jp;
	}

	public boolean isPropPage()
	{
		return true;
	}

	public String getTabName(IPropertiedSet propset)
	{
		return ICXMEMBER_TAB_LABEL;
	}

	public boolean acceptsSet(IPropertiedSet ipropset)
	{
		return getOperand(ipropset) != null;
	}

	public boolean modifiesSet(IPropertiedSet propset)
	{
		return acceptsSet(propset) && propset.isConnectivityEditable();
	}

	public void edit(IPropertiedSet propset)
	{
		IInterconnectConductor oper = getOperand(propset);
		if (oper == null || m_sortedTableModel == null) {
			return;
		}
		//
		// Accept whatever the user has entered...
		//
		stopCurrentEdit();
		//
		// Add the intermediate objects.
		//
		List imed = m_tableModel.getProxies();
		//
		// Get a hash of the EXISTING ones. Use to trim off the pruned ones.
		//
		Set existing = new HashSet();
		for (IInterconnectMemberIterator imitr = oper.getMembers(); imitr.hasNext();) {
			IInterconnectMember im = imitr.getNext();
			existing.add(im);
		}
		for (Iterator itr = imed.iterator(); itr.hasNext();) {
			MemberProxy mp = (MemberProxy) itr.next();
			List ims = mp.getRealObjects();
			for (Iterator imitr = ims.iterator(); imitr.hasNext();) {
				IInterconnectMember robj = (IInterconnectMember) imitr.next();
				if (robj != null) {
					// Was already there
					existing.remove(robj);
				}
				else {
					// New one!
					IInterconnectMember im =
							FactoryMgr.getCableFactory().createInterconnectMember(FactoryMgr.createUID());
					im.setLibraryRef(mp.getLibraryRef());
					im.setPartClass(mp.getPartClass());
					oper.addMember(im);
				}
			}
		}
		//
		// Now clean up those that are to be deleted.
		//
		for (Iterator itr = existing.iterator(); itr.hasNext();) {
			IInterconnectMember mbr = (IInterconnectMember) itr.next();
			mbr.delete();
		}
		MemberProxy.reset();
	}

	public Set<ISharedObject> getSharedObjects()
	{
		return Collections.EMPTY_SET;
	}

	public Set<ISharedObject> getEditedSharedObjects()
	{
		return Collections.EMPTY_SET;
	}

	public void stopEditing(IPropertiedSet propset)
	{
		stopCurrentEdit();
	}

	public void destroy()
	{
	}

	public boolean isValid()
	{
		return true;
	}

	private IInterconnectConductor getOperand(IPropertiedSet propset)
	{
		if (propset.editType(chs.cof.logical.schem.IConductor.class)) {
			chs.cof.logical.schem.IConductor schemCond = (chs.cof.logical.schem.IConductor) propset.getConductorRep();
			if (schemCond != null && schemCond.getConnectivity() instanceof IInterconnectConductor) {
				return (IInterconnectConductor) schemCond.getConnectivity();
			}
		}
		return null;
	}

	private void stopCurrentEdit()
	{
		if (m_table == null) {
			return;
		}
		TableCellEditor e = m_table.getCellEditor();
		if (e != null) {
			e.stopCellEditing();
		}
	}

	public void addValidityListener(IValidityListener listener)
	{
	}

	public void removeValidityListener(IValidityListener listener)
	{
	}

	@Override public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener)
	{
	}
}

