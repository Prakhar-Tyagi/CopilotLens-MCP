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

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Connectivity related info
 */
class ConnectivityInfo implements IUpdateableConnectivityInfo
{

	@NotNull private final SortedSet<String> m_connectedPinUIDs = new TreeSet<>();

	@Override
	@NotNull public SortedSet<String> getConnectedPinUIDs()
	{
		return m_connectedPinUIDs;
	}

	@Override
	public void addPinConnection(@NotNull String pinUID)
	{
		m_connectedPinUIDs.add(pinUID);
	}

	@Override public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (!IConnectivityInfo.class.isInstance(obj)) {
			return false;
		}
		IConnectivityInfo otherConnectivityInfo = (IConnectivityInfo) obj;
		return Arrays.equals(getConnectedPinUIDs().toArray(), otherConnectivityInfo.getConnectedPinUIDs().toArray());
	}

	@Override public int hashCode()
	{
		return Arrays.hashCode(getConnectedPinUIDs().toArray());
	}
}
