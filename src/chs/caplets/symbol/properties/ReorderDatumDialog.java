/*
 * Copyright 2007-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.properties;

import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.images.CHSImageLoader;
import chs.utilities.ui.property.BorderValue;
import chs.utilities.ui.property.ChoiceTypeValue;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IActionProperty;
import chs.utilities.ui.property.IPropertyAttributes;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.ListSelectionValue;
import chs.utilities.ui.property.PropertyGroup;
import chs.utilities.ui.property.PropertyPanel;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: skelkar Date: Jun 26, 2007 Time: 11:07:23 AM To change this template use File |
 * Settings | File Templates.
 */
public class ReorderDatumDialog extends CAFOkCancelDialog
{

	private ReorderListProperty m_reorderPropertyGroup;

	public ReorderDatumDialog(Frame frame, List<String> datumNames, boolean modal)
	{
		super(frame, "Reorder Datum", modal);

		m_reorderPropertyGroup = new ReorderListProperty("ReorderListProperty", datumNames);
		initPanel();
	}

	private void initPanel()
	{
		PropertyPanel panel = new PropertyPanel("Edit", m_reorderPropertyGroup);
		add(panel);
	}

	@Nullable public List<String> getReorderdDatumNames()
	{
		if (m_reorderPropertyGroup != null) {
			return m_reorderPropertyGroup.getValues();
		}
		return null;
	}

	/**
	 * A Reordering Component. This component can be used independent of implementation TODO: Move it to a Property
	 * framework component
	 */
	private static class ReorderListProperty extends PropertyGroup
	{

		private List<String> m_values = null;
		private int m_selectedIndex = 0;
		private IStringProperty m_datumListProperty;
		private IActionProperty m_moveTopAction;
		private IActionProperty m_moveUpAction;
		private IActionProperty m_moveDownAction;
		private IActionProperty m_moveBottomAction;

		ReorderListProperty(String name, List<String> values)
		{
			super(name);
			m_values = values;
			initForm();
		}

		private void initForm()
		{
			setGroupType(GroupTypeValue.ROW);
			setAttribute(IPropertyAttributes.LABELLED_GROUP, Boolean.TRUE);
			setBorder(BorderValue.NONE);
			setVerticalFill(true);
			setHorizontalFill(true);
			setAttribute(IPropertyAttributes.INHERIT_ENABLED, Boolean.TRUE);

			IPropertyGroup listGrp = createPropertyGroup("reordergroup");
			listGrp.setAttribute(IPropertyAttributes.INHERIT_ENABLED, Boolean.TRUE);

			m_datumListProperty = listGrp.createStringProperty("datumlist");
			m_datumListProperty.setValuesList(m_values);
			m_datumListProperty.setChoiceType(ChoiceTypeValue.LIST);
			m_datumListProperty.setHorizontalFill(true);
			m_datumListProperty.setVerticalFill(true);
			m_datumListProperty.setMultiLine(true);
			m_datumListProperty.setAttribute(IPropertyAttributes.LIST_SELECTION, ListSelectionValue.SINGLE);

			// Create the moveup/down button broup
			IPropertyGroup moveButtonGrp = createPropertyGroup("upDownGrp", GroupTypeValue.COLUMN);
			moveButtonGrp.setAttribute(IPropertyAttributes.INHERIT_VALIDITY, Boolean.TRUE);
			moveButtonGrp.setVerticalFill(true);
			moveButtonGrp.setHorizontalFill(false);
			moveButtonGrp.setBorder(BorderValue.NONE);

			// Move Top button
			m_moveTopAction = moveButtonGrp.createActionProperty("moveTopAction");
			m_moveTopAction.setI18NName("TopButton");
			m_moveTopAction.setAttribute(IPropertyAttributes.INHERIT_ENABLED, Boolean.FALSE);
			m_moveTopAction.setIcon(CHSImageLoader.loadImageIcon(
					"chs/images/general/ico_movetop.gif"));
			m_moveTopAction.setDisabledIcon(CHSImageLoader.loadImageIcon(
					"chs/images/general/ico_movetop_inactive.gif"));

			// Move Up button
			m_moveUpAction = moveButtonGrp.createActionProperty("moveUpAction");
			m_moveUpAction.setI18NName("UpButton");
			m_moveUpAction.setAttribute(IPropertyAttributes.INHERIT_ENABLED, Boolean.FALSE);
			m_moveUpAction.setIcon(CHSImageLoader.loadImageIcon(
					"chs/images/general/ico_moveup.gif"));
			m_moveUpAction.setDisabledIcon(CHSImageLoader.loadImageIcon(
					"chs/images/general/ico_moveup_inactive.gif"));

			// Move Down button
			m_moveDownAction = moveButtonGrp.createActionProperty("moveDownAction");
			m_moveDownAction.setI18NName("DownButton");
			m_moveDownAction.setAttribute(IPropertyAttributes.INHERIT_ENABLED, Boolean.FALSE);
			m_moveDownAction.setIcon(CHSImageLoader.loadImageIcon(
					"chs/images/general/ico_movedown.gif"));
			m_moveDownAction.setDisabledIcon(CHSImageLoader.loadImageIcon(
					"chs/images/general/ico_movedown_inactive.gif"));

			// Move Bottom button
			m_moveBottomAction = moveButtonGrp.createActionProperty("moveButtomAction");
			m_moveBottomAction.setI18NName("BottomButton");
			m_moveBottomAction.setAttribute(IPropertyAttributes.INHERIT_ENABLED, Boolean.FALSE);
			m_moveBottomAction.setIcon(CHSImageLoader.loadImageIcon(
					"chs/images/general/ico_movebottom.gif"));
			m_moveBottomAction.setDisabledIcon(CHSImageLoader.loadImageIcon(
					"chs/images/general/ico_movebottom_inactive.gif"));

			registerListeners();
			resetMoveButtonStates();
		}

