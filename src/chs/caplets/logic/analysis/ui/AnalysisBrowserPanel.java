/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2025 Siemens
 */

package chs.caplets.logic.analysis.ui;

import chs.analysis.AnalysisServices;
import chs.analysis.CapitalAnalysisFactory;
import chs.analysis.DynSimControllerChangeEvent;
import chs.analysis.IAnalysisNetlistScope;
import chs.analysis.IAnalysisSimulationSessionController;
import chs.analysis.IBuildListCreationDeletionListener;
import chs.analysis.ICapitalAnalysis;
import chs.analysis.IDynSimControllerListener;
import chs.analysis.importer.AnalysisSimulationResultsImporter;
import chs.analysis.scope.AbstractAnalysisNetlistScope;
import chs.analysis.scope.IAnalysisNetlistScopeChangeListener;
import chs.analysis.scope.ScopeComponent;
import chs.analysis.scope.ScopedComponentsIdentifier;
import chs.analysis.ui.AbstractAnalysisSimulationResultsConsumer;
import chs.analysis.ui.IAnalysisSimulationStatusIndicator;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ActionNodeIterator;
import chs.caf.CAFUtils;
import chs.caf.IActionNode;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IDisplayContextListener;
import chs.caf.caplet.IGraphicsFilterChangeListener;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.ViewChangeEvent;
import chs.caf.caplet.WindowChangeEvent;
import chs.caf.caplet.action.IActionUI;
import chs.caf.caplet.helpers.browser.IBrowserUpdatableUI;
import chs.caf.caplet.helpers.graphics.IGraphicsFilterControl;
import chs.caf.helpers.ui.common.CAFToolBar;
import chs.caf.helpers.ui.common.ResourceHolder;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.analysis.EnableRobustNetlistingAction;
import chs.caplets.logic.actions.analysis.ResetActionUI;
import chs.caplets.logic.actions.analysis.SetAnalysisNetlistScopeAction;
import chs.caplets.logic.actions.analysis.SimulateActionUI;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.caplets.shared.BaseController;
import chs.caplets.shared.BaseLogicResource;
import chs.cof.COFTypeEnum;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IDesign;
import chs.cof.project.IProject;
import chs.cof.project.IProjectChangeEvent;
import chs.cof.project.IProjectChangeListener;
import chs.cof.project.buildlist.IBuildList;
import chs.cof.project.buildlist.IBuildListMgr;
import chs.common.IDesignContainer;
import chs.common.IUID;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.utilities.AppInfo;
import chs.utilities.ListSet;
import chs.utilities.ResourceMgr;
import chs.utilities.SortedList;
import chs.utilities.StringUtils;
import chs.utilities.suite.ApplicationSuiteInfo;
import chs.utilities.suite.IApplicationSuite;
import chs.utilities.ui.BasicUIFactory;
import chs.utilities.ui.CHSColors;
import chs.utilities.ui.tree.Drawer;
import chs.utility.AnalysisHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.ui.CHSSwingUtils;
import chs.utility.ui.IconUtils;
import chs.utility.ui.chstable.renderers.CheckCellRenderer;
import chs.utility.ui.chstable.renderers.ComboBoxRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicListUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author rharring
 */
