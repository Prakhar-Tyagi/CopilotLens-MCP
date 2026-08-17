/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2013-2025 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.shared.UnshareModularConnectorRenameTree;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IChangeListener;
import chs.common.IGenericContext;
import chs.common.IViewNotifier;
import chs.ctf.caf.ui.SimpleOkCancelDialog;
import chs.services.gfx.GfxView;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 29 Apr, 2013.
 */
public class ModularConnectorUnshareHelper extends ConnectorUnshareHelper
{

	protected Map<IConnector, String> m_newNames = new HashMap<IConnector, String>();
	protected List<IConnector> m_connsInHierarchy = new ArrayList<IConnector>();
	protected Map<IConnector, ISharedConnector> splMap = new HashMap<IConnector, ISharedConnector>();
	private static final int RENAME_DIALOG_WIDTH = 300;

	public ModularConnectorUnshareHelper(IDesign theDesign, @Nullable ISchemDiagram diagram)
	{
		super(theDesign, diagram);
	}

	@Override
	@NotNull public IActionEnum setup(BaseShareActionOperands operands, @Nullable ISchemDiagram diagram)
	{
		IActionEnum actionResult = super.setup(operands, diagram);
		populateConnectorsInHierarchy();
		for (IConnector connector : m_connsInHierarchy) {
			splMap.put(connector, (ISharedConnector) connector.getSharedPinList());
		}
		return actionResult;
	}

	private void populateConnectorsInHierarchy()
	{
		m_connsInHierarchy.addAll(getConnectorsInHierarchy());
	}

	@NotNull
	private Collection<IConnector> getConnectorsInHierarchy()
	{
		Collection<IConnector> connectors = ((IConnector) cablePinList).getAllConnectorsInHierarchy();

		return connectors;
	}

	@Override protected void unshareThisPinList()
	{
		for (IConnector connector : m_connsInHierarchy) {
			connector.setSharedPinList(null, true);
			unshareBackShells(connector);
		}
	}

	protected Set<ILogicObject> getLockableCableObjects()
	{
		Set<ILogicObject> lockables = new HashSet<>();
		lockables.addAll(super.getLockableCableObjects());
		for (IConnector childConnector : getConnectorsInHierarchy()) {
			lockables.add(childConnector);
			IBackshell backshell = getBackshell(childConnector);
			if (backshell != null) {
				lockables.add(backshell);
			}
		}
		return lockables;
	}

	protected void unsharePins()
	{
		for (IConnector connector : m_connsInHierarchy) {
			unsharePins(connector);
		}
	}

	@Override protected Collection<? extends IPinList> getAdditionalSchemObjectsToProcess()
	{
		Collection<IPinList> allConnsInHierarchy = new HashSet<IPinList>();
		IConnector topLevelConnector = (IConnector) cablePinList;
		Collection<IConnector> connectors = topLevelConnector.getAllConnectorsInHierarchy();

		for (IConnector conn : connectors) {
			for (IDiagramObject obj : design.getRepresentations(conn.getUID())) {
				allConnsInHierarchy.add((IPinList) obj);
			}
		}

		return allConnsInHierarchy;
	}

	@Override protected void copyInfoFromSharedToLogicObject(ISharedPinList spl)
	{
		for (Map.Entry<IConnector, ISharedConnector> entry : splMap.entrySet()) {
			replicator.replicateCopyableObject(entry.getValue(), entry.getKey());
		}
	}

	@Override protected void renamePinlist(chs.cof.logical.cable.IPinList pinlist, chs.cof.logical.cable.IPinList mate)
	{
		for (IConnector connector : m_connsInHierarchy) {
			String newName = m_newNames.get(connector);
			if (newName != null) {
				connector.setName(null);
				connector.setName(newName);
			}
		}
	}

	protected boolean promptRenameLocalPinList(@NotNull chs.cof.logical.cable.IPinList pinlist,
			@Nullable chs.cof.logical.cable.IPinList mateConn)
	{
		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view == null) {
			return true;
		}

		UnshareModularConnectorRenameTree tree = new UnshareModularConnectorRenameTree((IConnector) pinlist);

		UnshareModularConnectorRenameDialog dialog =
				new UnshareModularConnectorRenameDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
						ResourceMgr.getString(ModularConnectorUnshareHelper.class,
								"ModularConnectorUnshareHelper.ModularConnectorRename.Title"), tree);

		JPanel treePanel = new JPanel(new BorderLayout(5, 10));
		JScrollPane treeView = new JScrollPane(tree.getTree());
		treeView.setPreferredSize(new Dimension(RENAME_DIALOG_WIDTH, 100));
		treePanel.add(treeView, BorderLayout.CENTER);
		dialog.setComponent(treePanel, true);
		tree.registerListener(dialog, null);
		dialog.pack();
		dialog.setVisible(true);

		if (dialog.isCancelled()) {
			return false;
		}
		m_newNames = new HashMap<IConnector, String>(tree.getObjNameMap());
		return true;
	}

	public static class UnshareModularConnectorRenameDialog extends SimpleOkCancelDialog implements IChangeListener
	{

		private UnshareModularConnectorRenameTree treeComp = null;

		public UnshareModularConnectorRenameDialog(Frame frame, String title, UnshareModularConnectorRenameTree tree)
		{
			super(frame, title);
			treeComp = tree;
			getOkButton().setEnabled(treeComp.isTreeValid());
			getHelpButton().setVisible(false);
		}

		@Override public void changeNotify(IViewNotifier source, String message, IGenericContext context)
		{
			getOkButton().setEnabled(treeComp.isTreeValid());
		}
	}

	protected boolean determineToUnshareAll(IDesignWideUsageMgr dwum)
	{
		return true;
	}

	protected boolean isOkForUnshareAll()
	{
		return true;
	}
}
