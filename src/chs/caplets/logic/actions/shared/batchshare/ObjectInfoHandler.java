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

import chs.cof.logical.cable.ConnectorTypeEnum;
import chs.utilities.CommonUtils;
import chs.utilities.IXMLTags;
import chs.utilities.sax.SAXUtils;
import chs.utility.helpers.SingleLineHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * SAX parser event handler for collecting object infos for given types
 */
class ObjectInfoHandler extends DefaultHandler
{

	@Nullable private String m_currentDesignUID;
	@NotNull private final Map<String, IObjectInfo> m_objectInfos = new HashMap<>();
	@NotNull private final Map<String, SingleLineInfo> singleLineMulticores = new HashMap<>();
	@NotNull private final Map<String, String> m_objectToSharedObjectUIDMap = new HashMap<>();
	@NotNull private final Map<String, IXMLElementHandler> m_xmlHandlerMap = new HashMap<>();
	@Nullable private IXMLElementHandler m_xmlHandler = null;
	@NotNull private final Predicate<IObjectInfo> m_objectFilter;

	ObjectInfoHandler(@NotNull Set<ShareableEntityTypeEnum> typesToBeHandled,
			@NotNull Predicate<IObjectInfo> objectFilter)
	{
		Set<ShareableEntityTypeEnum> candidateTypes = expandTypesToBeHandled(typesToBeHandled);
		for (ShareableEntityTypeEnum type : candidateTypes) {
			String xmlName = type.getXMLName();
			if (ShareableEntityTypeEnum.DEVICE.equals(type) || ShareableEntityTypeEnum.GROUND.equals(type) ||
					ShareableEntityTypeEnum.SPLICE.equals(type) ||
					ShareableEntityTypeEnum.PLUG.equals(type) || ShareableEntityTypeEnum.JACK.equals(type) ||
					ShareableEntityTypeEnum.INLINE.equals(type) || ShareableEntityTypeEnum.RING_TERMINAL.equals(type)) {
				m_xmlHandlerMap.putIfAbsent(xmlName, new PinlistXMLElementHandler());
			}
			else if (ShareableEntityTypeEnum.WIRE.equals(type) || ShareableEntityTypeEnum.NET.equals(type)) {
				m_xmlHandlerMap.putIfAbsent(xmlName, new ConductorXMLElementHandler());
			}
			else if (ShareableEntityTypeEnum.HIGHWAY.equals(type) || ShareableEntityTypeEnum.SINGLE_LINE.equals(type)) {
				m_xmlHandlerMap.putIfAbsent(xmlName, new HighwayXMLElementHandler());
			}
			else if (ShareableEntityTypeEnum.MULTICORE.equals(type) || ShareableEntityTypeEnum.OVERBRAID.equals(type)) {
				m_xmlHandlerMap.putIfAbsent(xmlName, new MulticoreXMLElementHandler());
			}
			else {
				throw new IllegalArgumentException();
			}
		}
		m_objectFilter = objectFilter;
	}

	@NotNull private Set<ShareableEntityTypeEnum> expandTypesToBeHandled(
			@NotNull Set<ShareableEntityTypeEnum> typesToBeHandled)
	{
		Set<ShareableEntityTypeEnum> extendedTypesToBeHandled = EnumSet.copyOf(typesToBeHandled);
		Set<ShareableEntityTypeEnum> conductorTypes =
				Set.of(ShareableEntityTypeEnum.MULTICORE, ShareableEntityTypeEnum.OVERBRAID,
						ShareableEntityTypeEnum.WIRE, ShareableEntityTypeEnum.NET);
		if (conductorTypes.stream().anyMatch(conductorType -> typesToBeHandled.contains(conductorType))) {
			extendedTypesToBeHandled.addAll(conductorTypes);
		}
		return extendedTypesToBeHandled;
	}

