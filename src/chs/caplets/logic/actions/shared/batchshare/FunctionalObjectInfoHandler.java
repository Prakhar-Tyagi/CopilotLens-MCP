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
import chs.utilities.sax.SAXUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Extended SAX parser event handler for collecting object infos for functional objects
 */
public class FunctionalObjectInfoHandler extends DefaultHandler
{

	@NotNull private final Set<IObjectInfo> objectInfos = new HashSet<>();
	@Nullable private String currentDesignUID;
	@NotNull protected final Map<String, IXMLElementHandler> xmlHandlerMap = new HashMap<>();
	@Nullable private IXMLElementHandler xmlHandler = null;
	@NotNull private final Predicate<IObjectInfo> objectFilter;

	public FunctionalObjectInfoHandler(@NotNull Set<ShareableEntityTypeEnum> typesToBeHandled,
			@NotNull Predicate<IObjectInfo> objectFilter)
	{
		mapXMLHandlers(typesToBeHandled);
		this.objectFilter = objectFilter;
	}

	/**
	 * Maps XML handler specific for a shareable entity type
	 *
	 * @param typesToBeHandled collection of shareable entity types
	 */
	private void mapXMLHandlers(@NotNull Set<ShareableEntityTypeEnum> typesToBeHandled)
	{
		for (ShareableEntityTypeEnum type : typesToBeHandled) {
			String xmlName = type.getXMLName();
			if (IXMLTags.FUNCTIONMESSAGE.equals(xmlName)) {
				xmlHandlerMap.putIfAbsent(IXMLTags.FUNCTIONMESSAGE, new MessageXMLElementHandler());
			}
			else if (IXMLTags.FUNCTIONCONDUCTOR.equals(xmlName)) {
				xmlHandlerMap.putIfAbsent(IXMLTags.FUNCTIONCONDUCTOR, new SignalXMLElementHandler());
			}
			else {
				throw new IllegalArgumentException();
			}
		}
	}

	@Override public void startElement(String uri, String localName, String qName, Attributes attributes)
	{
		String elementName = SAXUtils.getName(localName, qName);
		if (IXMLTags.FUNCTIONDESIGN.equals(elementName)) {
			currentDesignUID = attributes.getValue(IXMLTags.ID);
		}

		if (xmlHandler == null) {
			xmlHandler = xmlHandlerMap.get(elementName);
		}

		if (xmlHandler != null && currentDesignUID != null) {
			xmlHandler.startElement(uri, localName, qName, attributes, currentDesignUID);
		}
	}

	@Override public void endElement(String uri, String localName, String qName)
	{
		String elementName = SAXUtils.getName(localName, qName);
		if (xmlHandler != null) {
			IObjectInfo objectInfo = xmlHandler.endElement(uri, localName, qName);
			if (xmlHandlerMap.containsKey(elementName)) {
				if (objectInfo != null && objectFilter.test(objectInfo)) {
					objectInfos.add(objectInfo);
				}

				xmlHandler.clear();
				xmlHandler = null;
			}
		}
		if (IXMLTags.FUNCTIONDESIGN.equals(elementName)) {
			currentDesignUID = null;
		}
	}

	@NotNull public Collection<IObjectInfo> getObjectInfos()
	{
		return objectInfos;
	}

	/**
	 * XML element handler for collecting function message object infos
	 */
	private static class MessageXMLElementHandler implements IXMLElementHandler
	{

		@Nullable private ConductorInfo messageInfo = null;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes,
				@NotNull String designUID)
		{
			String elementName = SAXUtils.getName(localName, qName);
			if (IXMLTags.FUNCTIONMESSAGE.equals(elementName)) {
				messageInfo = new ConductorInfo(designUID, ShareableEntityTypeEnum.FUNCTION_MESSAGE,
						IXMLElementHandler.getAttributesMap(attributes));
			}
			else if (IXMLTags.PROPERTY.equals(elementName) && messageInfo != null) {
				String propertyName = Objects.requireNonNull(attributes.getValue(IXMLTags.NAME));
				String propertyValue = Objects.requireNonNull(attributes.getValue(IXMLTags.VAL));
				messageInfo.addProperty(propertyName, propertyValue);
			}
			else if (IXMLTags.CUSTOMATTRIBUTE.equals(elementName) && messageInfo != null) {
				String attributeName = attributes.getValue(IXMLTags.NAME);
				String attributeValue = attributes.getValue(IXMLTags.VALUE);
				messageInfo.addCustomAttribute(attributeName, attributeValue);
			}
			else if (IXMLTags.CONNECTION.equals(elementName) && messageInfo != null) {
				String connectedPinUID = Objects.requireNonNull(attributes.getValue(IXMLTags.PINREF));
				messageInfo.addConnection(connectedPinUID);
			}
		}

		@Override
		@Nullable public IObjectInfo endElement(String uri, String localName, String qName)
		{
			return Objects.requireNonNull(messageInfo);
		}

		@Override
		public void clear()
		{
			messageInfo = null;
		}
	}

	/**
	 * XML element handler for collecting function signal object infos
	 */
	private static class SignalXMLElementHandler implements IXMLElementHandler
	{

		@Nullable private ConductorInfo signalInfo = null;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes,
				@NotNull String designUID)
		{
			String elementName = SAXUtils.getName(localName, qName);
			if (IXMLTags.FUNCTIONCONDUCTOR.equals(elementName)) {
				signalInfo = new ConductorInfo(designUID, ShareableEntityTypeEnum.FUNCTION_SIGNAL,
						IXMLElementHandler.getAttributesMap(attributes));
			}
			else if (IXMLTags.PROPERTY.equals(elementName) && signalInfo != null) {
				String propertyName = Objects.requireNonNull(attributes.getValue(IXMLTags.NAME));
				String propertyValue = Objects.requireNonNull(attributes.getValue(IXMLTags.VAL));
				signalInfo.addProperty(propertyName, propertyValue);
			}
			else if (IXMLTags.CUSTOMATTRIBUTE.equals(elementName) && signalInfo != null) {
				String attributeName = attributes.getValue(IXMLTags.NAME);
				String attributeValue = attributes.getValue(IXMLTags.VALUE);
				signalInfo.addCustomAttribute(attributeName, attributeValue);
			}
			else if (IXMLTags.CONNECTION.equals(elementName) && signalInfo != null) {
				String connectedPinUID = Objects.requireNonNull(attributes.getValue(IXMLTags.PINREF));
				signalInfo.addConnection(connectedPinUID);
			}
		}

		@Override
		@Nullable public IObjectInfo endElement(String uri, String localName, String qName)
		{
			return Objects.requireNonNull(signalInfo);
		}

		@Override
		public void clear()
		{
			signalInfo = null;
		}
	}
}