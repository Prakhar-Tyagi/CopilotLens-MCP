/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.common.attr.IAttribute;
import chs.ctf.editui.AttributesUIProperty;
import chs.utilities.ResourceMgr;
import chs.utility.attr.AttributeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * @author rmahato
 */
public class InlineAttributeDetailsInfo extends AttributeDetailsTableInfo
{

	public static final String INLINE_JACK_VALUE_COLOUM_NAME = ResourceMgr.getString(
			InlineAttributeDetailsInfo.class, "InlineAttributeDetailsInfo.inlineJackValueColoumnName.text");
	public static final String INLINE_PLUG_VALUE_COLOUM_NAME = ResourceMgr.getString(
			InlineAttributeDetailsInfo.class, "InlineAttributeDetailsInfo.inlinePlugValueColoumnName.text");

	public InlineAttributeDetailsInfo()
	{
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(ATTRIBUTE_COLOUM_NAME, "");
		coloumValueMap.put(INLINE_JACK_VALUE_COLOUM_NAME, "");
		coloumValueMap.put(INLINE_PLUG_VALUE_COLOUM_NAME, "");
	}

	public InlineAttributeDetailsInfo(@NotNull String attribute, @NotNull String jackValue, @NotNull String plugValue)
	{
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(ATTRIBUTE_COLOUM_NAME, attribute);
		coloumValueMap.put(INLINE_JACK_VALUE_COLOUM_NAME, jackValue);
		coloumValueMap.put(INLINE_PLUG_VALUE_COLOUM_NAME, plugValue);
	}

	@NotNull @Override Collection<DetailsTableInfo> getTableData(@Nullable ILogicObject selectedObject)
	{
		IInlineJackConnector jackConnector = (IInlineJackConnector) selectedObject;
		assert jackConnector != null;
		IInlinePlugConnector plugConnector = null;
		Set<IConnector> matedConnectors = jackConnector.getMates();
		if (matedConnectors != null && matedConnectors.iterator().hasNext()) {
			plugConnector = (IInlinePlugConnector) jackConnector.getMates().iterator().next();
		}

		List<DetailsTableInfo> rows = new ArrayList<>();
		Collection<IAttribute> attributes = AttributeUtils.getUserVisibleAttributes(jackConnector);
		for (IAttribute attribute : attributes) {
			if(AttributesUIProperty.shouldShowModuleCodeAttribute(attribute)) {
				String attributeName = attribute.getDisplayName();
				String jackValue = attribute.getAsString() == null ? "" : attribute.getAsString();
				String plugValue =
						plugConnector != null ? getPinlistAttributeValue(plugConnector, attribute.getName()) : "";
				rows.add(new InlineAttributeDetailsInfo(attributeName, jackValue, plugValue));
			}
		}
		return rows;
	}

	@NotNull private String getPinlistAttributeValue(@NotNull IPinList pinlist, @NotNull String attributeName)
	{
		IAttribute attribute = pinlist.getAttribute(attributeName);

		String attributeValue = attribute == null ? "" : attribute.getAsString();
		return attributeValue == null ? "" : attributeValue;
	}
}
