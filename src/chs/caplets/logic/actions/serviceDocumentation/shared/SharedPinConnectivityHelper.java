/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.serviceDocumentation.shared;

import chs.cof.logical.ILogicObjectDesignContainer;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinIterator;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IUID;
import chs.utility.logic.sharedpinconnection.ISharedPinMatingsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * This class uses {@link ISharedPinMatingsProvider} to fetch the shared details and uses shared pin specific details
 * <p>
 * Caches the shared pin -> mated shared pins details for all the shared pins of the pinlist.
 * <p>
 * The shared pins details are brought for the 'active build list' The shared pins details are brought only when there
 * is connectivity, i.e., if the pins have no connectivity in any of the designs in active build list, then no details
 * are cached.
 */
public class SharedPinConnectivityHelper
{

	private Map<IUID, Map<IUID, Set<String>>> m_sharedPinDetailsCache;
	private BiFunction<ISharedPinList, ILogicObjectDesignContainer, ISharedPinMatingsProvider>
			m_sharePinMatingsProvider;

	public SharedPinConnectivityHelper(
			BiFunction<ISharedPinList, ILogicObjectDesignContainer, ISharedPinMatingsProvider> sharePinMatingsProvider)
	{
		m_sharedPinDetailsCache = new HashMap<>();
		m_sharePinMatingsProvider = sharePinMatingsProvider;
	}

	//cache shared pin details for these pinlists
	void cacheSharedPinDetails(Set<chs.cof.logical.schem.IPinList> pinLists)
	{
		for (chs.cof.logical.schem.IPinList pinList : pinLists) {
			IPinList pinListConnectivity = pinList.getConnectivity();
			ISharedPinList sharedPinList = pinListConnectivity.getSharedPinList();
			ILogicObjectDesignContainer design = pinListConnectivity.getDesign();
			if (sharedPinList != null) {
				assert design != null;
				doCache(sharedPinList, design);
			}
		}
	}

	boolean allowPlacePinWithNoConnection(IAbstractPin pin1, chs.cof.logical.schem.IPinList anchor)
	{
		return isPinNotAlreadyPresent(pin1, anchor);
	}

	boolean allowConnectionWithPlaceHolder(IAbstractPin pin1, @NotNull chs.cof.logical.schem.IPinList anchor,
			chs.cof.logical.schem.IPinList matedSchemPL)
	{
		//if the pin is already present on the pinlist, then do not allow
		if (!isPinNotAlreadyPresent(pin1, anchor)) {
			return false;
		}
		//if there is a mating to this pin in this design
		Set<IAbstractPin> matedPinsInDesign =
				matedSchemPL
						.getConnectivity()
						.getPins()
						.stream()
						.collect(Collectors.toSet());
		Collection<IAbstractPin> connectedPins = pin1.getConnectedPins();
		boolean isConnectedInDesign = connectedPins.retainAll(matedPinsInDesign);
		if (isConnectedInDesign) {
			return true;
		}
		//else check if the mated pinlist has atleast on pin which is mated with this pin in some design
		//we check this by checking the all shared mated pins and check the shared pins of the mated pinlists
		//and check if atleast there is one match
		IPinList pin1Parent = anchor.getConnectivity();
		Set<String> matedPinSharedUIDs = new HashSet<>(getSharedPinMatingDetails(pin1, pin1Parent));
		Set<String> sharedUIDsOfPinsOnMatedPL = matedSchemPL
				.getPins()
				.stream()
				.map(IPin::getConnectivity)
				.filter(pin -> {
					return checkIfValidMate(pin1, pin1Parent, pin);
				})
				.map(IAbstractPin::getSharedObjectUID)
				.filter(Objects::nonNull)
				.map(IUID::getString)
				.collect(Collectors.toSet());
		matedPinSharedUIDs.retainAll(sharedUIDsOfPinsOnMatedPL);
		return matedPinSharedUIDs.isEmpty();
	}

