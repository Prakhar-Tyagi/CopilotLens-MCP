/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAnalysableSymbolAssociatable;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedConnectorPin;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedDeviceConnector;
import chs.cof.logical.shared.ISharedDeviceConnectorPin;
import chs.cof.logical.shared.ISharedDevicePin;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.symbol.ISymbolDef;
import chs.common.IAnalysable;
import chs.common.IAnalysablePin;
import chs.common.IAttributePropertyProvider;
import chs.common.IDesignObject;
import chs.common.IProperty;
import chs.common.IReadOnlyNamedObject;
import chs.common.IReadOnlyRevisionedObject;
import chs.common.attr.AttributeType;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeTypes;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utility.attr.AttributeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * creates snapshot object
 */
public class AttrPropExtractor
{

	public static final String ANALYSIS_INTERFACE = "Analysis:Interface";
	public static final String ANALYSIS_FAILURES = "Analysis:Failures";
	public static final String ANALYSABLE_SYMBOL = "Analysis Symbol";
	public static final String ANALYSIS_PIN_MAPPING = "Analysis: Pin Mapping";

	public static final Map<String, String> attributeDisplayNameMap = new HashMap<>();
	public static final Map<String, String> propertyDisplayNameMap = new HashMap<>();

	private AttrPropExtractor()
	{
	}

	@NotNull public static ICachedObject extractObjectInfo(@Nullable ICachedObject parent,
			@NotNull IAttributePropertyProvider obj)
	{
		String parentUID = null;
		IDesignObject designObject = CommonUtils.cast(obj, IDesignObject.class);
		if (designObject != null && designObject.getDesignContainer() != null) {
			parentUID = designObject.getDesignContainer().getUID().getString();
		}
		String name = getName(obj);
		ICachedObject cachedObject = getCachedObject(parent, obj, parentUID, name);
		collectAttributes(obj, name, cachedObject);
		extractProperties(obj, cachedObject);
		extractSharedObjectDetails(obj, cachedObject);
		extractChildren(obj, cachedObject);
		return cachedObject;
	}

	@NotNull private static String getName(@NotNull IAttributePropertyProvider obj)
	{
		if (obj instanceof IDevicePin) {
			return Objects.requireNonNull(((IReadOnlyNamedObject) obj).getName());
		}
		return Objects.requireNonNull(Objects.requireNonNull(obj.getAttribute(IAttributeTypes.NAME)).getAsString());
	}

	@NotNull static ICachedObject getCachedObject(@Nullable ICachedObject parent,
			@NotNull IAttributePropertyProvider obj, @Nullable String parentUID, @NotNull String name)
	{
		if (obj instanceof IDevice || obj instanceof ISharedDevice) {
			return new DeviceSnapShotObject(parent, name,
					obj.getUID().getString(), parentUID, COFTypeEnum.from_object(obj));
		}
		if (obj instanceof ISharedPin) {
			return new CachedObject(parent, name,
					obj.getUID().getString(), parentUID, COFTypeEnum.Pin);
		}
		if (obj instanceof ISharedDeviceConnector) {
			return new CachedObject(parent, name,
					obj.getUID().getString(), parentUID, COFTypeEnum.DeviceConnector);
		}
		if (obj instanceof IConnector || obj instanceof ISharedConnector) {
			return new ConnectorSnapShot(parent, name,
					obj.getUID().getString(), parentUID, COFTypeEnum.from_object(obj));
		}
		return new CachedObject(parent, name,
				obj.getUID().getString(), parentUID, COFTypeEnum.from_object(obj));
	}

	private static void extractChildren(@NotNull IAttributePropertyProvider obj, @NotNull ICachedObject cachedObject)
	{
		if (obj instanceof IDeviceConnector || obj instanceof ISharedDeviceConnector) {
			return;
		}

		extractPinlistChildren(obj, cachedObject);
		extractSharedPinListInfo(obj, cachedObject);
		extractMultiCoreInfo(obj, cachedObject);
		extractSharedMulticoreInfo(obj, cachedObject);
	}

	private static void extractSharedMulticoreInfo(@NotNull IAttributePropertyProvider obj,
			@NotNull ICachedObject cachedObject)
	{
		ISharedMulticore sharedMC = CommonUtils.cast(obj, ISharedMulticore.class);
		if (sharedMC != null) {
			for (ISharedConductor cond : sharedMC.getConductors()) {
				ICachedObject pinObject = extractObjectInfo(cachedObject, cond);
				cachedObject.addChild(pinObject);
			}
			for (ISharedMulticore cond : sharedMC.getMulticores()) {
				ICachedObject pinObject = extractObjectInfo(cachedObject, cond);
				cachedObject.addChild(pinObject);
			}
			ISharedConductor shield = sharedMC.getShield();
			if (shield != null) {
				cachedObject.addChild(extractObjectInfo(cachedObject, shield));
			}
		}
	}

