/*
 * Copyright 2010-2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.helpers.GfxViewHelper;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IDesign;
import chs.common.DesignUtils;
import chs.common.IUID;
import chs.ctf.ui.form.sharedobjectrevisioning.ShowUsagesSharedObjTreeModel;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.tree.action.ITreeDataChangeListener;
import chs.utilities.ui.tree.action.IZoomSelectedUsageInvalidStateNotifier;
import chs.utilities.ui.tree.action.SelectedOrRootTreeNodeAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: vthippan Date: Mar 13, 2010 Time: 7:29:03 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.ArtisanFunction},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
public class ZoomSelectedUsagesAction extends SelectedOrRootTreeNodeAction implements
		IZoomSelectedUsageInvalidStateNotifier
{

	private Collection<ITreeDataChangeListener> listeners;

	/**
	 * @param name name for the action, used for a menu or button.
	 * @param description description for the action, used for tooltip text and context-sensitive help.
	 * @param smallIcon small icon for the action, used for toolbar buttons.
	 * @param mnemonicKey key code used as the mnemonic for the action.
	 * @param acceleratorKey accelerator key
	 */
	public ZoomSelectedUsagesAction(String name, String description, ImageIcon smallIcon,
			Integer mnemonicKey, KeyStroke acceleratorKey)
	{
		super(name, description, smallIcon, mnemonicKey, acceleratorKey);
		listeners = new ArrayList<ITreeDataChangeListener>();
	}

	protected void actionPerformed(TreePath targetNodePath)
	{
		ShowUsagesSharedObjTreeModel.DiagramNodeUserObject diagramNode = getDiagramNode(targetNodePath);
		if (diagramNode == null) {
			return;
		}

		// If there is no active selection mgr, don't proceed
		ISelectMgr cafSelectMgr = getActiveSelectMgr();
		if (cafSelectMgr == null) {
			assert false : "SelectMgr should not be null when changing views.";
			return;
		}

		IBaseDiagram diagram = getDiagramFromNode(diagramNode);
		if (diagram == null) {
			assert false : "The diagram that this selection refers to should not be null";
			return;
		}

		GfxView view = GfxViewHelper.openDiagram(diagram);
		if (isGfxViewValid(view)) {
			SelectSet schemObjectSelection = constructSelectSetFromDiagramObject(diagramNode);
			addSelectionsToActiveSelectSet(schemObjectSelection);
			GfxViewHelper.zoomSelected(view, true);
		}
		else {
			//This means that there is some change in the design data. So, reload the design tree.
			notifyTreeDataChangeListeners();
		}
	}

	protected boolean isGfxViewValid(@Nullable GfxView view)
	{
		return view != null;
	}

	@Nullable protected ShowUsagesSharedObjTreeModel.DiagramNodeUserObject getDiagramNode(TreePath targetNodePath)
	{
		ShowUsagesSharedObjTreeModel.DiagramNodeUserObject diagramNode = null;
		Object last = targetNodePath.getLastPathComponent();
		if (last instanceof DefaultMutableTreeNode) {
			Object userObj = ((DefaultMutableTreeNode) last).getUserObject();
			if (userObj instanceof ShowUsagesSharedObjTreeModel.DiagramNodeUserObject) {
				diagramNode = (ShowUsagesSharedObjTreeModel.DiagramNodeUserObject) userObj;
			}
		}
		return diagramNode;
	}

	@Nullable protected IBaseDiagram getDiagramFromNode(ShowUsagesSharedObjTreeModel.DiagramNodeUserObject diagramNode)
	{
		IUID designUID = FactoryMgr.getCommonFactory().getUID(diagramNode.getDiagramData().getDesignUID());
		IDesign design = DesignUtils.getLoadedDesign(designUID, IDesign.class);
		IUID diagramUID = FactoryMgr.getCommonFactory().constructUID(diagramNode.getDiagramData().getDiagramUID());
		return design == null ? null : design.getDiagram(diagramUID);
	}

	@NotNull protected SelectSet constructSelectSetFromDiagramObject(
			ShowUsagesSharedObjTreeModel.DiagramNodeUserObject diagramNode)
	{
		SelectSet schemObjectSelection = new SelectSet();

		Set<String> strSchemUIDs = diagramNode.getDiagramData().getSchemObjects();
		for (String strSchemUID : strSchemUIDs) {
			IUID schemObjectUID = FactoryMgr.getCommonFactory().getUID(strSchemUID);
			if (schemObjectUID == null || schemObjectUID.getObject() == null) {
				continue;
			}
			schemObjectSelection.add(new Selection(schemObjectUID.getObject()));
		}
		return schemObjectSelection;
	}

	protected void addSelectionsToActiveSelectSet(SelectSet schemObjectSelection)
	{
		// If there is an active selection mgr, then update the current set with it's in progress selections
		ISelectMgr cafSelectMgr = getActiveSelectMgr();
		SelectSet currentSet = cafSelectMgr != null ? cafSelectMgr.getPreSelections() : null;
		if (currentSet != null) {
			currentSet.clear();
			currentSet.add(schemObjectSelection);
		}
	}

	@Nullable protected ISelectMgr getActiveSelectMgr()
	{
		return CAFUtils.getInstance().getActiveSelectMgr();
	}

	protected boolean isEnabled(TreePath targetNodePath)
	{
		Object last = targetNodePath.getLastPathComponent();
		if (last instanceof DefaultMutableTreeNode) {
			Object userObj = ((DefaultMutableTreeNode) last).getUserObject();
			if (userObj instanceof ShowUsagesSharedObjTreeModel.DiagramNodeUserObject) {
				return true;
			}
		}
		return false;
	}

	public static AbstractAction getZoomTreeMenuAction()
	{
		return new ZoomSelectedUsagesAction(
				ResourceMgr.getString(ZoomSelectedUsagesAction.class, "ZoomSelectedUsagesAction.name.text_1"),
				ResourceMgr.getString(ZoomSelectedUsagesAction.class, "ZoomSelectedUsagesAction.description.text_2"),
				null, KeyEvent.VK_Z, KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0));
	}

	@Override public void addTreeDataChangeListener(ITreeDataChangeListener treeDataChangeListener)
	{
		listeners.add(treeDataChangeListener);
	}

	@Override public void notifyTreeDataChangeListeners()
	{
		for (ITreeDataChangeListener treeDataChangeListener : listeners) {
			treeDataChangeListener.treeDataChanged();
		}
	}
}