	@Override public void startElement(String uri, String localName, String qName, Attributes attributes)
	{
		String elementName = SAXUtils.getName(localName, qName);
		if (IXMLTags.LOGICALDESIGN.equals(elementName)) {
			m_currentDesignUID = attributes.getValue(IXMLTags.ID);
		}
		else if (IXMLTags.PIN.equals(elementName)) {
			String pinUID = Objects.requireNonNull(attributes.getValue(IXMLTags.ID));
			String sharedPinUID = attributes.getValue(IXMLTags.SHAREDPIN);
			if (sharedPinUID != null) {
				m_objectToSharedObjectUIDMap.put(pinUID, sharedPinUID);
			}
		}
		else if (IXMLTags.TERMINATION.equals(elementName)) {
			String pinUID = Objects.requireNonNull(attributes.getValue(IXMLTags.ID));
			String sharedPinUID = attributes.getValue(IXMLTags.SHAREDTERMINATION);
			if (sharedPinUID != null) {
				m_objectToSharedObjectUIDMap.put(pinUID, sharedPinUID);
			}
		}
		else if (IXMLTags.ASSEMBLYELEMENT.equals(elementName)) {
			String elementUID = Objects.requireNonNull(attributes.getValue(IXMLTags.REF));
			removeObjectInfo(elementUID);
		}
		if (m_xmlHandler == null) {
			m_xmlHandler = m_xmlHandlerMap.get(elementName);
		}

		if (m_xmlHandler != null && m_currentDesignUID != null) {
			m_xmlHandler.startElement(uri, localName, qName, attributes, m_currentDesignUID);
		}
	}

	private void removeObjectInfo(@Nullable String elementUID)
	{
		IObjectInfo removedInfo = m_objectInfos.remove(elementUID);
		if (removedInfo instanceof IMulticoreInfo) {
			IMulticoreInfo multicoreInfo = (IMulticoreInfo) removedInfo;
			for (String innercoreUID : multicoreInfo.getInnercoreUIDs()) {
				removeObjectInfo(innercoreUID);
			}
		}
		else if (removedInfo instanceof IInlineConnectorInfo) {
			IInlineConnectorInfo inlineConnectorInfo = (IInlineConnectorInfo) removedInfo;
			IInlineConnectorInfo matedConnector = inlineConnectorInfo.getMatedConnector();
			removeObjectInfo(matedConnector.getUID());
		}
	}

	@Override public void endElement(String uri, String localName, String qName)
	{
		String elementName = SAXUtils.getName(localName, qName);
		if (m_xmlHandler != null) {
			IObjectInfo objectInfo = m_xmlHandler.endElement(uri, localName, qName);
			if (m_xmlHandlerMap.containsKey(elementName)) {
				if (objectInfo != null) {
					if (objectInfo.getSharedUID() != null) {
						m_objectToSharedObjectUIDMap.put(objectInfo.getUID(), objectInfo.getSharedUID());
					}
					String refUID = objectInfo.isNonShared() ? objectInfo.getUID() : objectInfo.getSharedUID();
					if (m_objectInfos.containsKey(refUID)) {
						//update shared connectivity info
						IObjectInfo sharedObjectInfo = m_objectInfos.get(refUID);
						IConnectivityInfo sharedConnectivityInfo = sharedObjectInfo.getConnectivityInfo();
						IConnectivityInfo connectivityInfo = objectInfo.getConnectivityInfo();
						if (sharedConnectivityInfo instanceof IUpdateableConnectivityInfo && connectivityInfo != null) {
							connectivityInfo.getConnectedPinUIDs().forEach(connectedPinUID -> {
								((IUpdateableConnectivityInfo) sharedConnectivityInfo)
										.addPinConnection(connectedPinUID);
							});
						}
					}
					else if (m_objectFilter.test(objectInfo)) {
						handleSingleLineRelationInfo(objectInfo);
						m_objectInfos.put(refUID, objectInfo);
					}
				}
				m_xmlHandler.clear();
				m_xmlHandler = null;
			}
		}
		if (IXMLTags.LOGICALDESIGN.equals(elementName)) {
			m_currentDesignUID = null;
			m_objectToSharedObjectUIDMap.clear();
		}
	}

