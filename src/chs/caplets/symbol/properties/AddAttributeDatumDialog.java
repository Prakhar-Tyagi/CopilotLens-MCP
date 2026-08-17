/*
 * Copyright 2008-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.properties;

import chs.cof.COFTypeEnum;
import chs.cof.harness.IHarnessDesign;
import chs.cof.harness.physical.IBundleInsulation;
import chs.cof.harness.physical.IClip;
import chs.cof.harness.physical.IGrommet;
import chs.cof.harness.physical.IInsulationRun;
import chs.cof.harness.physical.IMultiLocationComponent;
import chs.cof.harness.physical.INodeInsulation;
import chs.cof.logical.cable.IAdditionalComponent;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IInternalPosition;
import chs.cof.logical.cable.IInternalPositionedObject;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.ISplicePin;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.project.objectinfo.IObjectTypeInfo;
import chs.cof.project.objectinfo.properties.IPropertyTemplateIterator;
import chs.cof.topology.physical.IBundle;
import chs.common.DatumTypeEnum;
import chs.common.attr.IAttributeType;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.system.FactoryMgr;
import chs.system.ISystemObjectTypeInfoMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.ChoiceTypeValue;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IPropertyAttributes;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.IStringProperty;
import chs.utilities.ui.property.OrientationValue;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyPanel;
import chs.utility.attr.DiagramObjectsAttributesHelper;
import chs.utility.ui.DialogHelper;
import chs.utility.ui.property.NamedObjectPropertyValueRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: amittal Date: Apr 3, 2008 Time: 11:02:41 AM To change this template use File |
 * Settings | File Templates.
 */
public class AddAttributeDatumDialog extends CAFOkCancelDialog
{

	public static final Map<COFTypeEnum, Class<?>> cofTypeEnumClassMap = new HashMap<COFTypeEnum, Class<?>>();

	static {
		cofTypeEnumClassMap.put(COFTypeEnum.HarnessDesign, IHarnessDesign.class);
		cofTypeEnumClassMap.put(COFTypeEnum.Bundle, IBundle.class);
		cofTypeEnumClassMap.put(COFTypeEnum.Connector, IConnector.class);
		cofTypeEnumClassMap.put(COFTypeEnum.Clip, IClip.class);
		cofTypeEnumClassMap.put(COFTypeEnum.Grommet, IGrommet.class);
		cofTypeEnumClassMap.put(COFTypeEnum.Splice, ISplice.class);
		cofTypeEnumClassMap.put(COFTypeEnum.NodeInsulation, INodeInsulation.class);
		cofTypeEnumClassMap.put(COFTypeEnum.InsulationRun, IInsulationRun.class);
		cofTypeEnumClassMap.put(COFTypeEnum.BundleInsulation, IBundleInsulation.class);
		cofTypeEnumClassMap.put(COFTypeEnum.MLC, IMultiLocationComponent.class);
		cofTypeEnumClassMap.put(COFTypeEnum.Wire, IWireConductor.class);
		cofTypeEnumClassMap.put(COFTypeEnum.ConnectorPin, IConnectorPin.class);
		cofTypeEnumClassMap.put(COFTypeEnum.SplicePin, ISplicePin.class);
		cofTypeEnumClassMap.put(COFTypeEnum.Position, IInternalPosition.class);
		cofTypeEnumClassMap.put(COFTypeEnum.AdditionalComponent, IAdditionalComponent.class);
		cofTypeEnumClassMap.put(COFTypeEnum.PositionedObject, IInternalPositionedObject.class);
		cofTypeEnumClassMap.put(COFTypeEnum.ShieldBody, IShieldBody.class);
	}

	private static final String ATTRIBUTE = ResourceMgr.getString(AddAttributeDatumDialog.class, "AddAttributeDatumDialog.attr");
	private static final String PROPERTY = ResourceMgr.getString(AddAttributeDatumDialog.class, "AddAttributeDatumDialog.prop");
	private static final String OBJECT_TYPE = ResourceMgr.getString(AddAttributeDatumDialog.class, "AddAttributeDatumDialog.objType");


	private IStringProperty m_objChooserProperty = null;
	private IStringProperty m_nameChooserProperty;
	private IStringProperty m_subType;

	private COFTypeEnum m_selectedObjType = null;
	private AttrOrPropWrapper m_selectedObjName;

	private DatumTypeEnum m_selectedObjSubType;

	private static final String TITLE = ResourceMgr.getString(AddAttributeDatumDialog.class, "AddAttributeDatumDialog.name");
	private static final String ATTR_PROP = ResourceMgr.getString(AddAttributeDatumDialog.class, "AddAttributeDatumDialog.attrProp");

	public AddAttributeDatumDialog(Frame frame, boolean modal)
	{
		this(frame, modal, null);
	}

	public AddAttributeDatumDialog(Frame frame, boolean modal, @Nullable COFTypeEnum cofTypeEnum)
	{
		super(frame, TITLE, modal);

		m_selectedObjType = cofTypeEnum;
		initPanel();
	}

