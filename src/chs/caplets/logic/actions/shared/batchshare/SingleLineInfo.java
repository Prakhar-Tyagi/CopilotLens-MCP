/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.utilities.IXMLTags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents Single Line object
 */
public class SingleLineInfo extends ObjectInfo
{

	@NotNull private Set<String> multicoreUIDs = new HashSet<>();
	@Nullable private MulticoreInfo multicoreInfo;

	SingleLineInfo(@NotNull String designUID,@NotNull Map<String, String> attributes)
	{
		super(designUID, ShareableEntityTypeEnum.SINGLE_LINE, attributes);
		m_sharedUID = attributes.get(IXMLTags.SHAREDHIGHWAYID);
	}

	@NotNull public Set<String> getMulticoreUIDs()
	{
		return Collections.unmodifiableSet(multicoreUIDs);
	}

	public void addMulticore(@NotNull String multicoreUID)
	{
		multicoreUIDs.add(multicoreUID);
	}

	public void addMulticoreInfo(@NotNull MulticoreInfo info)
	{
		multicoreInfo = info;
	}

	@Nullable @Override public String getPropertyValue(@NotNull String propertyName)
	{
		if (multicoreInfo != null) {
			return multicoreInfo.getPropertyValue(propertyName);
		}
		return super.getPropertyValue(propertyName);
	}

	@Nullable @Override public String getAttributeValue(@NotNull String attributeXMLName)
	{
		if (multicoreInfo != null) {
			return multicoreInfo.getAttributeValue(attributeXMLName);
		}
		return super.getAttributeValue(attributeXMLName);
	}
}
