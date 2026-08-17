/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */
package chs.caplets.logic;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.IResource;
import chs.caf.cafmain.actions.EditLeaderLineJustificationAction;
import chs.caf.cafmain.actions.LogicGenerateNamesAction;
import chs.caf.cafmain.actions.ela.ELADataManagerAction;
import chs.caf.caplet.IBrowserClient;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.IEditClient;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IDeferredAction;
import chs.caf.caplet.action.IDeferredActionProcessor;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.DimensionFlipAction;
import chs.caf.caplet.helpers.DoubleEndedToolbar;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.caf.caplet.helpers.PropertiesAction;
import chs.caf.caplet.helpers.PurgeFunctionalModuleCodeAction;
import chs.caf.caplet.helpers.RegenerateGraphicsAction;
import chs.caf.caplet.helpers.SmartEditPropertiesAction;
import chs.caf.caplet.helpers.SymbolInvokeFromDesignToolsAction;
import chs.caf.caplet.helpers.browser.LayoutBrowserTree;
import chs.caf.caplet.helpers.browser.LogicBrowserTree;
import chs.caf.caplet.helpers.graphics.AddCommentSymbolAction;
import chs.caf.caplet.helpers.graphics.SymbolPlaceAsGraphicsAction;
import chs.caf.caplet.helpers.tabulareditor.IFilterableObjectType;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.helpers.ui.common.CAFToolBar;
import chs.caf.helpers.ui.common.ResourceHolder;
import chs.caplets.layout.properties.LayoutPropertiesClient;
import chs.caplets.layout.properties.QAPLayoutPropertiesClient;
import chs.caplets.logic.actions.AddConductorNameAction;
import chs.caplets.logic.actions.AddPinListAction;
import chs.caplets.logic.actions.BatchDevicePlacementAction;
import chs.caplets.logic.actions.CreateDuctWithoutPartAndSymbolAction;
import chs.caplets.logic.actions.CreateLayoutComponentInstanceAction;
import chs.caplets.logic.actions.CreateModularSchematicsAction;
import chs.caplets.logic.actions.CreateMountWithoutPartAndSymbolAction;
import chs.caplets.logic.actions.CreateOtherComponentOnlyWithPartAction;
import chs.caplets.logic.actions.CreateOtherComponentOnlyWithSymbolAction;
import chs.caplets.logic.actions.CreateOtherComponentWithPartAndSymbolAction;
import chs.caplets.logic.actions.CreateOtherComponentWithoutPartAndSymbolAction;
import chs.caplets.logic.actions.DeleteLayoutBOMIDsAction;
import chs.caplets.logic.actions.PlaceAssemblyTreeAction;
import chs.caplets.logic.actions.RegenerateLayoutBOMIDsAction;
import chs.caplets.logic.actions.RemoveToDoItemAction;
import chs.caplets.logic.actions.ResetAssemblyAction;
import chs.caplets.logic.actions.SetPinNonReferenceAction;
import chs.caplets.logic.actions.SetPinReferenceAction;
import chs.caplets.logic.actions.SmartEditAction;
import chs.caplets.logic.actions.TerminateAtSpliceAction;
import chs.caplets.logic.actions.UpdateLayoutBOMIDsAction;
import chs.caplets.logic.actions.bridges.BridgeOutFilterAction;
import chs.caplets.logic.actions.layout.LayoutAssociateDesignsAction;
import chs.caplets.logic.actions.layout.LayoutDesignResyncAction;
import chs.caplets.logic.actions.layout.LayoutXDimensionAction;
import chs.caplets.logic.actions.layout.LayoutXYDimensionAction;
import chs.caplets.logic.actions.layout.LayoutYDimensionAction;
import chs.caplets.logic.actions.serviceDocumentation.smartflows.ChangeFlowDirectionAction;
import chs.caplets.logic.actions.shared.AddGeneralHighwayAction;
import chs.caplets.logic.actions.shared.AddSingleLineAction;
import chs.caplets.shared.BaseController;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.SymbolSubTypeEnum;
import chs.ctf.caf.ui.TextAttributesEditor;
import chs.ctf.editui.LogicEditSelectionHelper;
import chs.system.FactoryMgr;
import chs.utility.SymbolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Provides a controller specific to {@link LayoutCaplet}
 */
public class LayoutController extends BaseController
{

	@Nullable private LayoutDesignPanel mLayoutDesignPanel;

