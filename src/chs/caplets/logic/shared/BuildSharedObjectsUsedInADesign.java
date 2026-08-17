/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.caplets.logic.shared;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.IBaseDesignSharedUsage;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

/**
 * Finds shared pinlists and shared multicores used in a design
 */
class BuildSharedObjectsUsedInADesign
{

	private Collection<IUID> sharedPinlistUIDsInDesign;
	private Collection<IUID> sharedMulticoreUIDsInDesign;

	BuildSharedObjectsUsedInADesign(@NotNull ILogicDesign design)
	{
		sharedPinlistUIDsInDesign = new HashSet<IUID>();
		IConnectivity connectivity = design.getConnectivity();
		if (connectivity != null) {
			for (IPinList pinList : connectivity.getPinLists()) {
				ISharedPinList sharedPinList = pinList.getSharedPinList();
				if (sharedPinList != null) {
					sharedPinlistUIDsInDesign.add(sharedPinList.getUID());
				}
			}
		}
		sharedMulticoreUIDsInDesign = new HashSet<IUID>();

		for (IBaseDesignSharedUsage usage : design.getSharedUsageMgr().getMulticoreUsages()) {
			ISharedObject sharedObject = usage.getSharedObject();
			if (sharedObject instanceof ISharedMulticore) {
				sharedMulticoreUIDsInDesign.add(sharedObject.getUID());
			}
		}
	}

	public boolean isSharedPinlistUsedInDesign(@NotNull ISharedPinList sharedPinList)
	{
		return sharedPinlistUIDsInDesign.contains(sharedPinList.getUID());
	}

	public boolean isSharedMulticoreUsedInDesign(@NotNull ISharedMulticore sharedMulticore)
	{
		return sharedMulticoreUIDsInDesign.contains(sharedMulticore.getUID());
	}
}
