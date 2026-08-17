/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2010-2024 Siemens
 */
package chs.caplets.logic.merge;

import chs.cof.COFTypeEnum;
import chs.cof.library.IFootprintable;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.common.IDesignContainer;
import chs.common.IUID;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.logic.DesignHelper;
import chs.utility.logic.PinUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 12-Mar-2010 Time: 15:59:44
 */
public class Mergeable
{

	private String m_message = "";

	public static final Mergeable Possible = new Mergeable("");
	public static final Mergeable LibraryDifferentLibraries =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.differentlibraries.reason"));
	public static final Mergeable NumberOfCavitiesMismatch =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.cavitiesnumbermismatch.reason"));
	public static final Mergeable SharedTarget =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.sharedtarget.reason"));
	public static final Mergeable DifferentObjectTypes = new Mergeable("");
	public static final Mergeable SameObject = new Mergeable("");
	public static final Mergeable MulticoredConductor =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.multicored.reason"));
	public static final Mergeable InlinesNotBothAreInlines =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.bothnotinlines.reason"));
	public static final Mergeable InlinesInvalidPinPairs =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.invalidpinpairs.reason"));
	public static final Mergeable InlinesLibraryPartsMismatch =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.inlinelibrarymismatch.reason"));
	public static final Mergeable InlinesPinPairsMismatch =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.inlinemismatch.reason"));
	public static final Mergeable NonMergeableObjectType =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.nonmergeabletype.reason"));
	public static final Mergeable LibraryDifferentFootprints =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.differentfootprints.reason"));
	public static final Mergeable LibraryCavityNamesMismatch =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.cavitynamesmismatch.reason"));
	public static final Mergeable WireCenterstripAsPin =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.wirecenterstrippin.reason"));
	public static final Mergeable SymbolCollision =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.symbolpincollision.reason"));
	public static final Mergeable RingTerminalCavityNamesMismatch =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.ringterminalcavitynamesmismatch.reason"));
	public static final Mergeable ResultingDuplicatePinsOnTarget =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.duplicatepins.reason"));
	public static final Mergeable SelectedModularConnectorHasAssociatedConnectors =
			new Mergeable(ResourceMgr
					.getString(Mergeable.class, "MergeStatus.SelectedModularConnectorHasAssociatedConnectors.reason"));
	public static final Mergeable CannotMergeIntoAChildModularConnector =
			new Mergeable(
					ResourceMgr.getString(Mergeable.class, "MergeStatus.CannotMergeIntoAChildModularConnector.reason"));
	public static final Mergeable ModularConnectorCannotBeMergedIntoNonLibrariedConnector =
			new Mergeable(ResourceMgr.getString(Mergeable.class,
					"MergeStatus.ModularConnectorCannotBeMergedIntoNonLibrariedConnector.reason"));
	public static final Mergeable NonModularConnectorCannotBeMergedIntoModularConnector =
			new Mergeable(ResourceMgr.getString(Mergeable.class,
					"MergeStatus.NonModularConnectorCannotBeMergedIntoModularConnector.reason"));
	public static final Mergeable CannotMergeAChildModularConnector =
			new Mergeable(
					ResourceMgr.getString(Mergeable.class, "MergeStatus.CannotMergeAChildModularConnector.reason"));

	public static final Mergeable SourceAndTargetHaveDifferentMates =
			new Mergeable(ResourceMgr
					.getString(Mergeable.class, "MergeStatus.MismatchMatesAsRingTerminalAndConnector.reason"));
	public static final Mergeable ConversionOfStudToNormalNotPossible =
			new Mergeable(
					ResourceMgr.getString(Mergeable.class, "MergeStatus.ConversionOfStudToNormalNotPossible.reason"));
	public static final Mergeable SourcePartOfLibraryAssembly =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.SourcePartOfLibraryAssembly.reason"));
	public static final Mergeable SourceCavitiesBlockedInTarget =
			new Mergeable(ResourceMgr.getString(Mergeable.class, "MergeStatus.CannotMergeBlockedCavities.reason"));

	public Mergeable(String message)
	{
		m_message = message;
	}

	public String getReason()
	{
		return m_message;
	}

