/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caplets.logic.commands.AssociateConnectorCommand;
import chs.cof.library.ILibrariedObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInternalPosition;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedInternalPosition;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.SharedPinHelper;
import chs.cof.parts.ILibraryBaseConnector;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryHousingDefinition;
import chs.cof.parts.ILibraryObject;
import chs.common.IDesignAbstraction;
import chs.common.IReadOnlyNamedObject;
import chs.common.UIDObjectCollection;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.ctf.editui.IInternalPositionUsageManager;
import chs.ctf.editui.logic.LogicInternalPositionUsageManager;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.GroupTypeValue;
import chs.utilities.ui.property.IComponentProperty;
import chs.utilities.ui.property.IPropertyGroup;
import chs.utilities.ui.property.PropertyFactory;
import chs.utilities.ui.property.PropertyPanel;
import chs.utilities.ui.tree.BaseJTree;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.logic.ModularConnectorHelper;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class AssociateConnectorDialog extends CAFOkCancelDialog
{

	private static AssociateConnectorCommand m_command;
	private IComponentProperty m_treeComponentProp;
	private static JTree m_tree;
	private static AssociateConnectorDialog m_dialog;
	private IInternalPositionUsageManager m_positionUsageManager;
	private boolean m_showDialog = true;

	public enum DIALOG_RESULT
	{
		OK, CANCEL, NOSHOW
	}

	private AssociateConnectorDialog(Frame frame, String title, AssociateConnectorCommand command)
	{
		super(frame, title, true);
		m_command = command;
		buildView();
	}

	private void buildView()
	{
		IPropertyGroup mainGrp = PropertyFactory.createPropertyGroup("Associate Connector", GroupTypeValue.COLUMN);
		mainGrp.setI18NName("ModularConnectorDialog");
		mainGrp.setLabel(
				ResourceMgr.getString(AssociateConnectorDialog.class, "AssociateConnectorDialog.modular.label"));

		m_treeComponentProp = mainGrp.createComponentProperty("Modular Connectors");
		m_treeComponentProp.setI18NName("ModularTreepanel");
		if (populateValidModularTree()) {
			getContentPane().add(new PropertyPanel("Select Connector", mainGrp));
		}
	}

	private boolean populateValidModularTree()
	{
		UIDObjectCollection validConns = m_command.getValidConnectors();
		IConnector selectedConnector = m_command.getSelectedConnector();
		JScrollPane scrollpane = new JScrollPane();
		scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		boolean sharedConnector = false;
		List<DefaultMutableTreeNode> sharedPositionNode = new ArrayList<DefaultMutableTreeNode>();
		if (validConns.size() > 0) {
			DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root", true);
			for (Object validConn : validConns) {

				if (validConn instanceof IConnector) {
					IConnector pConn = (IConnector) validConn;

					m_positionUsageManager = new LogicInternalPositionUsageManager(pConn);

					DefaultMutableTreeNode node = new DefaultMutableTreeNode(pConn, true);
					Collection<IInternalPosition> positions = pConn.getPositions();
					for (IInternalPosition position : positions) {
						if (!position.isOccupied() &&
								housingDefMatches(position, pConn, m_command.getSelectedConnector()) &&
								ModularConnectorHelper.canCavitiesBeBlocked(pConn, getCavitiesToBeBlocked(pConn, selectedConnector,
										position.getName()))) {
							//Check if there are any blocked positions
							if (m_positionUsageManager.canAssignPositionTo(position.getName(), selectedConnector,
									pConn.getLibraryObject())) {
								node.add(new DefaultMutableTreeNode(position, false));
							}
						}
					}
					if (node.getChildCount() > 0) {
						rootNode.add(node);
					}
				}
				else if (validConn instanceof ISharedConnector) {
					sharedConnector = true;
					ISharedConnector pConn = (ISharedConnector) validConn;

					m_positionUsageManager = new LogicInternalPositionUsageManager(pConn);

					DefaultMutableTreeNode node = new DefaultMutableTreeNode(pConn, true);
					Collection<ISharedInternalPosition> positions = pConn.getPositions();
					for (ISharedInternalPosition position : positions) {
						if (!position.isOccupied() &&
								sharedHousingDefMatches(position, pConn, m_command.getSelectedConnector()) &&
								ModularConnectorHelper.canCavitiesBeBlocked(pConn, getCavitiesToBeBlocked(pConn, selectedConnector,
										position.getName()))) {
							//Check if there are any blocked positions
							if (m_positionUsageManager.canAssignPositionTo(position.getName(), selectedConnector,
									pConn.getLibraryObject())) {
								node.add(new DefaultMutableTreeNode(position, false));
								sharedPositionNode.add(node);
							}
						}
					}
				}
			}

			if (sharedConnector) {
				Map<IDesignAbstraction, List<DefaultMutableTreeNode>> connectorAbsMap =
						sortAsPerAbstraction(sharedPositionNode);

				SortedSet<IDesignAbstraction> keys =
						new TreeSet<IDesignAbstraction>(new MappedTreeModelComparator<IDesignAbstraction>());
				keys.addAll(connectorAbsMap.keySet());
				DefaultMutableTreeNode node = null;
				for (IDesignAbstraction designAbstraction : keys) {
					if (designAbstraction != null) {
						node = new DefaultMutableTreeNode(designAbstraction, true);

						List<DefaultMutableTreeNode> connectorList = connectorAbsMap.get(designAbstraction);
						List<DefaultMutableTreeNode> sortedConnectorList = sort(connectorList);
						for (DefaultMutableTreeNode connector : sortedConnectorList) {
							node.add(connector);
						}
						if (node.getChildCount() > 0) {
							rootNode.add(node);
						}
					}
					else {
						List<DefaultMutableTreeNode> connectorList = connectorAbsMap.get(designAbstraction);
						List<DefaultMutableTreeNode> sortedConnectorList = sort(connectorList);
						for (DefaultMutableTreeNode connector : sortedConnectorList) {
							rootNode.add(connector);
						}
					}
				}
			}
			else {
				int numberofChildren = rootNode.getChildCount();
				List<DefaultMutableTreeNode> connectorList = new ArrayList<DefaultMutableTreeNode>();
				for (int i = 0; i < numberofChildren; i++) {
					DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
					connectorList.add(childNode);
				}
				List<DefaultMutableTreeNode> sortedConnectorList = sort(connectorList);
				rootNode.removeAllChildren();
				for (DefaultMutableTreeNode node : sortedConnectorList) {
					rootNode.add(node);
				}
			}

			if (rootNode.getChildCount() == 0) {
				m_showDialog = false;
				return false;
			}

			m_tree = new BaseJTree(new DefaultTreeModel(rootNode));
			m_tree.setName("modulartree");
			m_tree.setRootVisible(false);
			m_tree.setCellRenderer(new LogicModularTreeCellRenderer());
			m_tree.addTreeSelectionListener(new ModularTreeSelectionListener(m_command));
			m_tree.setShowsRootHandles(true);
			m_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			scrollpane.getViewport().setView(m_tree);
		}
		m_treeComponentProp.setObject(scrollpane);
		return true;
	}

	private List<DefaultMutableTreeNode> sort(List<DefaultMutableTreeNode> nodeList)
	{
		DefaultMutableTreeNode[] array = new DefaultMutableTreeNode[nodeList.size()];
		int index = 0;
		for (DefaultMutableTreeNode node : nodeList) {
			array[index++] = node;
		}
		boolean swap = false;
		for (int i = 0; i < array.length - 1; i++) {
			for (int j = i + 1; j < array.length; j++) {
				swap = false;
				if (((DefaultMutableTreeNode) array[1]).getUserObject() instanceof ISharedConnector) {
					if (((ISharedConnector) ((DefaultMutableTreeNode) array[j]).getUserObject()).getName().compareTo(
							((ISharedConnector) ((DefaultMutableTreeNode) array[i]).getUserObject()).getName()) < 0) {
						swap = true;
					}
				}
				if (((DefaultMutableTreeNode) array[1]).getUserObject() instanceof IConnector) {
					if (((IConnector) ((DefaultMutableTreeNode) array[j]).getUserObject()).getName().compareTo(
							((IConnector) ((DefaultMutableTreeNode) array[i]).getUserObject()).getName()) < 0) {
						swap = true;
					}
				}
				if (swap) {
					DefaultMutableTreeNode temp = array[i];
					array[i] = array[j];
					array[j] = temp;
				}
			}
		}
		return Arrays.asList(array);
	}

	private class MappedTreeModelComparator<IDesignAbstraction> extends NamedObjectComparator<IDesignAbstraction>
	{

		public MappedTreeModelComparator()
		{
			super(true);
		}

		public int compare(Object n1, Object n2)
		{
			IDesignAbstraction da1 = (IDesignAbstraction) n1;
			IDesignAbstraction da2 = (IDesignAbstraction) n2;
			int c = super.compare(da1, da2);
			if (c != 0) {
				return c;
			}
			else if (da1 != null) {
				// Only o1 has an abstraction level.
				return 1;
			}
			else if (da2 != null) {
				// Only o2 has an abstraction level.
				return -1;
			}
			return 0;
		}
	}

	private Map<IDesignAbstraction, List<DefaultMutableTreeNode>> sortAsPerAbstraction(
			List<DefaultMutableTreeNode> sharedPositionNode)
	{
		Map<IDesignAbstraction, List<DefaultMutableTreeNode>> connectorAbsMap = new HashMap();

		for (DefaultMutableTreeNode node : sharedPositionNode) {
			if (node.getUserObject() instanceof ISharedConnector) {
				ISharedConnector connector = (ISharedConnector) node.getUserObject();
				List<DefaultMutableTreeNode> nodeList = connectorAbsMap.get(connector.getDesignAbstraction());
				if (nodeList == null) {
					nodeList = new ArrayList<DefaultMutableTreeNode>();
					connectorAbsMap.put(connector.getDesignAbstraction(), nodeList);
				}
				nodeList.add(node);
			}
		}

		return connectorAbsMap;
	}

	@NotNull private <T extends ILibrariedObject> Set<String> getCavitiesToBeBlocked(@NotNull T parentConnector,
			@NotNull T connectorInsert, @NotNull String positionName)
	{
		ILibraryBaseObject childLibConn = connectorInsert.getLibraryObject();
		ILibraryBaseObject parentLibConnector = parentConnector.getLibraryObject();
		if (parentLibConnector instanceof ILibraryBaseConnector && childLibConn instanceof ILibraryBaseConnector) {
			return LibraryHelper.getCavitiesToBeBlocked((ILibraryBaseConnector) parentLibConnector, positionName,
					(ILibraryBaseConnector) childLibConn);
		}
		return Collections.emptySet();
	}



	public static boolean sharedHousingDefMatches(ISharedInternalPosition sharedInternalPosition,
			ISharedConnector sharedConnector,
			IConnector connector)
	{
		String sharedselpartNumber = "";

		if (connector.getLibraryObject() != null) {
			sharedselpartNumber = ((ILibraryObject) connector.getLibraryObject()).getPartNumber();
		}

		return housingDefMatches(((ILibraryObject) sharedConnector.getLibraryObject()),
				sharedInternalPosition.getName(), sharedselpartNumber);
	}

	public static boolean housingDefMatches(ILibraryObject libraryObject, String positionName, String selpartNumber)
	{
		Collection housingDefs = libraryObject.getHousingDefinitions();

		for (Object housingDef1 : housingDefs) {
			ILibraryHousingDefinition housingDef = (ILibraryHousingDefinition) housingDef1;
			if (positionName.equalsIgnoreCase(housingDef.getInternalPosition())) {
				String partNumber = housingDef.getLibraryObject().getPartNumber();
				if (partNumber.equalsIgnoreCase(selpartNumber)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean housingDefMatches(IInternalPosition position, IConnector conn, IConnector connector)
	{
		String selpartNumber = "";
		if (connector.getLibraryObject() != null) {
			selpartNumber = ((ILibraryObject) connector.getLibraryObject()).getPartNumber();
		}
		return housingDefMatches(((ILibraryObject) conn.getLibraryObject()), position.getName(), selpartNumber);
	}

	public void windowClosing(WindowEvent e)
	{
		super.windowClosing(e);
		m_dialog.setCancelled(true);
	}

	public static DIALOG_RESULT showDialog(Frame owner, String title, AssociateConnectorCommand command)
	{
		m_dialog = new AssociateConnectorDialog(owner, title, command);
		if (!m_dialog.m_showDialog) {
			return DIALOG_RESULT.NOSHOW;
		}
		m_dialog.pack();
		m_dialog.getOkButton().addActionListener(new AssociateActionListener());
		m_dialog.getOkButton().setEnabled(false);
		m_dialog.getCancelButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_dialog.setCancelled(true);
				m_dialog.dispose();
			}
		});
		m_dialog.setVisible(true);
		if (m_dialog.isCancelled()) {
			return DIALOG_RESULT.CANCEL;
		}
		return DIALOG_RESULT.OK;
	}

	public class LogicModularTreeCellRenderer extends DefaultTreeCellRenderer
	{

		LogicModularTreeCellRenderer()
		{
			setClosedIcon(null);
			setOpenIcon(null);
			setLeafIcon(null);
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus)
		{
			JLabel treeCellRendererComponent =
					(JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			Object usrObj = ((DefaultMutableTreeNode) value).getUserObject();
			if (usrObj instanceof IConnector) {
				treeCellRendererComponent.setText(((IConnector) usrObj).getName());
				treeCellRendererComponent.setIcon(IconUtils.getIcon((IConnector) usrObj, IconUtils.ACTIVE, false));
			}
			else if (usrObj instanceof IDesignAbstraction) {
				treeCellRendererComponent.setIcon(IconUtils.getDesignAbstractionIcon(IconUtils.ACTIVE));
			}
			else if (usrObj instanceof ISharedConnector) {
				treeCellRendererComponent.setText(
						((ISharedConnector) usrObj).getName() + ":" + ((ISharedConnector) usrObj).getRevision());
				treeCellRendererComponent
						.setIcon(IconUtils.getIcon((ISharedConnector) usrObj, IconUtils.ACTIVE, false));
			}
			else if (usrObj instanceof IReadOnlyNamedObject) {
				treeCellRendererComponent.setText(((IReadOnlyNamedObject) usrObj).getName());
			}
			else if (usrObj instanceof IInternalPosition) {
				treeCellRendererComponent.setText(((IInternalPosition) usrObj).getName());
				treeCellRendererComponent
						.setIcon(IconUtils.getIcon((IInternalPosition) usrObj, IconUtils.ACTIVE, false));
			}
			else if (usrObj instanceof ISharedInternalPosition) {
				treeCellRendererComponent.setText(((ISharedInternalPosition) usrObj).getName());
				treeCellRendererComponent
						.setIcon(IconUtils.getIcon((ISharedInternalPosition) usrObj, IconUtils.ACTIVE, false));
			}
			return treeCellRendererComponent;
		}
	}

	private static class AssociateActionListener implements ActionListener
	{

		public void actionPerformed(ActionEvent e)
		{
			if (((DefaultMutableTreeNode) m_tree.getLastSelectedPathComponent())
					.getUserObject() instanceof ISharedInternalPosition) {

				DefaultMutableTreeNode sharedPositionNode =
						(DefaultMutableTreeNode) m_tree.getLastSelectedPathComponent();
				ISharedInternalPosition selectedPosition =
						(ISharedInternalPosition) sharedPositionNode.getUserObject();

				ISharedConnector parentConnector =
						(ISharedConnector) ((DefaultMutableTreeNode) sharedPositionNode.getParent()).getUserObject();
				m_command.setSelectedSharedParent(parentConnector);
				m_command.setSelectedSharedPosition(selectedPosition);
			}
			else {
				IInternalPosition selectedPosition =
						(IInternalPosition) ((DefaultMutableTreeNode) m_tree.getLastSelectedPathComponent())
								.getUserObject();
				m_command.setSelectedPosition(selectedPosition);
			}

			m_dialog.setCancelled(false);
			m_dialog.dispose();
		}
	}

	private class ModularTreeSelectionListener implements TreeSelectionListener
	{

		private AssociateConnectorCommand m_cmd;

		public ModularTreeSelectionListener(AssociateConnectorCommand cmd)
		{
			m_cmd = cmd;
		}

		public void valueChanged(TreeSelectionEvent e)
		{

			DefaultMutableTreeNode selectedObj = null;
			if (e.getNewLeadSelectionPath() != null) {
				selectedObj =
						(DefaultMutableTreeNode) e.getNewLeadSelectionPath().getLastPathComponent();
			}
			else {
				return;
			}

			Object usrObj = selectedObj.getUserObject();
			if (!(usrObj instanceof IInternalPosition || usrObj instanceof ISharedInternalPosition)) {
				getOkButton().setEnabled(false);
			}
			else {
				if (usrObj instanceof ISharedInternalPosition) {
					if (!m_cmd.canAdd((ISharedConnector) ((DefaultMutableTreeNode) selectedObj.getParent())
							.getUserObject())) {
						return;
					}
				}
				getOkButton().setEnabled(true);
			}
		}
	}
}

