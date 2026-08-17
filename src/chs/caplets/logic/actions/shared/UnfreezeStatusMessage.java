/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.common.IUID;
import chs.ctf.ui.utility.statusmessage.DesignStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents feedback messages reported during bulk unfreeze
 */
public class UnfreezeStatusMessage extends HyperLinkStatusMessage implements IUnfreezeStatusMessage
{

	public UnfreezeStatusMessage(@NotNull DesignStatus status, @NotNull String message, @NotNull String name,
			@Nullable IUID designUID, @Nullable IUID objectUID)
	{
		m_status = status;
		m_message = message;
		m_objectDetailText = name;
		m_objectDetailLink = "";
		if (designUID != null && objectUID != null) {
			m_objectDetailLink = IHyperLinkStatusMessage.getHyperlink(designUID, objectUID);
		}
	}
}
