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

import chs.caplets.capture.actions.ddt.transmodel.DDTTypeTransient;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.utilities.AlphaNumComparator;
import chs.utilities.ResourceMgr;
import chs.utilities.SortedList;
import chs.utilities.ui.property.ChoiceTypeValue;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IListProperty;
import chs.utilities.ui.property.IObjectProperty;
import chs.utilities.ui.property.IPropertyAttributes;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyPanel;
import chs.utilities.ui.property.PropertyResourceBuilder;
import chs.utilities.ui.property.reuse.PropertyButtonGroup;

import javax.swing.JDialog;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Creation information Date: Jul 22, 2005 Time: 11:05:15 AM Description:
 * <p/>
 * This dialog is used to add, edit, and change DDTType objects that are contained within the DDTTypeMgr.
 */
public class EditDDTTypeMgrDialog extends CAFOkCancelDialog
{

	private EditDDTTypesDialogModel m_model;
	private JDialog m_selDiag = this; // For access via inner classes
	private boolean m_validated;
	private IObjectProperty m_typesObjProp; // UI for the list of types

	public EditDDTTypeMgrDialog(Frame frame, EditDDTTypesDialogModel model)
	{
		super(frame, ResourceMgr.getString(EditDDTTypesDialog.class, "EditDDTTypeMgrDialog.title"), true);
		m_model = model;
		getContentPane().add(new PropertyPanel("Dialog", buildTypeSelector()), BorderLayout.CENTER);
		getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_validated = true;
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

	public void reset()
	{
		List vals = m_typesObjProp.getValuesList();
		m_typesObjProp.setValue(null);
		Collection modelTypeNames = m_model.getTypeNames();
		vals.clear();
		vals.addAll(modelTypeNames);
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

		m_typesObjProp = root.createObjectProperty("TypesList");
		final IListProperty typesListProp = root.createListProperty("TypesList",
				new SortedList(m_model.getTypeNames(), new AlphaNumComparator()));

		m_typesObjProp.setValuesList(typesListProp);
		m_typesObjProp.setLabel("Types");
		m_typesObjProp.setChoiceType(ChoiceTypeValue.LIST);
		m_typesObjProp.setAttribute(IPropertyAttributes.PREFERRED_SIZE, new Dimension(300, 500));

		m_typesObjProp.setHorizontalFill(true);
		m_typesObjProp.setVerticalFill(true);

		PropertyButtonGroup btnGrp = new PropertyButtonGroup("ButtonGroup", m_typesObjProp);
		root.addProperty(btnGrp);
		btnGrp.createAddButton(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				Set existingNames = new LinkedHashSet(m_model.getTypeNames());
				existingNames.add(DDTTypeTransient.DEFAULT_TYPE_NAME);
				DDTTypeTransient selectedType = new DDTTypeTransient();
				EditDDTTypesDialog diag = new EditDDTTypesDialog(m_selDiag, selectedType, existingNames);
				diag.pack();
				diag.setVisible(true);
				if (diag.wasValidated()) {
					m_model.addNewType(selectedType);
					List vals = m_typesObjProp.getValuesList();
					vals.add(selectedType.getName());
				}
			}
		}, null, PropertyButtonGroup.BUTTONMODE_FORDIALOG);

		btnGrp.createEditButton(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String selectedTypeName = (String) m_typesObjProp.getValue();
				Set existingNames = new LinkedHashSet(m_model.getTypeNames());
				existingNames.add(DDTTypeTransient.DEFAULT_TYPE_NAME);
				existingNames.remove(selectedTypeName);

				List curVals = m_typesObjProp.getValuesList();
				DDTTypeTransient selectedType = m_model.getType(selectedTypeName);
				if (selectedType != null) {
					String oldName = selectedType.getName();
					EditDDTTypesDialog diag = new EditDDTTypesDialog(m_selDiag, selectedType, existingNames);
					diag.pack();
					diag.setVisible(true);
					curVals.remove(oldName);
					curVals.add(selectedType.getName());
				}
			}
		}, m_typesObjProp, PropertyButtonGroup.BUTTONMODE_FORDIALOG);

		btnGrp.createRemoveButton(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String selectedTypeName = (String) m_typesObjProp.getValue();
				m_model.removeTypeByName(selectedTypeName);
				List vals = m_typesObjProp.getValuesList();
				vals.remove(selectedTypeName);
			}
		}, m_typesObjProp, PropertyButtonGroup.BUTTONMODE_PLAIN);

		PropertyResourceBuilder prb =
				new PropertyResourceBuilder(PropertyButtonGroup.class);

//		prb.buildResource(pinBtnGroup);
		prb.loadResources(btnGrp);

		return root;
	}
}