	boolean allowConnectionWithPin(IAbstractPin pin1, @NotNull IPinList pin1Parent, IAbstractPin pin2)
	{
		boolean isConnectedInDesign = pin1
				.getConnectedPins()
				.contains(pin2);
		if (isConnectedInDesign) {
			return true;
		}
		ISharedPin sharedPin2 = pin2.getSharedPin();
		Set<String> matedPinSharedUIDs = getSharedPinMatingDetails(pin1, pin1Parent);
		if (matedPinSharedUIDs.isEmpty()) {
			return false;
		}
		if (sharedPin2 != null) {
			return matedPinSharedUIDs.contains(sharedPin2.getUID().getString());
		}
		return true;
	}

	private boolean isPinNotAlreadyPresent(IAbstractPin pin1, chs.cof.logical.schem.IPinList anchor)
	{
		boolean pinAlreadyPresentInStack = anchor
				.getStackPins()
				.stream()
				.map(ISchemStackPin::getAllConnectivity)
				.flatMap(Set::stream)
				.filter(pin -> {
					return pin.equals(pin1);
				})
				.findFirst()
				.isPresent();
		boolean pinAlreadyPresent = anchor
				.getPins()
				.stream()
				.map(IPin::getConnectivity)
				.filter(pin -> {
					return pin.equals(pin1);
				})
				.findFirst()
				.isPresent();
		return !pinAlreadyPresentInStack && !pinAlreadyPresent;
	}

	private boolean checkIfValidMate(IAbstractPin pin1, @NotNull IPinList pin1Parent,
			IAbstractPin pinOnMate)
	{
		IAbstractPin connectedPin = pinOnMate.getConnectedPin(pin1Parent);
		return connectedPin != null && connectedPin.equals(pin1);
	}

	@Nullable String getSharedMateId(IAbstractPin pin1, @NotNull IPinList pin1Parent)
	{
		Set<String> matedPinSharedUIDs = getSharedPinMatingDetails(pin1, pin1Parent);
		if (matedPinSharedUIDs.isEmpty()) {
			return null;
		}
		return matedPinSharedUIDs.iterator().next();
	}

	@NotNull Set<String> getSharedPinMatingDetails(IAbstractPin pin1, @NotNull IPinList pin1Parent)
	{
		IPinList owner1 = pin1Parent;
		ISharedPinList sharedPinList1 = owner1.getSharedPinList();
		ILogicObjectDesignContainer design = owner1.getDesign();
		ISharedPin sharedPin1 = pin1.getSharedPin();
		if (sharedPinList1 != null && design != null && sharedPin1 != null) {
			Map<IUID, Set<String>> sharedPinMatingDetails = doCache(sharedPinList1, design);
			Set<String> matedPinSharedUIDs =
					sharedPinMatingDetails.getOrDefault(sharedPin1.getUID(), Collections.emptySet());
			return matedPinSharedUIDs;
		}
		return Collections.emptySet();
	}

	@NotNull private Map<IUID, Set<String>> doCache(ISharedPinList sharedPinList, ILogicObjectDesignContainer design)
	{
		IUID sharedPinList1UID = sharedPinList.getUID();
		return m_sharedPinDetailsCache
				.computeIfAbsent(sharedPinList1UID, (key) -> getSharedPinMatingDetails(design, sharedPinList));
	}

	@NotNull
	protected ISharedPinMatingsProvider createSharedPinMatingsProvider(ISharedPinList sharedPinList,
			ILogicObjectDesignContainer design)
	{
		return m_sharePinMatingsProvider.apply(sharedPinList, design);
	}

	@NotNull
	private Map<IUID, Set<String>> getSharedPinMatingDetails(ILogicObjectDesignContainer design,
			ISharedPinList sharedPinList)
	{
		ISharedPinMatingsProvider finder = createSharedPinMatingsProvider(sharedPinList, design);
		ISharedPinIterator pins = sharedPinList.getPins();
		Map<IUID, Set<String>> sharedPinMatingDetails = new HashMap<>();
		while (pins.hasNext()) {
			ISharedPin next = pins.getNext();
			Set<String> matedPinSharedUIDs = finder.getMatedPinSharedUID(next);
			sharedPinMatingDetails.put(next.getUID(), matedPinSharedUIDs);
		}
		return sharedPinMatingDetails;
	}
}