	public static Mergeable areMergeable(ILogicObject sourceObject, ILogicObject targetObject)
	{
		if (COFTypeEnum.from_object(sourceObject) != COFTypeEnum.from_object(targetObject)) {
			return DifferentObjectTypes;
		}
		if (sourceObject == targetObject) {
			return SameObject;
		}
		if (targetObject.isShared()) {
			return SharedTarget;
		}

		IUID sourceLibRef = sourceObject.getLibraryRef();
		IUID targetLibRef = targetObject.getLibraryRef();
		if (sourceLibRef != null && targetLibRef != null && sourceLibRef != targetLibRef) {
			return LibraryDifferentLibraries;
		}

		if (sourceObject instanceof IFootprintable && targetObject instanceof IFootprintable) {
			ILibraryDeviceFootprint sourceFootprint =
					((IFootprintable) sourceObject).getFootprint();
			ILibraryDeviceFootprint targetFootprint =
					((IFootprintable) targetObject).getFootprint();
			if (sourceFootprint != null && targetFootprint != null && sourceFootprint != targetFootprint) {
				return LibraryDifferentFootprints;
			}
		}

		if (sourceObject instanceof IConnector && targetObject instanceof IConnector) {
			Mergeable mergeable = ConnectorPinlistMerger.areMergeable((IConnector) sourceObject,
					(IConnector) targetObject);
			if (mergeable != Possible) {
				return mergeable;
			}
		}

		if ((sourceLibRef == null || targetLibRef == null) && sourceObject instanceof IPinList &&
				targetObject instanceof IPinList) {
			Mergeable mergeable = PinlistMerger.areMergeable((IPinList) sourceObject, (IPinList) targetObject);
			if (mergeable != Possible) {
				return mergeable;
			}
		}

		if (targetObject instanceof IConductor && sourceObject instanceof IConductor) {
			boolean belongToMulticore = ((IConductor) targetObject).getMulticore() != null ||
					((IConductor) sourceObject).getMulticore() != null;
			if (belongToMulticore) {
				return MulticoredConductor;
			}
		}

		if (sourceObject instanceof IWireConductor && targetObject instanceof IWireConductor) {
			if (WireConductorMerger.areMergeable((IWireConductor) sourceObject, (IWireConductor) targetObject)) {
				return WireCenterstripAsPin;
			}
		}

		if (sourceObject instanceof IGenericInlineConnector) {
			Mergeable inlineMergeability =
					ConnectorPinlistMerger.inlinesMergeability((IGenericInlineConnector) sourceObject,
							(IGenericInlineConnector) targetObject);
			if (inlineMergeability != Possible) {
				return inlineMergeability;
			}
		}

		if (sourceObject instanceof IDevice && targetObject instanceof IDevice) {
			Mergeable canMerge = DevicePinlistMerger.areMergeable((IDevice) sourceObject, (IDevice) targetObject);
			if (canMerge != Possible) {
				return canMerge;
			}
		}

		if (!sourceObject.isMergeable() || !targetObject.isMergeable()) {
			return NonMergeableObjectType;
		}

		if (sourceObject instanceof IPinList && targetObject instanceof IPinList) {
			List<Pair<IPinList, IPinList>> pairsToCheck = getPairsToCheckForPinDuplication(sourceObject, targetObject);
			for (Pair<IPinList, IPinList> pair : pairsToCheck) {
				if (checkForDuplicatePinsAfterMerge(pair.getFirst(), pair.getSecond())) {
					return ResultingDuplicatePinsOnTarget;
				}
			}
		}

		if (sourceObject.getAssembly() != null && sourceObject.getAssembly().isPartAssigned()) {
			return SourcePartOfLibraryAssembly;
		}

		if (sourceObject instanceof ISingleLine sourceSingleLine &&
				targetObject instanceof ISingleLine targetSingleLine) {

			if (SingleLineHelper.canMergeSingleLines(sourceSingleLine, targetSingleLine)) {
				return Possible;
			}
			return NonMergeableObjectType;
		}
		return Possible;
	}

	private static List<Pair<IPinList, IPinList>> getPairsToCheckForPinDuplication(ILogicObject sourceObject,
			ILogicObject targetObject)
	{
		List<Pair<IPinList, IPinList>> pairsToCheck = new ArrayList<Pair<IPinList, IPinList>>(2);
		if (sourceObject instanceof IGenericInlineConnector && targetObject instanceof IGenericInlineConnector) {
			IGenericInlineConnector sourceMate =
					((IGenericInlineConnector) sourceObject).getMatedInlines().iterator().next();
			IGenericInlineConnector targetMate =
					((IGenericInlineConnector) targetObject).getMatedInlines().iterator().next();
			List<IGenericInlineConnector> plugPair = new ArrayList<IGenericInlineConnector>(2);
			List<IGenericInlineConnector> jackPair = new ArrayList<IGenericInlineConnector>(2);
			//first we need to identify source plug/jack and then target because we want source 1st in the list.
			if (sourceObject instanceof IInlinePlugConnector) {
				plugPair.add((IGenericInlineConnector) sourceObject);
				jackPair.add(sourceMate);
			}
			else {
				plugPair.add(sourceMate);
				jackPair.add((IGenericInlineConnector) sourceObject);
			}
			if (targetObject instanceof IInlinePlugConnector) {
				plugPair.add((IGenericInlineConnector) targetObject);
				jackPair.add(targetMate);
			}
			else {
				plugPair.add(targetMate);
				jackPair.add((IGenericInlineConnector) targetObject);
			}
			if ((plugPair.size() == 2) && (jackPair.size() == 2)) {
				Iterator<IGenericInlineConnector> pItr = plugPair.iterator();
				Iterator<IGenericInlineConnector> jItr = jackPair.iterator();
				pairsToCheck.add(new Pair<IPinList, IPinList>(pItr.next(), pItr.next()));
				pairsToCheck.add(new Pair<IPinList, IPinList>(jItr.next(), jItr.next()));
			}
			assert (plugPair.size() == 2) && (jackPair.size() == 2) : "Invalid inline mates!!!";
		}
		else {
			pairsToCheck.add(new Pair<IPinList, IPinList>((IPinList) sourceObject, (IPinList) targetObject));
		}
		return pairsToCheck;
	}

	private static boolean checkForDuplicatePinsAfterMerge(IPinList sourceParent, IPinList targetParent)
	{
		IDesignContainer design = DesignHelper.getDesign(sourceParent);
		if (!(design instanceof ILogicDesign)) {
			return false;
		}
		if (PinUtils.allowDuplicatePinsOnDesign(design.getProject())) {
			return false;
		}
		Map<IAbstractPin, IAbstractPin> pinMapping = PinlistMerger.getPinMappingForMerge(sourceParent, targetParent);
		for (Map.Entry<IAbstractPin, IAbstractPin> entry : pinMapping.entrySet()) {
			IAbstractPin pin = entry.getKey();
			IAbstractPin targetPin = entry.getValue();
			if (targetPin != null && pin != null) {

				if (pin.isReusable()) {
					continue;
				}
				if (PinUtils.isPinUsedInDesign(pin, design) && PinUtils.isPinUsedInDesign(targetPin, design)) {
					return true;
				}
			}
		}
		return false;
	}
}
