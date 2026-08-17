/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.capture.actions.ddt;

import chs.cof.project.ddtrans.IDDTType;
import chs.cof.project.ddtrans.IDDTTypeMgr;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.utilities.AlphaNumComparator;
import chs.utilities.ResourceMgr;
import chs.utilities.SortedList;
import chs.utilities.ui.property.ChoiceTypeValue;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IListProperty;
import chs.utilities.ui.property.IObjectProperty;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyPanel;

import javax.swing.Box;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Creation information Date: Jul 22, 2005 Time: 11:05:15 AM Description:
 * <p/>
 * This dialog is used to select a ddt type
 */
public class SelectDDTTypeDialog extends CAFOkCancelDialog
{

	private IDDTTypeMgr m_model;  // This is read only. (Dialog's only for selection)
	private boolean m_validated;
	private IListProperty m_typesListProp;
	private IObjectProperty m_typesObjProp;

	public SelectDDTTypeDialog(Frame frame, IDDTTypeMgr model)
	{
		super(frame, ResourceMgr.getString(EditDDTTypesDialog.class, "SelectDDTTypesDialog.title"), true);
		m_model = model;
		getContentPane().add(new PropertyPanel("Dialog", buildTypeSelector()), BorderLayout.CENTER);
		// Ensure mimimum size for title
		getContentPane().add(Box.createHorizontalStrut((getTitle().length() + 4) * 7), BorderLayout.NORTH);

		getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_validated = getSelectedType() != null;
				setVisible(false);
			}
		});

		getCancelButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_validated = false;
				setVisible(false);
			}
		});
	}

	public IDDTType getSelectedType()
	{
		return (IDDTType) m_typesObjProp.getValue();
	}

	public boolean wasValidated()
	{
		return m_validated;
	}

	private IPropertyGroup buildTypeSelector()
	{
		IPropertyGroup root = PropertyFactory.createPropertyGroup("Root");
		root.setGroupType(GroupTypeValue.ROW);
		root.setVerticalFill(true);

		m_typesObjProp = root.createObjectProperty("TypesObj");
		List typesList = new SortedList(m_model.getDDTTypes(), new AlphaNumComparator());
		m_typesListProp = root.createListProperty("TypesList", typesList);
		m_typesObjProp.setValuesList(m_typesListProp);
		m_typesObjProp.addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				getOkButton().setEnabled(m_typesObjProp.getValue() != null);
			}
		});
		if (!typesList.isEmpty()) {
			m_typesObjProp.setValue(m_typesListProp.get(0));
		}
		m_typesObjProp.setLabel("Types");
		m_typesObjProp.setChoiceType(ChoiceTypeValue.LIST);

		m_typesObjProp.setHorizontalFill(true);
		m_typesObjProp.setVerticalFill(true);

		getOkButton().setEnabled(m_typesObjProp.getValue() != null);
		return root;
	}
}