	private static void extractMultiCoreInfo(@NotNull IAttributePropertyProvider obj,
			@NotNull ICachedObject cachedObject)
	{
		IMulticore mc = CommonUtils.cast(obj, IMulticore.class);
		if (mc != null) {
			for (IConductor cond : mc.getConductors()) {
				ICachedObject pinObject = extractObjectInfo(cachedObject, cond);
				cachedObject.addChild(pinObject);
			}
			for (IMulticore cond : mc.getMulticores()) {
				ICachedObject pinObject = extractObjectInfo(cachedObject, cond);
				cachedObject.addChild(pinObject);
			}
			IShieldConductor shield = mc.getShield();
			if (shield != null) {
				cachedObject.addChild(extractObjectInfo(cachedObject, shield));
			}
		}
	}

	private static void extractSharedPinListInfo(@NotNull IAttributePropertyProvider obj,
			@NotNull ICachedObject cachedObject)
	{
		ISharedPinList sharedPinList = CommonUtils.cast(obj, ISharedPinList.class);
		if (sharedPinList == null) {
			return;
		}
		for (ISharedPin pin : sharedPinList.getPins()) {
			ICachedObject pinObject = extractObjectInfo(cachedObject, pin);
			cachedObject.addChild(pinObject);
		}
		extractAdditionalInfoForSharedDevice(obj, cachedObject);
		extractAdditionalInfoForSharedConnector(obj, cachedObject);
	}

	private static void extractAdditionalInfoForSharedDevice(
			@NotNull IAttributePropertyProvider obj,
			@NotNull ICachedObject cachedObject)
	{
		ISharedDevice sharedDevice = CommonUtils.cast(obj, ISharedDevice.class);
		if (sharedDevice == null) {
			return;
		}
		for (ISharedPin pin : sharedDevice.getPins()) {
			ISharedDevicePin devPin = (ISharedDevicePin) pin;
			((IDeviceSnapShotObject) cachedObject).addDevicePinUID(devPin.getUID().getString());
			ISharedPin matePin = devPin.getMatePin();
			if (matePin instanceof ISharedDeviceConnectorPin) {
				((IDeviceSnapShotObject) cachedObject)
						.addPinToDeviceConnectMapping(devPin.getUID().getString(), Objects
								.requireNonNull(matePin.getOwner()).getUID().getString());
			}
		}
		for (ISharedDeviceConnector dc : ((ISharedDevice) obj).getDeviceConnectors()) {
			cachedObject.addChild(extractObjectInfo(cachedObject, dc));
		}
	}

	private static void extractAdditionalInfoForSharedConnector(
			@NotNull IAttributePropertyProvider obj,
			@NotNull ICachedObject cachedObject)
	{
		ISharedConnector sharedConnector = CommonUtils.cast(obj, ISharedConnector.class);
		if (sharedConnector == null) {
			return;
		}
		for (ISharedPin pin : sharedConnector.getPins()) {
			ISharedConnectorPin devPin = (ISharedConnectorPin) pin;
			((IConnectorSnapShot) cachedObject).addConnectorPinUID(devPin.getUID().getString());
		}
		ISharedBackshell backshell = sharedConnector.getBackshell();
		if (backshell != null) {
			((IConnectorSnapShot) cachedObject).addBackShellUID(backshell.getUID().getString());
			cachedObject.addChild(extractObjectInfo(cachedObject, backshell));
		}
	}



	private static void extractPinlistChildren(@NotNull IAttributePropertyProvider obj,
			@NotNull ICachedObject cachedObject)
	{
		IPinList pinList = CommonUtils.cast(obj, IPinList.class);
		if (pinList == null) {
			return;
		}
		for (IAbstractPin pin : pinList.getPins()) {
			cachedObject.addChild(extractObjectInfo(cachedObject, pin));
		}
		extractAdditionInfoForDevice(obj, cachedObject);
		extractAdditionInfoForConnector(obj, cachedObject);
	}

	private static void extractAdditionInfoForConnector(@NotNull IAttributePropertyProvider obj,
			@NotNull ICachedObject cachedObject)
	{
		IConnector connector = CommonUtils.cast(obj, IConnector.class);
		if (connector == null || !(cachedObject instanceof IConnectorSnapShot)) {
			return;
		}
		if (connector.getBackshell() != null) {
			cachedObject.addChild(extractObjectInfo(cachedObject, connector.getBackshell()));
			((IConnectorSnapShot) cachedObject).addBackShellUID(connector.getBackshell().getUID().getString());
		}
		connector.getPins()
				.forEach(pin -> ((IConnectorSnapShot) cachedObject).addConnectorPinUID(pin.getUID().getString()));
	}

