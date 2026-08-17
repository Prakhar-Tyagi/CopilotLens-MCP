/*
 * Copyright 2005-2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.shared;

import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedAbstractable;
import chs.common.IDesignAbstraction;
import chs.utility.helpers.NamedObjectComparator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

public class MappedTreeModel extends DefaultMutableTreeNode
{

	private Map<IDesignAbstraction, SortableTreeNode> m_abstractions =
			new HashMap<IDesignAbstraction, SortableTreeNode>();
	private TreeNode m_firstInteresting;

	private static class SortableTreeNode extends DefaultMutableTreeNode
	{
		SortableTreeNode(Object o)
		{
			super(o);
		}

		public void sortChildren(Comparator comparator)
		{
			if (children != null) {
				Collections.sort(children, comparator);
			}
		}
	}

	public void addElement(Object o, final boolean doSort)	
	{
		SortableTreeNode toadd = new SortableTreeNode(o);
		if (o instanceof ILogicObject) {
			o = ((ILogicObject) o).getSharedObject();
		}
		if (!(o instanceof ISharedAbstractable
				&& ((ISharedAbstractable) o).getDesignAbstraction() != null)) {
			// It doesn't have an abstraction level.
			add(toadd, doSort);
		}
		else {
			IDesignAbstraction da = ((ISharedAbstractable) o).getDesignAbstraction();
			SortableTreeNode datn = m_abstractions.get(da);
			if (datn == null) {
				datn = new SortableTreeNode(da)
				{
					public void add(MutableTreeNode newChild)
					{
						super.add(newChild);
						if (doSort) {
							sortChildren(new MappedTreeModelComparator());
						}
					}
				};
				m_abstractions.put(da, datn);
				add(datn, doSort);
			}
			datn.add(toadd);
		}
		if (m_firstInteresting == null) {
			m_firstInteresting = toadd;
		}
	}

	public void addElement(Object o)
	{
		addElement(o, true);
	}

	public void add(MutableTreeNode newChild, boolean doSort)
	{
		super.add(newChild);
		if (doSort && children!=null) {
			Collections.sort(children, new MappedTreeModelComparator());
		}
	}

	public void addElements(List elements)
	{
		for (Iterator iter = elements.iterator(); iter.hasNext();) {
			addElement(iter.next(), false);
		}

		// Sort once everything added
		MappedTreeModelComparator comparator = new MappedTreeModelComparator();
		if (children != null) {
			Collections.sort(children, comparator);
		}
		// Also sort children of abstraction nodes
		for (SortableTreeNode datn : m_abstractions.values()) {
			datn.sortChildren(comparator);
		}

	}

	public TreeNode getRoot()
	{
		return super.getRoot();
	}

	public void removeAllElements()
	{
		removeAllChildren();
		m_abstractions.clear();
		m_firstInteresting = null;
	}

	public int size()
	{
		return getChildCount();
	}

	public TreePath getInterestingNodePath()
	{
		TreeNode node = m_firstInteresting;
		List list = new ArrayList();

		// Add all nodes to list
		while (node != null) {
			list.add(node);
			node = node.getParent();
		}
		Collections.reverse(list);

		// Convert array of nodes to TreePath
		return new TreePath(list.toArray());
	}

	private static class MappedTreeModelComparator extends NamedObjectComparator<Object>
	{

		private MappedTreeModelComparator()
		{
			super(true,true,false);
		}

		public int compare(Object n1, Object n2)
		{
			Object o1 = ((DefaultMutableTreeNode) n1).getUserObject();
			Object o2 = ((DefaultMutableTreeNode) n2).getUserObject();
			if (o1 instanceof ILogicObject) {
				o1 = ((ILogicObject) o1).getSharedObject();
			}
			if (o2 instanceof ILogicObject) {
				o2 = ((ILogicObject) o2).getSharedObject();
			}

			if (o1 instanceof ISharedAbstractable
					&& ((ISharedAbstractable) o1).getDesignAbstraction() != null
					&& o2 instanceof ISharedAbstractable
					&& ((ISharedAbstractable) o2).getDesignAbstraction() != null) {
				// Both elements have an abstraction level.
				IDesignAbstraction da1 = ((ISharedAbstractable) o1).getDesignAbstraction();
				IDesignAbstraction da2 = ((ISharedAbstractable) o2).getDesignAbstraction();
				int c = super.compare(da1, da2);
				if (c != 0) {
					return c;
				}
			}
			else if (o1 instanceof ISharedAbstractable
					&& ((ISharedAbstractable) o1).getDesignAbstraction() != null) {
				// Only o1 has an abstraction level.
				return 1;
			}
			else if (o2 instanceof ISharedAbstractable
					&& ((ISharedAbstractable) o2).getDesignAbstraction() != null) {
				// Only o2 has an abstraction level.
				return -1;
			}

			return super.compare(o1, o2);
		}
	}
}
