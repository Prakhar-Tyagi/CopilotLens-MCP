/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import chs.services.ui.BaseUIDHyperlinkListener;
import chs.services.ui.IHyperlinkHandler;
import org.jetbrains.annotations.NotNull;

public class HyperLinkHandler extends BaseUIDHyperlinkListener implements IHyperlinkHandler
{

	@Override protected boolean handleSpecializedUIDHyperlink(@NotNull String link)
	{
		return false;
	}

	@Override public void process(String hyperlink)
	{
		zoomToSelected(hyperlink);
	}
}