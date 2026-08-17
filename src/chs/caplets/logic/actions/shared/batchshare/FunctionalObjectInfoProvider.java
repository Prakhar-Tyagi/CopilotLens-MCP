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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Object info provider for function design objects
 */
class FunctionalObjectInfoProvider extends AbstractObjectInfoProvider
{

	@NotNull @Override protected Collection<IObjectInfo> collectObjectInfos(@NotNull InputSource inputSource,
			@NotNull Set<ShareableEntityTypeEnum> candidateTypes, @NotNull Predicate<IObjectInfo> objectInfoFilter)
	{
		Collection<IObjectInfo> objectsInfos = Collections.emptySet();
		try {
			if (m_parser != null) {
				FunctionalObjectInfoHandler handler = new FunctionalObjectInfoHandler(candidateTypes, objectInfoFilter);
				m_parser.parse(inputSource, handler);
				objectsInfos = handler.getObjectInfos();
			}
		}
		catch (IOException | SAXException ignore) {
		}
		return objectsInfos;
	}

	@NotNull @Override protected String getDesignTagName()
	{
		return IXMLTags.FUNCTIONDESIGN;
	}
}