		/**
		 * Set the move up / down button states depending on the value selected in the table.
		 */
		private void resetMoveButtonStates()
		{
			m_moveTopAction.setEnabled(false);
			m_moveUpAction.setEnabled(false);
			m_moveUpAction.setEnabled(false);
			m_moveDownAction.setEnabled(false);
			m_moveBottomAction.setEnabled(false);
		}

		private void registerListeners()
		{
			m_datumListProperty.addPropertyChangeListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					Object selectedValue = evt.getNewValue();
					if (selectedValue != null) {
						String selectedString = m_datumListProperty.getValue();
						m_selectedIndex = m_values.indexOf(selectedString);
					}
					m_moveBottomAction.setEnabled(m_selectedIndex >= 0 && m_selectedIndex < m_values.size() - 1);
					m_moveTopAction.setEnabled(m_selectedIndex > 0);
					m_moveUpAction.setEnabled(m_selectedIndex > 0);
					m_moveDownAction.setEnabled(m_selectedIndex >= 0 && m_selectedIndex < m_values.size() - 1);
				}
			});

			//		 Move Top Button
			m_moveTopAction.setActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if (m_selectedIndex > 0) {
						String node = m_values.get(m_selectedIndex);

						for (int i = m_selectedIndex; i > 0; i--) {
							m_values.set(i, m_values.get(i - 1));
						}

						m_values.set(0, node);

						m_datumListProperty.touchProperty();
					}
				}
			});

			// Move Bottom Button
			m_moveBottomAction.setActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					int maxIndex = m_values.size() - 1;
					if (m_selectedIndex >= 0 && m_selectedIndex < maxIndex) {
						String node = m_values.get(m_selectedIndex);

						for (int i = m_selectedIndex; i < maxIndex; i++) {
							m_values.set(i, m_values.get(i + 1));
						}

						m_values.set(maxIndex, node);

						m_datumListProperty.touchProperty();
					}
				}
			});

			// Move Up Button
			m_moveUpAction.setActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if (m_selectedIndex > 0) {
						String node = m_values.get(m_selectedIndex);

						m_values.set(m_selectedIndex, m_values.get(m_selectedIndex - 1));
						m_values.set(m_selectedIndex - 1, node);

						m_datumListProperty.touchProperty();
					}
				}
			});

			// Move Down Button
			m_moveDownAction.setActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if (m_selectedIndex >= 0 && m_selectedIndex < m_values.size() - 1) {
						String node = m_values.get(m_selectedIndex);

						m_values.set(m_selectedIndex, m_values.get(m_selectedIndex + 1));
						m_values.set(m_selectedIndex + 1, node);

						m_datumListProperty.touchProperty();
					}
				}
			});
		}

		List<String> getValues()
		{
			return m_values;
		}
	}
}
