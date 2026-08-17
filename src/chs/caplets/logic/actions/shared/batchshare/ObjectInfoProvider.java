/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.utilities.IXMLTags;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Parses set of designs without loading them into memory to get info of all connectivity objects
 */
class ObjectInfoProvider extends AbstractObjectInfoProvider
{

	@NotNull @Override protected Collection<IObjectInfo> collectObjectInfos(@NotNull InputSource inputSource,
			@NotNull Set<ShareableEntityTypeEnum> candidateTypes, @NotNull Predicate<IObjectInfo> objectInfoFilter)
	{
		Collection<IObjectInfo> objectsInfos = Collections.emptySet();
		try {
			if (m_parser != null) {
				ObjectInfoHandler handler = new ObjectInfoHandler(candidateTypes, objectInfoFilter);
				m_parser.parse(inputSource, handler);
				objectsInfos = handler.getObjectInfos();
			}
		}
		catch (IOException | SAXException ignore) {
		}
		return objectsInfos;
	}

	@Override @NotNull protected String getDesignTagName()
	{
		return IXMLTags.LOGICALDESIGN;
	}
}
