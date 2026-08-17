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
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.BorderValue;
import chs.utilities.ui.property.ChoiceTypeValue;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IActionProperty;
import chs.utilities.ui.property.IListProperty;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyAttributes;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.IPropertyValidator;
import chs.utilities.ui.property.IPropertyValidityListener;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.OrientationValue;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyPanel;
import chs.utilities.ui.property.PropertyResourceBuilder;
import chs.utilities.ui.property.ValidityChangeEvent;
import chs.utilities.ui.property.reuse.PropertyButtonGroup;

import javax.swing.JDialog;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class EditDDTTypesDialog extends CAFOkCancelDialog
{

	private DDTTypeTransient m_model;
	private boolean m_validated;
	private IStringProperty m_nameProp;

	/**
	 * Keep track of the existing type names so not to duplicate the name.
	 */
	private Set m_existingTypeNames;
	private IStringProperty m_fieldNames;
	private IStringProperty m_pinFieldNames;

	public EditDDTTypesDialog(JDialog owner, DDTTypeTransient model, Set existingTypeNames)
	{
		super(owner, ResourceMgr.getString(EditDDTTypesDialog.class, "EditDDTTypesDialog.title"), true);
		m_model = model;
		m_existingTypeNames = existingTypeNames;
		getContentPane().add(new PropertyPanel("Dialog", buildEditGroup()), BorderLayout.CENTER);
		getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String newName = (String) m_nameProp.getValue();
				m_model.setName(newName);
				Collection newFields = m_fieldNames.getValuesList();
				m_model.replaceFields(newFields);
				Collection newPinFields = m_pinFieldNames.getValuesList();
				m_model.replacePinFields(newPinFields);
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

	public boolean wasValidated()
	{
		return m_validated;
	}

	private IPropertyGroup buildEditGroup()
	{
		IPropertyGroup root = PropertyFactory.createPropertyGroup("Root");
		root.setGroupType(GroupTypeValue.COLUMN);
		root.setVerticalFill(true);
		root.setBorder(BorderValue.NONE);

		// Creates a labeled column group containing the name property
		IPropertyGroup nameGrp = root.createPropertyGroup("NameGrp");
		m_nameProp = nameGrp.createStringProperty("Name");
		m_nameProp.setDefaultValue(m_model.getName());
		m_nameProp.setValue(m_model.getName());
		String nameFieldString = ResourceMgr.getString(EditDDTTypesDialog.class, "EditDDTTypesDialog.DDTNameFieldText");
		m_nameProp.setLabel(nameFieldString);
		m_nameProp.addValidator(new NameValidator());
		//m_nameProp.setFill(OrientationValue.HORIZONTAL);
		nameGrp.setFill(OrientationValue.HORIZONTAL);

		IPropertyGroup topLevelDevGrp = root.createPropertyGroup("TopLevelDevGrp");
		topLevelDevGrp.setGroupType(GroupTypeValue.COLUMN);
		String devFieldString = ResourceMgr.getString(EditDDTTypesDialog.class, "EditDDTTypesDialog.DeviceFieldText");
		topLevelDevGrp.setLabel(devFieldString);
		FieldButtonPair fbPair = createAddGroup(topLevelDevGrp);
		IPropertyGroup devGrp = topLevelDevGrp.createPropertyGroup("DevGrp");
		devGrp.setGroupType(GroupTypeValue.ROW);
		devGrp.setFill(OrientationValue.BOTH);

		m_fieldNames = devGrp.createStringProperty("FieldNames");
		final IListProperty typesListProp = root.createListProperty("DeviceFieldsList",
				new LinkedList(m_model.getFields()));
		m_fieldNames.setValuesList(typesListProp);
		m_fieldNames.setChoiceType(ChoiceTypeValue.LIST);
		m_fieldNames.setFill(OrientationValue.BOTH);
		createButtonGroup(devGrp, m_fieldNames);
		hookFieldAndList(fbPair, m_fieldNames);

		IPropertyGroup topLevelPinGrp = root.createPropertyGroup("TopLevelPinGrp");
		topLevelPinGrp.setGroupType(GroupTypeValue.COLUMN);
		String pinFieldString = ResourceMgr.getString(EditDDTTypesDialog.class, "EditDDTTypesDialog.PinFieldText");
		topLevelPinGrp.setLabel(pinFieldString);
		fbPair = createAddGroup(topLevelPinGrp);
		IPropertyGroup pinGrp = topLevelPinGrp.createPropertyGroup("PinGrp");
		pinGrp.setGroupType(GroupTypeValue.ROW);
		pinGrp.setFill(OrientationValue.BOTH);
		m_pinFieldNames = pinGrp.createStringProperty("FieldNames");
		final IListProperty pinFieldsListProp = root.createListProperty("PinFieldsList",
				new LinkedList(m_model.getPinFields()));
		m_pinFieldNames.setValuesList(pinFieldsListProp);
		m_pinFieldNames.setChoiceType(ChoiceTypeValue.LIST);
		m_pinFieldNames.setAttribute(IPropertyAttributes.PREFERRED_SIZE, new Dimension(300, 200));
		m_pinFieldNames.setFill(OrientationValue.BOTH);
		createButtonGroup(pinGrp, m_pinFieldNames);
		hookFieldAndList(fbPair, m_pinFieldNames);

		return root;
	}

	private void createButtonGroup(IPropertyGroup grp, final IStringProperty modList)
	{
		PropertyButtonGroup pinBtnGroup = new PropertyButtonGroup("DevBtnGrp", modList);
		pinBtnGroup.setUseMnemonics(false);
		grp.addProperty(pinBtnGroup);
		pinBtnGroup.setFill(OrientationValue.NONE);
		pinBtnGroup.createUpButton();
		pinBtnGroup.createDownButton();
		pinBtnGroup.createRemoveButton(PropertyButtonGroup.BUTTONMODE_PLAIN);
		PropertyResourceBuilder prb =
				new PropertyResourceBuilder(PropertyButtonGroup.class);

		prb.loadResources(pinBtnGroup);
	}

	private FieldButtonPair createAddGroup(IPropertyGroup grp)
	{
		IPropertyGroup addGroup = grp.createPropertyGroup("AddGrp");
		addGroup.setGroupType(GroupTypeValue.ROW);
		addGroup.setHorizontalFill(true);
		addGroup.setBorder(BorderValue.SIMPLE);

		FieldButtonPair fbPair = new FieldButtonPair();

		fbPair.fieldProp = addGroup.createStringProperty("AddField");
		fbPair.fieldProp.setLabel("Field");
		fbPair.fieldProp.setHorizontalFill(true);

		fbPair.addProp = addGroup.createActionProperty("Add");
		String addStr = ResourceMgr.getString(EditDDTTypesDialog.class, "EditDDTTypesDialog.addButtonText");
		//fbPair.addProp.setMnemonic(ResourceMgr.getMnemonic(EditDDTTypesDialog.class,"EditDDTTypesDialog.addButtonMnemoic"));
		fbPair.addProp.setLabel(addStr);

		return fbPair;
	}

	/**
	 * Hooks up the 'add button', it's field and the list to be added to
	 *
	 * @param fbPair The field and the attached button
	 * @param list List to be added to
	 */
	private void hookFieldAndList(final FieldButtonPair fbPair,
			final IStringProperty list)
	{
		fbPair.addProp.setActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String newField = fbPair.fieldProp.getValue();

				List valList = list.getValuesList();
				valList.add(newField);
				list.setValuesList(valList);

				// Bit of a hack.. Perhaps.. But does the job.  Causes the field to be "revalidated", thus not
				// allowing dups
				fbPair.fieldProp.setValue(fbPair.fieldProp.getValue());
			}
		});
		fbPair.addProp.setEnabled(false);

		fbPair.fieldProp.addValidator(new FieldValidator(list));

		fbPair.fieldProp.addValidityListener(new IPropertyValidityListener()
		{
			public void validityChanged(ValidityChangeEvent evt)
			{
				if (evt.isValid()) {
					fbPair.addProp.setEnabled(true);
				}
				else {
					fbPair.addProp.setEnabled(false);
				}
			}

			public void invalidReasonChanged(IProperty property)
			{
				// Don't care why something isn't valid
			}
		});
	}

	private class NameValidator implements IPropertyValidator
	{

		public boolean validate(IProperty property)
		{
			IStringProperty sprop = (IStringProperty) property;
			String name = sprop.getValue();
			if (m_existingTypeNames.contains(name)) {
				getOkButton().setEnabled(false);
				return false;
			}
			getOkButton().setEnabled(true);
			return true;
		}

		public String getValidityReason()
		{
			String invalidReason = ResourceMgr.getString(EditDDTTypesDialog.class, "EditDDTTypesDialog.dupNameError");
			return invalidReason;
		}
	}

	private class FieldValidator implements IPropertyValidator
	{

		private IStringProperty m_existingFields; // For duplicate checking
		private String m_invalidReason = "";

		public FieldValidator(IStringProperty list)
		{
			m_existingFields = list;
		}

		public boolean validate(IProperty property)
		{
			IStringProperty sprop = (IStringProperty) property;
			String name = sprop.getValue();

			if ((name == null) || (name.trim().equals(""))) {
				m_invalidReason = ResourceMgr.getString(EditDDTTypesDialog.class, "EditDDTTypesDialog.emptyName");
				return false;
			}

			List list = m_existingFields.getValuesList();

			if (list.contains(name.trim())) {
				m_invalidReason = ResourceMgr.getString(EditDDTTypesDialog.class, "EditDDTTypesDialog.fieldExists");
				return false;
			}

			return true;
		}

		public String getValidityReason()
		{
			return m_invalidReason;
		}
	}

	private class FieldButtonPair
	{

		public IStringProperty fieldProp;
		public IActionProperty addProp;
	}
}
