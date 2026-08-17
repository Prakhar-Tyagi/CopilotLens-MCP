/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.IShareRow;
import chs.common.attr.IAttributeType;
import chs.utility.attr.AttributeHelper;
import com.mentor.capital.javafx.table.ColumnInformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolver for dynamically created attribute and property columns in batch share/unshare table views.
 * <p>
 * This class is responsible for resolving column names into ColumnInformation objects for
 * dynamically added columns based on object attributes and properties.
 */
public class CustomColumnResolver
{

	public static final String ATTRIBUTE_PREFIX = "Attribute:";
	public static final String PROPERTY_PREFIX = "Property:";

	private final Map<String, IAttributeType> allAttributeMap;

	public CustomColumnResolver(@NotNull IBatchShareFilterObjectType[] filterObjectTypes)
	{
		allAttributeMap = buildAttributeTypeMap(filterObjectTypes);
	}

	@NotNull
	private static Map<String, IAttributeType> buildAttributeTypeMap(IBatchShareFilterObjectType[] filterObjectTypes)
	{
		Set<IAttributeType> allAttributeTypes =
				Arrays.stream(filterObjectTypes).flatMap(filterType -> filterType.getRepresentedObjectTypes().stream())
						.flatMap(type -> AttributeHelper.getAttributeTypes(type.getAttributeProviderClass()).values()
								.stream().filter(attr -> type.getAvailableAttributeNames().contains(attr.getName())))
						.collect(Collectors.toSet());

		Map<String, IAttributeType> map = new HashMap<>();
		allAttributeTypes.forEach(type -> map.put(type.getName(), type));
		return map;
	}

	@Nullable public <R extends IShareRow> ColumnInformation<R> resolveCustomColumn(@NotNull String name,
			@NotNull Function<IAttributeType, ColumnInformation<R>> attributeColumnCreator,
			@Nullable Function<String, ColumnInformation<R>> propertyColumnCreator)
	{
		if (name.startsWith(ATTRIBUTE_PREFIX)) {
			String[] attrSet = name.split(":");
			if (attrSet.length == 2) {
				String attname = attrSet[1];
				if (allAttributeMap.containsKey(attname)) {
					IAttributeType attrType = allAttributeMap.get(attname);
					return attributeColumnCreator.apply(attrType);
				}
			}
		}

		if (propertyColumnCreator != null && name.startsWith(PROPERTY_PREFIX)) {
			String[] propSet = name.split(":");
			if (propSet.length == 2) {
				String propName = propSet[1];
				return propertyColumnCreator.apply(propName);
			}
		}

		return null;
	}
}
