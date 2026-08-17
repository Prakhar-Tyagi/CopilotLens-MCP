/*
 * Copyright 2005-2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ActionSeparator;
import chs.caf.CAFUtils;
import chs.caf.IActionNode;
import chs.caf.IFIB;
import chs.caf.action.utility.ActionUtilities;
import chs.caf.caplet.IBrowserClient;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.ISpecialSelection;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.helpers.browser.BrowserTreeHelper;
import chs.caf.caplet.helpers.browser.IBrowserTreeSelectionModel;
import chs.caf.caplet.selection.SelectEvent;
import chs.caplets.shared.actions.SelectAction;
import chs.common.IObjectFilter;
import chs.common.IUIDObject;
import chs.common.IUIDObjectIterator;

import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class GenericBrowserTree extends BrowserTreeHelper implements ISpecialSelectMgr
{

	protected ActionContainer combinedContextActionMenu;
	protected ActionContainer contextMenu;
	protected MouseAdapter genericBrowserTreeMouseListener;
	private ISpecialSelection m_specialSelectDelegate;

	private static final IObjectFilter<Object> PASS_ALL_FILTER = new IObjectFilter<Object>()
	{
		public boolean accept(Object obj)
		{
			return true;
		}
	};

	protected GenericBrowserTree(IBrowserClient client, String name)
	{
		super(client, name);
		combinedContextActionMenu = new ActionContainer("Combined Tree Actions");
		contextMenu = new ActionContainer("Specific Tree Actions");
		combinedContextActionMenu.add(contextMenu);
		setBrowserTreeSelectionModel(new GenericBrowserSelectionModel());
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		m_specialSelectDelegate = new SpecialSelectionDelegate(this, getSelectionFilter());
		ActionContainer treeActiontMenu = new ActionContainer("General Tree Actions");
		treeActiontMenu.add(new ActionSeparator());
		ActionUtilities.addTreeActions(treeActiontMenu, this);

		combinedContextActionMenu.add(treeActiontMenu);
		genericBrowserTreeMouseListener = new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				IActionMgr actionMgr = CAFUtils.getInstance().getActiveActionMgr();
				IAction activeAction = null;
				if (actionMgr != null) {
					activeAction = actionMgr.getActiveAction();
				}
				//SP1704_dts0101252799_[CH] SP1704 java.lang.NullPointerException  at chs.ctf.caf.ui.FreezeUnfreezeSharedObjectCmd.&lt;init&gt;(FreezeUnfreezeSharedObjectCmd.java)
				if(activeAction instanceof SelectAction) {
					showContextMenu(e);
					notifySelectionChanged();
				}
			}

			public void mouseReleased(MouseEvent e)
			{
				IActionMgr actionMgr = CAFUtils.getInstance().getActiveActionMgr();
				IAction activeAction = null;
				if (actionMgr != null) {
					activeAction = actionMgr.getActiveAction();
				}
				//SP1704_dts0101252799_[CH] SP1704 java.lang.NullPointerException  at chs.ctf.caf.ui.FreezeUnfreezeSharedObjectCmd.&lt;init&gt;(FreezeUnfreezeSharedObjectCmd.java)
				if(activeAction instanceof SelectAction) {
					showContextMenu(e);
				}
			}
		};
		addMouseListener(genericBrowserTreeMouseListener);
	}

	protected IObjectFilter<Object> getSelectionFilter()
	{
		return PASS_ALL_FILTER;
	}

	public void showContextMenu(MouseEvent event)
	{
		if (event.isPopupTrigger()) {
			CAFUtils.getInstance().getWindowMgr().displayPopupMenu(combinedContextActionMenu, event, null);
		}
	}

	@Override public void destroy()
	{
		super.destroy();
		contextMenu = null;
		combinedContextActionMenu = null;
	}

	private void notifySelectionChanged()
	{
		m_client.selectionChanged();
		for (IActionNode node : combinedContextActionMenu.getMembers()) {
			if (node instanceof ActionContainer) {
				node.updateUI();
			}
		}
		//FIXME remove this after proper ribbon update is done PDV-8078

		IFIB fib = CAFUtils.getInstance().getFIB();

		fib.getUIMgr().getRibbon().ifPresent(ribbon -> fib.getWindowMgr().tickleUI());
	}

	public IUIDObjectIterator getSelectedObjects()
	{
		return m_specialSelectDelegate.getSelectedObjects();
	}

	public IUIDObject getParent(IUIDObject uidObject)
	{
		return m_specialSelectDelegate.getParent(uidObject);
	}

	public IUIDObjectIterator getChildren(IUIDObject uidObject)
	{
		return m_specialSelectDelegate.getChildren(uidObject);
	}

	public int getChildCount(IUIDObject uidObject)
	{
		return m_specialSelectDelegate.getChildCount(uidObject);
	}

	public void contextMenuAddAction(ActionEntry actionEntry)
	{
		ActionContainer newActions = new ActionContainer("");
		newActions.add(actionEntry);
		contextMenu.add(newActions);
	}

	// Turn off lots of stuff the superclass does that we don't want to do
	public void selectionChanged(SelectEvent e)
	{
	}

	protected void registerListeners()
	{ // no-op non-abstract method OK
	}

	protected void unregisterListeners()
	{
	}

	protected class GenericBrowserSelectionModel extends DefaultTreeSelectionModel
			implements IBrowserTreeSelectionModel
	{

		public void setRebuildingTree(boolean rebuildingTree)
		{
		}

		public void setSelectionFromMgr()
		{
			notifySelectionChanged();
		}

		public void updateSelectionFromMgr(SelectEvent se)
		{
			notifySelectionChanged();
		}
	}
}