	private void handleSingleLineRelationInfo(IObjectInfo objectInfo)
	{
		if (objectInfo instanceof SingleLineInfo singleLineInfo) {
			singleLineInfo.getMulticoreUIDs()
					.forEach(multicoreUID -> singleLineMulticores.put(multicoreUID, singleLineInfo));
		}
		if(objectInfo instanceof MulticoreInfo multicoreInfo){
			SingleLineInfo singleLineInfo = singleLineMulticores.get(multicoreInfo.getUID());
			if(singleLineInfo != null){
				singleLineInfo.addMulticoreInfo(multicoreInfo);
			}
		}
	}

	@NotNull public Collection<IObjectInfo> getObjectInfos()
	{
		return m_objectInfos.values();
	}

	private class PinlistXMLElementHandler implements IXMLElementHandler
	{

		@Nullable private PinlistInfo m_pinlistInfo = null;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes,
				@NotNull String designUID)
		{
			String elementName = SAXUtils.getName(localName, qName);
			if (IXMLTags.DEVICE.equals(elementName)) {
				m_pinlistInfo = new PinlistInfo(designUID, ShareableEntityTypeEnum.DEVICE,
						IXMLElementHandler.getAttributesMap(attributes));
			}
			else if (IXMLTags.GROUNDDEVICE.equals(elementName)) {
				m_pinlistInfo = new PinlistInfo(designUID, ShareableEntityTypeEnum.GROUND,
						IXMLElementHandler.getAttributesMap(attributes));
			}
			else if (IXMLTags.SPLICE.equals(elementName)) {
				m_pinlistInfo = new PinlistInfo(designUID, ShareableEntityTypeEnum.SPLICE,
						IXMLElementHandler.getAttributesMap(attributes));
			}
			else if (IXMLTags.CONNECTOR.equals(elementName)) {
				String occupiedPosition = attributes.getValue(IXMLTags.OCCUPIEDPOSITION);
				if (occupiedPosition == null) {
					String connectorUsageType = attributes.getValue(IXMLTags.CONNECTORUSAGE);
					if (ConnectorTypeEnum.TYPE_RINGTERMINAL.toString().equals(connectorUsageType)) {
						m_pinlistInfo = new PinlistInfo(designUID, ShareableEntityTypeEnum.RING_TERMINAL,
								IXMLElementHandler.getAttributesMap(attributes));
					}
					else if (ConnectorTypeEnum.TYPE_CONNECTOR.toString().equals(connectorUsageType)) {
						String connectorType = attributes.getValue(IXMLTags.TYPE);
						if (IXMLTags.PLUG.equals(connectorType)) {
							m_pinlistInfo = new PinlistInfo(designUID, ShareableEntityTypeEnum.PLUG,
									IXMLElementHandler.getAttributesMap(attributes));
						}
						else if (IXMLTags.JACK.equals(connectorType)) {
							m_pinlistInfo = new PinlistInfo(designUID, ShareableEntityTypeEnum.JACK,
									IXMLElementHandler.getAttributesMap(attributes));
						}
						else if (IXMLTags.INLINEPLUG.equals(connectorType)) {
							m_pinlistInfo = new InlineConnectorInfo(designUID, false,
									IXMLElementHandler.getAttributesMap(attributes));
						}
						else if (IXMLTags.INLINEJACK.equals(connectorType)) {
							m_pinlistInfo = new InlineConnectorInfo(designUID, true,
									IXMLElementHandler.getAttributesMap(attributes));
						}
					}
				}
			}
			else if (IXMLTags.PROPERTY.equals(elementName) && m_pinlistInfo != null) {
				String propertyName = Objects.requireNonNull(attributes.getValue(IXMLTags.NAME));
				String propertyValue = Objects.requireNonNull(attributes.getValue(IXMLTags.VAL));
				m_pinlistInfo.addProperty(propertyName, propertyValue);
			}
			else if (IXMLTags.REFMATE.equals(elementName) && m_pinlistInfo instanceof IInlineConnectorInfo) {
				String mateConnectorUID = Objects.requireNonNull(attributes.getValue(IXMLTags.MATEREF));
				String refUID = m_objectToSharedObjectUIDMap.getOrDefault(mateConnectorUID, mateConnectorUID);
				IInlineConnectorInfo mateConnector =
						CommonUtils.cast(m_objectInfos.get(refUID), IInlineConnectorInfo.class);
				if (mateConnector != null) {
					((IInlineConnectorInfo) m_pinlistInfo).setMatedConnector(mateConnector);
				}
			}
			else if (IXMLTags.INTERNALPOSITION.equals(elementName)) {
				m_pinlistInfo = null;
			}
		}

