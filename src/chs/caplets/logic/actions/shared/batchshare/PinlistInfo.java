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
 * Base Info for pinlists
 */
public class PinlistInfo extends ObjectInfo
{

	public PinlistInfo(@NotNull String designUID, @NotNull ShareableEntityTypeEnum type,
			@NotNull Map<String, String> attributes)
	{
		super(designUID, type, attributes);
		m_sharedUID = attributes.get(IXMLTags.SHAREDSOURCE);
	}
}
