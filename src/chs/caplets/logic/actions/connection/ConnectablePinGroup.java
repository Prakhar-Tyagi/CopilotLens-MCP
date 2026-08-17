/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.connection;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents pins to be connected
 */
public class ConnectablePinGroup implements IConnectablePinGroup
{

	@NotNull private final List<IConnectablePin> m_connectablePins;

	public ConnectablePinGroup(@NotNull Set<IConnectablePin> connectablePins)
	{
		if (connectablePins.size() < 2) {
			throw new IllegalArgumentException("Atleast two pins are required for connection");
		}
		m_connectablePins = connectablePins.stream().sorted().collect(Collectors.toList());
	}

	@NotNull @Override public List<IConnectablePin> getConnectablePins()
	{
		return Collections.unmodifiableList(m_connectablePins);
	}
}