/*
 * Copyright 2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caplets.helpers.HyperlinkList;
import chs.caplets.helpers.ViewRelatedHyperlinkDialog;
import org.jetbrains.annotations.NotNull;

import java.awt.Frame;

/**
 * For help tagging
 */
public class ViewRelatedDialog extends ViewRelatedHyperlinkDialog
{

	protected ViewRelatedDialog(@NotNull HyperlinkList comp, @NotNull Frame frame, @NotNull String title)
	{
		super(comp, frame, title);
	}
}
