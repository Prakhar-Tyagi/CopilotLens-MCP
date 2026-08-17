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
import chs.cof.logical.IDesign;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.ILockable;
import chs.common.RefreshStatusEnum;
import chs.images.CHSImageLoader;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.tree.IObjectUIFilterOption;
import chs.utility.logic.ISharedObjectAvailabilityChecker;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.HashSet;
import java.util.Set;

/**
 * @author chandras on 24-01-2016.
 */
public abstract class AbstractLockedSharedObjectFilter implements IObjectUIFilterOption
{

	private static final Icon LOCK_ICON = CHSImageLoader.loadImageIcon("chs/images/general/ico_locked_record.gif");
	private Set<ILockable> m_lockedOut = new HashSet<>();
	@Nullable protected IDesign m_design;

	@NotNull private ISharedObjectAvailabilityChecker m_sharedObjAvailabiltyChecker =
			new SharedObjectAvailabilityChecker();

	public boolean isSharedObjectAvailable(@NotNull ISharedObject sharedObject, @Nullable IDesign design,
			@NotNull ISharedObjectAvailabilityReporter reporter)
	{
		return m_sharedObjAvailabiltyChecker.check(sharedObject, design, reporter);
	}

	public boolean isSharedObjectEditable(@NotNull ISharedObject sharedObject, @Nullable IDesign design,
			@NotNull ISharedObjectAvailabilityReporter reporter)
	{
		if (!sharedObject.isEditable()) {
			reporter.report(ISharedObjectAvailabilityReporter.FailureReason.DOMAIN_ON_SHARED_OBJECT,
					sharedObject, null);
			return false;
		}
		return true;
	}

	public void setDesign(@NotNull IDesign design)
	{
		m_design = design;
	}

	@Nullable @Override public Icon getIcon(@NotNull Object obj)
	{
		return LOCK_ICON;
	}

	@Override public boolean filterIn(@NotNull Object obj)
	{
		return !m_lockedOut.contains(obj);
	}

	@NotNull @Override public String getDescription(@NotNull Object obj)
	{
		return ResourceMgr.getString(ShareAction.class, "ShareAction.DisableReason.Restricted.text");
	}

	public void setLockedOut(ILockable lockable, boolean lockedOut)
	{
		if (lockedOut) {
			m_lockedOut.add(lockable);
		}
		else {
			m_lockedOut.remove(lockable);
		}
	}

	public boolean selected(@NotNull Object obj)
	{
		ISharedPinList newSPL = CommonUtils.cast(obj, ISharedPinList.class);
		if (newSPL == null) {
			return true;
		}
		if (!lock(newSPL)) {
			setLockedOut(newSPL, true);
			return false;
		}
		RefreshStatusEnum rs = refresh(newSPL);
		if (RefreshStatusEnum.eObjectDoesNotExist.equals(rs)) {
			onSharedPinlistDeleted(newSPL);
			return false;
		}
		final ISharedObjectAvailabilityReporter nullReporter = getSharedObjectAvailabilityReporter();
		if (!isSharedObjectAvailable(newSPL, m_design, nullReporter)) {
			// Can't share into an unfrozen pinlist if the design requires all its shared object to be frozen
			unlock(newSPL);
			setLockedOut(newSPL, true);
			return false;
		}
		if (!isSharedObjectEditable(newSPL, m_design, nullReporter)) {
			unlock(newSPL);
			setLockedOut(newSPL, true);
			return false;
		}
		setLockedOut(newSPL, false);
		return true;
	}

	@NotNull protected abstract ISharedObjectAvailabilityReporter getSharedObjectAvailabilityReporter();

	protected abstract RefreshStatusEnum refresh(@NotNull ISharedPinList newSPL);

	protected abstract boolean lock(@NotNull ISharedPinList newSPL);

	protected abstract void unlock(@NotNull ISharedPinList newSPL);

	protected abstract void onSharedPinlistDeleted(@NotNull ISharedPinList newSPL);
}
