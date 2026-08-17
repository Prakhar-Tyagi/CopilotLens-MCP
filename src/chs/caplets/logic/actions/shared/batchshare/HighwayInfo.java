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

import java.util.Map;

/**
 * Highway object info
 */
class HighwayInfo extends ObjectInfo
{

	HighwayInfo(@NotNull String designUID, @NotNull Map<String, String> attributes)
	{
		super(designUID, ShareableEntityTypeEnum.HIGHWAY, attributes);
		m_sharedUID = attributes.get(IXMLTags.SHAREDHIGHWAYID);
	}
}