		@Override
		@Nullable public IObjectInfo endElement(String uri, String localName, String qName)
		{
			String elementName = SAXUtils.getName(localName, qName);
			if (IXMLTags.DEVICE.equals(elementName) || IXMLTags.GROUNDDEVICE.equals(elementName) ||
					IXMLTags.SPLICE.equals(elementName) || IXMLTags.CONNECTOR.equals(elementName)) {
				return m_pinlistInfo;
			}
			return null;
		}

		@Override
		public void clear()
		{
			m_pinlistInfo = null;
		}
	}

	private class ConductorXMLElementHandler implements IXMLElementHandler
	{

		@Nullable private ConductorInfo m_conductorInfo = null;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes,
				@NotNull String designUID)
		{
			String elementName = SAXUtils.getName(localName, qName);
			if (IXMLTags.NET.equals(elementName)) {
				m_conductorInfo = new ConductorInfo(designUID, ShareableEntityTypeEnum.NET,
						IXMLElementHandler.getAttributesMap(attributes));
			}
			else if (IXMLTags.WIRE.equals(elementName)) {
				m_conductorInfo = new ConductorInfo(designUID, ShareableEntityTypeEnum.WIRE,
						IXMLElementHandler.getAttributesMap(attributes));
			}
			else if (IXMLTags.PROPERTY.equals(elementName) && m_conductorInfo != null) {
				String propertyName = Objects.requireNonNull(attributes.getValue(IXMLTags.NAME));
				String propertyValue = Objects.requireNonNull(attributes.getValue(IXMLTags.VAL));
				m_conductorInfo.addProperty(propertyName, propertyValue);
			}
			else if (IXMLTags.CONNECTION.equals(elementName) && m_conductorInfo != null) {
				String connectedPinUID = Objects.requireNonNull(attributes.getValue(IXMLTags.PINREF));
				connectedPinUID = m_objectToSharedObjectUIDMap.getOrDefault(connectedPinUID, connectedPinUID);
				m_conductorInfo.addConnection(connectedPinUID);
			}
		}

		@Override
		@Nullable public IObjectInfo endElement(String uri, String localName, String qName)
		{
			String elementName = SAXUtils.getName(localName, qName);
			if (IXMLTags.NET.equals(elementName)
					|| IXMLTags.WIRE.equals(elementName)
					|| IXMLTags.SHIELD.equals(elementName)) {
				return Objects.requireNonNull(m_conductorInfo);
			}
			return null;
		}

		@Override
		public void clear()
		{
			m_conductorInfo = null;
		}
	}

	private static class HighwayXMLElementHandler implements IXMLElementHandler
	{

		@Nullable private HighwayInfo m_highwayInfo = null;
		@Nullable private SingleLineXMLElementHandler singleLineXMLElementHandler;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes,
				@NotNull String designUID)
		{
			String elementName = SAXUtils.getName(localName, qName);
			if(SingleLineHelper.isSingleLine(attributes)){
				singleLineXMLElementHandler = new SingleLineXMLElementHandler();
			}
			if(singleLineXMLElementHandler != null) {
				//delegate to Single Line handler
				singleLineXMLElementHandler.startElement(uri, localName, qName, attributes, designUID);
			}
			else if (IXMLTags.HIGHWAY.equals(elementName)) {
				m_highwayInfo = new HighwayInfo(designUID, IXMLElementHandler.getAttributesMap(attributes));
			}
			else if (IXMLTags.PROPERTY.equals(elementName) && m_highwayInfo != null) {
				String propertyName = Objects.requireNonNull(attributes.getValue(IXMLTags.NAME));
				String propertyValue = Objects.requireNonNull(attributes.getValue(IXMLTags.VAL));
				m_highwayInfo.addProperty(propertyName, propertyValue);
			}
		}

		@Override
		@Nullable public IObjectInfo endElement(String uri, String localName, String qName)
		{
			if(singleLineXMLElementHandler != null){
				//delegate to Single Line handler
				return singleLineXMLElementHandler.endElement(uri, localName, qName);
			}
			String elementName = SAXUtils.getName(localName, qName);
			if (IXMLTags.HIGHWAY.equals(elementName)) {
				return Objects.requireNonNull(m_highwayInfo);
			}
			return null;
		}

		@Override
		public void clear()
		{
			if(singleLineXMLElementHandler != null){
				//delegate to Single Line handler
				singleLineXMLElementHandler.clear();
				singleLineXMLElementHandler = null;
			}
			else {
				m_highwayInfo = null;
			}
		}
	}

	private static class SingleLineXMLElementHandler implements IXMLElementHandler
	{

		@Nullable private SingleLineInfo singleLineInfo = null;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes,
				@NotNull String designUID)
		{
			String elementName = SAXUtils.getName(localName, qName);
			if (IXMLTags.HIGHWAY.equals(elementName)) {
				singleLineInfo = new SingleLineInfo(designUID, IXMLElementHandler.getAttributesMap(attributes));
			}
			else if (IXMLTags.SINGLELINEMULTICOREMEMBER.equals(elementName) && singleLineInfo != null) {
				singleLineInfo.addMulticore(attributes.getValue(IXMLTags.REF));
			}
		}

		@Override
		@Nullable public IObjectInfo endElement(String uri, String localName, String qName)
		{
			String elementName = SAXUtils.getName(localName, qName);
			if (IXMLTags.HIGHWAY.equals(elementName)) {
				return Objects.requireNonNull(singleLineInfo);
			}
			return null;
		}

		@Override
		public void clear()
		{
			singleLineInfo = null;
		}
	}

	private static class MulticoreXMLElementHandler implements IXMLElementHandler
	{

		@Nullable private MulticoreInfo m_multicoreInfo = null;

		@Override public void startElement(String uri, String localName, String qName, Attributes attributes,
				@NotNull String designUID)
		{
			String elementName = SAXUtils.getName(localName, qName);
			if (IXMLTags.MULTICORE.equals(elementName)) {
				boolean isOverbraid = Boolean.parseBoolean(attributes.getValue(IXMLTags.ISOVERBRAID));
				m_multicoreInfo = new MulticoreInfo(designUID,
						isOverbraid ? ShareableEntityTypeEnum.OVERBRAID : ShareableEntityTypeEnum.MULTICORE,
						IXMLElementHandler.getAttributesMap(attributes));
			}
			else if (IXMLTags.MEMBER.equals(elementName) && m_multicoreInfo != null) {
				m_multicoreInfo.addInnercore(attributes.getValue(IXMLTags.REF));
			}
			else if (IXMLTags.PROPERTY.equals(elementName) && m_multicoreInfo != null) {
				String propertyName = Objects.requireNonNull(attributes.getValue(IXMLTags.NAME));
				String propertyValue = Objects.requireNonNull(attributes.getValue(IXMLTags.VAL));
				m_multicoreInfo.addProperty(propertyName, propertyValue);
			}
		}

		@Nullable @Override public IObjectInfo endElement(String uri, String localName, String qName)
		{
			String elementName = SAXUtils.getName(localName, qName);
			if (IXMLTags.MULTICORE.equals(elementName)) {
				return Objects.requireNonNull(m_multicoreInfo);
			}
			return null;
		}

		@Override public void clear()
		{
			m_multicoreInfo = null;
		}
	}
}
