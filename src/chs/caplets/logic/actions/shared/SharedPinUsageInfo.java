/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.UserSessionException;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.ICommonFactory;
import chs.common.IUID;
import chs.common.IUIDProvider;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.StringUtils;
import chs.utilities.WrappingRuntimeException;
import chs.utility.IObjectInUseService;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pin Usage Info of shared pins
 */
public class SharedPinUsageInfo
{
	private final ISharedPinList sharedPinList;
	private Set<IUID> pinsUsed = null;

	public SharedPinUsageInfo(@NotNull ISharedPinList sharedPinList)
	{
		this.sharedPinList = sharedPinList;
	}

	private void computeCache()
	{
		final Set<ISharedPin> sharedPins = CollectionUtils.createSet(sharedPinList.getPins());
		pinsUsed = new HashSet<>(sharedPins.size());
		if (sharedPins.isEmpty()) {
			return;
		}

		Pair<String, String> objectType = IObjectInUseService.determineObjectType(sharedPins.iterator().next());
		assert objectType != null;
		String type = objectType.getKey();

		Set<String> pinUIDs = sharedPins.stream()
				.map(IUIDProvider::getUID)
				.map(IUID::getString)
				.collect(Collectors.toSet());
		String[] sharedPinsUID = pinUIDs.toArray(StringUtils.EMPTY_STRING_ARRAY);

		IUserSession userSession = FactoryMgr.getCHSSystem().getUserSession();
		ICommonFactory commonFactory = FactoryMgr.getCommonFactory();

		try {
			String[] objectsInUse = userSession.objectsInUse(type, sharedPinsUID);

			for (String uid : objectsInUse) {
				pinsUsed.add(commonFactory.constructUID(uid));
			}
		}
		catch (UserSessionException e) {
			throw new WrappingRuntimeException("Exception while trying to determine if shared pins are being used", e);
		}
	}

	public boolean isUsed(@NotNull ISharedPin sharedPin)
	{
		if (pinsUsed == null) {
			computeCache();
		}
		return pinsUsed.contains(sharedPin.getUID());
	}
}

