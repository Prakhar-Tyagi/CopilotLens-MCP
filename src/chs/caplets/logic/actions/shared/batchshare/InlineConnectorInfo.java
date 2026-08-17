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

import java.util.Map;

/**
 * Inline object info
 */
public class InlineConnectorInfo extends PinlistInfo implements IInlineConnectorInfo
{

	private IInlineConnectorInfo m_matedConnector;
	private boolean m_isJack;

	public InlineConnectorInfo(@NotNull String designUID, boolean isJack, @NotNull Map<String, String> attributes)
	{
		super(designUID, ShareableEntityTypeEnum.INLINE, attributes);
		m_isJack = isJack;
	}

	@NotNull public IInlineConnectorInfo getMatedConnector()
	{
		return m_matedConnector;
	}

	public void setMatedConnector(@NotNull IInlineConnectorInfo matedConnector)
	{
		if (m_matedConnector == null) {
			m_matedConnector = matedConnector;
			matedConnector.setMatedConnector(this);
		}
	}

	public boolean isJack()
	{
		return m_isJack;
	}
}
