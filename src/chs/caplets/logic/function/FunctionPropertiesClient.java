/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.function;

import chs.caf.caplet.IAttributeUIPropertyProvider;
import chs.caf.caplet.IPropertiesClientComponent;
import chs.caf.caplet.properties.FunctionImplControl;
import chs.caf.caplet.properties.FunctionTypeControl;
import chs.caf.caplet.properties.MessageImplControl;
import chs.caf.caplet.properties.MessageTypeControl;
import chs.caf.caplet.properties.PortTypeControl;
import chs.caf.caplet.properties.SignalDictionaryAssociationControl;
import chs.caf.caplet.properties.SignalImplControl;
import chs.caf.caplet.properties.SignalTypeControl;
import chs.caplets.logic.Model;
import chs.caplets.shared.properties.PropertiesClient;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.cable.IFunctionObject;
import chs.cof.project.objectinfo.names.INameTemplate;
import chs.common.attr.IAttributeProvider;
import chs.common.attr.IAttributeTypes;
import chs.common.attr.IReadOnlyFacet;
import chs.ctf.editui.AttributesUIProperty;
import chs.ctf.editui.IAttributesClient;
import chs.utilities.CommonUtils;
import chs.utilities.ui.property.IProperty;
import chs.utility.helpers.FunctionConductorUpdateHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A properties client tailored for the {@link FunctionCaplet}
 */
public class FunctionPropertiesClient extends PropertiesClient
{

	private final FunctionTypeControl m_functionTypeControl;

	public FunctionPropertiesClient(Model model)
	{
		super(model);
		m_functionTypeControl = setUpFuncTypeControl();
	}

	public FunctionPropertiesClient(Model model, boolean willEditSharedObjects)
	{
		super(model, willEditSharedObjects);
		m_functionTypeControl = setUpFuncTypeControl();
	}

	@NotNull private FunctionTypeControl setUpFuncTypeControl()
	{
		final FunctionTypeControl functionTypeControl = new FunctionTypeControl(this);
		m_clientComponents.add(functionTypeControl);
		m_clientComponents.add(new FunctionImplControl(this));
		m_clientComponents.add(new SignalTypeControl(this));
		m_clientComponents.add(new SignalImplControl(this));
		m_clientComponents.add(new MessageTypeControl(this));
		m_clientComponents.add(new MessageImplControl(this));
		m_clientComponents.add(new PortTypeControl(this));
		m_clientComponents.add(new SignalDictionaryAssociationControl(this));
		return functionTypeControl;
	}

	public void editObjectTypeRelation(INameTemplate template)
	{
		IFunctionMessage functionMessage =
				CommonUtils.cast(m_propertiedSet.getSingleNamedObject(), IFunctionMessage.class);
		FunctionConductorUpdateHelper.makeChangesInFunctionMessageAccordingToOTI(template, functionMessage);
	}

	@Override public boolean ignoreAttribute(IReadOnlyFacet f)
	{
		if (super.ignoreAttribute(f)) {
			return true;
		}

		// Ignore attributes that are set via separate control rather than in the attribute table
		final String attributeName = f.getName();
		return attributeName.equals(IAttributeTypes.FUNCTION_TYPE) ||
				attributeName.equals(IAttributeTypes.FUNCTION_IMPL_TYPE) ||
				attributeName.equals(IAttributeTypes.SIGNAL_TYPE) ||
				attributeName.equals(IAttributeTypes.MESSAGE_TYPE) ||
				attributeName.equals(IAttributeTypes.MESSAGE_IMPL_TYPE) ||
				attributeName.equals(IAttributeTypes.SIGNAL_IMPL_TYPE) ||
				attributeName.equals(IAttributeTypes.PORT_TYPE);
	}

	@Nullable @Override public IAttributesClient getAttributesClient(@NotNull Set<IAttributeProvider> attributeProviders,
			@NotNull Set<IRepresentedObject> repObjs, boolean connectivityEditable, boolean hasNamedObjects, boolean inEditMode)
	{
		if (isFunctionPropertiesClientNeeded(attributeProviders)) {
			return new ConcordFnPropertiesAttributesClient(attributeProviders, m_propertiedSet.getNamedObjOwners(),
					isConnectivityEditable(), inEditMode);
		}
		return super
				.getAttributesClient(attributeProviders, repObjs, connectivityEditable, hasNamedObjects, inEditMode);
	}

	@Override protected void doUpdateUi()
	{
		m_functionTypeControl.updateUi();
	}

	@Override public void resetClientComponents()
	{
		m_functionTypeControl.reset();
	}

	@Override protected boolean shouldUpdateUi(@NotNull IProperty propertyUI)
	{
		// Update if the Show All Attributes checkbox has been toggled or if a combo box item has been changed
		if (getPropertyIdentifier(propertyUI).equals(TOGGLE_SHOW_ALL_ATTRIBUTES_PROP)) {
			return true;
		}
		return m_functionTypeControl.isProperty(propertyUI);
	}

	private boolean isFunctionPropertiesClientNeeded(Set<IAttributeProvider> attributeProviders)
	{
		for (IAttributeProvider att : attributeProviders) {
			if (att instanceof IFunctionObject) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Function specific attributes client. This client needs an attribute UI that supports filtering of attributes in
	 * the table based on function type. This client ensures that only the applicable attribute changes are
	 * persistable.
	 */
	private class ConcordFnPropertiesAttributesClient extends CAFPropertiesAttributesClient
	{

		ConcordFnPropertiesAttributesClient(Set<IAttributeProvider> obj, Set<IRepresentedObject> namedObjOwners,
				boolean isConnectivityEditable, boolean inEditMode)
		{
			super(obj, namedObjOwners, isConnectivityEditable, inEditMode);
		}

		@NotNull @Override protected AttributesUIProperty getAttributesUIProperty()
		{
			return new FilterableAttributesUIProperty();
		}

		private class FilterableAttributesUIProperty extends AttributesUIProperty
		{

			private FilterableAttributesUIProperty()
			{
				super(ConcordFnPropertiesAttributesClient.this);
			}

			@Override protected boolean isPropertyRowVisible(@NotNull IReadOnlyFacet att)
			{
				final String attributeName = att.getName();
				final AttributesUIProperty ui = m_attributesUI.getAttributesTableUI();
				if (ui != null) {
					return ui.hasPropertyRow(attributeName);
				}
				return super.isPropertyRowVisible(att);
			}

			@Nullable @Override public IProperty getUIProperty(String attributeName)
			{
				IProperty uiProperty = super.getUIProperty(attributeName);
				if (uiProperty != null) {
					return uiProperty;
				}
				for (IPropertiesClientComponent clientComponent : m_clientComponents) {
					if (!(clientComponent instanceof IAttributeUIPropertyProvider)) {
						continue;
					}
					IProperty comboUI = ((IAttributeUIPropertyProvider) clientComponent).getUIProperty(attributeName);
					if (comboUI != null) {
						return comboUI;
					}
				}
				return null;
			}
		}
	}
}