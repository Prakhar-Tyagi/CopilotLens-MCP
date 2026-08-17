package chs.caplets.logic.actions.shared;

import chs.caplets.logic.actions.ui.MCNode;
import chs.caplets.logic.actions.ui.MCSharedNode;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.security.IDomain;
import chs.common.IUID;
import chs.ctf.caf.ui.SimpleOkCancelDialog;
import chs.utilities.BuildInfo;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IPropertyValidityListener;
import chs.utilities.ui.property.ValidityChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * UI Dialog for Share/Share-Into Multicore
 */
public class SharedMulticoreDialog extends SimpleOkCancelDialog
		implements ChangeListener, IPropertyValidityListener, IMulticoreShareContextProvider
{

	private static final int PreferredPanelWidth = 600;
	private static final int PreferredPanelHeight = 220;
	private static final int MinimumPanelWidth = 616;
	private static final int MinimumPanelHeight = 314;
	static final Dimension PreferredPanelSize = new Dimension(PreferredPanelWidth, PreferredPanelHeight);
	static final Dimension MinimumPanelSize = new Dimension(MinimumPanelWidth, MinimumPanelHeight);
	protected final SharedMulticoreModel sharedMulticoreModel;
	protected final IMulticore m_multicore;
	protected final MulticoreSharedController multicoreSharedController;
	private final JPanel boxHolder;
	private MulticoreSharedPanel multicoreSharedPanel;
	private MulticoreMapPanel mapCoresPanel;
	private JTabbedPane tabHolder;
	private Component mapperTab;

	public SharedMulticoreDialog(@Nullable Frame frame, @Nullable String title, IMulticore multicore, IDesign design)
	{
		super(frame, title);
		m_multicore = multicore;
		setMinimumSize(MinimumPanelSize);
		sharedMulticoreModel = createSharedMulticoreModel();
		sharedMulticoreModel.addChangeListener(this);
		sharedMulticoreModel.addPropertyValidityChangeListener(this);
		multicoreSharedController = createMulticoreSharedController(design);
		multicoreSharedPanel = createMulticoreSharedPanel(design);
		mapCoresPanel = createMulticoreMapPanel();
		createTabbedPane();
		boxHolder = new JPanel();
		boxHolder.setLayout(new GridBagLayout());
		JCheckBox styleToggleCB = new JCheckBox(ResourceMgr.getString(SharedMulticoreDialog.class,
				"SharedMulticoreDialog.TabbedStyle.text"), true);
		styleToggleCB.setName("TabbedStyleCB");
		styleToggleCB.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == ItemEvent.SELECTED) {
					createTabbedPane();
				}
				else {
					createBoxedPane();
				}
			}
		});

		if (BuildInfo.getBuildInfo().areQAExtensionsEnabled() ||
				BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled()) {
			getContentPane().add(styleToggleCB, BorderLayout.NORTH);
		}

		getOkButton().addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				boolean complete = multicoreSharedController.onCompletion();
				if (complete) {
					setVisible(false);
					dispose();
				}
			}
		});
		getCancelButton().addActionListener(new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				ISharedMulticore shareMulticore = sharedMulticoreModel.getSharedMulticore();
				if (shareMulticore != null) {
					multicoreSharedController.onCancel();
					setVisible(false);
					setCancelled(true);
				}
			}
		});
		pack();
	}

	@NotNull protected SharedMulticoreModel createSharedMulticoreModel()
	{
		return new SharedMulticoreModel(m_multicore, null);
	}

	@NotNull protected MulticoreSharedController createMulticoreSharedController(IDesign design)
	{
		return new MulticoreSharedController(sharedMulticoreModel, design);
	}

	@NotNull protected MulticoreSharedPanel createMulticoreSharedPanel(IDesign design)
	{
		return new MulticoreSharedPanel(sharedMulticoreModel, design, multicoreSharedController);
	}

	@NotNull protected MulticoreMapPanel createMulticoreMapPanel()
	{
		return new MulticoreMapPanel(sharedMulticoreModel, multicoreSharedController);
	}

	private void createBoxedPane()
	{
		getContentPane().remove(tabHolder);
		tabHolder.removeAll();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridx = 0;
		int preferredPanelHeight = 0;
		if (multicoreSharedPanel != null) {
			c.gridy++;
			preferredPanelHeight += PreferredPanelSize.height;
			boxHolder.add(multicoreSharedPanel, c);
			multicoreSharedPanel.setBorder(null);
		}
		if (mapCoresPanel != null) {
			c.gridy++;
			preferredPanelHeight += PreferredPanelSize.height;
			boxHolder.add(mapCoresPanel, c);
			TitledBorder titleBorder =
					BorderFactory.createTitledBorder(ResourceMgr.getString(SharedMulticoreDialog.class,
							"SharedMulticoreDialog.dialog.mapPanel"));
			mapCoresPanel.setBorder(titleBorder);
		}
		boxHolder.setPreferredSize(new Dimension(PreferredPanelSize.width, preferredPanelHeight));
		getContentPane().add(boxHolder, BorderLayout.CENTER);
		pack();
	}

	@Override public void stateChanged(ChangeEvent e)
	{
		ISharedMulticore shareMulticore = sharedMulticoreModel.getSharedMulticore();
		boolean okEnabled = true;
		boolean mapperEnabled = shareMulticore != null;
		if (shareMulticore == null &&
				(sharedMulticoreModel.getSharedMulticoreName() == null ||
						sharedMulticoreModel.getSharedMulticoreRevision() == null)) {
			okEnabled = false;
		}
		String okButtonToolTip = null;
		if (okEnabled && shareMulticore != null) {
			if (multicoreSharedController.getRootMCProxy() != null) {
				if (!multicoreSharedController.mappingDone()) {
					okEnabled = false;
					okButtonToolTip = ResourceMgr.getString(SharedMulticoreDialog.class,
							"SharedMulticoreDialog.multicore.notassigned.tooltip");
				}
			}
		}
		getOkButton().setEnabled(okEnabled);
		getOkButton().setToolTipText(okButtonToolTip);
		if (mapCoresPanel != null) {
			tabHolder.setEnabledAt(tabHolder.indexOfComponent(mapperTab), mapperEnabled);
		}
	}

	private void createTabbedPane()
	{
		if (boxHolder != null) {
			getContentPane().remove(boxHolder);
			boxHolder.removeAll();
		}
		JPanel displayPanel = new JPanel(new BorderLayout());
		tabHolder = new JTabbedPane(SwingConstants.TOP);
		tabHolder.setName("SharedMulticoreTabbedPane");
		tabHolder.setPreferredSize(PreferredPanelSize);
		if (multicoreSharedPanel != null) {
			if (m_multicore instanceof IOverbraid) {
				tabHolder.add(ResourceMgr.getString(SharedMulticoreDialog.class,
						"SharedMulticoreDialog.dialog.sharedOverbraidPanel"), multicoreSharedPanel);
			}
			else {
				tabHolder.add(ResourceMgr.getString(SharedMulticoreDialog.class,
						"SharedMulticoreDialog.dialog.sharedMulticorePanel"), multicoreSharedPanel);
			}
			multicoreSharedPanel.setBorder(null);
		}
		if (mapCoresPanel != null) {
			mapperTab = tabHolder.add(ResourceMgr.getString(SharedMulticoreDialog.class,
					"SharedMulticoreDialog.dialog.mapPanel"), mapCoresPanel);
			mapCoresPanel.setBorder(null);
			tabHolder.setEnabledAt(tabHolder.indexOfComponent(mapperTab), false);
		}
		displayPanel.add(tabHolder, BorderLayout.CENTER);
		displayPanel.add(createEditSharedDetailsInfo(), BorderLayout.SOUTH);
		getContentPane().add(displayPanel, BorderLayout.CENTER);
		if (multicoreSharedPanel != null) {
			multicoreSharedPanel.init();
		}
	}

	@NotNull private JPanel createEditSharedDetailsInfo()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		JLabel noteOnSharedChange =
				new JLabel(ResourceMgr.getString(SharedMulticoreDialog.class,
						"SharedMulticoreDialog.dialog.undodisablenote"),
						SwingConstants.LEFT);
		noteOnSharedChange.setName("undodisablenote");
		panel.add(noteOnSharedChange, BorderLayout.WEST);
		return panel;
	}

	@Override protected void cleanupOnDispose()
	{
		super.cleanupOnDispose();
		if (multicoreSharedPanel != null) {
			multicoreSharedPanel.dispose();
		}
	}

	@Override
	@Nullable public IUID getSharedMulticoreUID()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			final ISharedMulticore sharedMulticore = multicoreSharedController.getSharedMulticore();
			return sharedMulticore != null ? sharedMulticore.getUID() : null;
		}
	}

	@Override
	@Nullable public String getSharedMulticoreName()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return multicoreSharedController.getSharedMulticoreName();
		}
	}

	@Override
	@Nullable public String getSharedMulticoreRevision()
	{
		if (isCancelled()) {
			return null;
		}
		else {
			return multicoreSharedController.getSharedMulticoreRevision();
		}
	}

	@Override public boolean isSharedMulticoreNameGenerated()
	{
		return multicoreSharedController.isSharedMulticoreNameGenerated();
	}

	@Override @NotNull public Map<ILogicObject, IUID> getMulticoreToSharedHierarchyMap()
	{
		Map<ILogicObject, IUID> multicoreToSharedHierarchyMap = new HashMap<>();
		MCNode rootMCNode = getRootMCProxy();
		if (rootMCNode != null && rootMCNode.hasRef()) {
			MCSharedNode sharedRootProxyNode = rootMCNode.getSharedProxy();
			if (sharedRootProxyNode != null && sharedRootProxyNode.getRef() != null) {
				multicoreToSharedHierarchyMap.put(rootMCNode.getRef(), sharedRootProxyNode.getRef().getUID());
			}
			for (Enumeration<TreeNode> enumerator = rootMCNode.breadthFirstEnumeration();
					enumerator.hasMoreElements(); ) {
				MCNode node = CommonUtils.cast(enumerator.nextElement(), MCNode.class);
				if (node != null && node.hasRef()) {
					MCSharedNode sharedProxyNode = node.getSharedProxy();
					if (sharedProxyNode != null && sharedProxyNode.getRef() != null) {
						multicoreToSharedHierarchyMap.put(node.getRef(), sharedProxyNode.getRef().getUID());
					}
				}
			}
		}
		return Collections.unmodifiableMap(multicoreToSharedHierarchyMap);
	}

	@Nullable public MCNode getRootMCProxy()
	{
		return multicoreSharedController.getRootMCProxy();
	}

	@Override @Nullable public Set<IDomain> getSharedDomains()
	{
		return multicoreSharedPanel.getSharedDomains();
	}

	@Override public void validityChanged(ValidityChangeEvent evt)
	{

	}

	@Override public void invalidReasonChanged(IProperty property)
	{

	}
}