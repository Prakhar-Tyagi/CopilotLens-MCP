/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared;

import chs.ctf.ui.utility.statusmessage.IStatus;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;

public abstract class HyperLinkStatusMessage implements IHyperLinkStatusMessage
{

	@NotNull protected IStatus m_status;
	@NotNull protected String m_message;
	@NotNull protected String m_designName;
	@NotNull protected String m_objectDetailText;
	@NotNull protected String m_objectDetailLink;

	public boolean isSharedObjectLink()
	{
		return false;
	}

	@Override
	@NotNull public IStatus getStatus()
	{
		return m_status;
	}

	@Override
	@NotNull public String getMessage()
	{
		return m_message;
	}

	@Override
	@NotNull public String getDesignName()
	{
		return m_designName;
	}

	@Override
	@NotNull public String getObjectDetailText()
	{
		return m_objectDetailText;
	}

	@Override
	@NotNull public String getObjectDetailLink()
	{
		return StringUtils.emptyIfBlank(m_objectDetailLink);
	}
}
