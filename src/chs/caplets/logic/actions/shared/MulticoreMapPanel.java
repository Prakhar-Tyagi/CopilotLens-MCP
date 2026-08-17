package chs.caplets.logic.actions.shared;

import chs.caplets.logic.actions.ui.AbstractMCProxyTree;
import chs.caplets.logic.actions.ui.MCNode;
import chs.caplets.logic.actions.ui.MCNodeCreator;
import chs.caplets.logic.actions.ui.MCSharedNode;
import chs.caplets.logic.actions.ui.MCSharedNodeCreator;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.shared.ISharedMulticore;
import chs.utilities.ui.BasicUIFactory;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.tree.BaseJTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

/**
 * JPanel class to map Multicore/Overbraid inner cores in Share-Into Multicore Dialog
 */
public class MulticoreMapPanel extends JPanel
{

	public static final int WIDTH = 225;
	public static final int HEIGHT = 200;
	private JButton m_assignButton;
	private JButton m_deassignButton;
	private JButton m_assignAllButton;
	private JButton m_deassignAllButton;
	private MCNode m_localMCRoot;
	private IMulticore m_multicore;
	private SharedMulticoreModel m_sharedMulticoreModel;
	private AbstractMCProxyTree<?> m_sharedMCRoot;
	protected JTree m_localMCTree;
	protected JTree m_sharedMCTree;
	private MulticoreSharedController m_multicoreSharedController;
	private static final Icon m_decorationIcon =
			CHSImageLoader.loadImageIcon("chs/images/app/dec_signal_route_status_routed.gif");

	public MulticoreMapPanel(@NotNull SharedMulticoreModel sharedMulticoreModel,
			MulticoreSharedController multicoreSharedController)
	{
		setName("MulticoreMapPanel");
		m_sharedMulticoreModel = sharedMulticoreModel;
		m_multicore = m_sharedMulticoreModel.getMulticore();
		m_multicoreSharedController = multicoreSharedController;
		setLayout(new BorderLayout());
		// Left side is the Instance Multicore Tree
		JPanel left = createLeftTreePanel();
		// Center is the buttons.
		JPanel center = createButtonPanel();
		// Right is the Shared Multicore tree.
		JPanel right = createRigthTreePanel();
		JPanel holder = new JPanel();
		holder.setLayout(new GridLayout(1, 3));
		holder.add(left);
		holder.add(center);
		holder.add(right);
		add(holder, BorderLayout.CENTER);
		sharedMulticoreModel.addSharedChangeListener(new SharedMulticoreChangeListener());
	}