	private void initPanel()
	{

		List<COFTypeEnum> cofTypes = getListOfCofObjectTypesForPanel();

		IPropertyGroup datumChooserGroup =
				PropertyFactory.createPropertyGroup("datumChooserGrp");

		if (m_selectedObjType == null) {
			IPropertyGroup objectChooserGroup =
					datumChooserGroup.createPropertyGroup("ObjectChooserGroup", GroupTypeValue.COLUMN);
			objectChooserGroup.setLabel(OBJECT_TYPE);
			// Create the Object List
			m_objChooserProperty =
					objectChooserGroup.createStringProperty("ObjectChooserProp", "Harness Object", "Object Type");

			m_objChooserProperty.setValuesList(cofTypes);
			m_objChooserProperty.setChoiceType(ChoiceTypeValue.LIST);
			m_objChooserProperty.setValueRenderer(new NamedObjectPropertyValueRenderer());
			m_objChooserProperty.setVerticalFill(true);
			m_objChooserProperty.setHorizontalFill(true);
		}

		// Create the Attributes List
		IPropertyGroup attrChooserGroup =
				datumChooserGroup.createPropertyGroup("attrChooserGroup", GroupTypeValue.COLUMN);
		String label = ATTR_PROP;//"Attribute/Property";
		if (m_selectedObjType != null) {
			label = m_selectedObjType.toString() + " " + label;
		}
		attrChooserGroup.setLabel(label);

		m_subType = attrChooserGroup.createStringProperty("name");
		m_subType.setChoiceType(ChoiceTypeValue.RADIO);
		m_subType.setColumns(2);

		List<String> values = new ArrayList<String>();
		values.add(ATTRIBUTE);
		values.add(PROPERTY);

		m_subType.setValuesList(values);
		m_subType.setAttribute(IPropertyAttributes.ORIENTATION, OrientationValue.HORIZONTAL);
		m_subType.setHorizontalFill(true);
		m_subType.setVerticalFill(false);

		m_nameChooserProperty = attrChooserGroup.createStringProperty("AttrChooserProp", "Attribute Name", "");
		m_nameChooserProperty.setChoiceType(ChoiceTypeValue.LIST);
		m_nameChooserProperty.setVerticalFill(true);
		m_nameChooserProperty.setHorizontalFill(true);

		PropertyPanel objChooserPanel = new PropertyPanel("objectchooser", datumChooserGroup);
		getContentPane().add(objChooserPanel, BorderLayout.CENTER);
		pack();
		addListeners();
		m_subType.setObject(ATTRIBUTE);
	}

	List<COFTypeEnum> getListOfCofObjectTypesForPanel()
	{
		List<COFTypeEnum> types = CollectionUtils.createList(cofTypeEnumClassMap.keySet().iterator());
		Collections.sort(types, new AlphaNumComparator<COFTypeEnum>());
		return types;
	}

