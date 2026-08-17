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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Multicore object info
 */
public class MulticoreInfo extends ObjectInfo implements IMulticoreInfo
{

	@NotNull private Set<String> m_innercoreUIDs = new HashSet<>();

	MulticoreInfo(@NotNull String designUID, @NotNull ShareableEntityTypeEnum type,
			@NotNull Map<String, String> attributes)
	{
		super(designUID, type, attributes);
		m_sharedUID = attributes.get(IXMLTags.SHAREDSOURCE);
		String shieldUID = attributes.get(IXMLTags.SHIELDCONDUCTOR);
		if (shieldUID != null) {
			addInnercore(shieldUID);
		}
	}

	public void addInnercore(@NotNull String innercoreUID)
	{
		m_innercoreUIDs.add(innercoreUID);
	}

	@Override @NotNull public Set<String> getInnercoreUIDs()
	{
		return m_innercoreUIDs;
	}
}
