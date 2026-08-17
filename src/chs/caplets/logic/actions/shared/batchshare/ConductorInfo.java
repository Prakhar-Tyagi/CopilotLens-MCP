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

import chs.utilities.IXMLTags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Base info for Conductors
 */
class ConductorInfo extends ObjectInfo implements IConductorInfo
{

	@NotNull private IUpdateableConnectivityInfo m_connectivityInfo;

	ConductorInfo(@NotNull String designUID, @NotNull ShareableEntityTypeEnum type,
			@NotNull Map<String, String> attributes)
	{
		super(designUID, type, attributes);
		m_connectivityInfo = new ConnectivityInfo();
		m_sharedUID = attributes.get(IXMLTags.SHAREDCONDUCTOR);
	}

	public void addConnection(@NotNull String connectedPinUID)
	{
		m_connectivityInfo.addPinConnection(connectedPinUID);
	}

	@Override
	@Nullable public IConnectivityInfo getConnectivityInfo()
	{
		return m_connectivityInfo;
	}
}
