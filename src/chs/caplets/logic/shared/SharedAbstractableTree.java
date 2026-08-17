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

import chs.caf.CAFUtils;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedAbstractable;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IOptionedObject;
import chs.cog.ICOGLoadable;
import chs.common.IDesignAbstraction;
import chs.common.IReadOnlyNamedObject;
import chs.common.IRevisionedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utilities.StringUtils;
import chs.utilities.ui.tree.IFilterOption;
import chs.utilities.ui.tree.IObjectUIFilterOption;
import chs.utilities.ui.tree.TreeUtils;
import chs.utility.DuplicateNamedObjectChecker;
import chs.utility.NamedObjectListUtils;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SharedAbstractableTree extends JTree
{

	private MappedTreeModel m_pinListModel;
	@NotNull private Set<IObjectUIFilterOption> m_filters = new LinkedHashSet<>();
	private IDesign m_design;

	public SharedAbstractableTree(IDesign design)
	{
		this(Collections.EMPTY_LIST, design);
	}

	public SharedAbstractableTree(List elements, IDesign design)
	{
		super(new MappedTreeModel());
		m_design = design;
		setRootVisible(false);
		m_pinListModel = (MappedTreeModel) getModel().getRoot();
		addElements(elements);
		setCellRenderer(new DefaultTreeCellRenderer()
		{
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
					boolean leaf, int row, boolean hasFocus)
			{
				Icon icn = null;
				Object txtval = value;
				if (value != null && value instanceof DefaultMutableTreeNode) {

					// De-reference value
					DefaultMutableTreeNode dtm = (DefaultMutableTreeNode) value;
					value = dtm.getUserObject();
					if (value instanceof IUID) {
						value = chs.system.UIDMgr.getObject((IUID) value);
					}

					// Determine icon
					if (value instanceof IDesignAbstraction) {
						icn = IconUtils.getDesignAbstractionIcon(IconUtils.ACTIVE);
					}
					else if (value instanceof IUIDObject) {
						icn = IconUtils.getIcon((IUIDObject) value);
					}

					// Determine text
					if (value instanceof IRevisionedObject) {
						IRevisionedObject robj = (IRevisionedObject) value;
						txtval = robj.getName() + ":" + robj.getRevision();
					}
					else if (value instanceof IReadOnlyNamedObject) {
						IReadOnlyNamedObject nobj = (IReadOnlyNamedObject) value;
						txtval = nobj.getName();
					}
					else if (value instanceof MappedTreeModel) {
						txtval = "Shared Objects";
					}
					else {
						txtval = value;
					}
				}

				super.getTreeCellRendererComponent(tree, txtval, selected, expanded, leaf, row, hasFocus);

				// Work out what text to display - take the initial value as starters
				String text = getText();
				if (value instanceof IInlinePlugConnector) {
					IInlinePlugConnector pl = (IInlinePlugConnector) value;
					text = pl.getName() + "::" + NamedObjectListUtils.convertNamedObjectListToString(pl.getMates());
				}
				setText(text);

				String toolTipText = "";
				if (icn != null) {
					ISharedObject shared = null;
					if (value instanceof ILogicObject) {
						shared = ((ILogicObject) value).getSharedObject();
					}
					else if (value instanceof ISharedObject) {
						shared = (ISharedObject) value;
					}
					if (shared != null) {
						for (IObjectUIFilterOption filter : m_filters) {
							if (!filter.filterIn(shared)) {
								final Icon newIcon = filter.getIcon(shared);
								if (newIcon != null) {
									icn = newIcon;
								}
								toolTipText = filter.getDescription(shared);
								break;
							}
						}
					}
					setIcon(icn);
				}

				setToolTipText(determineToolTipText(value, toolTipText));
				return this;
			}

			@Nullable public String determineToolTipText(@Nullable Object object, @Nullable String clientToolTipText)
			{
				if (!StringUtils.isBlank(clientToolTipText)) {
					return clientToolTipText;
				}
				if (object instanceof IOptionedObject) {
					// todo: optimise costly operation
					return DuplicateNamedObjectChecker.getToolTipForDuplicateNamedObject((IOptionedObject) object,
							CAFUtils.getInstance().getCurrentProject());
				}
				return clientToolTipText;
			}
		});
	}

	@NotNull public Set<IObjectUIFilterOption> getFilters()
	{
		return m_filters;
	}

	public void registerFilter(@NotNull IObjectUIFilterOption filterOption)
	{
		m_filters.add(filterOption);
	}

	@SuppressWarnings("unused")
	public void unregisterFilter(@NotNull IFilterOption filterOption)
	{
		m_filters.remove(filterOption);
	}

	public void clear()
	{
		m_pinListModel.removeAllElements();
	}

	public void addElement(Object o)
	{
		m_pinListModel.addElement(o);
	}

	public void addElements(List elements)
	{
		addElements(elements,true);
	}

	public void addElements(List<?> elements, boolean expand)
	{
		m_pinListModel.addElements(elements);
		TreeNode tn = m_pinListModel.getRoot();
		if (expand) {
			for (int i = 0; i < tn.getChildCount(); i++) {
				expandPath(new TreePath(new Object[]{tn, tn.getChildAt(i)}));
			}
			setSelectionPath();
		}
	}

	public void setSelectionPath()
	{
		if (m_pinListModel.size() > 0) {
			TreePath treePath = m_pinListModel.getInterestingNodePath();
			if (treePath != null) {
				Object lastComponent = treePath.getLastPathComponent();
				if (lastComponent instanceof DefaultMutableTreeNode) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastComponent;
					Object userObject = node.getUserObject();
					if (userObject instanceof ISharedConnector) {
						((ICOGLoadable) userObject).loadChildren();
					}
				}
			}
			super.setSelectionPath(m_pinListModel.getInterestingNodePath());
		}
	}

	/**
	 * Uses SharedNameComparator to find a likely shared object match based on name and design abstraction.  Selects it
	 * in the tree if found.
	 *
	 * @param schemName Name of shared object to match
	 */
	public void selectShareIntoCandidate(String schemName)
	{
		DefaultMutableTreeNode match = TreeUtils.getNodeForObject(m_pinListModel, schemName, SharedNameComparator);
		if (match != null) {
			setSelectionPath(new TreePath(match.getPath()));
			scrollPathToVisible(new TreePath(match.getPath()));
		}
	}

	// Compares a string NAME with a shared object name - and uses some other factors to pick best candidate.
	private final Comparator<Object> SharedNameComparator = new Comparator<Object>()
	{
		public int compare(Object o1, Object o2)
		{
			assert (o1 instanceof DefaultMutableTreeNode);
			assert (o2 instanceof String);

			DefaultMutableTreeNode node = (DefaultMutableTreeNode) o1;
			String schemName = (String) o2;

			Object obj = node.getUserObject();
			if (!(obj instanceof ISharedAbstractable)) {
				// Its a abstraction node - no match
				return -1;
			}

			assert (obj instanceof ISharedPinList || obj instanceof ISharedMulticore);
			if(obj instanceof ISharedPinList) {
				ISharedPinList sharedPinList = (ISharedPinList) obj;

				if (m_design != null && sharedPinList.getDesignAbstraction() != m_design.getDesignAbstraction()) {
					// Not in the same abstraction - no match
					return -1;
				}

				if (sharedPinList.getName().equalsIgnoreCase(schemName)) {
					// Matched
					return 0;
				}
			}
			else {
				ISharedMulticore sharedMulticore = (ISharedMulticore) obj;

				if (m_design != null && sharedMulticore.getDesignAbstraction() != m_design.getDesignAbstraction()) {
					// Not in the same abstraction - no match
					return -1;
				}

				if (sharedMulticore.getName().equalsIgnoreCase(schemName)) {
					// Matched
					return 0;
				}
			}

			// No match
			return -1;
		}
	};
}
