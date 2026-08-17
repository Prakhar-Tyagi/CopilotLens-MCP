/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.shared;

import chs.caplets.logic.actions.shared.ShareAction;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.images.CHSImageLoader;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.tree.IObjectUIFilterOption;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.SharePinListLockHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * @author chandras on 24-01-2016.
 */
public class LockedCablePinListFilter implements IObjectUIFilterOption
{

	private static final Icon LOCK_ICON = CHSImageLoader.loadImageIcon("chs/images/general/ico_locked_record.gif");

	private final IPinList m_sourceCablePL;
	private Boolean m_triStateFilterIn = null;

	public LockedCablePinListFilter(@NotNull IPinList sourceCablePL)
	{
		m_sourceCablePL = sourceCablePL;
	}

	@Nullable @Override public Icon getIcon(@NotNull Object obj)
	{
		return LOCK_ICON;
	}

	@Override public boolean filterIn(@NotNull Object obj)
	{
		return doFilter(obj, false);
	}

	@NotNull @Override public String getDescription(@NotNull Object obj)
	{
		return ResourceMgr.getString(ShareAction.class, "ShareAction.DisableReason.LockAttached.text");
	}

	public boolean selected(@NotNull Object obj)
	{
		return doFilter(obj, true);
	}

	private boolean doFilter(@NotNull Object obj, boolean attemptLock)
	{
		ISharedPinList sharedPinList = CommonUtils.cast(obj, ISharedPinList.class);
		if (sharedPinList == null) {
			return true;
		}
		ILogicDesign logicDesign = m_sourceCablePL.getLogicDesign();
		if (logicDesign == null) {
			return true;
		}
		IConnectivity connectivity = logicDesign.getConnectivity();
		if (connectivity == null) {
			return true;
		}
		IPinList pinList = connectivity.findSharedPinList(sharedPinList);
		if (pinList == null) {
			return true;
		}
		if (LogicObjectLockFinder.isLogicObjectLockedInOtherSession(pinList)) {
			return false;
		}
		if (m_triStateFilterIn != null && m_triStateFilterIn) {
			return true;
		}
		if (attemptLock) {
			//evaluation pending. don't worry. try again
			m_triStateFilterIn =
					SharePinListLockHandler.attemptLockOnSourceImpactedRelatedObjectForShare(m_sourceCablePL);
		}
		if (m_triStateFilterIn != null) {
			return m_triStateFilterIn;
		}
		return true;
	}
}
