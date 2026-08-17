/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.utils;

import org.jetbrains.annotations.NotNull;

/**
 * Hyperlink details holder
 */
public class HyperlinkInfo
{

	@NotNull private String m_displayText;
	@NotNull private String m_linkText;

	private boolean showPlainText;

	public HyperlinkInfo(@NotNull String m_displayText, @NotNull String m_linkText, boolean showPlainText)
	{
		this.m_displayText = m_displayText;
		this.m_linkText = m_linkText;
		this.showPlainText = showPlainText;
	}

	public HyperlinkInfo(@NotNull String displayText, @NotNull String linkText)
	{
		this(displayText, linkText, false);
	}

	@NotNull public String getDisplayText()
	{
		return m_displayText;
	}

	@NotNull public String getLinkText()
	{
		return m_linkText;
	}

	@Override
	@NotNull public String toString()
	{
		return m_displayText;
	}

	public boolean isShowPlainText()
	{
		return showPlainText;
	}
}
