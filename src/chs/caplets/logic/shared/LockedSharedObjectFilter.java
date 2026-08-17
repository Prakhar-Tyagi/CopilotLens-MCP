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

import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.common.RefreshStatusEnum;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * @author chandras on 24-01-2016.
 */
public class LockedSharedObjectFilter extends AbstractLockedSharedObjectFilter
{

	@Nullable protected SharedAbstractableTree m_sharedPinListTree;

	public void setSharedAbstractableTree(@NotNull SharedAbstractableTree sharedPinListTree)
	{
		m_sharedPinListTree = sharedPinListTree;
	}

	protected RefreshStatusEnum refresh(@NotNull ISharedPinList newSPL)
	{
		return newSPL.refresh();
	}

	protected boolean lock(@NotNull ISharedPinList newSPL)
	{
		return SharedPinListHelper.lock(newSPL);
	}

	protected void unlock(@NotNull ISharedPinList newSPL)
	{
		SharedPinListHelper.unlock(newSPL);
	}

	protected void onSharedPinlistDeleted(@NotNull ISharedPinList newSPL)
	{
		LogicActionMessageHelper.warnDeleted(newSPL);
		if (m_sharedPinListTree != null) {
			TreePath path = m_sharedPinListTree.getSelectionPath();
			if (path != null) {
				DefaultTreeModel model = (DefaultTreeModel) m_sharedPinListTree.getModel();
				MutableTreeNode node = (MutableTreeNode) path.getLastPathComponent();
				model.removeNodeFromParent(node);
			}
		}
	}

	@NotNull protected ISharedObjectAvailabilityReporter getSharedObjectAvailabilityReporter()
	{
		return ISharedObjectAvailabilityReporter.NULL_REPORTER;
	}
}
