/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.icd;

import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.caplet.helpers.browser.BrowserTreeHelper;
import chs.caf.caplet.helpers.browser.IBrowserTreeNode;
import chs.caf.caplet.helpers.browser.ICDNameFilter;
import chs.cof.icd.IICD;
import chs.common.IUID;

import javax.swing.ToolTipManager;
import java.util.Collection;
import java.util.Collections;


public class ICDBrowserTree extends BrowserTreeHelper
{

	public ICDBrowserTree(ICDBrowserClient icdBrowserClient, String icdBrowser)
	{
		super(icdBrowserClient, icdBrowser);
		initFilters();
	}

	private void initFilters()
	{
		IFIB fib = CAFUtils.getInstance().getFIB();
		setSearchFilter(new ICDNameFilter());
	}

	@Override public boolean includeIfChildrenIncluded(Object obj)
	{
		return true;
	}

	@Override public boolean isRootVisible()
	{
		return false;
	}

	protected void registerListeners()
	{
		// add this as a model change listener
		m_client.getController().getCapletModel().addModelChangeListener(this);

		// add this as a model activation listener
		m_client.getController().getCapletModel().addModelActivationListener(this);

		ToolTipManager.sharedInstance().registerComponent(this);
	}

	protected void unregisterListeners()
	{
		m_client.getController().getCapletModel().removeModelChangeListener(this);
		ToolTipManager.sharedInstance().unregisterComponent(this);
	}

	@Override public void destroy()
	{
		super.destroy();
		unregisterListeners();
	}

	@Override protected void addChildren(IBrowserTreeNode childNode, IBrowserTreeNode parent)
	{
		if (someFilterEnabled()) {
			Collection<IUID> children = m_client.getChildren(childNode);
			addChildren(childNode, children);
		}
		else {
			super.addChildren(childNode, parent);
		}
	}

	public void reBuildICDTree()
	{
		clearSelection();
		setTreeDirty(true);
		rebuild(Collections.emptySet());
	}

	protected void setTreeDirty(boolean dirty)
	{
		if (dirty) {
			((ICDBrowserClient) m_client).invalidate();
		}
		super.setTreeDirty(dirty);
	}

	public boolean areMultipleICDsSelected()
	{
		return getSelectMgr().getCurrentSelections().getSelectedObjects(IICD.class).size() > 1;
	}
}