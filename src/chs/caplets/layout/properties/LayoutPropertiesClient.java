/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.layout.properties;

import chs.caf.caplet.IPropertiedSet;
import chs.caplets.logic.Model;
import chs.caplets.shared.properties.PropertiesClient;
import chs.cof.draw.IText;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.schem.IConnectivityRef;
import chs.common.attr.IAttributeProvider;
import chs.common.attr.IReadOnlyFacet;
import chs.ctf.editui.IAttributesClient;
import chs.ctf.editui.PropertiesAttributesClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is a properties client instance tailored to the LayoutCaplet
 */
public class LayoutPropertiesClient extends PropertiesClient
{

	public LayoutPropertiesClient(Model model)
	{
		super(model);
	}

	public LayoutPropertiesClient(Model model, boolean willEditSharedObjects)
	{
		super(model, willEditSharedObjects);
	}

	@Override public boolean allowNameEdit()
	{
		if (!isPropertiedSetEditable()) {
			return false;
		}
		return super.allowNameEdit();
	}

	@Override protected boolean allowPartNumberChanges()
	{
		if (!isPropertiedSetEditable()) {
			return false;
		}
		return super.allowPartNumberChanges();
	}

	@Override public boolean isConnectivityEditable()
	{
		if (!isPropertiedSetEditable()) {
			return false;
		}
		return super.isConnectivityEditable();
	}

	@Override public boolean allowOptionModuleLevelEditing()
	{
		if (!isPropertiedSetEditable()) {
			return false;
		}
		return super.allowOptionModuleLevelEditing();
	}

	@Override public boolean allowOptionViewing()
	{
		return true;
	}

	@Override public boolean arePropertiesAddOrDeleteable()
	{
		if (!isPropertiedSetEditable()) {
			return false;
		}
		return super.arePropertiesAddOrDeleteable();
	}

	private boolean isPropertiedSetEditable()
	{
		IPropertiedSet propertiedSet = getPropertiedSet();
		if (propertiedSet != null) {
			Set<ILogicObject> candidateSet = propertiedSet.getFilteredObjects(ILogicObject.class);
			candidateSet.addAll(propertiedSet.getFilteredObjects(IText.class).stream()
					.map(txt -> txt.getContainer())
					.filter(IConnectivityRef.class::isInstance)
					.map(connRef -> ((IConnectivityRef) connRef).getConnectivity())
					.collect(Collectors.toSet()));
			return candidateSet.stream().allMatch(ILogicOtherComponent.class::isInstance);
		}
		return true;
	}

	@Override
	@Nullable public IAttributesClient getAttributesClient(@NotNull Set<IAttributeProvider> attributeProviders,
			@NotNull Set<IRepresentedObject> repObjs, boolean connectivityEditable, boolean hasNamedObjects, boolean inEditMode)
	{
		if (isReadOnlyAttributesClientNeeded(attributeProviders)) {
			return new ReadOnlyAttributesClient(attributeProviders, repObjs, connectivityEditable, inEditMode);
		}
		return super
				.getAttributesClient(attributeProviders, repObjs, connectivityEditable, hasNamedObjects, inEditMode);
	}

	private boolean isReadOnlyAttributesClientNeeded(Set<IAttributeProvider> attributeProviders)
	{
		for (IAttributeProvider attributeProvider : attributeProviders) {
			if (attributeProvider instanceof ILogicObject &&
					!ILogicOtherComponent.class.isInstance(attributeProvider)) {
				return true;
			}
		}
		return false;
	}

	private static class ReadOnlyAttributesClient extends PropertiesAttributesClient
	{

		ReadOnlyAttributesClient(Set<IAttributeProvider> objs,
				Set<IRepresentedObject> namedObjOwner, boolean isConnectivityEditable, boolean inEditMode)
		{
			super(objs, namedObjOwner, isConnectivityEditable, inEditMode);
		}

		@Override public boolean isReadOnly(IReadOnlyFacet attr)
		{
			return true;
		}
	}
}