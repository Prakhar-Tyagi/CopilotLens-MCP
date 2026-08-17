/*
 * Copyright 2002-2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.action.dragdrop.DragActionInvocationTransferHandler;
import chs.caf.action.dragdrop.IDragDropActionInvocationControl;
import chs.caf.caplet.ICapletController;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.TransferHandler;

public class LayoutBrowserClient extends BrowserClient
{

	public LayoutBrowserClient(ICapletController controller)
	{
		super(controller);
	}

	@Override protected void obtainSkippedFolders()
	{
		m_skippedFolders.add(LogicFolder.LOGIC_BLOCKS.getDisplayName());
		m_skippedFolders.add(LogicFolder.HIGHWAYS.getDisplayName());
		m_skippedFolders.add(LogicFolder.SINGLE_LINES.getDisplayName());
	}

	@Nullable public TransferHandler createTransferTreeHandler(
			@NotNull IDragDropActionInvocationControl actionInocationControl)
	{
		return new DragActionInvocationTransferHandler();
	}
}
