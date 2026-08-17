/*
 * Copyright 2004-2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 * created by : Pranab Chakravarty Date: Jul 4, 2013 Time: 6:52:09 PM
 */
package chs.caplets.logic;

import chs.caf.cafmain.actions.link.LinkHandler;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ILinkClient;
import chs.caf.caplet.helpers.browser.BrowserFolder;
import chs.caf.caplet.helpers.browser.BrowserFolderComparator;
import chs.caf.caplet.helpers.browser.LinkBrowserTree;
import chs.caf.caplet.helpers.browser.LinkTreeClientDelegate;
import chs.caf.caplet.helpers.browser.LinksFolder;
import chs.caf.caplet.helpers.browser.LogicBrowserTree;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.cof.links.LinkType;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IMulticore;
import chs.common.IUID;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JPanel;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogicLinkBrowserClient extends BrowserClient implements ILinkClient
{

	private LinkTreeClientDelegate m_browserClientDelegate;

	public LogicLinkBrowserClient(ICapletController controller, LinkHandler handler)
	{
		super(controller);
		m_browserClientDelegate = new LinkTreeClientDelegate(this, getModel().getDesign(), handler);
	}

	protected void buildChildrenFolders()
	{

	}

	public void setSelection(String link)
	{
		m_browserClientDelegate.setSelection(link);
	}

	@Override @Nullable public Icon getIcon(@NotNull IUID uid)
	{
		Icon icon = m_browserClientDelegate.getApplicableIcon(getObject(uid));
		if (icon == null) {
			icon = super.getIcon(uid);
		}
		return icon;
	}

	public void destroy()
	{
		m_browserClientDelegate.clearCache();
	}

	public List<IUID> getChildren(IUID uid)
	{
		return m_browserClientDelegate.getChildren(uid, new LogicFolderComparator(), cmp, null);
	}

	public boolean hasChildren(IUID uid, IUID parentUID)
	{
		// the root node always has children
		if (uid == getRoot()) {
			return true;
		}
		setParentUID(parentUID);
		return m_browserClientDelegate.nonRootObjectshaveChildren(uid);
	}

	@Nullable public BrowserFolder getBrowserFolderForObject(Set<BrowserFolder> folderSet, IUIDObject obj)
	{
		LogicFolder logfolder = getLogicFolderForObject(obj);
		BrowserFolder folder = null;
		if (logfolder != null) {
			IUIDObject folderObj = getObjectFolder(logfolder, folderSet);
			if (folderObj == null) {
				folder = createObjectFolder(logfolder);
			}
			else {
				folder = (BrowserFolder) folderObj;
			}
		}
		return folder;
	}

	@NotNull public List<IUID> getBrowserFolderChildrens(@NotNull IUIDObject obj)
	{
		return getFolderChildren(obj);
	}

	public IUIDObject getSpecialBrowserFolderObject(BrowserFolder fold)
	{
		return m_browserClientDelegate.getSpecialBrowserFolderObject(fold);
	}

	public void createSpecialFoldersAndChilds(LinksFolder linkFolder, Set<IUIDObject> linkedObjects,
			Map<BrowserFolder, List<IUID>> folderObjectMap, Set<BrowserFolder> folderSet)
	{
	}

	@Override public Set<LinkType> getApplicableLinkTypes()
	{
		return LinkTreeClientDelegate.getApplicableLinkTypes(getModel().getDesign(), false);
	}

	@Override public JPanel buildToolbar()
	{
		return m_browserClientDelegate.buildToolbar();
	}

	@Override public JPanel getToolbar()
	{
		return m_browserClientDelegate.getToolbar();
	}

	@Override public void setTree(LinkBrowserTree tree)
	{
		m_browserClientDelegate.setTree(tree);
	}

	public void selectionChanged()
	{
		m_browserClientDelegate.selectionChanged();
	}

	@Override public Set<IUIDObject> getLinksFor(@NotNull IUIDObject obj)
	{
		return m_browserClientDelegate.getLinksFor(obj);
	}

	public String getToolTipText(IUID uid, IUID parentUID)
	{
		String tooltip = m_browserClientDelegate.getApplicableToolText(getObject(uid));
		if (tooltip == null) {
			tooltip = super.getToolTipText(uid, parentUID);
		}
		return tooltip;
	}

	@Override public SelectSet adjustSelectSet(SelectSet selectSet)
	{
		SelectSet superSet = new SelectSet();
		for (SelectionIterator it = selectSet.getSelected(); it.hasNext(); ) {
			Selection sel = it.getNext();
			superSet.add(sel, false);
			if (sel.getNonDeletedObject() instanceof IMulticore) {
				IMulticore mc = (IMulticore) sel.getNonDeletedObject();
				if (mc != null) {
					for (IConductor c : mc.getAllConductorsInHierarchy(true)) {
						superSet.add(new Selection(c), false);
					}
				}
			}
		}

		return LogicBrowserTree.adjustSelectSet(superSet);
	}

	@Override public void updateDimmer(List<IUID> dimObjects)
	{

	}

	public static class LogicFolderComparator extends BrowserFolderComparator
	{

		private static final int DEVICE_ORDINAL = 0;
		private static final int CONNECTOR_ORDINAL = 1;
		private static final int BLOCK_ORDINAL = 2;
		private static final int INLINE_ORDINAL = 3;
		private static final int SPLICE_ORDINAL = 4;
		private static final int RING_TERMINAL_ORDINAL = 5;
		private static final int WIRE_ORDINAL = 6;
		private static final int MULTICORE_ORDINAL = 7;
		private static final int OVERBRAID_ORDINAL = 8;
		private static final int HIGHWAYS_ORDINAL = 9;
		private static final int ASSEMBLY_ORDINAL = 10;
		private static final int FUNCTION_COMP_ORDINAL = 11;
		private static final int FUNCTION_COND_ORDINAL = 12;
		private static final int CABLES_ORDINAL = 13;
		private static final int UNKNOWN_ORDINAL = 14;

		protected int get_OrdinalForType(BrowserFolder obj)
		{
			if (obj.getName().equals(LogicFolder.CONNECTOR.getDisplayName())) {
				return CONNECTOR_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.RING_TERMINAL.getDisplayName())) {
				return RING_TERMINAL_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.SPLICE.getDisplayName())) {
				return SPLICE_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.CONDUCTORS.getDisplayName())) {
				return WIRE_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.OVERBRAID.getDisplayName())) {
				return OVERBRAID_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.ASSEMBLY.getDisplayName())) {
				return ASSEMBLY_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.DEVICE.getDisplayName())) {
				return DEVICE_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.INLINE.getDisplayName())) {
				return INLINE_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.MULTICORE.getDisplayName())) {
				return MULTICORE_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.HIGHWAYS.getDisplayName())) {
				return HIGHWAYS_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.SINGLE_LINES.getDisplayName())) {
				return CABLES_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.LOGIC_BLOCKS.getDisplayName())) {
				return BLOCK_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.FUNCTION_COMPONENT.getDisplayName())) {
				return FUNCTION_COMP_ORDINAL;
			}
			else if (obj.getName().equals(LogicFolder.FUNCTION_CONDUCTOR.getDisplayName())) {
				return FUNCTION_COND_ORDINAL;
			}
			return UNKNOWN_ORDINAL;
		}
	}
}
