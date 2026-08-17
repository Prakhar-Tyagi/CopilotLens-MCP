/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.caplets.logic.shared;

import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.utilities.ListMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Creates map of Pinlist type vs Shared pinlists
 */
class DivideSharedPinlistsByType
{

	private ListMap<PinListTypeEnum, ISharedPinList> sharedObjectsOfAType = new ListMap<>();

	DivideSharedPinlistsByType()
	{

	}

	DivideSharedPinlistsByType(@NotNull Collection<ISharedPinList> sharedPinLists)
	{
		for (ISharedPinList pinList : sharedPinLists) {
			sharedObjectsOfAType.add(pinList.getType(), pinList);
		}
	}

	public void addSharedPinList(@NotNull ISharedPinList sharedPinList)
	{
		sharedObjectsOfAType.add(sharedPinList.getType(), sharedPinList);
	}

	@NotNull List<ISharedPinList> getSharedPinListsForTypes(@NotNull Set<PinListTypeEnum> pinListTypes)
	{
		List<ISharedPinList> sharedPinLists = new ArrayList<>();
		for (PinListTypeEnum pinListType : pinListTypes) {
			sharedPinLists.addAll(sharedObjectsOfAType.pullReadOnlySafeList(pinListType));
		}
		return sharedPinLists;
	}
}