	@NotNull private JPanel createLeftTreePanel()
	{
		JPanel left = new JPanel();
		left.setLayout(new BorderLayout());
		JLabel dialogInstanceLbl =
				new JLabel(ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.sourceMulticoreLbl.text"));
		if (m_multicore instanceof IOverbraid) {
			dialogInstanceLbl = new JLabel(
					ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.sourceOverbraidLbl.text"));
		}
		left.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		left.add(dialogInstanceLbl, BorderLayout.NORTH);
		m_localMCRoot = Objects.requireNonNull(new MCNodeCreator(m_multicore).execute().getParentMcNode());
		MCNode localMCNode = (MCNode) m_localMCRoot.getFirstChild();
		m_multicoreSharedController.setRootMCNode(localMCNode);
		m_localMCTree = new BaseJTree(m_localMCRoot);
		m_localMCTree.setName("local_tree");
		m_localMCTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		m_localMCTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener()
		{
			@Override public void valueChanged(TreeSelectionEvent e)
			{
				TreePath mcPath = m_localMCTree.getSelectionPath();
				MCNode mcNode = mcPath != null ? (MCNode) mcPath.getLastPathComponent() : null;
				autoSelectOtherEnd(mcNode);
				determineButtonEnablement();
				m_sharedMulticoreModel.fireNodeChangeEvent(new ChangeEvent(e.getSource()));
			}
		});
		// Set the TreeRenderer
		m_localMCTree.setCellRenderer(new ShareMulticoreTreeRenderer());
		m_localMCTree.setRootVisible(false);
		m_localMCTree.setShowsRootHandles(true);
		JScrollPane localMCTree_scrollpane = new JScrollPane(m_localMCTree);
		localMCTree_scrollpane.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		left.add(localMCTree_scrollpane, BorderLayout.CENTER);
		return left;
	}

	@NotNull private JPanel createRigthTreePanel()
	{
		JPanel right = new JPanel();
		right.setLayout(new BorderLayout());
		JLabel sharedObjectLbl =
				new JLabel(ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.sharedMulticoreLbl.text"));
		if (m_multicore instanceof IOverbraid) {
			sharedObjectLbl = new JLabel(
					ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.sharedOverbraidLbl.text"));
		}
		right.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		right.add(sharedObjectLbl, BorderLayout.NORTH);
		m_sharedMCRoot = Objects.requireNonNull(new MCNodeCreator(m_multicore).execute().getParentMcNode());
		m_sharedMCTree = new BaseJTree(m_sharedMCRoot);
		m_sharedMCTree.setName("shared_tree");
		m_sharedMCTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		m_sharedMCTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener()
		{
			@Override public void valueChanged(TreeSelectionEvent e)
			{
				TreePath sharedMCPath = m_sharedMCTree.getSelectionPath();
				MCSharedNode sharedMCNode =
						sharedMCPath != null ? (MCSharedNode) sharedMCPath.getLastPathComponent() : null;
				autoSelectOtherEnd(sharedMCNode);
				determineButtonEnablement();
				m_sharedMulticoreModel.fireNodeChangeEvent(new ChangeEvent(e.getSource()));
			}
		});

		// Set the TreeRenderer
		m_sharedMCTree.setCellRenderer(new ShareMulticoreTreeRenderer());
		m_sharedMCTree.setRootVisible(false);
		m_sharedMCTree.setShowsRootHandles(true);
		JScrollPane sharedMClist_scrollpane = new JScrollPane(m_sharedMCTree);
		sharedMClist_scrollpane.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		right.add(sharedMClist_scrollpane, BorderLayout.CENTER);
		return right;
	}

	@NotNull private JPanel createButtonPanel()
	{
		JPanel center = new JPanel()
		{
			@NotNull public Dimension getMaximumSize()
			{
				return super.getPreferredSize();
			}
		};
		center.setLayout(new GridLayout(4, 1, 5, 5));
		m_assignButton = createAssociateButton();
		center.add(m_assignButton);
		m_assignAllButton = createAssociateAllButton();
		center.add(m_assignAllButton);
		m_deassignButton = createDisAssociateButton();
		center.add(m_deassignButton);
		m_deassignAllButton = createDisAssociateAllButton();
		center.add(m_deassignAllButton);
		JPanel outerMapButtonPanel = new JPanel();
		outerMapButtonPanel.setLayout(new BoxLayout(outerMapButtonPanel, BoxLayout.Y_AXIS));
		outerMapButtonPanel.add(Box.createVerticalGlue());
		outerMapButtonPanel.add(center);
		outerMapButtonPanel.add(Box.createVerticalGlue());
		return outerMapButtonPanel;
	}

	@NotNull private JButton createAssociateButton()
	{
		JButton assignButton =
				BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.associate.text"));
		assignButton
				.setToolTipText(ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.associate.tooltip"));
		assignButton
				.setMnemonic(ResourceMgr.getMnemonic(MulticoreMapPanel.class, "MulticoreMapPanel.associate.mnemonic"));
		assignButton.setName("btn_assign");
		assignButton.setAlignmentX(CENTER_ALIGNMENT);
		assignButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				TreePath selectedPath = m_localMCTree.getSelectionPath();
				MCNode mcNode = selectedPath != null ? (MCNode) selectedPath.getLastPathComponent() : null;
				TreePath sharedMCPath = m_sharedMCTree.getSelectionPath();
				MCSharedNode sharedMCNode =
						sharedMCPath != null ? (MCSharedNode) sharedMCPath.getLastPathComponent() : null;
				m_multicoreSharedController.associate(mcNode, sharedMCNode);
				((DefaultTreeModel) m_localMCTree.getModel()).reload();
				((DefaultTreeModel) m_sharedMCTree.getModel()).reload();
				m_localMCTree.clearSelection();
				m_sharedMCTree.clearSelection();
				if (mcNode != null && sharedMCNode != null) {
					expandTrees();
					autoSelectNodes(mcNode, sharedMCNode);
				}
				determineButtonEnablement();
			}
		});
		assignButton.setEnabled(false);
		return assignButton;
	}

