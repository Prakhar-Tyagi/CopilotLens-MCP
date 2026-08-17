/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.symbol.actions;

import chs.cof.COFTypeEnum;
import chs.common.IObjectFilter;
import chs.common.IUIDObject;
import chs.common.reln.IRelatedEntityType;
import chs.common.reln.Relation;
import chs.common.styles.REDApplicableTypeEnum;
import chs.utilities.CapabilityHelper;
import chs.utilities.ResourceMgr;
import chs.utilities.SupportedFeatureInfo;
import chs.utilities.ui.property.BorderValue;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyPanel;
import chs.utility.reln.RelatedEntityUtils;
import chs.utility.ui.HelpMgr;
import chs.utility.ui.OkCancelDialog;
import chs.utility.ui.UIUtils;
import chs.utility.ui.property.NamedObjectPropertyValueRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class REDDatumDialog extends OkCancelDialog
{

	private IRelatedEntityType m_type = IRelatedEntityType.Unknown;
	private IProperty m_REDApplicableTypeProperty;
	private IProperty m_datumProperty;
	private List<IRelatedEntityType> m_entityTypes = new ArrayList<IRelatedEntityType>();

	private COFTypeEnum m_selectedType;

	public REDDatumDialog(Frame parent, String title, boolean modal, COFTypeEnum selectedDatumType)
	{
		super(parent, title, modal);
		m_selectedType = selectedDatumType;
		build();
		setPreferredSize(UIUtils.getStandardDialogMaximumSize());
		setMaximumSize(getPreferredSize());
		pack();
		addListeners();
	}

	private void build()
	{
		IPropertyGroup mainPropertyGroup = PropertyFactory.createPropertyGroup("RelatedEntityDatumDialog");
		IPropertyGroup intGrp =
				mainPropertyGroup.createPropertyGroup("bundlelblGrp", GroupTypeValue.LABELLED_COLUMN);
		intGrp.setBorder(BorderValue.NONE);

		m_REDApplicableTypeProperty = intGrp.createStringProperty("ret");
		m_REDApplicableTypeProperty.setLabel(
				ResourceMgr.getStringForLabel(REDDatumDialog.class, "REDDatumDialog.RelatedEntity.text"));
		List<COFTypeEnum> enums = new ArrayList<COFTypeEnum>();
		if (m_selectedType != null) {
			enums.add(m_selectedType);
			m_REDApplicableTypeProperty.setValuesList(enums);
			m_REDApplicableTypeProperty.setObject(m_selectedType);
			m_REDApplicableTypeProperty.setEditable(false);
		}
		else {
			for (REDApplicableTypeEnum type : REDApplicableTypeEnum.values()) {
				List<IRelatedEntityType> relatedEntity =
						getRelatedEntities(type.getObjectType().value());
				if (!relatedEntity.isEmpty()) {
					enums.add(type.getObjectType());
				}
			}
			m_REDApplicableTypeProperty.setValuesList(enums);
			m_REDApplicableTypeProperty.setObject(enums.toArray()[0]);
		}
		m_REDApplicableTypeProperty.setValueRenderer(new NamedObjectPropertyValueRenderer());

		Object obj = m_REDApplicableTypeProperty.getObject();
		if (obj instanceof COFTypeEnum) {
			COFTypeEnum cofTypeEnum = (COFTypeEnum) obj;
			m_entityTypes = getRelatedEntities(cofTypeEnum.value());
		}

		m_datumProperty = intGrp.createStringProperty("redenum");
		m_datumProperty
				.setLabel(ResourceMgr.getStringForLabel(REDDatumDialog.class, "REDDatumDialog.relatedobject.text"));
		m_datumProperty.setObject(m_entityTypes.toArray()[0]);

		m_datumProperty.setValuesList(m_entityTypes);
		m_datumProperty.setValueRenderer(new NamedObjectPropertyValueRenderer());

		JPanel pPanel = new PropertyPanel("Related Entity Panel", mainPropertyGroup);
		getContentPane().add(pPanel);

		m_REDApplicableTypeProperty.addPropertyChangeListener(new PropertyChangeListener()
		{

			public void propertyChange(PropertyChangeEvent evt)
			{
				COFTypeEnum cofTypeEnum = (COFTypeEnum) evt.getNewValue();
				m_entityTypes = getRelatedEntities(cofTypeEnum.value());
				m_datumProperty.setObject(null);
				if (!m_entityTypes.isEmpty()) {
					m_datumProperty.setObject(m_entityTypes.toArray()[0]);
					m_datumProperty.setValuesList(m_entityTypes);
				}
			}
		});

		m_REDApplicableTypeProperty.touch();
	}

	public static List<IRelatedEntityType> getRelatedEntities(final Class<? extends IUIDObject> cls){

		if (cls == null) {
			return Collections.emptyList();
		}
		//Enable Connector Datum in VeSys and SEElectrical
		if(COFTypeEnum.Connector.value().equals(cls)&& CapabilityHelper.supports(SupportedFeatureInfo.Feature.CONNECTOR_DATUM_DECORATION))
		{
			List<IRelatedEntityType> relatedEntities=new ArrayList<IRelatedEntityType>();
			relatedEntities.add(RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Connector, Relation.ConnectorPin));
			relatedEntities.addAll(RelatedEntityUtils.getRelatedEntities(cls, getRelatedEntityTypeFilter()));
			return relatedEntities;
		}
		return RelatedEntityUtils.getRelatedEntities(cls, getRelatedEntityTypeFilter());
	}

	@NotNull private static IObjectFilter<IRelatedEntityType> getRelatedEntityTypeFilter()
	{
		return new IObjectFilter<IRelatedEntityType>()
		{
			public boolean accept(IRelatedEntityType entityType)
			{
				return isREDSupported(entityType);
			}
		};
	}

	private static boolean isREDSupported(IRelatedEntityType entityType)
	{
		return entityType.getTargetEntityType() != COFTypeEnum.WireEnd;
	}

	public IRelatedEntityType getRelatedEntityType()
	{
		return m_type;
	}

	private void addListeners()
	{
		getOkButton().addActionListener(new ActionListener()
		{
			/**
			 * Called in response to an action event
			 *
			 * @param e ActionEvent
			 */
			public void actionPerformed(ActionEvent e)
			{
				m_type = (IRelatedEntityType) m_datumProperty.getObject();
				setVisible(false);
				setCancelled(false);
			}
		});

		getCancelButton().addActionListener(new ActionListener()
		{
			/**
			 * Called in response to an action event
			 *
			 * @param e ActionEvent
			 */
			public void actionPerformed(ActionEvent e)
			{
				setVisible(false);
				setCancelled(true);
			}
		});

		getHelpButton().addActionListener(new ActionListener()
		{
			/**
			 * Called in response to an action event
			 *
			 * @param e ActionEvent
			 */
			public void actionPerformed(ActionEvent e)
			{
				HelpMgr.getInstance().showHelpTopic(getHelpID(), REDDatumDialog.this);
			}
		});
	}

	@NotNull public String getHelpID()
	{
		// Note: donot change helpID, if changed intimate the documentation team
		String helpID = "RED_Datum_Dialog";
		return helpID + "_Add_RED_Datum";
	}
}