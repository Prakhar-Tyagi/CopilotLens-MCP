/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * XML element handler for collecting object infos
 */
public interface IXMLElementHandler
{

	@NotNull static Map<String, String> getAttributesMap(@Nullable Attributes attributes)
	{
		if (attributes == null || attributes.getLength() == 0) {
			return Collections.emptyMap();
		}
		Map<String, String> attributesMap = new HashMap<>();
		for (int i = 0; i < attributes.getLength(); i++) {
			String attrName = attributes.getQName(i);
			String attrValue = attributes.getValue(attrName);
			attributesMap.put(attrName, attrValue);
		}
		return attributesMap;
	}

	void startElement(String uri, String localName, String qName, Attributes attributes, @NotNull String designUID);

	@Nullable IObjectInfo endElement(String uri, String localName, String qName);

	void clear();
}
