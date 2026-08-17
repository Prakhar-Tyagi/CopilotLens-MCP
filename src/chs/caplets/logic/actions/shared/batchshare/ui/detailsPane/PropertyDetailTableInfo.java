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
import chs.common.IProperty;
import chs.common.IPropertyIterator;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @author rmahato
 */
public class PropertyDetailTableInfo extends DetailsTableInfo
{

	private static final String PROPERTY_COLOUM_NAME = ResourceMgr
			.getString(PropertyDetailTableInfo.class, "PropertyDetailTableInfo.propertyColoumnName.text");
	private static final String VALUE_COLOUM_NAME =
			ResourceMgr.getString(PropertyDetailTableInfo.class, "PropertyDetailTableInfo.valueColoumnName.text");
	private static final String TYPE_COLOUM_NAME =
			ResourceMgr.getString(PropertyDetailTableInfo.class, "PropertyDetailTableInfo.typeColoumnName.text");

	public PropertyDetailTableInfo()
	{
		this("", "", "");
	}

	@NotNull @Override public Collection<DetailsTableInfo> getTableData(@Nullable ILogicObject selectedObject)
	{
		Set<DetailsTableInfo> rows = new HashSet<>();
		if (selectedObject == null) {
			return rows;
		}

		addPropertyDetailRows(selectedObject, rows);
		return rows;
	}

	protected void addPropertyDetailRows(@NotNull ILogicObject selectedObject, @NotNull Set<DetailsTableInfo> rows)
	{
		IPropertyIterator iterator = selectedObject.getProperties();
		while (iterator.hasNext()) {
			IProperty property = iterator.next();
			String propName = property.getName();
			String propValue = property.getAsString();
			String proptype = property.getType().getDisplayName();
			rows.add(new PropertyDetailTableInfo(propName, propValue == null ? "" : propValue, proptype));
		}
	}

	public PropertyDetailTableInfo(@NotNull String propertyName, @NotNull String value, @NotNull String type)
	{
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(PROPERTY_COLOUM_NAME, propertyName);
		coloumValueMap.put(VALUE_COLOUM_NAME, value);
		coloumValueMap.put(TYPE_COLOUM_NAME, type);
	}
}
