/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.IFIB;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedConductorMgr;
import org.jetbrains.annotations.NotNull;

import java.awt.Frame;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Apr 14, 2004 Time: 10:41:13 AM
 */
public class ShareConductorGroupActionHelper extends AbstractShareConductorGroupActionHelper
{

	private IFIB m_fib;

	public ShareConductorGroupActionHelper(IFIB fib, @NotNull ILogicDesign design)
	{
		super(design);
		m_fib = fib;
	}

	public ShareConductorGroupActionHelper(@NotNull ILogicDesign design)
	{
		this(null, design);
	}

	@Override
	protected boolean createAndValidateMulticoreShareContextProvider(@NotNull IMulticore multicore, String dialogTitle)
	{
		return showDialog(dialogTitle, multicore);
	}

	@Override protected boolean shouldSyncWithLibraryPart()
	{
		return true;
	}

	protected boolean showDialog(String dialogTitle, @NotNull IMulticore multicore)
	{
		SharedMulticoreDialog dialog = createDialog(dialogTitle, multicore);
		setMulticoreShareContextProvider(dialog);
		return !dialog.isCancelled();
	}

	@NotNull protected SharedMulticoreDialog createDialog(String dialogTitle, @NotNull IMulticore multicore)
	{
		Frame owner = m_fib.getWindowMgr().getDialogFrame();
		SharedMulticoreDialog dialog = getSharedMulticoreDialog(dialogTitle, multicore, owner);
		dialog.setHelpID(ShareConductorGroupActionHelper.class.getName());
		dialog.setVisible(true);
		return dialog;
	}

	protected void setMulticoreShareContextProvider(
			@NotNull IMulticoreShareContextProvider multicoreShareContextProvider)
	{
		m_multicoreShareContextProvider = multicoreShareContextProvider;
	}

	@NotNull
	protected SharedMulticoreDialog getSharedMulticoreDialog(String dialogTitle, @NotNull IMulticore multicore,
			Frame owner)
	{
		return new SharedMulticoreDialog(owner, dialogTitle, multicore, m_design);
	}

	@Override protected void reportSharedCondMgrLocked(ISharedConductorMgr sharedCondrMgr)
	{
		LogicActionMessageHelper.warnLocked(sharedCondrMgr);
	}

	@Override protected boolean isBulkPromotion()
	{
		return false;
	}

	@Override protected boolean isChangeReportingRequired()
	{
		return true;
	}
}