public class AnalysisBrowserPanel extends AbstractAnalysisSimulationResultsConsumer
		implements PropertyChangeListener, IAnalysisSimulationStatusIndicator, IBrowserUpdatableUI,
		IAnalysisNetlistScopeChangeListener, IModelChangeListener, IDynSimControllerListener,
		ActionListener, IGraphicsFilterChangeListener, IBuildListCreationDeletionListener,
		IProjectChangeListener, IDisplayContextListener,IBuildListMgr.IBuildListMgrRefreshListener
{

	public static final String LOCK_OBJECT = "";
	private static final String RIGHTARROW_UNICODE = "\u2192";

	// ////////////////// //
	// Instance variables //
	// ////////////////// //

	/**
	 * The Qual and Spice radio buttons
	 */
	protected JButton simulateButton;
	protected JButton resetButton;
	private JToolBar toolbar;
	private JPanel northPanel;
	private JPanel southPanel;
	private JSplitPane centralSplitPane;

	/**
	 * The progress bar
	 */
	protected JProgressBar bar;
	private JScrollPane scrollPane;

	protected ICaplet caplet;
	private ICapletController controller;
	private IProject project;

	private boolean update = false;
	protected AnalysisBrowserTableModel tablemodel;
	protected AnalysisBrowserTable table;
	private AnalysisBrowserTableRow[] rowsUpdate;
	private AnalysisBrowserFunctionList functionList;

	private ActionContainer toolBarActionContainer;
	protected AbstractAnalysisNetlistScope currentScope;
	@Nullable private IAnalysisSimulationSessionController simSession;
	private Set<String> scopeFunctions;
	private int currentMode;
	private boolean toolbarCreated;

	private JPanel toolBarPanel;

	private boolean isDesignClosing = false;

	/**
	 * Creates a new instance of AnalysisBrowserPanel
	 *
	 * @param capletController
	 */
	public AnalysisBrowserPanel(ICapletController capletController)
	{
		caplet = capletController.getCaplet();
		controller = capletController;

		tablemodel = new AnalysisBrowserTableModel();

		currentMode = AnalysisServices.DYN_SIM_OFF;

		initializeGui(capletController.getCaplet());

		AnalysisServices.addScopeChangeListener(this);
		AnalysisServices.addAnalysisResultsConsumer(this);

		// we need to register ourselves with the Logic Analysis services such
		// that we get all notifications after they have been processed by the
		// Analysis Services (which may rebuilt the lists of models that we're
		// looking at).
		((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).addPostModelChangeListener(this);

		// we do a similar thing to gain notification of any filter changes
		((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).addPostFilterChangeListener(this);

		AnalysisServices.addBuildListCreationDeletionListener(this);

		project = caplet.getFIB().getProjectMgr().getCurrentProject();
		if (project != null) {
			project.addProjectChangeListener(this);
			project.getBuildListMgr().addRefreshListener(this);
		}

		toolbarCreated = false;

		if (CAFUtils.getInstance().getWindowMgr() != null) {
			CAFUtils.getInstance().getWindowMgr().addDisplayContextListener(this);
		}
	}

	public void setDesignClosing(boolean isClosing)
	{
		isDesignClosing = isClosing;
	}

	public void setVisible(boolean aFlag)
	{
		//Set scope from preference only when we are making the panel visible but design is not closing ( no design close event )
		if (!isDesignClosing && aFlag) {
			setAnalysisScopeFromPreference();
		}
		if (CAFUtils.getInstance().getActiveCapletController() != null &&
				AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			CAFUtils.getInstance().getActiveCapletController().addAuditTab();
		}
		if (!isVisible() && aFlag && !isDesignClosing) {
			updateToolBar();
		}
		setDesignClosing(false);
		super.setVisible(aFlag);
	}

	protected void setAnalysisScopeFromPreference()
	{
		if (AnalysisServices.getCurrentAnalysisNetlistScope() == null) {

			IDesignContainer activeDesign = CAFUtils.getInstance().getActiveDesignContainer();
			if (activeDesign != null) {
				IProject activeProject = activeDesign.getProject();

				boolean isSetFromPref =
						AnalysisServices
								.setPreferenceAnalysisScope(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
										activeDesign, activeProject, false);
				if (isSetFromPref && CAFUtils.getInstance().getActiveCapletController() != null) {
					ICapletController capletController = CAFUtils.getInstance().getActiveCapletController();
					capletController.addAuditTab();
					CAFUtils.getInstance().tickleUI(capletController.getCaplet().getFIB());
				}
			}
		}
	}

	/**
	 * This method should generally tidy up and remove any listeners added above...
	 */
	public void destroy()
	{
		if (CAFUtils.getInstance().getWindowMgr() != null) {
			CAFUtils.getInstance().getWindowMgr().removeDisplayContextListener(this);
		}
		if (currentScope != null) {
			currentScope.resetScopedComponentsList();
			simulationInProgress(false);
			AnalysisServices.unregisterSimulationStatusIndicator(currentScope.getUid());
		}
		AnalysisServices.removeScopeChangeListener(this);
		AnalysisServices.removeAnalysisResultsConsumer(this);
		AnalysisServices.removeBuildListCreationDeletionListener(this);

		((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).removePostModelChangeListener(this);
		((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).removePostFilterChangeListener(this);

		if (AnalysisHelper.getInstance().isLegacyAnalysisMode() && controller != null && controller.getCaplet() != null &&
				controller.getCaplet().getResource() != null) {
			BaseLogicResource tmp = (BaseLogicResource) controller.getCaplet().getResource();
			tmp.getQualSimAction().removePropertyChangeListener(this);
			tmp.getSpiceSimAction().removePropertyChangeListener(this);
		}

		if (project != null) {
			project.removeProjectChangeListener(this);
			project.getBuildListMgr().removeRefreshListener(this);
		}

		project = null;
		caplet = null;
		controller = null;
		currentScope = null;
		simSession = null;

		EnableRobustNetlistingAction.storeRobustEnabledState();
	}

	// ////////////// //assert caplet != null; // no-op here - stop IntelliJ whining that this should be abstract
	// ////////////// //

	/**
	 * This method initializes the gui
	 */
	@SuppressWarnings({"ConstantConditions"})
	public void createToolbar()
	{
		if (toolbarCreated) {
			return;
		}
		// create the Analysis toolbar actions...

		toolBarPanel = new JPanel();

		toolBarPanel.setLayout(new BorderLayout());

		generateToolBar();
		// create the layout constraints and then add the toolbar to the toolbar panel
//		GridBagConstraints gridBagConstraints = new GridBagConstraints(GridBagConstraints.RELATIVE,
//				GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
//				new Insets(3, 5, 0, 4), 0, 0
//		);
		toolBarPanel.add(toolbar, BorderLayout.CENTER);
		toolBarPanel.revalidate();

		// add the toolbar to the layout...
		northPanel.add(toolBarPanel, BorderLayout.NORTH);
		northPanel.revalidate();

		changeCurrentScope();
		if (currentScope != null && CAFUtils.getInstance().getActiveDesignContainer() != null) {
			for (IUID designUID : currentScope.getDesignUIDs()) {
				if (designUID.equals(CAFUtils.getInstance().getActiveDesignContainer().getUID())) {
					buildUI();
					break;
				}
			}
		}

		toolbarCreated = true;
	}

	protected void createAnalysisToolBarActions()
	{
		CHSSwingUtils.invoke(new Runnable()
		{
			public void run()
			{
				((BaseController) controller).createAnalysisToolbarActions();
			}
		}, true);
	}

	protected void updateAnalysisToolBarScopes()
	{
		CHSSwingUtils.invoke(new Runnable()
		{
			public void run()
			{
				((BaseController) controller).refreshAnalysisScopeAction();
			}
		}, true);

		updateToolBarScopes();
	}

	protected void generateToolBar()
	{

		createAnalysisToolBarActions();

		// get the toolbar...
		toolBarActionContainer = ((BaseLogicResource) caplet.getResource()).getAnalysisBrowserToolbar();
		//ac = initAnalysisBrowserToolbar();

		toolBarActionContainer.setEnabled(true);
		// create the Java toolbar to hold the action container's actions..
		toolbar = ResourceHolder.createToolBar((String) toolBarActionContainer.getValue(Action.NAME),
				toolBarActionContainer.getMembers(), null, controller);
		toolbar.setBorder(null);

		if (AnalysisHelper.getInstance().isLegacyAnalysisMode()) {
			((BaseLogicResource) caplet.getResource()).getQualSimAction().addPropertyChangeListener(this);
			((BaseLogicResource) caplet.getResource()).getSpiceSimAction().addPropertyChangeListener(this);
		}

		toolBarActionContainer.updateUI(); // We need to update all the UIs here to ensure that the qual & numeric
		// sim actions are correct.

		toolbarCreated = true;
	}

	protected void updateToolBarScopes()
	{
		// lets look to see if we can activate the correct scope.
		if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
			ActionContainer scopeAction = ((BaseLogicResource) caplet.getResource()).getScopeAction();
			ActionNodeIterator ani = scopeAction.getMembers();
			while (ani.hasNext()) {
				IActionNode node = ani.getNext();
				if (node instanceof ActionEntry &&
						((ActionEntry) node).getAction() instanceof SetAnalysisNetlistScopeAction) {
					SetAnalysisNetlistScopeAction sansa =
							(SetAnalysisNetlistScopeAction) ((ActionEntry) node).getAction();
					if (sansa.isActive()) {
						SetAnalysisNetlistScopeAction
								.setSelectedComponent((SetAnalysisNetlistScopeAction) ((ActionEntry) node).getAction());
					}
				}
			}
		}
	}

	protected void updateToolBar()
	{
		// If we haven't already we need to create one.
		if (!toolbarCreated) {
			createToolbar();
		}
		else {
			// We need to update the scopes to show the available and selected scopes
			updateAnalysisToolBarScopes();
			// We need to update the toolbar
			if (toolBarPanel != null) {
				toolBarPanel.removeAll();
				toolBarPanel.add(toolbar, BorderLayout.CENTER);
				toolBarPanel.revalidate();
			}
		}
	}

	public void buildListCreatedOrDeleted(@NotNull IBuildList bl)
	{
		// When the toolbar is not created, there is no need to update the toolbar on build list changes.
		if (!toolbarCreated) {
			return;
		}
		//since the build list creation/deletion is invoked we are sure that the scope container requires update.
		//when a new build list is created/deleted - we also need to configure the button - hence recreating the drawer
		updateAnalysisScopeContainerForChangedScopeMembers(true);
	}

	private void updateAnalysisScopeContainerForChangedScopeMembers(boolean isUpdatingAnalysisScopeContainerRequired)
	{
		//updating all the changes so that we can get an updated controller first
		updateToolBarForChangedScopeCandidates();

		updateAnalysisScopeContainer(isUpdatingAnalysisScopeContainerRequired);
	}

	private JSplitPane createVerticalSplitPane()
	{
		JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		return pane;
	}

	public AnalysisBrowserTableModel getTableModel()
	{
		return tablemodel;
	}

	protected void buildUI()
	{
		removeCentralSplitPane();

		table = new AnalysisBrowserTable();
		table.setName("AnalysisBrowserTable");
		table.getTableHeader().setReorderingAllowed(false);

		scrollPane = new JScrollPane(buildBrowserTable());

		// add a component listener to ensure the table always gets resized
		// when the scroll pane does.
		scrollPane.addComponentListener(new ComponentAdapter()
		{
			public void componentResized(ComponentEvent ce)
			{
				Dimension bounds = scrollPane.getSize(new Dimension());

				if (bounds.width > table.getSize(new Dimension()).width) {
					updateTableSize(bounds);
				}
			}
		});
		JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.add(scrollPane, BorderLayout.CENTER);
		tablePanel.add(createFilterButtons(), BorderLayout.SOUTH);
		tablePanel.setBorder(BorderFactory.createTitledBorder(
				ResourceMgr.getString(AnalysisBrowserPanel.class, "AnalysisBrowserPanel.controlsPanel.border")));

		centralSplitPane = createVerticalSplitPane();
		centralSplitPane.add(tablePanel);
		// Application suites that support a restricted version of analysis do not need functions.
		functionList = new AnalysisBrowserFunctionList();

		JPanel listPanel = new JPanel(new BorderLayout());
		JScrollPane sp = new JScrollPane(functionList);
		listPanel.add(sp, BorderLayout.CENTER);
		listPanel.add(createFunctionListFilterButtons(), BorderLayout.SOUTH);
		listPanel.setBorder(BorderFactory.createTitledBorder(
				ResourceMgr.getString(AnalysisBrowserPanel.class, "AnalysisBrowserPanel.functionsPanel.border")));
		centralSplitPane.add(listPanel);
		functionList.addKeyListener(new KeyListener()
		{

			public void keyTyped(KeyEvent e)
			{
			}

			public void keyReleased(KeyEvent e)
			{
			}

			// If Ctrl-F5 is pressed over the functions tab, reload the project model
			public void keyPressed(KeyEvent e)
			{
				if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
					if (e.getKeyCode() == KeyEvent.VK_F5) {
						if (project != null) {
							removeFunctionListCache(project.getAnalysisMgr().getSubsystemId());
							updateFunctionList(true);
						}
					}
				}
			}
		});

		add(centralSplitPane, BorderLayout.CENTER);
		update = true; // we've created the table and list anything else is an update.
		updateFunctionList(true);

		// set the divider location on the
		// event thread at an appropriate time
		final JSplitPane pane = centralSplitPane;
		Runnable r = new Runnable()
		{
			public void run()
			{
				pane.setDividerLocation(0.7);
			}
		};
		SwingUtilities.invokeLater(r);
	}

	private void persistRowExpansionStates()
	{
		for (int i = 0; i < tablemodel.data.size(); i++) {
			String name = tablemodel.data.get(i).name;
			if (table.parentRowExpansionState(name) != null) {
				if (table.parentRowExpansionState(name) == true) {
					table.toggleRowVisibility(i, false);
				}
			}
		}
	}

	public void updateTable()
	{
		if (table != null) {
			if (update && table.getCellEditor() != null) {
				table.getCellEditor().stopCellEditing();
			}
			synchronized (LOCK_OBJECT) {
				if (update) {
					Dimension d = new Dimension();
					table.getSize(d);

					// we must ensure the table is accurately repainted..
					// We do so on the event thread to ensure that there is no
					// contention for the updates or multiple repaints.
					final Dimension d2 = d;
					Runnable r = new Runnable()
					{
						public void run()
						{
							try {
								buildBrowserTable();
								persistRowExpansionStates();

								updateFunctionList(false);

								updateTableSize(d2);
								table.invalidate();
								table.revalidate();
								table.repaint();
								functionList.repaint();
							}
							catch (Exception e) {
								// do nothing...
							}
						}
					};
					if (SwingUtilities.isEventDispatchThread()) {
						r.run();
					}
					else {
						SwingUtilities.invokeLater(r);
					}
				}
			}
		}
	}

	private void updateTableSize(Dimension d)
	{
		table.setSize(d);
		//table.setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
	}

	/**
	 * This map maintains a cache of the function info
	 */
	private static Map<Integer, List<String>> functionListMap = new HashMap<Integer, List<String>>();

	/**
	 * This method allows the cache to be cleared
	 */
	public static void removeFunctionListCache(int id)
	{
		functionListMap.remove(id);
	}

	private void clearFunctionList()
	{
		synchronized (LOCK_OBJECT) {
			if (functionList != null) {
				functionList.clear();
			}
		}
	}

	public void updateFunctionList(boolean shouldClear)
	{
		// first clear the list
		if (functionList != null) {
			if (shouldClear) {
				clearFunctionList();

				functionList.setScopeFunctions(scopeFunctions);

				// get the project id
				IProject proj = caplet.getFIB().getProjectMgr().getCurrentProject();
				if (proj != null) {
					int id = currentScope.getProjectId();

					List<String> functionInfo = functionListMap.get(id);

					// if we've not cached the function Info load it...
					if (functionInfo == null) {
						// ICapitalAnalysis
						ICapitalAnalysis analysis = CapitalAnalysisFactory.getAnalysisInterface();

						functionInfo = analysis.getFunctionInformationForProjectModel(id);

						functionListMap.put(id, functionInfo);
					}

					for (String s : functionInfo) {
						functionList.addFunction(s);
					}
				}

				functionList.restoreFromFilters();
			}
		}
	}

	private void initializeGui(ICaplet caplet)
	{

		// we want a BorderLayout with the status at the top
		// the sim types in the center and the simulate and
		// reset buttons at the bottom

		setLayout(new BorderLayout());

		// northern area
		northPanel = new JPanel();
		northPanel.setLayout(new BorderLayout());

		northPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		bar = new JProgressBar();
		bar.setName("AnalysisProgressBar");

		JPanel temp = new JPanel();
		temp.setLayout(new BorderLayout());
		temp.add(bar, BorderLayout.CENTER);
		JButton cancelButton = BasicUIFactory.getInstance().createTTButton();
		cancelButton.setIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_cancel_simulation.gif"));
		cancelButton.setName("AnalysisDynamicSimulationCancelButton");
		cancelButton.setToolTipText(ResourceMgr.getString(AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.cancelSimulationButton.tooltip"));
		cancelButton.addActionListener(this);

		JToolBar tempTbar = new JToolBar();
		tempTbar.setRollover(true);
		tempTbar.setFloatable(false);
		tempTbar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
		tempTbar.setBorder(BorderFactory.createEmptyBorder());
		tempTbar.add(cancelButton);

		temp.add(tempTbar, BorderLayout.EAST);
		northPanel.add(temp, BorderLayout.SOUTH);

		GridBagConstraints gbc = new GridBagConstraints();

		southPanel = new JPanel();
		southPanel.setLayout(new GridBagLayout());
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.gridy = 0;
		gbc.weightx = 1.25;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		simulateButton =
				BasicUIFactory.getInstance().createTTButton(caplet.getActionUI(SimulateActionUI.class.getName()));
		simulateButton.setName("AnalysisSimulateButton");
		southPanel.add(simulateButton, gbc);

		gbc.weightx = 1;
		gbc.gridx = 1;

		resetButton = BasicUIFactory.getInstance().createTTButton(caplet.getActionUI(ResetActionUI.class.getName()));
		resetButton.setName("AnalysisResetButton");

		southPanel.add(resetButton);

		add(northPanel, BorderLayout.NORTH);
		add(southPanel, BorderLayout.SOUTH);
	}

	private static Image createGhostImage(Image img)
	{
		JLabel imgObserver = new JLabel();
		BufferedImage ghost = new BufferedImage(img.getWidth(imgObserver)
				, img.getHeight(imgObserver), BufferedImage.TYPE_INT_ARGB_PRE);
		Graphics2D g2 = ghost.createGraphics();

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));
		g2.drawImage(img, 0, 0, ghost.getWidth(), ghost.getHeight(), imgObserver);
		g2.dispose();
		return ghost;
	}

	private MouseAdapter browserTableAdapter;

	private void setRowAttributes(@NotNull AnalysisBrowserTableRow row, @NotNull ScopeComponent component)
	{
		row.setComponentType(component.getComponentType());
		row.setHasFunctions(component.hasFunction());
	}

	@Nullable
	private Icon getComponentIcon(ScopeComponent component, boolean componentHasFailure, boolean componentAlreadyAdded)
	{
		if (componentAlreadyAdded) {
			return null;
		}
		else if (component.isParentComponent()) {
			Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_arrow_down.gif");
			return icon;
		}
		else {
			Icon icon = component.getIcon(componentHasFailure);
			return icon;
		}
	}

	private void addScopeFunctions(ScopeComponent component)
	{
		if (component == null) {
			return;
		}
		for (String s : component.getFunctions()) {
			scopeFunctions.add(s);
		}
		return;
	}

	private String failureString(ScopeComponent component)
	{
		// have we got a failed component...
		assert simSession != null;
		DefaultMutableTreeNode failureNode =
				simSession.getComponentFailures(currentScope.getUid(), component.getName());
		String failure = hasActiveFailure(failureNode);
		return failure;
	}

	@Nullable
	private AnalysisBrowserTableRow addAnalysisRowDuringSimulation(ScopeComponent component, @Nullable String failure,
			@Nullable String currentValue, SortedList<AnalysisBrowserTableRow> rows,
			@Nullable AnalysisBrowserTableRow parentRow)
	{
		AnalysisBrowserTableRow row = null;
		if (simSession != null) {
			boolean componentHasFailure = failure != null;
			if (component.isWire()) {
				row = new AnalysisBrowserTableRow(component.getUid(),
						getComponentIcon(component, componentHasFailure, false),
						getComponentNameInRow(component, false), component.getDesignUID(), null, null,
						component.getAnalysisModel(), component.isChildComponent(), null);
				setRowAttributes(row, component);
				row.setFailed(componentHasFailure, failure);
				rows.add(row);
				return row;
			}
			else {
				DefaultMutableTreeNode inputNode =
						simSession.getComponentInputProperties(currentScope.getUid(), component.getName());
				if (inputNode != null && inputNode.getChildCount() > 0) {
					Map<String, List<String>> properties = getInputInterfaceProperties(inputNode);
					Iterator<String> keys = properties.keySet().iterator();
					boolean added = false;
					AnalysisBrowserTableRow rowToReturnToParent = null;
					String value = currentValue;
					while (keys.hasNext()) {
						String key = keys.next();
						List<String> inputPropertyDetails = properties.get(key);
						List<String> items = new ArrayList<>();
						String firstElemet = inputPropertyDetails.get(0);
						String type = firstElemet.substring(firstElemet.lastIndexOf(',') + 1);
						String propertyName = "";
						for (String s : inputPropertyDetails) {
							StringTokenizer st = new StringTokenizer(s, ",");
							if ("E".equals(type) || "B".equals(type)) {
								propertyName = st.nextToken();
								String propValue = st.nextToken();
								if ("E".equals(type)) {
									items.add(propValue);
								}
								if ("true".equals(st.nextToken())) {
									value = propValue;
								}
							}
						}
						if ("E".equals(type) || "B".equals(type)) {
							row = addAnalysisTableRowForBooleanAndEnumeration(component, parentRow, componentHasFailure,
									added, value, items, type,
									propertyName);
							if (row != null) {
								if (added) {
									row.setNonDisplayParentName(component.getName());
								}
								else {
									rowToReturnToParent = null;
								}
								setRowAttributes(row, component);
								row.setHasInputs(true);
								row.setFailed(componentHasFailure, failure);
								rows.add(row);

								added = true;
								if (parentRow != null) {
									parentRow.addChild(row);
								}
							}
						}
						else if ("F".equals(type)) {
							// we only add a float property entry if it has not already
							// been added and there are no further property keys available. If there
							// are further keys then they may be more relevant to add here...
							//
							// i.e. we ensure that we don't end up with a boolean and a float property in the table
							// just because we see the float property first. Instead we only take the boolean as
							// this may be being used to control the models in the simulation.
							//
							if (!added && !keys.hasNext()) {
								// component's first row in the browser table
								row = createAnalysisTableRowForFloatProperties(component, parentRow,componentHasFailure);
								setRowAttributes(row, component);
								row.setHasInputs(false); // inputs of type 'F' don't count...
								row.setFailed(componentHasFailure, failure);
								rows.add(row);
								added = true;
								rowToReturnToParent = row;
							}
						}
					}
					return rowToReturnToParent;
				}
				else {
					// component with no input properties
					if (component.isChildComponent()) {
						row = new AnalysisBrowserTableRow(component.getUid(),
								getComponentIcon(component, componentHasFailure, false)
								// should be null for removing icon;
								, getComponentNameInRow(component, false), component.getDesignUID(),
								null, null, component.getAnalysisModel(), component.isChildComponent()
								, parentRow);
					}
					else {
						row = new AnalysisBrowserTableRow(component.getUid(),
								getComponentIcon(component, componentHasFailure, false),
								getComponentNameInRow(component, false),
								component.getDesignUID(),
								null, null, component.getAnalysisModel(), component.isChildComponent(),
								null);
					}
					setRowAttributes(row, component);
					row.setFailed(componentHasFailure, failure);
					rows.add(row);
					return row;
				}
			}
		}
		return row;
	}

	@NotNull private AnalysisBrowserTableRow createAnalysisTableRowForFloatProperties(
			@NotNull ScopeComponent component,
			@Nullable AnalysisBrowserTableRow parentRow,
			boolean componentHasFailure)
	{
		AnalysisBrowserTableRow row;
		if (component.isChildComponent()) {
			row = new AnalysisBrowserTableRow(component.getUid(),
					component.getIcon(componentHasFailure), component.getName(),
					component.getDesignUID(), null, null, component.getAnalysisModel(),
					component.isChildComponent(), parentRow);
		}
		else {
			row = new AnalysisBrowserTableRow(component.getUid(),
					component.getIcon(componentHasFailure), component.getName(),
					component.getDesignUID(), null, null, component.getAnalysisModel(),
					component.isChildComponent(), null);
		}
		return row;
	}

	@Nullable private AnalysisBrowserTableRow addAnalysisTableRowForBooleanAndEnumeration(
			@NotNull ScopeComponent component,
			@Nullable AnalysisBrowserTableRow parentRow,
			boolean componentHasFailure, boolean added, @Nullable String value, @NotNull List<String> items,
			@NotNull String type,
			@NotNull String propertyName)
	{
		if ("E".equals(type)) {
			return new AnalysisBrowserTableRow(component.getUid(),
					getComponentIcon(component, componentHasFailure, added),
					getComponentNameInRow(component, added),
					//should be null to remove icon
					component.getDesignUID(), propertyName, items.toArray(),
					value,
					component.getAnalysisModel(), component.isChildComponent()
					, component.isChildComponent() ? parentRow : null);
		}
		if ("B".equals(type)) {
			return new AnalysisBrowserTableRow(component.getUid(),
					getComponentIcon(component, componentHasFailure, added),
					getComponentNameInRow(component, added),
					component.getDesignUID(), propertyName, Boolean.valueOf(value),
					component.getAnalysisModel(), component.isChildComponent(),
					null);
		}
		return  null;
	}

	//Get input interface properties for the component.
	//It returns the properties as a map of property name vs list of values property can take.
	@NotNull private Map<String, List<String>> getInputInterfaceProperties(@NotNull DefaultMutableTreeNode inputNode)
	{
		Map<String, List<String>> properties = new HashMap<>();
		for (int i = 0; i < inputNode.getChildCount(); i++) {
			String nodeData =
					((DefaultMutableTreeNode) inputNode.getChildAt(i)).getUserObject().toString();
			StringTokenizer st = new StringTokenizer(nodeData, ",");
			String propName = st.nextToken();
			// add to list of same name props
			List<String> v = properties.get(propName);
			if (v == null) {
				v = new ArrayList<>();
				properties.put(propName, v);
			}
			v.add(nodeData);
		}
		return properties;
	}

	@Nullable private String getComponentNameInRow(@NotNull ScopeComponent component, boolean componentAlreadyAdded)
	{
		return componentAlreadyAdded ? null : component.getName();
	}

	@Nullable private JTable buildBrowserTable()
	{
		changeCurrentScope();
		if (isCurrentScopeApplicable()) {

			final List<ScopeComponent> componentsList =
					new ScopedComponentsIdentifier().getScopedComponents(currentScope);
			currentScope.setScopedComponents(componentsList);

			LogicAnalysisServices analysisService =
					((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices());
			simSession = analysisService.getSimSession(currentScope.getUid());
			scopeFunctions = new ListSet<String>();

			final SortedList<AnalysisBrowserTableRow> rows = new SortedList<AnalysisBrowserTableRow>();
			if (simSession == null) {
				 /**creating row for component
				  *if the component has children create row for parent
				  *		create rows for the children
				  *else move on the the next component
				  */
				 for (ScopeComponent component : componentsList) {
					 AnalysisBrowserTableRow parentRow = new AnalysisBrowserTableRow(component.getUid(),
							 getComponentIcon(component, false, false), component.getName(), component.getDesignUID(),
							 null,
							 null, component.getAnalysisModel(), component.isChildComponent(), null);

					 setRowAttributes(parentRow, component);
					 rows.add(parentRow);
					 if (component.isParentComponent()) {
						 for (ScopeComponent childComponent : component.getChildren()) {
							 AnalysisBrowserTableRow row = new AnalysisBrowserTableRow(childComponent.getUid(),
									 getComponentIcon(childComponent, false, false), childComponent.getName(),
									 childComponent.getDesignUID(), null,
									 null, childComponent.getAnalysisModel(), childComponent.isChildComponent(), null);
							 row.setParentName(component.getName());
							 setRowAttributes(row, childComponent);
							 rows.add(row);
							 parentRow.addChild(row);
						 }
					 }
					 addScopeFunctions(component);
				 }
			}
			else   // simSession != null (display input properties)
			{
				String currentValue = "";
				for (ScopeComponent component : componentsList) {
					addScopeFunctions(component);
					if (!StringUtils.isBlank(component.getAnalysisModel()) ||
							(!component.isDevice())) {
						String failure = failureString(component);
						AnalysisBrowserTableRow parentRow =
								addAnalysisRowDuringSimulation(component, failure, currentValue, rows, null);
						if (component.isParentComponent()) {
							for (ScopeComponent childComponent : component.getChildren()) {
								addScopeFunctions(childComponent);

								if (!StringUtils.isBlank(childComponent.getAnalysisModel()) ||
										(!component.isDevice())) {
									failure = failureString(childComponent);
									AnalysisBrowserTableRow row =
											addAnalysisRowDuringSimulation(childComponent, failure, currentValue,
													rows, parentRow);

									if (row != null) {
										if (parentRow != null) {
											parentRow.addChild(row);
										}
									}
								}
							}
						}
					}
				}
			}

			final List<AnalysisBrowserTableRow> namelessItems = new ArrayList<AnalysisBrowserTableRow>();
			rows.setComparator(new Comparator<AnalysisBrowserTableRow>()
			{
				@Override
				public int compare(AnalysisBrowserTableRow o1, AnalysisBrowserTableRow o2)
				{
					if (o1 == null || o2 == null) {
						return 0;
					}
					String name1 = o1.name;
					String name2 = o2.name;

					//Object without name should go to the bottom
					/**
					 * if o1 or o2 are children then the parent name if used to sort
					 * to keep the parent and children together.
					 * Also we can be sure that the parent comes before the children
					 * as the parent is added before any of its children
					 */
					if (name1 == null) {
						if (!namelessItems.contains(o1)) {
							namelessItems.add(o1);
						}
							return 1;
					}
					if (name2 == null) {
						if (!namelessItems.contains(o2)) {
							namelessItems.add(o2);
						}
							return -1;
					}
					//Comparing a device and not a device? Device to the bottom!
					if (!o1.isDevice() && o2.isDevice()) {
						return 1;
					}
					if (!o2.isDevice() && o1.isDevice()) {
						return -1;
					}
					return name1.compareTo(name2);
				}
			});
			//Sort input properties as well
			if (!namelessItems.isEmpty()) {
				Collections.sort(namelessItems, new Comparator<AnalysisBrowserTableRow>()
				{
					@Override
					public int compare(AnalysisBrowserTableRow o1, AnalysisBrowserTableRow o2)
					{
						if (o1.propertyName == null || o2.propertyName == null) {
							return 0;
						}
						return o1.propertyName.compareTo(o2.propertyName);
					}
				});
			}
			int insertIndex = 0;
			AnalysisBrowserTableRow[] myRows = new AnalysisBrowserTableRow[rows.size()];
			for (AnalysisBrowserTableRow currentRow : rows) {
				//myRows[i] = currentRow;
				//If no name - it is an input property, we will treat it later
				if (currentRow.name != null) {

					myRows[insertIndex] = currentRow;
					insertIndex++;

					//If not has inputs - do not check it
					if (currentRow.hasInputs()) {
						//Look nameless stuff
						for (AnalysisBrowserTableRow currentNamelessRow : namelessItems) {
							//If UIDs are the same - this input property belongs to the device - add it behind it
							if (currentNamelessRow.getNonDisplayParentName().equals(currentRow.name)) {
								myRows[insertIndex] = currentNamelessRow;
								insertIndex++;
							}
						}
					}
				}
			}
			rowsUpdate = myRows;
			updateTableDataModel(myRows);
			return table;
		}

		return null;
	}

	private void updateTableDataModel(AnalysisBrowserTableRow[] myRows)
	{
		tablemodel.setModelData(myRows);
		tablemodel.initDataVector();

		//table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setAutoCreateColumnsFromModel(false);

		table.removeMouseListener(browserTableAdapter);

		browserTableAdapter = new AnalysisBrowserTableMouseAdapter(controller, table);

		table.addMouseListener(browserTableAdapter);

		if (table.getColumnCount() == 0) {
			for (int k = 0; k < tablemodel.getColumnCount(); k++) {

				TableCellRenderer renderer = null;
				TableCellEditor editor = null;
				int width = 0;
				// todo: Width not working correctly.
				switch (k) {

					case 0:
						renderer = new IconTableCellRenderer();
						width = 45;
						break;
					case 1:
						renderer = new ComponentNameCellRenderer();
						width = 115;
						break;
					case 2:
						renderer = new InputPropertiesCellRenderer();
						width = 145;
						break;
				}

				TableColumn column = new TableColumn(k, width, renderer, editor);
				column.setMinWidth(50);
				column.setHeaderValue(tablemodel.getColumnName(k));

				// add column to table
				table.addColumn(column);
			}
		}
		table.setAutoCreateColumnsFromModel(false);
		table.setModel(tablemodel);

		// ensure the table is in the scroll panel -- if it has not already been
		// set the viewport view should be null
		if (scrollPane != null &&
				(scrollPane.getViewport() == null || scrollPane.getViewport().getView() == null)) {
			scrollPane.setViewportView(table);
		}

		//dts0100627828 : Apply the existing filter to the data stored in modeldata of the table
		table.applyFilter();

		updateTableRowHeights();
	}

	protected void changeCurrentScope()
	{
		if (currentScope != null) {
			simulationInProgress(false);
			AnalysisServices.unregisterSimulationStatusIndicator(currentScope.getUid());
		}

		currentScope = (AbstractAnalysisNetlistScope) AnalysisServices.getCurrentAnalysisNetlistScope();

		if (currentScope != null) {
			AnalysisServices.registerSimulationStatusIndicator(this, currentScope.getUid());
		}
	}

	// TODO: refactor this method
	protected synchronized void updateTableRowHeights()
	{
		table.setRowHeight(20);
		for (int i = 0; i < table.getRowCount(); i++) {
			AnalysisBrowserTableRow row = table.getTableRow(i);
			if (row != null && row.inputInterface != null) {
				table.setRowHeight(i, 20);
			}
		}
	}

	/**
	 * This method examines the tree node (which is as returned by the IAnalysisSimulationSession.getComponentFailures(
	 * ) method) to see if any of the failures listed for this component are currently active.
	 *
	 * @param failureNode, the failure node to examine.
	 *
	 * @return boolean, true if there is an active component.
	 */
	protected String hasActiveFailure(DefaultMutableTreeNode failureNode)
	{
		String foundActive = null;

		if (failureNode != null) {
			for (int i = 0; i < failureNode.getChildCount() && foundActive == null; i++) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) failureNode.getChildAt(i);
				String data = (String) child.getUserObject();
				if (data.endsWith("true")) {
					foundActive = data.substring(0, data.lastIndexOf(','));
				}
			}
		}

		return foundActive;
	}

	// ///////////// //
	// Other methods //
	// ///////////// //

	/**
	 * This method is called to inform us when a simulation is in progress
	 *
	 * @param inProgress, are we simulating ??
	 */
	public void simulationInProgress(final boolean inProgress)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				bar.setIndeterminate(inProgress);
			}
		};
		SwingUtilities.invokeLater(r);
	}

	// ////////////////////////////// //
	// PropertyChangeListener methods //
	// ////////////////////////////// //

	/**
	 * This method listens for changes to the SimulationControlsPanel.selected property on the actions. This allows the
	 * correct radio button to be selected at all times.
	 * <p>
	 * The actions decide whether they should be selected dependant upon whether their simtype is selected in the
	 * analysis subsystem, this propogates through to here via this event.
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{

		// if it's not a change we'return interested in.....
		if (evt.getPropertyName() == null || !"SimulationControlsPanel.selected".equals(evt.getPropertyName())) {
			return;
		}

		if (AnalysisHelper.getInstance().isLegacyAnalysisMode() && controller != null && controller.getCaplet() != null &&
				controller.getCaplet().getResource() != null) {
			BaseLogicResource resource = (BaseLogicResource) controller.getCaplet().getResource();
			IActionUI qualUI = resource.getQualSimAction();
			IActionUI spiceUI = resource.getSpiceSimAction();

			boolean value = (Boolean) evt.getNewValue();
			if (value) { // only fire when one is selected.

				IActionUI newValue = (IActionUI) evt.getSource();
				IActionUI actionToFire = newValue.equals(qualUI) ? spiceUI : qualUI;

				// Because of the way we build the browser panel we need to unset before setting it :-(
				actionToFire.putValue(Drawer.DRAWER_SELECTED, null);
				actionToFire.putValue(Drawer.DRAWER_SELECTED, newValue);
			}
		}
	}

	IAnalysisSimulationSessionController lastSimSession;

	public void updateBrowsableUI()
	{
		if (resetButton != null) {
			resetButton.getAction().setEnabled(resetButton.getAction().isEnabled());
		}
		if (simulateButton != null) {
			Action simulateAction = simulateButton.getAction();
			// update the UI the long winded way in
			if (simulateAction instanceof SimulateActionUI) {
				((SimulateActionUI) simulateAction).updateUI();
			}
		}
		if (toolBarActionContainer != null) {
			toolBarActionContainer.updateUI();
		}
		if (toolbar != null) {
			toolbar.updateUI();
			toolbar.repaint();
			toolbar.validate();
		}

		if (update) {

//			currentScope = (AbstractAnalysisNetlistScope) LogicAnalysisServices.getCurrentAnalysisNetlistScope();
			if (isCurrentScopeApplicable()) {

				LogicAnalysisServices analysisService =
						((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices());
				if (analysisService.getDynSimController(currentScope.getUid()) != null) {
					simSession = analysisService.getSimSession(currentScope.getUid());

					if (simSession != lastSimSession) {
						analysisService.getDynSimController(currentScope.getUid()).addDynSimControllerListener(this);
					}
				}
			}
		}
	}

	public JPanel createFilterButtons()
	{
		// create the toolbar to which we can add all the buttons
		JToolBar bar = new JToolBar();
		bar.setRollover(true);
		bar.setFloatable(false);
		bar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
		bar.setBorder(BorderFactory.createEmptyBorder());

		// create the buttons and add them to the bar.
		FilterButton attachementFilter = new FilterButton(table);
		// Define state 0 for the filter button.
		attachementFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_attachments_nofiltering.png"));
		attachementFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.attachementFilter.as_filter_attachments_nofiltering.tooltip"));
		attachementFilter.addValidTypes("");
		// Define state 1 for the filter button.
		attachementFilter
				.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_attachments_with_attachments.png"));
		attachementFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.attachementFilter.as_filter_attachments_with_attachments.tooltip"));
		attachementFilter.addValidTypes("With");
		// Define state 2 for the filter button.
		attachementFilter
				.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_attachments_without_attachments.png"));
		attachementFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.attachementFilter.as_filter_attachments_without_attachments.tooltip"));
		attachementFilter.addValidTypes("Without");

		attachementFilter.updateIcon();
		bar.add(attachementFilter);

		FilterButton instanceFilter = new FilterButton(table);
		instanceFilter.setName("InstanceFilter");

		instanceFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_objects_nofilter.png"));
		instanceFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.instanceFilter.as_filter_objects_nofilter.tooltip"));
		instanceFilter.addValidTypes("");

		instanceFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_objects_show_devices.png"));
		instanceFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.instanceFilter.as_filter_objects_show_devices.tooltip"));
		instanceFilter.addValidTypes("Devices");

		instanceFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_objects_show_wires.png"));
		instanceFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.instanceFilter.as_filter_objects_show_wires.tooltip"));
		instanceFilter.addValidTypes("Wires");

		instanceFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_objects_show_connectors.png"));
		instanceFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.instanceFilter.as_filter_objects_show_connectors.tooltip"));
		instanceFilter.addValidTypes("Connectors");

		instanceFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_objects_show_splices.png"));
		if (AppInfo.getAppInfo().isVeSys() || AppInfo.getAppInfo().isSEElectrical()) {
			instanceFilter.addToolTipText(ResourceMgr.getString(
					AnalysisBrowserPanel.class,
					"AnalysisBrowserPanel.instanceFilter.as_filter_objects_show_splices_derivatives.tooltip"));
		}
		else {
			instanceFilter.addToolTipText(ResourceMgr.getString(
					AnalysisBrowserPanel.class,
					"AnalysisBrowserPanel.instanceFilter.as_filter_objects_show_splices.tooltip"));
		}
		instanceFilter.addValidTypes("Splices");
		instanceFilter.updateIcon();
		bar.add(instanceFilter);

		instanceFilter.addIcon(IconUtils.decorateComplete(CHSImageLoader.loadImageIcon(CHSImages.NET_ICON_ENABLED)));
		if (AppInfo.getAppInfo().isVeSys() || AppInfo.getAppInfo().isSEElectrical()) {
			instanceFilter.addToolTipText(ResourceMgr.getString(
					AnalysisBrowserPanel.class,
					"AnalysisBrowserPanel.instanceFilter.as_filter_objects_show_shields_derivatives.tooltip"));
		}
		else {
			instanceFilter.addToolTipText(ResourceMgr.getString(
					AnalysisBrowserPanel.class,
					"AnalysisBrowserPanel.instanceFilter.as_filter_objects_show_nets.tooltip"));
		}
		instanceFilter.addValidTypes("NetsOrShields");
		instanceFilter.updateIcon();
		bar.add(instanceFilter);

		FilterButton inputsAndFunctionsFilter = new FilterButton(table);
		inputsAndFunctionsFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_objects_with_all.png"));
		inputsAndFunctionsFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.inputsAndFunctionsFilter.as_filter_objects_with_all.tooltip"));
		inputsAndFunctionsFilter.addValidTypes("");

		IApplicationSuite applicationSuite = ApplicationSuiteInfo.getInstance().getCurrentApplicationSuite();

		inputsAndFunctionsFilter
				.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_objects_with_inputs.png"));
		inputsAndFunctionsFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.inputsAndFunctionsFilter.as_filter_objects_with_inputs.tooltip"));
		inputsAndFunctionsFilter.addValidTypes("Inputs");

		inputsAndFunctionsFilter
				.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_objects_with_functions.png"));
		inputsAndFunctionsFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.inputsAndFunctionsFilter.as_filter_objects_with_functions.tooltip"));
		inputsAndFunctionsFilter.addValidTypes("Functions");

		inputsAndFunctionsFilter.updateIcon();
		bar.add(inputsAndFunctionsFilter);

		JPanel temp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		temp.add(bar);
		return temp;
	}

	public JPanel createFunctionListFilterButtons()
	{
		// create the toolbar to which we can add all the buttons
		JToolBar bar = new JToolBar();
		bar.setRollover(true);
		bar.setFloatable(false);
		bar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
		bar.setBorder(BorderFactory.createEmptyBorder());

		// create the buttons and add them to the bar.

		FilterButton inputsAndFunctionsFilter = new FilterButton(functionList);
		inputsAndFunctionsFilter
				.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_functions_by_direction_all.png"));
		inputsAndFunctionsFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.inputsAndFunctionsFilter.as_filter_functions_by_direction_all.tooltip"));
		inputsAndFunctionsFilter.addValidTypes("");

		inputsAndFunctionsFilter
				.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_functions_by_direction_control.png"));
		inputsAndFunctionsFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.inputsAndFunctionsFilter.as_filter_functions_by_direction_control.tooltip"));
		inputsAndFunctionsFilter.addValidTypes("1");

		inputsAndFunctionsFilter
				.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_functions_by_direction_output.png"));
		inputsAndFunctionsFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.inputsAndFunctionsFilter.as_filter_functions_by_direction_output.tooltip"));
		inputsAndFunctionsFilter.addValidTypes("0");
		inputsAndFunctionsFilter.updateIcon();
		bar.add(inputsAndFunctionsFilter);

		FilterButton activeFilter = new FilterButton(functionList);
		activeFilter.setName("ActionFilter");
		activeFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_functions_by_status_all.png"));
		activeFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.activeFilter.as_filter_functions_by_status_all.tooltip"));
		activeFilter.addValidTypes("");

		activeFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_functions_by_status_active.png"));
		activeFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.activeFilter.as_filter_functions_by_status_active.tooltip"));
		activeFilter.addValidTypes("active");

		activeFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_functions_by_status_sneak.png"));
		activeFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.activeFilter.as_filter_functions_by_status_sneak.tooltip"));
		activeFilter.addValidTypes("sneak");

		activeFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/failedFunction.png"));
		activeFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.activeFilter.failedFunction.tooltip"));
		activeFilter.addValidTypes("failed");

		activeFilter.updateIcon();
		bar.add(activeFilter);

		FilterButton scopeFilter = new FilterButton(functionList);
		scopeFilter.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_functions_byscope_all.png"));
		scopeFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.scopeFilter.as_filter_functions_byscope_all.tooltip"));
		scopeFilter.addValidTypes("");

		scopeFilter
				.addIcon(CHSImageLoader.loadImageIcon("chs/images/app/as_filter_functions_byscope_current_scope.png"));
		scopeFilter.addToolTipText(ResourceMgr.getString(
				AnalysisBrowserPanel.class,
				"AnalysisBrowserPanel.scopeFilter.as_filter_functions_byscope_current_scope.tooltip"));
		scopeFilter.addValidTypes("scope");
		scopeFilter.updateIcon();
		bar.add(scopeFilter);

		JPanel temp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		temp.add(bar);
		return temp;
	}

	public void scopeChanged(IAnalysisNetlistScope newScope)
	{
		if (table != null) {
			table.clearPreservedRowExpansionStates();
		}
		updateBrowsableUI();

		boolean isFirstTimeScopeSet = currentScope == null;

		changeCurrentScope();
		if (isCurrentScopeApplicable()) {
			if (!update) {
				buildUI();
			}
			else {
				if (table == null) {
					buildUI();
				}
				buildBrowserTable();
			}
		}
		else if (!isFirstTimeScopeSet) {
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if (table != null) {
						removeCentralSplitPane();
						centralSplitPane = createVerticalSplitPane();
						add(centralSplitPane, BorderLayout.CENTER);
					}
					repaint();
				}
			});
		}
	}

	public void removeCentralSplitPane()
	{
		if (centralSplitPane != null) {
			remove(centralSplitPane);
		}
		if (table != null) {
			int columnCount = table.getColumnCount();
			for (int i = 0; i < columnCount; i++) {
				TableColumn col = table.getColumn(tablemodel.getColumnName(i));
				table.removeColumn(col);
			}
			tablemodel.removeAllRows();
			table = null;
		}
	}

	/**
	 * Is the currently set scope applicable to me? Am I working on something that is contained within the current
	 * scope?
	 *
	 * @return boolean, true if this is the case
	 */
	protected boolean isCurrentScopeApplicable()
	{
		// We don't need to update anything that isn't in our view point.
		// If we're not the design being analysed or not part of the buildlist
		// being analysed we really dont care what's going on...
		if (currentScope != null) {
			Model m = (Model) controller.getCapletModel();
			if (m != null && m.getDesign() != null && currentScope.isInScope(m.getDesign())) {
				return true;
			}
		}
		return false;
	}

	protected void updateTableAfterEvent()
	{
		if (update) {
			changeCurrentScope();
			if (isCurrentScopeApplicable()) {
				updateTable();
			}
		}
	}

	public void filterChanged(IGraphicsFilterControl filterControl)
	{
		updateScopedComponentsWithNewFilter();
		updateTableAfterEvent();
	}

	protected void updateScopedComponentsWithNewFilter()
	{
		IAnalysisNetlistScope scope = AnalysisServices.getCurrentAnalysisNetlistScope();
		if (scope != null) {
			//reinitialize the list of scoped components for display  in browser panel.
			AnalysisServices.getCurrentAnalysisNetlistScope().resetScopedComponentsList();
			AnalysisServices.getCurrentAnalysisNetlistScope().getScopedComponents();
		}
	}

	public void modelPreChanged(ModelChangeEvent e)
	{

	}

	public void modelChanged(ModelChangeEvent e)
	{
		// don't update unless we've made some changes to the model.
		// There still remains the issue that too many instances need to be updated
		// one for each design, todo: rharring / melmorsy
		if (!e.getChangedObjectsUIDs().isEmpty() || !e.getNewObjectsUIDs().isEmpty()) {
			updateTableAfterEvent();
		}
		else if (isVisible()) {
			IDesign design = ((ILogicModel) e.getModel()).getDesign();
			if (design != null) {
				String currentDesignName = "\n " + design.getName() + "\n";
				ActionContainer scopeContainer =
						((BaseLogicResource) controller.getCaplet().getResource()).getScopeAction();
				if (scopeContainer != null) {
					String storedScopeString = scopeContainer.toString();
					if (!storedScopeString.contains(currentDesignName)) {
						IBaseDiagram activeDiagram = CAFUtils.getInstance().getActiveDiagram();
						if (activeDiagram != null) {
							boolean iAmActiveDesignBrowserPanel = activeDiagram.getDesignContainer()
									.equals(((BaseController) controller).getCapletModel().getDesign());
							if (iAmActiveDesignBrowserPanel) {
								updateToolBar();
							}
						}
					}
				}
			}
		}
	}// all changes to the sim controller causes this method to be messaged.

	public void dynamicSimControllerChange(DynSimControllerChangeEvent dscce)
	{
		// refresh the browser table and key the last simulation session in history
		LogicAnalysisServices analysisService = ((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices());
		if (currentScope != null && analysisService != null) {
			simSession = analysisService.getSimSession(currentScope.getUid());

			boolean rebuildTable = false;
			if (simSession != lastSimSession) {
				lastSimSession = simSession;
				rebuildTable = true;
			}
			if (currentMode != dscce.getCurrentMode()) {
				rebuildTable = true;
				currentMode = dscce.getCurrentMode();
			}
			if (rebuildTable) {
				reset();
				updateTable();
			}
		}
	}

	// ////////////////////////////////////////// //
	// IAnalysisSimulationResultsConsumer methods //
	// ////////////////////////////////////////// //
	private void reset()
	{
		if (functionList != null) {
			functionList.reset();
		}
	}

	public void setAchievedFunctions(Collection<String> data)
	{
		synchronized (LOCK_OBJECT) {
			if (functionList != null) {
				functionList.setActiveFunctions(data);
			}
		}
	}

	public void setFailedFunctions(Collection<String> data)
	{
		synchronized (LOCK_OBJECT) {
			if (functionList != null) {
				functionList.setFailedFunctions(data);
			}
		}
	}

	public void setSneakFunctions(Collection<String> data)
	{
		synchronized (LOCK_OBJECT) {
			if (functionList != null) {
				functionList.setSneakFunctions(data);
			}
		}
	}

	public void setReactions(Collection<String> data)
	{
		// do nothing...
	}

	public void setConsoleData(Collection<String> data)
	{
		// do nothing
	}

	public void setErrorData(Collection<String> data)
	{
		// do nothing
	}

	public void setFunctionResults(Collection<String> levelOrder, Collection<String> active, Collection<String> failed,
			Collection<String> sneak)
	{
		// do nothing
	}

	/**
	 * After we've updated the results we need to show them to the user so we need to update the table and the function
	 * list.
	 */
	public void updateComplete()
	{
		synchronized (LOCK_OBJECT) {
			if (functionList != null) {
				Runnable r = new Runnable()
				{
					public void run()
					{
						try {
							functionList.restoreFromFilters();
							functionList.repaint();
						}
						catch (Exception e) {
							// do nothing...
						}
					}
				};
				if (SwingUtilities.isEventDispatchThread()) {
					r.run();
				}
				else {
					try {
						SwingUtilities.invokeLater(r);
					}
					catch (Exception e) {
						// do nothing...
					}
				}
			}
		}
	}

	public void update(@NotNull AnalysisSimulationResultsImporter simResImporter)
	{
		setAchievedFunctions(simResImporter.getFunctionList());
		setFailedFunctions(simResImporter.getFailedList());
		setSneakFunctions(simResImporter.getSneakList());
		super.update(simResImporter);
	}

	public void actionPerformed(ActionEvent e)
	{
		LogicAnalysisServices analysisService = ((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices());
		if (currentScope != null) {
			simSession = analysisService.getSimSession(currentScope.getUid());
			if (simSession != null) {
				simSession.cancelSimulation(currentScope.getUid());
				AnalysisServices.DynSimController controller =
						LogicAnalysisServices.getAnalysisServices().getDynSimController(currentScope.getUid());
				if (controller.getMode() == AnalysisServices.DYN_SIM_BACKGROUND) {
					controller.setMode(AnalysisServices.DYN_SIM_DEMAND);
				}
			}
		}
	}

	public void projectChange(IProjectChangeEvent evt)
	{
		//Blank Implementation
	}

	public void updateToolBarForChangedScopeCandidates()
	{
		updateToolBar();
		CAFUtils.getInstance().tickleUI(CAFUtils.getInstance().getFIB());
	}

	public void windowChanged(WindowChangeEvent wce)
	{
		if (isVisible() && ((wce.getNewWindow() != null && wce.getOldWindow() == null) ||
				(wce.getNewWindow() == null && wce.getOldWindow() != null))) {
			IBaseDiagram activeDiagram = CAFUtils.getInstance().getActiveDiagram();
			if (activeDiagram != null) {
				boolean iAmActiveDesignBrowserPanel = activeDiagram.getDesignContainer()
						.equals(((BaseController) controller).getCapletModel().getDesign());

				if (iAmActiveDesignBrowserPanel) {
					if (!toolbarCreated) {
						createToolbar();
					}
					updateToolBarForChangedScopeCandidates();
				}
			}
		}
	}

	public void postWindowChanged(WindowChangeEvent wce)
	{
		if (wce.getNewWindow() instanceof ICapletWindow) {
			if (controller != null && controller.equals(((ICapletWindow) wce.getNewWindow()).getController())) {
				updateTableAfterEvent();
			}
		}
	}

	public void viewChanged(ViewChangeEvent vce)
	{
		//Not implemented here. So does nothing.
	}

	@Override public void notify(IBuildListMgr notifier)
	{
		// When the toolbar is not created, there is no need to update the toolbar on build list changes.
		if (!toolbarCreated) {
			return;
		}

		//it gets notified when any build list change occurs in build list manager
		//for an instance when there is a refresh action triggered and any build list has been found to be created or deleted
		//here we are first updating the controller - so that we can then fetch all the newly added items
		//The check of build list here to avoid updating the action container for build list other than analysis/logic
		updateAnalysisScopeContainerForChangedScopeMembers(checkIfBuildListsAndScopeChoicesForBuildListsAreEqual());
	}

	private void updateAnalysisScopeContainer(boolean isUpdatingAnalysisScopeContainerRequired)
	{
		if (toolbarCreated && isUpdatingAnalysisScopeContainerRequired) {
			//since the caplet has already been updated - we can assign the new action container
			toolBarActionContainer =
					((BaseLogicResource) caplet.getResource()).getAnalysisBrowserToolbar();
			((CAFToolBar) toolbar).updateCAFToolBarButton(null,
					toolBarActionContainer.getMembers(), 0);
			toolBarActionContainer.updateUI();
			CAFUtils.getInstance().tickleUI(CAFUtils.getInstance().getFIB());
		}
	}

	private boolean checkIfBuildListsAndScopeChoicesForBuildListsAreEqual()
	{
		//here we are fetching all the build lists that might be used in analysis of type logic and analysis
		//this yields the current build list count after either deletion, creation or refresh
		int numberOfAnalyzableBuildList = AnalysisServices.getAnalyzableBuildLists(project, false).size();

		//here we are retrieving the scope action items in the container
		//if the scope container count of build list is same as we get from our build list manager - then we don't need the update
		//reduce the number by 2 i.e. 1 (for active design) + 1 (for the separator)
		Drawer analysisScopeContainer = (Drawer) getAnalysisScopeContainer();
		assert analysisScopeContainer != null;
		int numberOfScopeChoicesAvailableForBuildList = analysisScopeContainer.getChoices().size() - 2;

		return numberOfAnalyzableBuildList != numberOfScopeChoicesAvailableForBuildList;
	}


	@Nullable private Component getAnalysisScopeContainer()
	{
		//since no name yet has been provided to container - we are finding it with index
		return toolbar.getComponentAtIndex(0);
	}

	public static class AnalysisBrowserTableRow
	{

		@Nullable public String uid;
		@Nullable public Icon icon;
		@Nullable public String name = "";
		@Nullable public String propertyName;
		@Nullable public Object inputInterface;
		@Nullable public String modelLocation = "";
		public Object[] possibleValues;
		private boolean hasInputs;
		private boolean hasFunctions;
		private boolean isFailed;
		@Nullable private String failureName;
		private String nonDisplayParentName;
		@Nullable private String designUID;
		private COFTypeEnum componentType;
		private boolean isChild;
		private boolean isVisible;
		@Nullable private Set<AnalysisBrowserTableRow> children;
		@Nullable private String parentName;

		public AnalysisBrowserTableRow(@Nullable String uid, @Nullable Icon icon, @Nullable String name, @Nullable
				String designUID, @Nullable String propertyName,
				@Nullable Object input,
				@Nullable String modelLocatioan, boolean isChild, @Nullable AnalysisBrowserTableRow parentRow)
		{
			this.uid = uid;
			this.icon = icon;
			this.name = name;
			inputInterface = input;
			modelLocation = modelLocatioan;
			this.designUID = designUID;
			this.propertyName = propertyName;
			isFailed = false;
			this.isChild = isChild;
			isVisible = true;
			parentName = parentRow != null ? parentRow.name : null;
		}

		public AnalysisBrowserTableRow(@Nullable String uid, @Nullable Icon icon, @Nullable String name, @Nullable
				String designUID, @Nullable String propertyName,
				Object[] possibleValues,
				@Nullable Object input, @Nullable String modelLocatioan, boolean isChild, @Nullable AnalysisBrowserTableRow parentRow)
		{
			this.uid = uid;
			this.icon = icon;
			this.name = name;
			inputInterface = input;
			modelLocation = modelLocatioan;
			this.designUID = designUID;
			this.possibleValues = possibleValues;
			this.propertyName = propertyName;
			isFailed = false;
			this.isChild = isChild;
			isVisible = true;
			parentName = parentRow != null ? parentRow.name : null;
		}

		public AnalysisBrowserTableRow(@Nullable String uid, @Nullable Icon icon, @Nullable String name, @Nullable
				String designUID, @Nullable String propertyName,
				@Nullable Object input,
				@Nullable String modelLocatioan)
		{
			this(uid, icon, name, designUID, propertyName, input, modelLocatioan, false, null);
		}

		public AnalysisBrowserTableRow(String uid, Icon icon, String name, String designUID, String propertyName,
				Object[] possibleValues,
				Object input, String modelLocatioan)
		{
			this(uid, icon, name, designUID, propertyName, possibleValues, input, modelLocatioan, false, null);
		}

		private void setParentName(@Nullable String parentName)
		{
			this.parentName = parentName;
		}

		public void setNonDisplayParentName(String name)
		{
			nonDisplayParentName = name;
		}

		@Nullable public String getDesignUID()
		{
			return designUID;
		}

		@Nullable public String getComponentUid()
		{
			return uid;
		}

		public String getNonDisplayParentName()
		{
			return nonDisplayParentName;
		}

		public boolean isFailed()
		{
			return isFailed;
		}

		public void setFailed(boolean failed, @Nullable String failureName)
		{
			isFailed = failed;
			this.failureName = failureName;
		}

		@Nullable public String getFailureName()
		{
			return failureName;
		}

		public boolean isWire()
		{
			return COFTypeEnum.Wire.equals(componentType);
		}

		public boolean isConnector()
		{
			return COFTypeEnum.Connector.equals(componentType);
		}

		public boolean isSplice()
		{
			return COFTypeEnum.Splice.equals(componentType);
		}

		public boolean isNet()
		{
			return COFTypeEnum.Net.equals(componentType);
		}

		public boolean isShield()
		{
			return COFTypeEnum.Shield.equals(componentType);
		}

		public boolean isNetOrShield()
		{
			return isNet() || isShield();
		}

		public boolean isDevice()
		{
			return Arrays.asList(COFTypeEnum.Device, COFTypeEnum.Ground).contains(componentType);
		}

		public boolean hasInputs()
		{
			return hasInputs;
		}

		public boolean hasFunctions()
		{
			return hasFunctions;
		}

		public void setHasInputs(boolean b)
		{
			hasInputs = b;
		}

		public void setHasFunctions(boolean b)
		{
			hasFunctions = b;
		}

		public void setComponentType(COFTypeEnum componentType)
		{
			this.componentType = componentType;
		}

		public COFTypeEnum getComponentType()
		{
			return componentType;
		}

		public boolean isChild()
		{
			return isChild;
		}

		public boolean isParent(){
			return !isChild && children != null;
		}
		@Nullable private Set<AnalysisBrowserTableRow> getChildren()
		{
			return children;
		}

		public void addChild(@NotNull AnalysisBrowserTableRow  row){
			if(children == null){
				children = new ListSet<>();
			}
			children.add(row);
		}
		private void setIsVisible(boolean aisVisible)
		{
			isVisible = aisVisible;
		}

		public boolean getIsVisible()
		{
			return isVisible;
		}

		public String parentName()
		{
			return parentName;
		}
	}
	// AnalysisBrowserTable

	public class AnalysisBrowserFunctionList extends JList implements IAnalysisFilterableBrowserComponent
	{

		protected List<FunctionRepresentation> masterList = new ArrayList<FunctionRepresentation>();
		protected Set<String> scopeFunctions;
		HashMap<Object, String> filters = new HashMap<Object, String>();
		protected final ListModel listModel = new DefaultListModel();

		public AnalysisBrowserFunctionList()
		{
			super();
			setUI(new BasicListUI()
			{
				public void paint(Graphics g, JComponent c)
				{
					synchronized (listModel) {
						super.paint(g, c);
					}
				}
			});
			setModel(listModel);
			setVisibleRowCount(4);
			this.setCellRenderer(new FunctionListCellRenderer());
		}

		public void clear()
		{
			//((DefaultListModel)getModel( )).clear();
			masterList.clear();
		}

		public void setScopeFunctions(Set<String> scopedFunctions)
		{
			scopeFunctions = scopedFunctions;
		}

		public void addFunction(String s)
		{
			StringTokenizer st = new StringTokenizer(s, ",");
			String name = st.nextToken();
			String id = st.nextToken();
			boolean output = "0".equals(id);
			boolean control = "1".equals(id);
			boolean hasChild = "true".equals(st.nextToken());
			List<String> children = new ArrayList<String>();
			while (st.hasMoreTokens()) {
				String child = st.nextToken();
				children.add(child);
			}
			// we only add control and output functions to the list
			// the product and controlled by functions should not be present.
			if (control || output) {
				FunctionRepresentation rep = new FunctionRepresentation(name, id, output, control, hasChild, children);

				synchronized (listModel) {
//					if (!masterList.contains(rep)) {
					((DefaultListModel) getModel()).addElement(rep);
					masterList.add(rep);
//					}
				}
			}
		}

		public void restoreFromMasterList(
				boolean filter, boolean output, boolean control,
				boolean active, boolean sneak, boolean failed, boolean scope)
		{

			DefaultListModel myModel;
			synchronized (listModel) {
				//myModel = (DefaultListModel) getModel();
				//myModel.clear();

				List<FunctionRepresentation> temp = new ArrayList<FunctionRepresentation>();

				for (FunctionRepresentation rep : masterList) {
					if (!filter) {
						//myModel.addLogicElement(rep);
						temp.add(rep);
					}
					else {
						if (scope) {
							if (scopeFunctions != null && scopeFunctions.contains(rep.getName())) {
								//myModel.addLogicElement(rep);
								temp.add(rep);
							}
							continue;
						}
						if (active) {
							if (rep.isActive()) {
								//myModel.addLogicElement(rep);
								temp.add(rep);
							}
							continue;
						}
						else if (sneak) {
							if (rep.isSneak()) {
								//myModel.addLogicElement(rep);
								temp.add(rep);
							}
							continue;
						}
						else if (failed) {
							if (rep.isFailed()) {
								//myModel.addLogicElement(rep);
								temp.add(rep);
							}
							continue;
						}
						if (output) {
							if (rep.isOutput()) {
								//myModel.addLogicElement(rep);
								temp.add(rep);
							}
						}
						else if (control) {
							if (rep.isControl()) {
								//myModel.addLogicElement(rep);
								temp.add(rep);
							}
						}
					}
				}
				myModel = (DefaultListModel) getModel();
				myModel.clear();
				for (FunctionRepresentation rep : temp) {
					myModel.addElement(temp);
				}
			}
		}

		protected boolean includeRep(FunctionRepresentation rep, String filter)
		{
			boolean retVal = true;
			if ("1".equals(filter)) {
				retVal = rep.isControl();
			}
			else if ("0".equals(filter)) {
				retVal = rep.isOutput();
			}
			else if ("active".equals(filter)) {
				retVal = rep.isActive();
			}
			else if ("failed".equals(filter)) {
				retVal = rep.isFailed();
			}
			else if ("sneak".equals(filter)) {
				retVal = rep.isSneak();
			}
			else if ("scope".equals(filter)) {
				retVal = scopeFunctions != null && scopeFunctions.contains(rep.getName());
				if (!retVal && rep.hasChildren()) {
					retVal = scopeContainsChildrenOfRep(scopeFunctions, rep);
				}
			}
			return retVal;
		}

		private boolean scopeContainsChildrenOfRep(Set<String> functions, FunctionRepresentation rep)
		{
			List<String> targets = new ArrayList(rep.getChildren());
			for (String target : functions) {
				if (targets.contains(target)) {
					targets.remove(target);
					continue;
				}
			}
			return targets.size() == 0;
		}

		public void restoreFromFilters()
		{
			ArrayList<FunctionRepresentation> list = new ArrayList<FunctionRepresentation>(masterList);

			Iterator i = filters.values().iterator();
			while (i.hasNext()) {
				String filter = (String) i.next();
				ArrayList<FunctionRepresentation> remove = new ArrayList<FunctionRepresentation>();
				for (FunctionRepresentation rep : list) {
					if (!includeRep(rep, filter)) {
						remove.add(rep);
					}
				}
				for (FunctionRepresentation rep : remove) {
					list.remove(rep);
				}
			}

			synchronized (listModel) {
				DefaultListModel myModel = (DefaultListModel) getModel();
				myModel.clear();
				for (FunctionRepresentation rep : list) {
					myModel.addElement(rep);
				}
			}
		}

		public void setFilter(Object obj, String filter)
		{
			filters.put(obj, filter);
			restoreFromFilters();
		}

		private FunctionRepresentation findFunctionByName(String name)
		{
			for (FunctionRepresentation rep : masterList) {
				if (rep.getName().equals(name)) {
					return rep;
				}
			}
			return null;
		}

		public void setActiveFunctions(Collection<String> data)
		{
			for (String s : data) {
				FunctionRepresentation rep = findFunctionByName(s);
				if (rep != null) {
					rep.setActive(true);
				}
			}
		}

		public void setFailedFunctions(Collection<String> data)
		{
			for (String s : data) {
				FunctionRepresentation rep = findFunctionByName(s);
				if (rep != null) {
					rep.setFailed(true);
				}
			}
		}

		public void setSneakFunctions(Collection<String> data)
		{
			for (String s : data) {
				FunctionRepresentation rep = findFunctionByName(s);
				if (rep != null) {
					rep.setSneak(true);
				}
			}
		}

		public Dimension getPreferredSize()
		{
			Dimension d = getSize();
			try {
				d = super.getPreferredSize();
			}
			catch (Exception e) {
				// do nothing...
			}
			return d;
		}

		public void reset()
		{
			for (FunctionRepresentation rep : masterList) {
				rep.setActive(false);
				rep.setFailed(false);
				rep.setSneak(false);
			}
		}
	}

	class FunctionListCellRenderer extends DefaultListCellRenderer
	{

		Icon outputIcon = CHSImageLoader.loadImageIcon("chs/images/app/output_function.png");
		Icon controlIcon = CHSImageLoader.loadImageIcon("chs/images/app/control_function.png");
		Icon outputWithChildren = CHSImageLoader.loadImageIcon("chs/images/app/multiFunction.png");
		Icon activeIcon = CHSImageLoader.loadImageIcon("chs/images/app/output_function.png");
		Icon sneakIcon = CHSImageLoader.loadImageIcon("chs/images/app/sneakFunction.png");
		Icon failedIcon = CHSImageLoader.loadImageIcon("chs/images/app/failedFunction.png");

		public Component getListCellRendererComponent(
				JList list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof FunctionRepresentation) {
				FunctionRepresentation fRep = (FunctionRepresentation) value;
				if (fRep.isControl()) {
					setIcon(controlIcon);
				}
				else if (fRep.isOutput()) {
					if (fRep.hasChildren()) {
						setIcon(outputWithChildren);
					}
					else {
						setIcon(outputIcon);
					}
				}

				if (fRep.isActive()) {
					setIcon(activeIcon);
				}

				if (fRep.isFailed()) {
					setIcon(failedIcon);
				}
				if (fRep.isSneak()) {
					setIcon(sneakIcon);
				}
			}
			return this;
		}
	}

	private static class FunctionRepresentation
	{

		private String name;
		private String id;
		private boolean output;
		private boolean control;
		private boolean hasChild;
		private boolean isActive;
		private boolean isFailed;
		private boolean isSneak;
		private List<String> children;

		private FunctionRepresentation(String name, String id,
				boolean output, boolean control, boolean hasChildren, List<String> children)
		{

			setName(name);
			setId(id);
			setOutput(output);
			setControl(control);
			setHasChild(hasChildren);
			setChildren(children);
		}

		public List<String> getChildren()
		{
			return children;
		}

		public String getName()
		{
			return name;
		}

		public String toString()
		{
			return getName();
		}

		public boolean isControl()
		{
			return control;
		}

		public boolean isOutput()
		{
			return output;
		}

		public boolean hasChildren()
		{
			return isHasChild();
		}

		private void clearActivity()
		{
			isActive = false;
			isFailed = false;
			isSneak = false;
		}

		public void setActive(boolean b)
		{
			clearActivity();
			isActive = b;
		}

		public boolean isActive()
		{
			return isActive;
		}

		public void setFailed(boolean b)
		{
			clearActivity();
			isFailed = b;
		}

		public boolean isFailed()
		{
			return isFailed;
		}

		public void setSneak(boolean b)
		{
			clearActivity();
			isSneak = b;
		}

		public boolean isSneak()
		{
			return isSneak;
		}

		@Override public boolean equals(Object obj)
		{
			if (obj instanceof FunctionRepresentation) {
				return ((FunctionRepresentation) obj).getName().equals(getName()) &&
						((FunctionRepresentation) obj).getId().equals(getId());
			}
			return false;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public String getId()
		{
			return id;
		}

		public void setId(String id)
		{
			this.id = id;
		}

		public void setOutput(boolean output)
		{
			this.output = output;
		}

		public void setControl(boolean control)
		{
			this.control = control;
		}

		public boolean isHasChild()
		{
			return hasChild;
		}

		public void setHasChild(boolean hasChild)
		{
			this.hasChild = hasChild;
		}

		public void setChildren(List<String> children)
		{
			this.children = children;
		}
	}

	public class AnalysisBrowserTable extends JTable implements IAnalysisFilterableBrowserComponent
	{

		private Map<Object, String> filterSettings = new HashMap<Object, String>();
		private int numRowsInView;
		private List<Integer> mapViewRowIndexToModel;
		private final Map<String, Boolean> parentRowExpansionState = new HashMap<String, Boolean>();

		protected boolean filterSet = false;

		private Boolean parentRowExpansionState(String name)
		{
			return parentRowExpansionState.get(name);
		}

		@Nullable public String getUidForViewRow(int row)
		{
			int modelRowIndex = convertRowIndexToModel(row);
			AnalysisBrowserTableRow dataRow = getModelRow(modelRowIndex);
			if (dataRow != null) {
				return dataRow.getComponentUid();
			}
			return null;
		}

		@Nullable public String getDesignUIDForViewRow(int row)
		{
			int modelRowIndex = convertRowIndexToModel(row);
			AnalysisBrowserTableRow dataRow = getModelRow(modelRowIndex);
			if (dataRow != null) {
				return dataRow.getDesignUID();
			}
			return null;
		}

		public TableCellEditor getCellEditor(int row, int column)
		{
			int modelRowIndex = convertRowIndexToModel(row);
			TableCellRenderer renderer = getCellRenderer(modelRowIndex, column);
			if (renderer instanceof InputPropertiesCellRenderer) {

				AnalysisBrowserTableRow dataRow = getModelRow(modelRowIndex);
				String name = (dataRow.name == null) ? dataRow.getNonDisplayParentName() : dataRow.name;

				JComponent c = (JComponent) renderer
						.getTableCellRendererComponent(table, table.getValueAt(row, column), true, true, row, column);
				if (c instanceof InputPropertyComponent) {
					if (dataRow.possibleValues != null) {
						Object reqCell = tablemodel.getValueAt(row, column);
						JComboBox comboBox = null;
						//TODO sram look into the if..else condition in detail
						if (reqCell == null || !(reqCell instanceof JComboBox)) {
							comboBox = new JComboBox(dataRow.possibleValues);
							tablemodel.setCellComponent(comboBox, row, column);
						}
						else {
							comboBox = (JComboBox) reqCell;
							((JComboBox) reqCell).setModel(new DefaultComboBoxModel(dataRow.possibleValues));
						}

						InputPropertyAction ipa = new InputPropertyAction(
								(Model) controller.getCapletModel(), dataRow.propertyName,
								comboBox.getSelectedItem().toString(), name,
								simSession, currentScope.getUid());
						ipa.lastIndex = comboBox.getSelectedIndex();

						// There's a bug here whereby we add and add to the list of action listeners
						// The threads then interact and cause odd behavior.
						// Here we remove the action listeners before adding a new one...this is not
						// perfect but is safe for 13.1
						for (ActionListener listener : comboBox.getActionListeners()) {
							comboBox.removeActionListener(listener);
						}
						comboBox.addActionListener(ipa);
						InputPropertyComponent newc = new InputPropertyComponent(dataRow.propertyName, comboBox);
						newc.setBackground(c.getBackground());
						return new InputPropertyCellEditor(newc);
					}
					else {
						JCheckBox checlkBox = new JCheckBox();

						checlkBox.addActionListener(new InputPropertyAction(
								(Model) controller.getCapletModel(), dataRow.propertyName,
								String.valueOf(checlkBox.isSelected()), name,
								simSession, currentScope.getUid()));
						InputPropertyComponent newc = new InputPropertyComponent(dataRow.propertyName, checlkBox);
						newc.setBackground(c.getBackground());
						return new InputPropertyCellEditor(newc);
					}
				}
				else {
					return null;
				}
			}
			else {
				return null;
			}
		}

		public int convertRowIndexToModel(int viewRowIndex)
		{
			if (viewRowIndex < 0 || !filterSet) {
				return viewRowIndex;
			}
			if (mapViewRowIndexToModel != null) {
				if (viewRowIndex >= mapViewRowIndexToModel.size()) {
					return -1;
				}
				return mapViewRowIndexToModel.get(viewRowIndex);
			}
			return -1;
		}

		public Dimension getPreferredSize()
		{
			Dimension d = getSize();
			try {
				d = super.getPreferredSize();
			}
			catch (Exception e) {
				// do nothing...
			}
			return d;
		}

		public AnalysisBrowserTableRow getTableRow(int row)
		{
			int modelRowIndex = convertRowIndexToModel(row);
			if (modelRowIndex < ((AnalysisBrowserTableModel) getModel()).data.size()) {
				if (((AnalysisBrowserTableModel) getModel()).data != null) {
					return getModelRow(modelRowIndex);
				}
			}
			return null;
		}

		public Object getValueAt(int row, int column)
		{
			int modelRowIndex = convertRowIndexToModel(row);

			if (((AnalysisBrowserTableModel) getModel()).data != null) {
				Object result;
				//AnalysisBrowserTableRow dataRow = ((AnalysisBrowserTableModel) getModel()).data.get(modelRowIndex);

				//dts0100706938 - Fix: check inside if the rowIndex is a valid one (>=0 && < size())
				// if it's not, then return null
				AnalysisBrowserTableRow dataRow = getModelRow(modelRowIndex);
				if (dataRow == null) {
					return null;
				}
				switch (convertColumnIndexToModel(column)) {
					case 0:
						result = dataRow.icon;
						break;
					case 1:
						result = dataRow.name;
						break;

					case 2:
						result = dataRow.inputInterface;
						break;

					default:
						throw new IllegalArgumentException("Audit trail table model column index out of bounds");
				}

				return result;
			}
			return null;
		}

		/**
		 * This method sets a filter to determine which table model rows should be included in the table view. Filtering
		 * is always done first; sorting, if required, is done later.
		 * <p>
		 * The filter's 'include(row)' method returns 'true' if the row should be included in the displayed view and
		 * 'false' if the row should be filtered out of the view.
		 *
		 * @param filter, the table row filter. If the argument is null, all model rows are to be included in the view.
		 */
		public void setRowFilter(AnalysisRowFilter filter)
		{
			if (filter == null || filter.isEmpty()) {
				numRowsInView = getModel().getRowCount();
				mapViewRowIndexToModel = null;
			}
			else {
				if (mapViewRowIndexToModel == null) {
					mapViewRowIndexToModel = new ArrayList<Integer>();
				}
				else {
					mapViewRowIndexToModel.clear();
				}
				numRowsInView = 0;
				for (int i = 0; i < getModel().getRowCount(); i++) {
					AnalysisBrowserTableRow rowData = getModelRow(i);
					if (filter.include(rowData)) {
						mapViewRowIndexToModel.add(i);
						numRowsInView++;
					}
				}
			}
		}

		/**
		 * This method gets the specified row of the model.
		 *
		 * @param rowIndex, the model row index of the row we want.
		 *
		 * @return List<Object>, the row data.
		 */
		@Nullable
		public synchronized AnalysisBrowserTableRow getModelRow(int rowIndex)
		{
			List<AnalysisBrowserTableRow> dataModel = ((AnalysisBrowserTableModel) getModel()).data;
			if (rowIndex >= 0 && rowIndex < dataModel.size()) {
				return dataModel.get(rowIndex);
			}
			else {
				return null;
			}
		}

		public synchronized void setFilter(Object filterObj, String newValue)
		{
			filterSettings.put(filterObj, newValue);
			updateTable();
		}

		//dts0100627828 : Apply the existing filter to the data stored in modeldata of the table
		public synchronized void applyFilter()
		{
			if (!filterSettings.isEmpty()) {
				Collection<String> filters = filterSettings.values();
				AnalysisRowFilter filter = new AnalysisRowFilter(filters);
				setRowFilter(filter);
				filterSet = true;
				updateTableRowHeights();
				revalidate();
				repaint();
			}
		}

		public int getRowCount()
		{
			if (((AnalysisBrowserTableModel) getModel()).data != null) {
				if (filterSet) {
					return numRowsInView;
				}
				else {
					return ((AnalysisBrowserTableModel) getModel()).data.size();
				}
			}
			else {
				return 0;
			}
		}

		public void toggleRowVisibility(int row, boolean preserveRowState)
		{
			AnalysisBrowserTableRow dataRow = tablemodel.data.get(row);
			if (dataRow.getChildren() != null) {
				boolean flag = false;
				for (AnalysisBrowserTableRow tableRow : dataRow.getChildren()) {
					tableRow.setIsVisible(!tableRow.getIsVisible());
					flag = !tableRow.getIsVisible();
				}
				if (!flag) {
					dataRow.icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_arrow_down.gif");
				}
				else {
					dataRow.icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_arrow_right.gif");
				}
				if (preserveRowState) {
					preserveRowExpansionStateOfParentRows(row);
				}
			}
			/**
			 * Here we have updated the row info for the rows that should be visible
			 * Now we need to recreate the table data and update the browser table
			 * hence we are calling the below three
			 */
			updateTableDataModel(rowsUpdate);
			table.revalidate();
			table.repaint();
		}

		/**
		 * This function is used to store the states of the rows that have
		 * been clicked on previously
		 * @param row row index to preserve its state(expanded/collapsed)
		 */
		private void preserveRowExpansionStateOfParentRows(int row)
		{
			AnalysisBrowserTableRow tableRow = getTableRow(row);
			if (tableRow.parentName() == null) {
				String rowName = tableRow.name;
				Boolean currentVisibilityState = parentRowExpansionState.get(rowName);

				if (currentVisibilityState == null) {
					parentRowExpansionState.put(rowName, true);
					return;
				}

				parentRowExpansionState.put(rowName, !currentVisibilityState);
			}
		}

		public void clearPreservedRowExpansionStates()
		{
			parentRowExpansionState.clear();
		}
	}

	class AnalysisRowFilter
	{

		Collection<String> filters;

		public AnalysisRowFilter(Collection<String> filters)
		{
			this.filters = filters;
		}

		public boolean isEmpty()
		{
			return filters.size() == 0;
		}

		public boolean include(AnalysisBrowserTableRow object)
		{
			for (String s : filters) {
				// Wires have default attachments so we ignore them by default.
				if ("With".equals(s)) {
					if (StringUtils.isBlank(object.modelLocation) && !object.isWire()) {
						return false;
					}
				}
				else if ("Without".equals(s)) {
					if (!StringUtils.isBlank(object.modelLocation) || object.isWire()) {
						return false;
					}
				}
				else if ("Devices".equals(s)) {
					if (!object.isDevice()) {
						return false;
					}
				}
				else if ("Wires".equals(s)) {
					if (!object.isWire()) {
						return false;
					}
				}
				else if ("Connectors".equals(s)) {
					if (!object.isConnector()) {
						return false;
					}
				}
				else if ("Splices".equals(s)) {
					if (!object.isSplice()) {
						return false;
					}
				}
				else if ("NetsOrShields".equals(s)) {
					if (!object.isNetOrShield()) {
						return false;
					}
				}
				else if ("Inputs".equals(s)) {
					if (!object.hasInputs()) {
						return false;
					}
				}
				else if ("Functions".equals(s)) {
					if (!object.hasFunctions()) {
						return false;
					}
				}
			}
			return true;
		}
	}

	public class AnalysisBrowserTableModel extends DefaultTableModel
	{

		public List<AnalysisBrowserTableRow> data = new ArrayList<AnalysisBrowserTableRow>();
		boolean filter = false;

		public String[] columnNames = {
				"AnalysisBrowserTable.Icon",
				"AnalysisBrowserTable.name",
				"AnalysisBrowserTable.InputInterface",
		};

		public void initDataVector()
		{
			Object[][] dataVector = new Object[getRowCount()][3];
			tablemodel.setDataVector(dataVector, columnNames);
		}

		public int getRowCount()
		{
			if (data != null) {
				return data.size();
			}
			else {
				return 0;
			}
		}

		public void removeAllRows()
		{
			data.clear();
			dataVector.clear();
		}

		public void setModelData(AnalysisBrowserTableRow[] data)
		{
			synchronized (this) {
				this.data = new ArrayList<AnalysisBrowserTableRow>();
				for (AnalysisBrowserTableRow aData : data) {
					if (aData != null && aData.getIsVisible()) {
						this.data.add(aData);
					}
				}
			}
			fireTableDataChanged();
		}

		public int getColumnCount()
		{
			return columnNames.length;
		}

		public Class getColumnClass(int column)
		{
			return getValueAt(0, column).getClass();
		}

		public String getColumnName(int column)
		{
			return ResourceMgr.getString(AnalysisBrowserTableModel.class, columnNames[column]);
		}

		public void setValueAt(Object value, int nRow, int nCol)
		{
			if (nRow < 0 || nRow >= getRowCount() || value == null) {
				return;
			}
			AnalysisBrowserTableRow row = data.get(nRow);
			switch (nCol) {
				case 2:
					row.inputInterface = value;
					break;
			}
		}

		public void setCellComponent(JComponent component, int row, int col)
		{
			Vector rowVector = (Vector) dataVector.elementAt(row);
			rowVector.setElementAt(component, col);
		}

		public boolean isCellEditable(int nRow, int nCol)
		{
			return nCol == 2;
		}
	}

	String prepareTooltip(AnalysisBrowserTableRow dataRow)
	{
		String modelLocation = dataRow.modelLocation;

		if (modelLocation != null) {
			if (dataRow.modelLocation.startsWith(":dynamicModel")) {
				modelLocation = "Auto generated";
			}
		}
		else {
			modelLocation = "";
		}

		String designName = CAFUtils.getInstance().getActiveDesignContainer() != null ?
				CAFUtils.getInstance().getActiveDesignContainer().getName() : "Unknown";
		return "<HTML>Location: " + designName + "<br>Model: " +
				modelLocation + getOperationalStateTooltip(dataRow);
	}

	protected String getOperationalStateTooltip(AnalysisBrowserTableRow dataRow)
	{
		if (dataRow.isFailed()) {
			return "<br>State: " + dataRow.getFailureName() + " <HTML>";
		}
		else {
			return "<br>State: Normal Operation<HTML>";
		}
	}

	private class AnalysisTableCellRenderer extends DefaultTableCellRenderer
	{
		protected void setComponentColor(boolean isSelected, boolean isChild, int row)
		{
			if (isSelected) {
				setComponentColors(table.getSelectionForeground(), table.getSelectionBackground());
			}
			else if (isChild) {
				setComponentColors(table.getForeground(), Color.LIGHT_GRAY);
			}
			else {
				if(row%2==0){
					setComponentColors(table.getForeground(), UIManager.getColor("Table.background"));
				}
				else{
					setComponentColors(table.getForeground(), CHSColors.getTableAlternateRowColor());
				}
			}
		}

		private void setComponentColors(@Nullable Color foregroundColor, @Nullable Color backgroundColor)
		{
			setForeground(foregroundColor);
			setBackground(backgroundColor);
		}

	}

	private class IconTableCellRenderer extends AnalysisTableCellRenderer
	{

		public void setValue(Object value)
		{
			if (value != null) {
				if (value instanceof Icon) {
					Icon shicondata = (Icon) value;
					setIcon(shicondata);
					setText("");
				}
				else {
					super.setValue(value);
				}
			}
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column)
		{
			int convertedRow = AnalysisBrowserPanel.this.table.convertRowIndexToModel(row);
			AnalysisBrowserTableRow dataRow = AnalysisBrowserPanel.this.table.getModelRow(convertedRow);
			if (value != null) {
				AnalysisBrowserTableRow datarow = tablemodel.data.get(row);
				JComponent c = (JComponent) super
						.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				String modelLocation = datarow.modelLocation;
				if (modelLocation != null) {
					if (datarow.modelLocation.startsWith(":dynamicModel")) {
						modelLocation = "Auto generated";
					}
					c.setToolTipText(prepareTooltip(datarow));
				}

				if(dataRow != null && dataRow.isChild()){
					IconTableCellComponent comp = new IconTableCellComponent(c);
					setComponentColor(isSelected,true, row);
					return comp;
				}
				else{
					setComponentColor(isSelected,false, row);
					super.setHorizontalAlignment(SwingConstants.CENTER);
					return c;
				}
			}
			JLabel c = new IconTableCellRenderer();
			if (dataRow != null) {
				((IconTableCellRenderer) c).setComponentColor(isSelected, dataRow.isChild(), row);
			}
			return c;
		}
	}

	private static class IconTableCellComponent extends JPanel
	{
		// We must store the component we're using for edits such that the
		// cell editor can access it.
		private Component comp;

		private IconTableCellComponent(@NotNull Component comp)
		{
			this.comp = comp;

			setLayout(new GridBagLayout());
			GridBagConstraints inpPanelConstraint = new GridBagConstraints();
			inpPanelConstraint.fill = GridBagConstraints.BOTH;
			inpPanelConstraint.weightx = 1;
			inpPanelConstraint.weighty = 1;

			((JComponent) comp).setBorder(new EmptyBorder(0, 35, 0, 0));
			comp.setBackground(Color.LIGHT_GRAY);
			add(comp, inpPanelConstraint);
		}

		// This allows package private access to the component we're using for edits...
		private Component getEditableComponent()
		{
			return comp;
		}
	}

	private class ComponentNameCellRenderer extends AnalysisTableCellRenderer
	{
		public String removeBracesInName(@NotNull String name)
		{
			if (name.isEmpty()) {
				return name;
			}
			int startingBraceIndex = name.indexOf('{');
			return name.substring(startingBraceIndex + 1, name.length() - 1);
		}

		// implements javax.swing.table.TableCellRenderer
		@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
		public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected,
				boolean hasFocus,
				int row, int column)
		{
			AnalysisBrowserTableRow dataRow = table.getTableRow(row);//tablemodel.data.get(row);
			JComponent comp = (JComponent) super.getTableCellRendererComponent(jTable, value, isSelected, hasFocus, row,
					column);

			comp.setToolTipText(prepareTooltip(dataRow));
			if (dataRow.isChild()) {
				String name = ((JLabel) comp).getText();
				((JLabel) comp).setText(removeBracesInName(name));
			}
			if (dataRow.isChild()) {
				ComponentNameCellComponent c = new ComponentNameCellComponent(comp);
				setComponentColor(isSelected, dataRow.isChild(), row);
				return c;
			}
			else {
				setComponentColor(isSelected, dataRow.isChild(), row);
				return comp;
			}
		}
	}

	private static class ComponentNameCellComponent extends JPanel
	{

		// We must store the component we're using for edits such that the
		// cell editor can access it.
		private Component comp;

		public ComponentNameCellComponent(Component comp)
		{
			this.comp = comp;
			setLayout(new GridBagLayout());
			GridBagConstraints inpPanelConstraint = new GridBagConstraints();
			inpPanelConstraint.fill = GridBagConstraints.BOTH;
			inpPanelConstraint.weightx = 1;
			inpPanelConstraint.weighty = 1;
			((JComponent) comp).setBorder(new EmptyBorder(0, 20, 0, 0));

			add(comp, inpPanelConstraint);
		}
	}

	protected class InputPropertiesCellRenderer extends JComponent implements TableCellRenderer
	{
		private void setComponentColor(@NotNull Component comp, boolean isSelected, boolean isChild, int row)
		{
			Color background = table.getBackground();
			Color foreground = table.getForeground();
			if(!isSelected) {
				if (isChild) {
					background = Color.LIGHT_GRAY;
				}
				else{
					if(row%2==0){
						background = UIManager.getColor("Table.background");
					}
					else{
						background = CHSColors.getTableAlternateRowColor();
					}
				}
			} else {
				background = table.getSelectionBackground();
				foreground = table.getSelectionForeground();
			}
			comp.setForeground(foreground);
			comp.setBackground(background);
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column)
		{
			int convertedRow = AnalysisBrowserPanel.this.table.convertRowIndexToModel(row);
			AnalysisBrowserTableRow dataRow = AnalysisBrowserPanel.this.table.getModelRow(convertedRow);

			// We need to check whether there is an active analysis running before displaying the controls.
			// We don't need to display them if the user is not simulating this scope.
			if (simSession != null && currentScope != null && AnalysisServices.isAnalysisActive(currentScope)) {

				String compName = dataRow.name != null ? dataRow.name : dataRow.getNonDisplayParentName();
				if (dataRow.inputInterface != null) {
					if (dataRow.inputInterface instanceof Boolean) {
						CheckCellRenderer checkrenderer = new CheckCellRenderer();
						Component componentToRender = checkrenderer
								.getTableCellRendererComponent(table, dataRow.inputInterface, isSelected, hasFocus, row,
										column);
						componentToRender.setName(compName + '_' + dataRow.propertyName);
						InputPropertyComponent comp =
								new InputPropertyComponent(dataRow.propertyName, componentToRender);

						comp.setToolTipText(prepareTooltip(dataRow));
						setComponentColor(comp, isSelected, dataRow.isChild(), row);
						return comp;
					}

					if (dataRow.inputInterface instanceof String) {
						ComboBoxRenderer comborenderer = new ComboBoxRenderer();

						if (dataRow.possibleValues != null) {
							for (Object possibleValue : dataRow.possibleValues) {
								comborenderer.addItem(possibleValue);
							}
						}
						Component componentToRender = comborenderer.getTableCellRendererComponent(table,
								dataRow.inputInterface, isSelected, hasFocus, row, column);
						componentToRender.setName(compName + '_' + dataRow.propertyName);
						InputPropertyComponent comp =
								new InputPropertyComponent(dataRow.propertyName, componentToRender);
						comp.setToolTipText(prepareTooltip(dataRow));
						setComponentColor(comp, isSelected, dataRow.isChild(), row);
						return comp;
					}
				}
			}

			//When simulation is not running
			JComponent comp = (JComponent) (new DefaultTableCellRenderer())
					.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
			comp.setToolTipText(prepareTooltip(dataRow));
			setComponentColor(comp, isSelected, dataRow.isChild(), row);
			return comp;
		}
	}

	protected static class InputPropertyComponent extends JPanel
	{

		// We must store the component we're using for edits such that the
		// cell editor can access it.
		private Component comp;

		protected InputPropertyComponent(String name, Component comp)
		{
			this.comp = comp;

			setLayout(new GridBagLayout());
			GridBagConstraints inpPanelConstraint = new GridBagConstraints();
			inpPanelConstraint.fill = GridBagConstraints.BOTH;
			inpPanelConstraint.gridy = 0;
			inpPanelConstraint.weightx = 0.5;

			inpPanelConstraint.gridx = 0;
			JLabel text = new JLabel(name);
			text.setFont(UIManager.getFont("Table.font"));
			add(text, inpPanelConstraint);

			inpPanelConstraint.gridx = 1;
			add(comp, inpPanelConstraint);

		}

		public void setBackground(Color bg) {
			if(comp != null) {
				comp.setBackground(bg);
			}
			super.setBackground(bg);
		}

		// This allows package private access to the component we're using for edits...
		Component getEditableComponent()
		{
			return comp;
		}
	}

	protected static class InputPropertyAction implements ActionListener
	{
		private int lastIndex;
		private String valueName;
		private String propertyName;
		private String componentName;
		private IAnalysisSimulationSessionController session;
		private String uid;
		private Model model;

		InputPropertyAction(Model theModel, String thePropertyName, String theValueName,
				String theComponentName,
				IAnalysisSimulationSessionController theSession, String theUid)
		{
			propertyName = thePropertyName;
			valueName = theValueName;
			componentName = theComponentName;
			session = theSession;
			uid = theUid;
			model = theModel;
		}

		public void actionPerformed(ActionEvent ae)
		{
			String value;
			if (ae.getSource() instanceof JComboBox) {
				value = ((JComboBox) ae.getSource()).getSelectedItem().toString();

				if (lastIndex == ((JComboBox) ae.getSource()).getSelectedIndex()) {
					return;
				}
			}
			else {
				value = String.valueOf(((JCheckBox) ae.getSource()).isSelected());
				value = value.toUpperCase();
			}

			session.setProperty(uid, componentName, propertyName, value);

			if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
				((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).updateSimulation(
						AnalysisServices.getCurrentAnalysisNetlistScope());
			}
			else {
				((LogicAnalysisServices) LogicAnalysisServices.getAnalysisServices()).updateSimulation(model);
			}
			if (ae.getSource() instanceof JComboBox) {
				lastIndex = ((JComboBox) ae.getSource()).getSelectedIndex();
			}
		}
	}

	private static class InputPropertyCellEditor extends AbstractCellEditor implements TableCellEditor
	{

		private InputPropertyComponent comp;

		private InputPropertyCellEditor(InputPropertyComponent comp)
		{
			this.comp = comp;
		}

		public Object getCellEditorValue()
		{
			if (comp.getEditableComponent() instanceof JComboBox) {
				return ((JComboBox) comp.getEditableComponent())
						.getSelectedItem();  //To change body of implemented methods use File | Settings | File Templates.
			}
			else {
				return ((JCheckBox) comp.getEditableComponent()).isSelected();
			}
		}

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column)
		{
			if (comp.getEditableComponent() instanceof JComboBox) {
				((JComboBox) comp.getEditableComponent()).setSelectedItem(value);
				return comp;
			}
			else {
				((JCheckBox) comp.getEditableComponent()).setSelected((Boolean) value);
				return comp;
			}
		}
	}
}