	public LayoutController(ICaplet caplet, ILogicDesign design, ISchemDiagram diagram)
	{
		super(caplet, design, diagram); // true means is Logic
		createLayoutControllerActions();
		FactoryMgr.getSystemFactory().getCHSSystem().getCHSUtils().setObjectBrowser(new LogicObjectGraphBrowser());
		IDeferredActionProcessor deferredActionProcessor = getDeferredActionProcessor();
		deferredActionProcessor.addDeferredAction(RegenerateGraphicsAction.getInstance());
		deferredActionProcessor.addDeferredAction((IDeferredAction) ConductorRouteAction.getInstance());
//		deferredActionProcessor.addDeferredAction(new LogicTableDataChangeAction(design, getCapletModel()));
	}

	@NotNull protected IFilterableObjectType.ObjectClass getTabularEditorObjectClass()
	{
		return IFilterableObjectType.ObjectClass.Layout;
	}

	protected boolean isICDBrowserSupported()
	{
		return false;
	}

	protected boolean areSharedObjectsSupported()
	{
		return false;
	}

	protected BrowserClient getDesignConnectivityBrowserClient()
	{
		BrowserClient browserClient = new LayoutBrowserClient(this);
		browserClient.setTreeNodeDimmer(getTreeNodeDimmer());
		return browserClient;
	}

	protected LogicBrowserTree constructDesignBrowserTree(IBrowserClient client, String name)
	{
		return new LayoutBrowserTree(client, name);
	}

	protected void createSharedControllerActions()
	{
		super.createSharedControllerActions();
		addAction(new BatchDevicePlacementAction(this));
	}

	protected void createLayoutControllerActions()
	{
		addAction(new PlaceAssemblyTreeAction(this));
		addAction(new ResetAssemblyAction(this));
		addAction(new TerminateAtSpliceAction(this));

		if (m_toDoView != null) {
			addAction(new RemoveToDoItemAction(this, m_toDoView));
		}

		addAction(new SymbolPlaceAsGraphicsAction(this));

		addAction(new SymbolInvokeFromDesignToolsAction(this));

		addAction(new SetPinReferenceAction(this)); // TODO - Include in CCapture?
		addAction(new SetPinNonReferenceAction(this)); // TODO - Include in CCapture?

		addAction(new LogicGenerateNamesAction(this));
		addAction(new AddConductorNameAction(this));

		addAction(new BridgeOutFilterAction(this));

		addActionToLibBrowser(SymbolPlaceAsGraphicsAction.class);
		addActionToLibBrowser(SymbolInvokeFromDesignToolsAction.class);
		addActionToLibBrowser(CreateOtherComponentOnlyWithSymbolAction.class);

		addAction(new ELADataManagerAction(this));

		// put Properties action last so it appears last on the context menu
		createZOrderActions();
		createNonElectricalGraphicsActions();
		addAction(new SmartEditAction(this));
		addAction(new PropertiesAction(this, createPropertiesClient(), new TextAttributesEditor()));
		addAction(new SmartEditPropertiesAction(this, createPropertiesClient(), new TextAttributesEditor()));

		//Add print region specific actions
		createPrintRegionActions();
		addAction(new EditLeaderLineJustificationAction(this));

		addAction(new AddPinListAction(this));
		addAction(new AddGeneralHighwayAction(this));
		addAction(new AddSingleLineAction(this));

		addAction(new PurgeFunctionalModuleCodeAction(this));

		addAction(new ChangeFlowDirectionAction(this));
		addAction(new CreateModularSchematicsAction(this));
		addAction(new LayoutXDimensionAction(this));
		addAction(new LayoutYDimensionAction(this));
		addAction(new LayoutXYDimensionAction(this));
		addAction(new DimensionFlipAction(this));
		addAction(new LayoutAssociateDesignsAction(this));
		addAction(new LayoutDesignResyncAction(this));
		addAction(new CreateOtherComponentWithPartAndSymbolAction(this));
		addAction(new CreateMountWithoutPartAndSymbolAction(this));
		addAction(new CreateDuctWithoutPartAndSymbolAction(this));
		addAction(new CreateOtherComponentWithoutPartAndSymbolAction(this));
		addAction(new CreateOtherComponentOnlyWithPartAction(this));
		addAction(new CreateOtherComponentOnlyWithSymbolAction(this));
		addAction(new CreateLayoutComponentInstanceAction(this));
		addAction(new UpdateLayoutBOMIDsAction(this));
		addAction(new RegenerateLayoutBOMIDsAction(this));
		addAction(new DeleteLayoutBOMIDsAction(this));
	}

	public String getDoubleClickAction()
	{
		LogicEditSelectionHelper hesHelper =
				new LogicEditSelectionHelper(getSelectMgr().getPreSelections());
		return hesHelper.getDoubleClickAction();
	}

