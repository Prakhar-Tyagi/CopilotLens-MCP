/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.capitalmanager.appserver.IUserSession;
import chs.cof.logical.ILogicDesign;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utilities.sax.CHSXMLSAXParser;
import chs.utility.persist.DataRequestHelper;
import com.mentor.capital.xml.SAXParserService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Abstract object info provider
 */
public abstract class AbstractObjectInfoProvider implements IObjectInfoProvider
{

	@Nullable protected SAXParser m_parser;

	protected AbstractObjectInfoProvider()
	{
		m_parser = getSaxParser();
	}

	@Override @NotNull public Collection<IObjectInfo> getObjectInfos(@NotNull Set<ILogicDesign> designs,
			@NotNull Set<ShareableEntityTypeEnum> candidateTypes, @NotNull Predicate<IObjectInfo> objectInfoFilter)
	{
		if (designs.isEmpty() || candidateTypes.isEmpty()) {
			return Collections.emptySet();
		}
		final IUserSession userSession = FactoryMgr.getSystemFactory().getCHSSystem().getUserSession();
		Reader realData = null;
		try {
			Set<IUID> designUIDs = designs.stream().map(IUIDObject::getUID).collect(Collectors.toSet());
			realData = loadConnectivity(designUIDs, userSession);
			Collection<IObjectInfo> objectInfos = Collections.emptySet();
			if (realData != null) {
				objectInfos = collectObjectInfos(new InputSource(realData), candidateTypes, objectInfoFilter);
			}
			return objectInfos;
		}
		finally {
			if (realData != null) {
				try {
					realData.close();
				}
				catch (IOException ignore) {
				}
			}
		}
	}

	/**
	 * Provides information of objects present in the designs without loading them into memory
	 *
	 * @param inputSource      loaded connectivity data
	 * @param candidateTypes   shareable entity types
	 * @param objectInfoFilter filter
	 * @return collection of object information
	 */
	@NotNull protected abstract Collection<IObjectInfo> collectObjectInfos(@NotNull InputSource inputSource,
			@NotNull Set<ShareableEntityTypeEnum> candidateTypes, @NotNull Predicate<IObjectInfo> objectInfoFilter);

	@Nullable protected SAXParser getSaxParser()
	{
		SAXParser parser = null;
		try {
			SAXParserFactory factory = SAXParserService.INSTANCE.newSAXParserFactoryXXEAndExternalTDDisabled();
			parser = new CHSXMLSAXParser(factory);
		}
		catch (ParserConfigurationException ignore) {
		}
		catch (SAXNotSupportedException ignore) {
		}
		catch (SAXNotRecognizedException ignore) {
		}
		catch (SAXException ignore) {
		}
		return parser;
	}

	@Nullable protected Reader loadConnectivity(@NotNull Set<IUID> designUIDs, @Nullable IUserSession userSession)
	{
		String szRequest = DataRequestHelper.createDesignsConnectivityRequestBatch(getDesignTagName(), designUIDs);
		Reader szResponse = DataRequestHelper.sendRequest(userSession, szRequest);
		return szResponse;
	}

	/**
	 * Provides design tag name to be queried to load connectivity
	 *
	 * @return design tag name to be queried
	 */
	@NotNull protected abstract String getDesignTagName();
}
