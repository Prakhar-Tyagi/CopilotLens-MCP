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

import chs.cof.logical.cable.ILogicObject;
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

/**
 * @author rmahato
 */
public class AttributeDetailsTableInfo extends DetailsTableInfo
{

	protected static final String ATTRIBUTE_COLOUM_NAME = ResourceMgr.getString(
			AttributeDetailsTableInfo.class, "AttributeDetailsTableInfo.attributeColoumnName.text");
	protected static final String VALUE_COLOUM_NAME = ResourceMgr.getString(
			AttributeDetailsTableInfo.class, "AttributeDetailsTableInfo.valueColoumnName.text");

	public AttributeDetailsTableInfo()
	{
		this("", "");
	}

	public AttributeDetailsTableInfo(@NotNull String attribute, @NotNull String value)
	{
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(ATTRIBUTE_COLOUM_NAME, attribute);
		coloumValueMap.put(VALUE_COLOUM_NAME, value);
	}

	@NotNull @Override Collection<DetailsTableInfo> getTableData(@Nullable ILogicObject selectedObject)
	{
		List<DetailsTableInfo> rows = new ArrayList<>();
		if (selectedObject == null) {
			return rows;
		}
		Collection<IAttribute> attributes = AttributeUtils.getUserVisibleAttributes(selectedObject);
		for (IAttribute attribute : attributes) {
			if (AttributesUIProperty.shouldShowModuleCodeAttribute(attribute)) {
				rows.add(new AttributeDetailsTableInfo(attribute.getDisplayName(),
						attribute.getAsString() == null ? "" : attribute.getAsString()));
			}
		}
		return rows;
	}
}
