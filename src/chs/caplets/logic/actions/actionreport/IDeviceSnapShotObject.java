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

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * specific to device
 */
public interface IDeviceSnapShotObject extends ICachedObject
{

	@NotNull Map<String, String> getDevicePinToDeviceConnectorMap();

	void addPinToDeviceConnectMapping(@NotNull String key, @NotNull String value);

	@NotNull Collection<String> getDeviceConnectorUIDs();

	@NotNull Collection<String> getDevicePinUIDs();

	void addDevicePinUID(@NotNull String uid);

}