	private static void extractAdditionInfoForDevice(@NotNull IAttributePropertyProvider obj,
			@NotNull ICachedObject cachedObject)
	{
		IDevice device = CommonUtils.cast(obj, IDevice.class);
		if (device == null) {
			return;
		}
		for (IAbstractPin pin : device.getPins()) {
			IDevicePin devPin = (IDevicePin) pin;
			((IDeviceSnapShotObject) cachedObject).addDevicePinUID(devPin.getUID().getString());
			IDeviceConnPin deviceConnectorPin = devPin.getDeviceConnectorPin();
			if (deviceConnectorPin != null) {
				((IDeviceSnapShotObject) cachedObject)
						.addPinToDeviceConnectMapping(devPin.getUID().getString(), Objects
								.requireNonNull(deviceConnectorPin.getOwner()).getUID().getString());
			}
		}
		for (IDeviceConnector dc : ((IDevice) obj).getDeviceConnectors()) {
			ICachedObject pinObject = extractObjectInfo(cachedObject, dc);
			cachedObject.addChild(pinObject);
		}
	}

	private static void extractSharedObjectDetails(@NotNull IAttributePropertyProvider obj,
			ICachedObject cachedObject)
	{
		ISharedObject sharedObject = null;
		if (obj instanceof ILogicObject) {
			sharedObject = ((ILogicObject) obj).getSharedObject();
		}
		else if (obj instanceof ISharedObject) {
			sharedObject = (ISharedObject) obj;
		}
		if (sharedObject != null) {
			cachedObject.setIsSharedObject(true);
			if (obj instanceof IRevisionedSharedObject) {
				cachedObject.getAttributes()
						.put(IAttributeTypes.SHARED_OBJECT_REVISION, ((IReadOnlyRevisionedObject) obj).getRevision());
			}
		}
	}

	private static void extractProperties(@NotNull IAttributePropertyProvider obj,
			@NotNull ICachedObject cachedObject)
	{
		for (IProperty property : obj.getProperties()) {
			cachedObject.getProperties().put(property.getName(), property.getAsString());
			propertyDisplayNameMap.put(property.getName(), property.getDisplayName());
		}
	}

	private static void collectAttributes(@NotNull IAttributePropertyProvider obj, String name,
			@NotNull ICachedObject cachedObject)
	{
		for (IAttribute attribute : AttributeUtils.getUserVisibleAttributes(obj)) {
			if (attribute != null) {
				cachedObject.getAttributes().put(attribute.getName(), attribute.getAsString());
				attributeDisplayNameMap.put(attribute.getName(), attribute.getDisplayName());
			}
		}
		cachedObject.getAttributes().put(IAttributeTypes.NAME, name);
		if (obj instanceof ISharedDevicePin) {
			ISharedDeviceConnectorPin matePin =
					CommonUtils.cast(((ISharedPin) obj).getMatePin(), ISharedDeviceConnectorPin.class);
			if (matePin != null) {
				cachedObject.getAttributes().put(IAttributeTypes.DEVICE_CONNECTOR_PIN, matePin.getName());
				attributeDisplayNameMap.putIfAbsent(IAttributeTypes.DEVICE_CONNECTOR_PIN,
						ResourceMgr.getString(AttributeType.class,
								"AttributeType." + IAttributeTypes.DEVICE_CONNECTOR_PIN));
			}
		}
		extractAnalysisAttributes(obj, cachedObject);
	}

	private static void extractAnalysisAttributes(IAttributePropertyProvider obj, ICachedObject cachedObject)
	{
		if (obj instanceof IAnalysable) {
			cachedObject.getAttributes().put(ANALYSIS_INTERFACE, ((IAnalysable) obj).getOverriddenAnalysisInterfaces());
			cachedObject.getAttributes()
					.put(ANALYSIS_FAILURES, ((IAnalysable) obj).getOverriddenAnalysisFailureModes());
		}
		if (obj instanceof IAnalysableSymbolAssociatable) {
			ISymbolDef analysableSymbol = ((IAnalysableSymbolAssociatable) obj).getAnalysableSymbol();
			if (analysableSymbol != null) {
				cachedObject.getAttributes().put(ANALYSABLE_SYMBOL, analysableSymbol.getName());
			}
		}
		if (obj instanceof IAnalysablePin) {
			cachedObject.getAttributes()
					.put(ANALYSIS_PIN_MAPPING, ((IAnalysablePin) obj).getAnalysisMappingType().name());
		}
	}
}
