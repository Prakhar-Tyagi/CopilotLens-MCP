/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.utilities.AlphaNumComparator;
import chs.utilities.IXMLTags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Base object info
 */
class ObjectInfo implements IObjectInfo
{

	@NotNull private String m_designUID;
	@NotNull private String m_uid;
	@NotNull private ShareableEntityTypeEnum m_type;
	@Nullable protected String m_sharedUID;
	@NotNull private final Map<String, String> m_attributes;
	@NotNull private final Map<String, String> m_properties;
	//TODO : add proper annotations on all attributes for xml names
	@NotNull private final Map<String, String> m_attributesXMLNameMap = Map.ofEntries(Map.entry("oscolor", "oscol"),
			Map.entry("multicorenote", "multnote"),
			Map.entry("includeoncutchart", "inccut"),
			Map.entry("modulecode", "module"),
			Map.entry("unmodifiedcutbackvalueend1", "ucutbck1"),
			Map.entry("unmodifiedcutbackvalueend2", "ucutbck2"),
			Map.entry("unmodifiedlength", "lengthu"),
			Map.entry("modifiedlength", "lengthm"),
			Map.entry("lengthchangevalue", "lengthch"),
			Map.entry("lengthchangetype", "lenchtyp"),
			Map.entry("cutbackvalueend1", "cutback1"),
			Map.entry("cutbackvalueend2", "cutback2"),
			Map.entry("cutbackchangevalueend1", "lengthc1"),
			Map.entry("cutbackchangevalueend2", "lengthc2"),
			Map.entry("cutbackchangetypeend1", "lenchty1"),
			Map.entry("cutbackchangetypeend2", "lenchty2"),
			Map.entry("customermulticorename", "custmult"),
			Map.entry("sealsrequired", "issealed"),
			Map.entry("plugsrequired", "isplugged"),
			Map.entry("greaserequired", "isgreased"),
			Map.entry("footprintconnectorname", "footprintconnname"),
			Map.entry("defaultterminalmaterial", "termmatref"),
			Map.entry("connectorassembly", "isconnectorassembly"),
			Map.entry("backshellplugrequired", "isbackshellplugged"),
			Map.entry("wirespecification", "wirespec"),
			Map.entry("toposignalname", "signalname"),
			Map.entry("repositionable", "isrepositionable"),
			Map.entry("variantposition", "isvariantposition"),
			Map.entry("terminalmaterial", "terminalmaterialcode"),
			Map.entry("customerwirenumber", "custwire"),
			Map.entry("bypass", "isbypass"),
			Map.entry("signaltype", IXMLTags.FUNCTIONCONDUCTORTYPE),
			Map.entry("signalimplementationtype", IXMLTags.FUNCTIONCONDUCTORIMPLTYPE),
			Map.entry("maximumage", IXMLTags.MAXAGE),
			Map.entry("maximumlatency", IXMLTags.LATENCY));

	ObjectInfo(@NotNull String designUID, @NotNull ShareableEntityTypeEnum type,
			@NotNull Map<String, String> attributes)
	{
		m_designUID = designUID;
		m_uid = Objects.requireNonNull(attributes.get(IXMLTags.ID));
		m_type = type;
		m_attributes = new HashMap<>(attributes);
		m_properties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	}

	public boolean isNonShared()
	{
		return m_sharedUID == null;
	}

	@Nullable public String getSharedUID()
	{
		return m_sharedUID;
	}

	@NotNull public String getUID()
	{
		return m_uid;
	}

	@NotNull public String getDesignUID()
	{
		return m_designUID;
	}

	@NotNull public ShareableEntityTypeEnum getType()
	{
		return m_type;
	}

	@Nullable @Override public String getAttributeValue(@NotNull String attributeXMLName)
	{
		return m_attributes.get(m_attributesXMLNameMap.getOrDefault(attributeXMLName, attributeXMLName));
	}

	@Nullable @Override public String getPropertyValue(@NotNull String propertyName)
	{
		return m_properties.get(propertyName);
	}

	public void addProperty(@NotNull String propertyName, @NotNull String propertyValue)
	{
		m_properties.put(propertyName, propertyValue);
	}

	@Nullable @Override public IConnectivityInfo getConnectivityInfo()
	{
		return null;
	}

	@Override public void addCustomAttribute(@NotNull String attributeName, @NotNull String attributeValue)
	{
		m_attributes.put(attributeName, attributeValue);
	}

	@Override public int compareTo(@NotNull IObjectInfo o)
	{
		int result = getType().compareTo(o.getType());
		if (result == 0) {
			String name1 = getAttributeValue(IXMLTags.NAME);
			String name2 = o.getAttributeValue(IXMLTags.NAME);
			result = name1 != null && name2 != null ? AlphaNumComparator.compare(name1, name2, false) : 0;
			if (result == 0) {
				result = getDesignUID().compareTo(o.getDesignUID());
				if (result == 0) {
					result = getUID().compareTo(o.getUID());
				}
			}
		}
		return result;
	}
}