	@NotNull private JButton createAssociateAllButton()
	{
		JButton assignAllButton =
				BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.associateAll.text"));
		assignAllButton.setToolTipText(
				ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.associateAll.tooltip"));
		assignAllButton.setMnemonic(
				ResourceMgr.getMnemonic(MulticoreMapPanel.class, "MulticoreMapPanel.associateAll.mnemonic"));
		assignAllButton.setName("btn_assign_all");
		assignAllButton.setAlignmentX(CENTER_ALIGNMENT);
		assignAllButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				Object mcroot = m_localMCTree.getModel().getRoot();
				Object sharedMCroot = m_sharedMCTree.getModel().getRoot();
				if (mcroot instanceof MCNode && sharedMCroot instanceof MCSharedNode) {
					MCNode mcNode = (MCNode) ((DefaultMutableTreeNode) mcroot).getFirstChild();
					MCSharedNode sharedMCNode = (MCSharedNode) ((DefaultMutableTreeNode) sharedMCroot).getFirstChild();
					m_multicoreSharedController.associateAll(mcNode, sharedMCNode);
					((DefaultTreeModel) m_sharedMCTree.getModel()).reload();
					((DefaultTreeModel) m_localMCTree.getModel()).reload();
					m_localMCTree.clearSelection();
					m_sharedMCTree.clearSelection();
					expandTrees();
					autoSelectNodes(mcNode, sharedMCNode);
					determineButtonEnablement();
				}
			}
		});
		assignAllButton.setEnabled(false);
		return assignAllButton;
	}

	@NotNull private JButton createDisAssociateButton()
	{
		JButton unAssignButton =
				BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.unassociate.text"));
		unAssignButton.setName("btn_unassign");
		unAssignButton.setToolTipText(
				ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.unassociate.tooltip"));
		unAssignButton.setMnemonic(
				ResourceMgr.getMnemonic(MulticoreMapPanel.class, "MulticoreMapPanel.unassociate.mnemonic"));
		unAssignButton.setAlignmentX(CENTER_ALIGNMENT);
		unAssignButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				TreePath selectedPath = m_localMCTree.getSelectionPath();
				MCNode mcNode = selectedPath != null ? (MCNode) selectedPath.getLastPathComponent() : null;
				TreePath sharedMCPath = m_sharedMCTree.getSelectionPath();
				MCSharedNode sharedMCNode =
						sharedMCPath != null ? (MCSharedNode) sharedMCPath.getLastPathComponent() : null;
				m_multicoreSharedController.disassociate(mcNode, sharedMCNode);
				((DefaultTreeModel) m_localMCTree.getModel()).reload();
				((DefaultTreeModel) m_sharedMCTree.getModel()).reload();
				m_localMCTree.clearSelection();
				m_sharedMCTree.clearSelection();
				if (mcNode != null && sharedMCNode != null) {
					expandTrees();
					autoSelectNodes(mcNode, sharedMCNode);
				}
				determineButtonEnablement();
			}
		});
		unAssignButton.setEnabled(false);
		return unAssignButton;
	}

	@NotNull private JButton createDisAssociateAllButton()
	{
		JButton unAssignAllButton =
				BasicUIFactory.getInstance().createSiemensCustomJButton(ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.unassociateAll.text"));
		unAssignAllButton.setName("btn_unassign_all");
		unAssignAllButton.setToolTipText(
				ResourceMgr.getString(MulticoreMapPanel.class, "MulticoreMapPanel.unassociateAll.tooltip"));
		unAssignAllButton.setMnemonic(
				ResourceMgr.getMnemonic(MulticoreMapPanel.class, "MulticoreMapPanel.unassociateAll.mnemonic"));
		unAssignAllButton.setAlignmentX(CENTER_ALIGNMENT);
		unAssignAllButton.addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				Object mcroot = m_localMCTree.getModel().getRoot();
				Object sharedMCroot = m_sharedMCTree.getModel().getRoot();
				if (mcroot instanceof MCNode && sharedMCroot instanceof MCSharedNode) {
					MCNode mcNode = (MCNode) ((DefaultMutableTreeNode) mcroot).getFirstChild();
					MCSharedNode sharedMCNode = (MCSharedNode) ((DefaultMutableTreeNode) sharedMCroot).getFirstChild();
					m_multicoreSharedController.disassociateAll(mcNode);
					((DefaultTreeModel) m_localMCTree.getModel()).reload();
					((DefaultTreeModel) m_sharedMCTree.getModel()).reload();
					m_localMCTree.clearSelection();
					m_sharedMCTree.clearSelection();
					expandTrees();
					autoSelectNodes(mcNode, sharedMCNode);
					determineButtonEnablement();
				}
			}
		});
		unAssignAllButton.setEnabled(false);
		return unAssignAllButton;
	}

	/**
	 * Select Source Multicore node on selection on Target Multicore
	 *
	 * @param sharedMCNode Selected Target Multicore node
	 */
	private void autoSelectOtherEnd(@Nullable MCSharedNode sharedMCNode)
	{
		if (sharedMCNode != null) {
			if (sharedMCNode.isAssigned()) {
				MCNode mcNode = sharedMCNode.getMCProxy();
				TreeNode[] treeNodes = Objects.requireNonNull(mcNode).getPath();
				if (treeNodes != null) {
					TreePath treePath = new TreePath(treeNodes);
					m_localMCTree.expandPath(treePath);
					// Now select the path
					m_localMCTree.getSelectionModel().setSelectionPath(treePath);
				}
			}
			else {
				// If node is not assigned check if other end node is already selected . If its already assigned reset.
				TreePath mcPath = m_localMCTree.getSelectionPath();
				MCNode mcNode = mcPath != null ? (MCNode) mcPath.getLastPathComponent() : null;
				if (mcNode != null && mcNode.isAssigned()) {
					m_localMCTree.clearSelection();
				}
			}
		}
	}

	/**
	 * @param mcNode Selected Source Multicore Node
	 */
	private void autoSelectOtherEnd(@Nullable MCNode mcNode)
	{
		if (mcNode != null) {
			if (mcNode.isAssigned()) {
				MCSharedNode sharedMcNode = mcNode.getSharedProxy();
				TreeNode[] treeNodes = Objects.requireNonNull(sharedMcNode).getPath();
				if (treeNodes != null) {
					TreePath treePath = new TreePath(treeNodes);
					m_sharedMCTree.expandPath(treePath);
					// Now select the path
					m_sharedMCTree.getSelectionModel().setSelectionPath(treePath);
				}
			}
			else {
				// If node is not assigned check if other end node is already selected . If its assigned reset.
				TreePath mcPath = m_sharedMCTree.getSelectionPath();
				MCSharedNode mcSharedNode = mcPath != null ? (MCSharedNode) mcPath.getLastPathComponent() : null;
				if (mcSharedNode != null && mcSharedNode.isAssigned()) {
					m_sharedMCTree.clearSelection();
				}
			}
		}
	}

	private class SharedMulticoreChangeListener implements ChangeListener
	{

		public void stateChanged(ChangeEvent e)
		{
			onSharedMulticoreChange();
		}
	}

	protected void onSharedMulticoreChange()
	{
		ISharedMulticore sharedMulticore = m_sharedMulticoreModel.getSharedMulticore();
		if (sharedMulticore != null) {
			m_sharedMCRoot =
					Objects.requireNonNull(
							new MCSharedNodeCreator(sharedMulticore).execute().getParentMcSharedNode());
			DefaultTreeModel model = (DefaultTreeModel) m_sharedMCTree.getModel();
			model.setRoot(m_sharedMCRoot);
			m_sharedMCTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			MCNode localMCNode = (MCNode) m_localMCRoot.getFirstChild();
			MCSharedNode sharedMCNode = (MCSharedNode) m_sharedMCRoot.getFirstChild();
			m_multicoreSharedController.reset(localMCNode);
			m_multicoreSharedController.autoAssignMulticores(localMCNode, sharedMCNode);
			m_multicoreSharedController.setRootMCNode(localMCNode);
			// Set the TreeRenderer
			m_sharedMCTree.setCellRenderer(new ShareMulticoreTreeRenderer());
			m_sharedMCTree.setRootVisible(false);
			m_sharedMCTree.setShowsRootHandles(true);
			model.reload();
			determineButtonEnablement();
			expandTrees();
			autoSelectNodes(localMCNode, sharedMCNode);
		}
		else {
			m_sharedMCRoot =
					Objects.requireNonNull(
							new MCNodeCreator(m_sharedMulticoreModel.getMulticore()).execute().getParentMcNode());
			DefaultTreeModel model = (DefaultTreeModel) m_sharedMCTree.getModel();
			model.setRoot(m_sharedMCRoot);
			model.reload();
		}
	}

	private void autoSelectNodes(@NotNull MCNode localMCNode, @NotNull MCSharedNode sharedMCNode)
	{
		m_localMCTree.setSelectionPath(new TreePath(localMCNode.getPath()));
		m_sharedMCTree.setSelectionPath(new TreePath(sharedMCNode.getPath()));
	}

	private void expandTrees()
	{
		for (int i = 0; i < m_localMCTree.getRowCount(); i++) {
			m_localMCTree.expandRow(i);
		}
		for (int i = 0; i < m_sharedMCTree.getRowCount(); i++) {
			m_sharedMCTree.expandRow(i);
		}
	}

	private void determineButtonEnablement()
	{
		MulticoreSharedController.ResultOfAssociationCheck result =
				new MulticoreSharedController.ResultOfAssociationCheck();
		boolean associateButtonState = allowAssociate(result);
		m_assignButton.setEnabled(associateButtonState);
		String text = m_multicoreSharedController.getToolTipTextForAssocButton(result.getResult());
		m_assignButton.setToolTipText(text);
		m_deassignButton.setEnabled(allowUnassociate());
		m_assignAllButton.setEnabled(isAllowAssociateAll());
		m_deassignAllButton.setEnabled(isAllowUnassociateAll());
	}

	private boolean allowUnassociate()
	{
		TreePath selectedPath = m_localMCTree.getSelectionPath();
		MCNode mcNode = selectedPath != null ? (MCNode) selectedPath.getLastPathComponent() : null;
		return m_multicoreSharedController.allowUnassociate(mcNode);
	}

	private boolean allowAssociate(MulticoreSharedController.ResultOfAssociationCheck result)
	{
		TreePath selectedPath = m_localMCTree.getSelectionPath();
		MCNode mcNode = selectedPath != null ? (MCNode) selectedPath.getLastPathComponent() : null;
		TreePath sharedMCPath = m_sharedMCTree.getSelectionPath();
		MCSharedNode sharedMCNode = sharedMCPath != null ? (MCSharedNode) sharedMCPath.getLastPathComponent() : null;
		return m_multicoreSharedController.allowAssociate(result, mcNode, sharedMCNode);
	}

	private boolean isAllowUnassociateAll()
	{
		Object mcroot = m_localMCTree.getModel().getRoot();
		if (mcroot instanceof MCNode) {
			return m_multicoreSharedController.isAllowUnassociateAll((MCNode) mcroot);
		}
		return false;
	}

	private boolean isAllowAssociateAll()
	{
		Object mcroot = m_localMCTree.getModel().getRoot();
		Object sharedMCroot = m_sharedMCTree.getModel().getRoot();
		if (mcroot instanceof MCNode && sharedMCroot instanceof MCSharedNode) {
			return m_multicoreSharedController.isAllowAssociateAll((MCNode) mcroot, (MCSharedNode) sharedMCroot);
		}
		return false;
	}

	private static class ShareMulticoreTreeRenderer extends DefaultTreeCellRenderer
	{

		@NotNull public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean sel,
				boolean expanded,
				boolean leaf, int row,
				@SuppressWarnings("ParameterHidesMemberVariable") boolean hasFocus)
		{
			AbstractMCProxyTree<?> multicoreNode = (AbstractMCProxyTree<?>) value;
			String txt = consturctNodeDisplayText(multicoreNode);

			JLabel jl = (JLabel) super.getTreeCellRendererComponent(tree, txt, sel, expanded, leaf, row, hasFocus);
			jl.setIcon(decorateAssignedNode(multicoreNode));
			return jl;
		}

		@NotNull public static String consturctNodeDisplayText(@NotNull AbstractMCProxyTree<?> multicoreNode)
		{
			StringBuilder multicoreNodeText = new StringBuilder("<html>");
			multicoreNodeText.append(multicoreNode.getName());
			String attibuteStr = multicoreNode.getAttributesListAsString();
			if (attibuteStr != null) {
				multicoreNodeText
						.append("<i>")
						.append(attibuteStr)
						.append("</i>");
			}
			multicoreNodeText.append("</html>");
			return multicoreNodeText.toString();
		}

		@Nullable public static Icon decorateAssignedNode(AbstractMCProxyTree<?> multicoreNode)
		{
			Icon icon = multicoreNode.getIcon();
			if (icon != null) {
				if (multicoreNode.isAssigned()) {
					return CHSImageLoader.getDecoratedImage(icon, m_decorationIcon, CHSImageLoader.REFERENCE_CORNER);
				}
				return icon;
			}
			return null;
		}
	}
}