	private void addListeners()
	{

		m_subType.addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				if ((m_subType.getValue()).equals(ATTRIBUTE)) {
					m_selectedObjSubType = DatumTypeEnum.Attribute;
					if (m_selectedObjType != null) {
						List<? extends AttrOrPropWrapper> attrs =
								getAttributesNamesObjectType(cofTypeEnumClassMap.get(m_selectedObjType));
						if (m_objChooserProperty != null) {
							m_objChooserProperty.setDefaultValueObject(m_selectedObjType);
						}

						if (attrs != null) {
							m_nameChooserProperty.setValuesList(attrs);
						}
					}
				}
				else if ((m_subType.getValue()).equals(PROPERTY)) {
					m_selectedObjSubType = DatumTypeEnum.Property;
					if (m_selectedObjType != null) {
						// Need to retireve the property names
						List<? extends AttrOrPropWrapper> attrs =
								getPropertiesFromObjectTypeInfoMgr(cofTypeEnumClassMap.get(m_selectedObjType));
						if (m_objChooserProperty != null) {
							m_objChooserProperty.setDefaultValueObject(m_selectedObjType);
						}
						m_nameChooserProperty.setValuesList(attrs);
					}
				}
				updateOkEnablement();
			}
		});

		if (m_objChooserProperty != null) {
			m_objChooserProperty.addPropertyChangeListener(new PropertyChangeListener()
			{
				public void propertyChange(PropertyChangeEvent evt)
				{
					Object selectedName = evt.getNewValue();
					if (selectedName != null) {
						m_selectedObjType = (COFTypeEnum) selectedName;

						if ((m_subType.getValue()).equals(ATTRIBUTE)) {
							List<? extends AttrOrPropWrapper> attrs =
									getAttributesNamesObjectType(cofTypeEnumClassMap.get(m_selectedObjType));
							m_objChooserProperty.setDefaultValueObject(selectedName);
							m_nameChooserProperty.setValuesList(attrs);
						}
						else if ((m_subType.getValue()).equals(PROPERTY)) {

							List<? extends AttrOrPropWrapper> attrs =
									getPropertiesFromObjectTypeInfoMgr(cofTypeEnumClassMap.get(m_selectedObjType));
							m_objChooserProperty.setDefaultValueObject(selectedName);
							m_nameChooserProperty.setValuesList(attrs);
						}
					}
					updateOkEnablement();
				}
			});
		}

		m_nameChooserProperty.addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				Object selectedName = evt.getNewValue();
				if (selectedName != null) {
					m_selectedObjName = (AttrOrPropWrapper) selectedName;
					m_nameChooserProperty.setDefaultValueObject(m_selectedObjName);
				}
				updateOkEnablement();
			}
		});
	}

	private void updateOkEnablement()
	{
		getOkButton().setEnabled(m_selectedObjName != null);
	}

	public void setVisible(boolean show)
	{
		if (show) {
			pack();
			DialogHelper.centerOnOwner(this);
		}
		super.setVisible(show);
	}

	public COFTypeEnum getSelectedCOFType()
	{
		return m_selectedObjType;
	}

	@Nullable public String getSelectedObjectName()
	{
		return m_selectedObjName == null ? null : m_selectedObjName.getCodeName();
	}

	public DatumTypeEnum getSelectedObjectSubType()
	{

		return m_selectedObjSubType;
	}

	@Nullable private List<? extends AttrOrPropWrapper> getAttributesNamesObjectType(Class<?> classType)
	{

		List<AttributeWrapper> attributes = getAttributesForClass(classType);

		Class<?> diagramObjClassType = DiagramObjectsAttributesHelper.getDiagramRepresentation(classType);
		if (diagramObjClassType != null) {
			List<AttributeWrapper> diagramAttrs = getAttributesForClass(diagramObjClassType);
			if (diagramAttrs != null) {
				assert attributes != null;
				attributes.addAll(diagramAttrs);
			}
		}

		if (attributes != null) {
			Collections.sort(attributes, new AlphaNumComparator<AttributeWrapper>());
		}

		return attributes;
	}

	@Nullable
	private List<AttributeWrapper> getAttributesForClass(Class<?> className)
	{
		List<AttributeWrapper> attributes = null;
		if (className != null) {
			Map<String, IAttributeType> attributeTypes = FactoryMgr.getCommonFactory().getAttributeTypes(className);

			if (attributeTypes != null) {

				attributes = new ArrayList<AttributeWrapper>(attributeTypes.size());
				for (Map.Entry<String, IAttributeType> entry : attributeTypes.entrySet()) {
					attributes.add(new AttributeWrapper(entry.getValue()));
				}
			}
		}
		return attributes;
	}

	private List<? extends AttrOrPropWrapper> getPropertiesFromObjectTypeInfoMgr(Class<?> objectType)
	{
		List<PropertyWrapper> properties = new ArrayList<PropertyWrapper>(3);

		ISystemObjectTypeInfoMgr objectTypeInfoMgr =
				FactoryMgr.getSystemFactory().getCHSSystem().getSystemData().getObjectTypeInfoMgr();

		if (objectTypeInfoMgr == null) {
			return Collections.emptyList();
		}

		IObjectTypeInfo objectTypeInfo = objectTypeInfoMgr.getByClass(objectType);
		IObjectTypeInfo generalObjectTypeInfo = objectTypeInfoMgr.getGeneralObjectTypeInfo();

		Collection<String> exist = new ArrayList<String>(3);
		// collecting the properties from the objectTypeInfo of the objectType
		if (objectTypeInfo != null) {
			for (IPropertyTemplateIterator itr = objectTypeInfo.getPropertyTemplates(); itr.hasNext();) {
				String propertyName = itr.getNext().getName();
				properties.add(new PropertyWrapper(propertyName));
				exist.add(propertyName.toLowerCase());
			}
		}

		// collecting the properties from the generalObjectTypeInfo
		for (IPropertyTemplateIterator itr = generalObjectTypeInfo.getPropertyTemplates(); itr.hasNext();) {
			String propertyName = itr.getNext().getName();
			if (!exist.contains(propertyName.toLowerCase())) {
				properties.add(new PropertyWrapper(propertyName));
			}
		}

		Collections.sort(properties, new AlphaNumComparator<PropertyWrapper>());

		return properties;
	}

	private abstract static class AttrOrPropWrapper
	{
		abstract String getCodeName();
	}

	private static class AttributeWrapper extends AttrOrPropWrapper
	{
		@NotNull private final IAttributeType attributeType;

		AttributeWrapper(@NotNull IAttributeType attrType)
		{
			attributeType = attrType;
		}

		@Override public String toString()
		{
			return attributeType.getDisplayName();
		}

		@Override String getCodeName()
		{
			return attributeType.getName();
		}
	}

	private static class PropertyWrapper extends AttrOrPropWrapper
	{
		@NotNull private final String propertyName;

		PropertyWrapper(@NotNull String propName)
		{
			propertyName = propName;
		}

		@Override public String toString()
		{
			return propertyName;
		}

		@Override String getCodeName()
		{
			return propertyName;
		}
	}
}