	@Nullable public IEditClient getEditClient(SelectSet selections, @Nullable Object owner)
	{
		LogicEditSelectionHelper hesHelper = new LogicEditSelectionHelper(selections);
		return hesHelper.getEditClient(this);
	}

	@NotNull @Override protected JPanel buildDesignPanel()
	{
		final JPanel treePanel = super.buildDesignPanel();
		mLayoutDesignPanel = new LayoutDesignPanel(treePanel);
		return mLayoutDesignPanel;
	}

	@Override public void destroy()
	{
		if (mLayoutDesignPanel != null) {
			mLayoutDesignPanel.destroy();
			mLayoutDesignPanel = null;
		}
		super.destroy();
	}

	public void createDesignBrowserToolbar()
	{
		if (mLayoutDesignPanel != null) {
			mLayoutDesignPanel.initToolbar();
		}
	}

	@Override @NotNull public IPropertiesClient createPropertiesClient()
	{
		return new LayoutPropertiesClient(getCapletModel());
	}

	/**
	 * Creates and returns a "Layout" properties client object specifically tailored for the Quick Access Panel.
	 *
	 * @param willLockSharedObject Flag indicating whether to lock shared objects: {@code true} to lock,
	 * {@code false} to leave unlocked
	 * @return IPropertiesClient A properties client object tailored for the Quick Access Panel
	 */
	@Override
	@NotNull
	public IPropertiesClient createPropertiesClientForQep(boolean willLockSharedObject)
	{
		return new QAPLayoutPropertiesClient(getCapletModel(), willLockSharedObject);
	}

	private class LayoutDesignPanel extends JPanel
	{

		@Nullable private DoubleEndedToolbar toolbar;
		private boolean toolbarCreated;

		private LayoutDesignPanel(@NotNull Component treePanel)
		{
			setLayout(new GridBagLayout());

			GridBagConstraints gridBagConstraints = new GridBagConstraints(0,
					0, 4, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
					new Insets(0, 5, 0, 5), 0, 0);

			toolbar = new DoubleEndedToolbar(0, 0, 0, 0, false);
			add(toolbar, gridBagConstraints);

			gridBagConstraints = new GridBagConstraints(0, 1, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER,
					GridBagConstraints.BOTH, new Insets(0, 5, 5, 5), 0, 0
			);
			add(treePanel, gridBagConstraints);
			toolbarCreated = false;
		}

		public void initToolbar()
		{
			if (toolbarCreated) {
				return;
			}
			IResource resource = getCaplet().getResource();
			if (resource instanceof ILayoutDesignToolbarProvider && toolbar != null) {
				ActionContainer leftActions = ((ILayoutDesignToolbarProvider) resource).getLayoutDesignLeftToolbar();
				final JPanel leftToolBar = createSubToolBarPanel(leftActions, FlowLayout.LEFT);
				toolbar.addToLeftToolbar(leftToolBar);

				ActionContainer rightActions = ((ILayoutDesignToolbarProvider) resource).getLayoutDesignRightToolbar();
				final JPanel rightToolBar = createSubToolBarPanel(rightActions, FlowLayout.RIGHT);
				toolbar.addToRightToolbar(rightToolBar);
			}
			toolbarCreated = true;
		}

		@NotNull private JPanel createSubToolBarPanel(ActionContainer leftActions, int flow)
		{
			JPanel toolbarPanel = new JPanel();
			toolbarPanel.setLayout(new FlowLayout(flow, 0, 0));
			final CAFToolBar leftToolBar =
					ResourceHolder
							.createToolBar((String) leftActions.getValue(Action.NAME), leftActions.getMembers(), null);
			leftToolBar.setBorder(null);
			toolbarPanel.add(leftToolBar);
			toolbarPanel.repaint();
			return toolbarPanel;
		}

		private void destroy()
		{
			if (toolbar != null) {
				toolbar.clearLeftToolbar();
				toolbar.clearRightToolbar();
				toolbar.removeAll();
				toolbar = null;
			}
			removeAll();
		}
	}

	@Nullable protected IAction determineActiveSymbolAction()
	{
		IStamp sub = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr().getActiveSymbol();
		if (sub instanceof ISymbolDef) {
			ISymbolDef subsd = (ISymbolDef) sub;
			if (SymbolUtils.isCommentSymbol(subsd)) {
				final SymbolSubTypeEnum symbolSubType = subsd.getSymbolSubType();
				if (SymbolSubTypeEnum.GENERIC.equals(symbolSubType)) {
					return getAction(AddCommentSymbolAction.class);
				}
				else {
					return getAction(CreateOtherComponentOnlyWithSymbolAction.class);
				}
			}
		}
		return null;
	}
}