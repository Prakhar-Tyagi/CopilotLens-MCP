/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import chs.cof.COFTypeEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * device snapshot
 */
public class DeviceSnapShotObject extends CachedObject implements IDeviceSnapShotObject
{

	private final Map<String, String> mDevicePinToDCPin = new HashMap<>();
	private final Set<String> mDevicePins = new HashSet<>();

	public DeviceSnapShotObject(@Nullable ICachedObject parent, @NotNull String name, @NotNull String uid,
			@Nullable String designUID, @NotNull COFTypeEnum objectType)
	{
		super(parent, name, uid, designUID, objectType);
	}

	@NotNull @Override public Map<String, String> getDevicePinToDeviceConnectorMap()
	{
		return mDevicePinToDCPin;
	}

	@Override public void addPinToDeviceConnectMapping(@NotNull String key, @NotNull String value)
	{
		mDevicePinToDCPin.put(key, value);
	}

	@NotNull @Override public Collection<String> getDeviceConnectorUIDs()
	{
		return mDevicePinToDCPin.values();
	}

	@NotNull @Override public Collection<String> getDevicePinUIDs()
	{
		return mDevicePins;
	}

	@Override public void addDevicePinUID(@NotNull String uid)
	{
		mDevicePins.add(uid);
	}
}
