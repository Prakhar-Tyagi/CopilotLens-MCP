/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.capitalmanager.appserver.UserSessionException;
import chs.cof.logical.shared.IRevisionedSharedObject;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.common.ILockable;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.ui.LockInfoDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Sep 7, 2004 Time: 1:36:34 PM
 * <p/>
 * A place to store messages that are common to two or more actions.
 */
public class LogicActionMessageHelper
{

	public static final String SHARED_PINLIST_MGR_LOCKED_HDG = ResourceMgr
			.getString(LogicActionMessageHelper.class, "LogicActionMessageHelper.SharedPinlistMgrLockedHeading");
	public static final String SHARED_PINLIST_MGR_LOCKED_MSG = ResourceMgr
			.getString(LogicActionMessageHelper.class, "LogicActionMessageHelper.SharedPinlistMgrLockedMessage");
	public static final String SHARED_PINLIST_MGR_FAIL_MSG = ResourceMgr
			.getString(LogicActionMessageHelper.class, "LogicActionMessageHelper.SharedPinlistMgrFailMessage");

	private LogicActionMessageHelper()
	{
	}

	public static void warn(String msg)
	{
		warn(msg, "");
	}

	public static void warn(String hdg, String msg)
	{
		MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), hdg, msg);
	}

	public static void warnDeleted(ISharedPinList spl)
	{
		warnDeleted(spl, "");
	}

	public static void warnDeleted(ISharedPinList spl, String furtherDetails)
	{
		String msg = getSharedPinlistDeletedMessage(spl);
		warn(msg, furtherDetails);
	}

	@NotNull public static String getSharedPinlistDeletedMessage(ISharedPinList spl)
	{
		String msg;
		if (spl.getType().equals(PinListTypeEnum.TypeDevice)) {
			msg = ResourceMgr.getString(LogicActionMessageHelper.class, "LogicActionMessageHelper.SharedDeviceDeleted",
					spl.getName());
		}
		else if (spl.getType().equals(PinListTypeEnum.TypeSplice)) {
			msg = ResourceMgr.getString(LogicActionMessageHelper.class, "LogicActionMessageHelper.SharedSpliceDeleted",
					spl.getName());
		}
		else if (spl.getType().equals(PinListTypeEnum.TypeGround)) {
			msg = ResourceMgr.getString(LogicActionMessageHelper.class, "LogicActionMessageHelper.SharedGroundDeleted",
					spl.getName());
		}
		else if(spl.getType().equals(PinListTypeEnum.TypeFunction)){
			msg = ResourceMgr
					.getString(LogicActionMessageHelper.class, "LogicActionMessageHelper.SharedFunctionDeleted",
							spl.getName());
		}
		else {
			msg = ResourceMgr.getString(LogicActionMessageHelper.class,
					"LogicActionMessageHelper.SharedConnectorDeleted", spl.getName());
		}
		return msg;
	}

	public static void warnRevisionedSharedObjectDeleted(IRevisionedSharedObject shared)
	{
		String msg;
		String hdr = ResourceMgr.getString(LogicActionMessageHelper.class,
				"LogicActionMessageHelper.RevisionedSharedObjectDeleted.header");
		if (shared.getDesignAbstraction() != null && shared.getDesignAbstraction().getName().length() > 0) {
			msg = ResourceMgr.getString(LogicActionMessageHelper.class,
					"LogicActionMessageHelper.RevisionedSharedObjectDeleted.Abstraction.message",
					shared.getName(), shared.getRevision(), shared.getDesignAbstraction().getName());
		}
		else {
			msg = ResourceMgr.getString(LogicActionMessageHelper.class,
					"LogicActionMessageHelper.RevisionedSharedObjectDeleted.NoAbstraction.message",
					shared.getName(), shared.getRevision());
		}
		warn(hdr, msg);
	}

	public static void warnLocked(ILockable lockable)
	{
		LockInfoDialog.showLockInfoDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				lockable,
				CAFUtils.getInstance().getUserSession()
		);
	}

	public static void warnLocked(ISharedPinList spl)
	{
		if (spl.isDeleted()) {
			warnDeleted(spl);
		}
		else {
			warnLocked((ILockable) spl);
		}
	}

	public static void warnLocked(ILockable lockable, String furtherDetails)
	{
		try {
			LockInfoDialog.showLockInfoDialog(lockable.getLockException(),
					lockable,
					CAFUtils.getInstance().getUserSession().getLockInfo(lockable.getUID().toString()),
					CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					furtherDetails
			);
		}
		catch (UserSessionException ignored) {
			assert false : ignored;
		}
	}

	public static void warnFrozen(ISharedPinList spl)
	{
		warnFrozen(spl, "");
	}

	public static void warnFrozen(ISharedPinList spl, String furtherDetails)
	{
		String msg;
		if(spl.getType().equals(PinListTypeEnum.TypeFunction)){
			msg = ResourceMgr.getString(LogicActionMessageHelper.class, "LogicActionMessageHelper.SharedFunctionFrozen",
					spl.getName());
		}
		else if (spl.getType().equals(PinListTypeEnum.TypeDevice) || spl.getType().equals(PinListTypeEnum.TypeGround)) {
			msg = ResourceMgr.getString(LogicActionMessageHelper.class, "LogicActionMessageHelper.SharedDeviceFrozen",
					spl.getName());
		}
		else if (spl.getType().equals(PinListTypeEnum.TypeSplice)) {
			msg = ResourceMgr.getString(LogicActionMessageHelper.class, "LogicActionMessageHelper.SharedSpliceFrozen",
					spl.getName());
		}
		else {
			msg = ResourceMgr.getString(LogicActionMessageHelper.class,
					"LogicActionMessageHelper.SharedConnectorFrozen", spl.getName());
		}
		warn(msg, furtherDetails);
	}

	public static void warnDeleted(@NotNull ISharedMulticore newSharedMulticore)
	{
		String msg = ResourceMgr.getString(LogicActionMessageHelper.class,
				"LogicActionMessageHelper.SharedMulticoreDeleted", newSharedMulticore.getName());
		warn(msg, "");
	}
}
