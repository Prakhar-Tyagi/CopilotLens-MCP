/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.caplet.ISpecialSelection;
import chs.caf.caplet.helpers.browser.BrowserTreeHelper;
import chs.caf.caplet.helpers.browser.IBrowserTreeNode;
import chs.common.IObjectFilter;
import chs.common.IUIDObject;
import chs.common.IUIDObjectIterator;
import chs.common.UIDObjectIterator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

public class SpecialSelectionDelegate implements ISpecialSelection
{

	private BrowserTreeHelper m_tree;
	private IObjectFilter m_filter;

	public SpecialSelectionDelegate(BrowserTreeHelper tree, IObjectFilter filter)
	{
		m_tree = tree;
		m_filter = filter;
	}

	public int getChildCount(IUIDObject uidObject)
	{
		TreePath path = m_tree.getSelectionPath();
		for (int i = 0; i < path.getPathCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getPathComponent(i);
			if (((IBrowserTreeNode) node).getUIDObject() == uidObject) {
				return node.getChildCount();
			}
		}
		return 0;
	}

	public IUIDObjectIterator getChildren(IUIDObject uidObject)
	{
		TreePath path = m_tree.getSelectionPath();
		Collection childColl = new ArrayList();
		for (int i = 0; i < path.getPathCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getPathComponent(i);
			if (((IBrowserTreeNode) node).getUIDObject() == uidObject) {
				for (Enumeration childEnum = node.children(); childEnum.hasMoreElements();) {
					IBrowserTreeNode childNode = (IBrowserTreeNode) childEnum.nextElement();
					childColl.add(childNode.getUIDObject());
				}
			}
		}
		return new UIDObjectIterator(childColl);
	}

	public IUIDObject getParent(IUIDObject uidObject)
	{
		TreePath path = m_tree.getSelectionPath();
		for (int i = 0; i < path.getPathCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getPathComponent(i);
			if (((IBrowserTreeNode) node).getUIDObject() == uidObject) {
				IBrowserTreeNode parentNode = (IBrowserTreeNode) node.getParent();
				if (parentNode != null) {
					return parentNode.getUIDObject();
				}
			}
		}
		return null;
	}

	public IUIDObjectIterator getSelectedObjects()
	{
		TreePath path = m_tree.getSelectionPath();
		Collection coll = new ArrayList();
		if (path != null) {
			IBrowserTreeNode node = (IBrowserTreeNode) path.getLastPathComponent();
			IUIDObject uidObject = node.getUIDObject();
			if (m_filter == null || m_filter.accept(uidObject)) {
				coll.add(uidObject);
			}
		}
		return new UIDObjectIterator(coll);
	}
}
