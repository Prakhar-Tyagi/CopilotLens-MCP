/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2026 Siemens
 */
package chs.caplets.logic;

import chs.analysis.AnalysisServices;
import chs.analysis.CapitalAnalysisFactory;
import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.IStatusBar;
import chs.caf.IWindowMgr;
import chs.caf.action.dragdrop.DropOnDiagramActionInvocationTransferHandler;
import chs.caf.action.utility.DummyView;
import chs.caf.cafmain.actions.ToggleOptionDescriptionAction;
import chs.caf.caplet.IActionable;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IGridTogglable;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.CapletUtils;
import chs.caf.caplet.helpers.LogicUpdateStyledGraphicsHandler;
import chs.caf.caplet.helpers.snapping.SnapToObjectAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caplets.logic.actions.CrossLinkAction;
import chs.caplets.logic.actions.OptionFilterSettingsAction;
import chs.caplets.logic.actions.OptionFilterSettingsActionUI;
import chs.caplets.logic.actions.ShowStackUsageAction;
import chs.caplets.shared.actions.SelectAction;
import chs.caplets.shared.actions.ToggleSubGridAction;
import chs.cof.draw.IGridConfig;
import chs.cof.draw.IVisitor;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.drawplus.ISegmentContainer;
import chs.cof.logical.IDesign;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.DesignUtils;
import chs.common.IExtent;
import chs.common.IReleaseLevel;
import chs.common.IUIDObject;
import chs.common.IUnit;
import chs.ctf.ui.StatedButton;
import chs.services.ui.DrawingModePanel;
import chs.services.ui.FunctionAutoRouteStatusPanel;
import chs.services.ui.IStatusPanel;
import chs.services.ui.LogicAutoRouteStatusPanel;
import chs.utilities.AppInfo;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.TransferHandler;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class extends the shared view to override the postCustomRender method to allow analysis coloring updates to be
 * made on views that are NOT the current view.
 *
 * @author rharring
 */
public class View extends chs.caplets.shared.View implements IGridTogglable
{

	/**
	 * The design's uid
	 */
	private String designUID;
	protected Model m_model = null;

	private LogicStatusBarController m_localStatusbarController = null;
	private DrawingModePanel m_gridvisibilityPanel = null;
	private DrawingModePanel m_snapToGridPanel = null;
	private DrawingModePanel m_snapToObjectPanel = null;
	private static LogicUpdateStyledGraphicsHandler logicUpdateStyledGraphicsHandler =
			new LogicUpdateStyledGraphicsHandler();
	@Nullable private DrawingModePanel m_autoGenerateConnectorToggle = null;

	/**
	 * Creates a new instance of View
	 *
	 * @param model  the caplet model
	 * @param window the caplet window
	 */
	public View(ICapletModel model, ICapletWindow window)
	{
		super(model, window);
		m_model = (Model) model;
		designUID = ((ILogicModel) model).getDesign().getUID().toString();

		if (m_filterStatusPanel != null) {
			ICapletController controller = getController();
			final ICaplet caplet = controller.getCaplet();
			m_filterStatusPanel.setAction(caplet.getActionUI(OptionFilterSettingsActionUI.class.getName()));
		}
		// else CCapture has no m_filterStatusPanel

		//FEAT15786 - update the styled graphics when the model changes	; this listener should run only after the view is notified			

		ICapletModel capletModel = getController().getCapletModel();
		if (capletModel != null && !capletModel.hasModelChangeListener(logicUpdateStyledGraphicsHandler)) {
			capletModel.addModelChangeListener(logicUpdateStyledGraphicsHandler);
		}

		m_localStatusbarController = new LogicStatusBarController();

		m_snapToGridPanel = new DrawingModePanel(
				m_model.isDrawingGridSnap(), "SNAP", m_localStatusbarController,
				ResourceMgr.getString(View.class, "View.GridSnap.tooltip.on"),
				ResourceMgr.getString(View.class, "View.GridSnap.tooltip.off"),
				"chs/images/general/ico_snap_to_grid_on.png",
				"chs/images/general/ico_snap_to_grid_off.png",
				ToggleSubGridAction.class.getName());

		m_snapToObjectPanel = new DrawingModePanel(
				m_model.isDrawingObjectSnap(), "OSNAP", m_localStatusbarController,
				ResourceMgr.getString(View.class, "View.ObjectSnap.tooltip.on"),
				ResourceMgr.getString(View.class, "View.ObjectSnap.tooltip.off"),
				"chs/images/general/ico_snap_to_object_on.png",
				"chs/images/general/ico_snap_to_object_off.png",
				SnapToObjectAction.class.getName());

		IGridConfig config = getGridConfig();
		boolean configVisible = config == null || config.isVisible();
		m_gridvisibilityPanel =
				new DrawingModePanel(configVisible, "GRID", m_localStatusbarController,
						ResourceMgr.getString(View.class, "View.GridVisibility.tooltip.on"),
						ResourceMgr.getString(View.class, "View.GridVisibility.tooltip.off"),
						"chs/images/general/ico_grid_on.png",
						"chs/images/general/ico_grid_off.png");

		m_autoGenerateConnectorToggle = getAutoGenerateConnectorToggle();
	}

	@Nullable private DrawingModePanel getAutoGenerateConnectorToggle()
	{
		if (m_model.supportsAutoGenerateConnectorMode()) {
			String toggleOnTooltip = AppInfo.isVeSys() || AppInfo.isSEElectrical() ?
					ResourceMgr.getString(View.class, "View.VesysAndSEE.AutoGenerateConnector.tooltip.on") :
					ResourceMgr.getString(View.class, "View.AutoGenerateConnector.tooltip.on");
			String toggleOffTooltip = AppInfo.isVeSys() || AppInfo.isSEElectrical() ?
					ResourceMgr.getString(View.class, "View.VesysAndSEE.AutoGenerateConnector.tooltip.off") :
					ResourceMgr.getString(View.class, "View.AutoGenerateConnector.tooltip.off");
			return new DrawingModePanel(m_model.getAutoGenerateConnectorMode(),
					"AUTO_GENERATE_CONNECTOR", m_localStatusbarController, toggleOnTooltip, toggleOffTooltip,
					"chs/images/general/auto-create-connector-small.png",
					"chs/images/general/auto-create-connector-disabled-small.png");
		}
		return null;
	}

	@NotNull @Override protected List<IStatusPanel> getOrderedStatusPanels()
	{
		List<IStatusPanel> statusPanels = super.getOrderedStatusPanels();
//		statusPanels.add(m_snapToGridPanel);
//		statusPanels.add(m_snapToObjectPanel);
//		statusPanels.add(m_gridvisibilityPanel);

		return statusPanels;
	}

	@Override public boolean supportsMultiOptionHighlighter()
	{
		return !(m_model.getDesign() instanceof IFunctionLogicDesign) && !AppInfo.isSvcDoc();
	}

	@NotNull protected Collection<AbstractLocationDisplayControl> getGraphicalDimensionDisplayControl(
			double dPosX, double dPosY, int gridSpacing, IUnit phyUnit
	)
	{
		if (m_model.getDesign() instanceof ILayoutLogicDesign) {
			return Collections.singleton(new TooltipLocationDisplayControl(phyUnit, gridSpacing, dPosX, dPosY));
		}
		else {
			return super.getGraphicalDimensionDisplayControl(dPosX, dPosY, gridSpacing, phyUnit);
		}
	}

	public synchronized void invalidate(IViewInvalidationEnum type)
	{
		super.invalidate(type);
		if (type == IViewInvalidationEnum.eFull) {
			ISchemDiagram diagram = CommonUtils.cast(getDiagram(), ISchemDiagram.class);
			if (diagram != null) {
				diagram.updateLayerAttributes();
			}
		}
	}

	protected void filterSelectionBasedUponAreaCoverage(IExtent selectRect, SelectSet selSet)
	{
		for (IDiagramObject diagramObject : selSet.getSelectedObjects(IDiagramObject.class)) {
			if (!diagramObject.allowSelectionOnAreaSelect(selectRect)) {
				selSet.remove(diagramObject.getUID());
				if (diagramObject instanceof IRepresentedObject) {
					final IUIDObject rawConnectivity = ((IRepresentedObject) diagramObject).getRawConnectivity();
					if (rawConnectivity != null) {
						selSet.remove(rawConnectivity.getUID());
					}
				}
			}
		}

		List<ISegmentContainer> candidates = new ArrayList<>();
		for (Selection sel : selSet.getSelected()) {
			ISegmentContainer obj = DesignUtils.getLoadedObject(sel.getUID(), ISegmentContainer.class);
			if (obj != null) {
				candidates.add(obj);
			}
		}
		candidates.forEach(obj -> CapletUtils.selectAssociatedSegments(true, selSet, obj, Collections.emptySet()));
	}

	protected void createViewActions()
	{
		CrossLinkAction crossLinkAction = new CrossLinkAction(this);
		addAction(crossLinkAction);
		ICapletController controller = getController();

		ICapletModel capletModel = getCapletModel();

		addAction(new OptionFilterSettingsAction(this));

		IAction baseAction = controller.getActionMgr().getBaseAction();
		if (baseAction instanceof SelectAction) {
			((SelectAction) baseAction).getSelectClient()
					.setKeyModifierActionProvider(new LogicModifierKeyActionProvider(crossLinkAction));
		}

		IActionable actionableDiagramBrowser = controller.getActionableBrowser("Diagram");
		if (actionableDiagramBrowser != null &&
				actionableDiagramBrowser.getAction(CrossLinkAction.class.getName()) == null) {
			// Add "View Related Items" as a context menu on the browser tree
			// we have to use a DummyView because Logic has only one Design Browser with multiple views (dts0100515947)
			actionableDiagramBrowser.addAction(new CrossLinkAction(new DummyView(controller)));
		}
		addAction(new ToggleOptionDescriptionAction(this, capletModel));
		addAction(new ShowStackUsageAction(this));

		super.createViewActions();
	}

	/**
	 * This method should return true if the dynamic graphics for the view should be redrawn. Usually, this will be done
	 * only for the current active window. However, when Capital Analysis is installed, Dynamic graphics (representing
	 * coloring information) may need to be updated for several Logic windows.
	 *
	 * @return true - for the current active view, false otherwise.
	 */
	@Override
	protected boolean isRedrawOfDynamicGraphicsRequired()
	{
		if (super.isRedrawOfDynamicGraphicsRequired()) {
			return true;
		}
		if (Environment.isImmersedMode()) {
			return getCapletModel() instanceof ILogicModel model && model.getDiagram() == getDiagram();
		}
		return false;
	}

	@Override protected boolean isRedrawOfSelectionGraphicsRequired()
	{
		if (super.isRedrawOfSelectionGraphicsRequired()) {
			return true;
		}
		if (Environment.isImmersedMode()) {
			return getCapletModel() instanceof ILogicModel model && model.getDiagram() == getDiagram();
		}
		return false;
	}

	/**
	 * This is called for post rendering on a print job, pass on the call to the normal postCustomRender
	 *
	 * @param v -
	 */
	public void printPostCustomRender(IVisitor v)
	{
		if (CapitalAnalysisFactory.getAnalysisInterface() != null) {
			boolean active = AnalysisServices.isAnalysisActive(designUID);
			getDynamicGfxService().draw(v, active, true, this);
		}
	}

	/**
	 * Determine if the release level allows printing.
	 *
	 * @return True if so, false otherwise.
	 */
	public boolean allowsPrinting()
	{
		ICapletModel capModel = getCapletModel();
		if (capModel instanceof Model) {
			Model logModel = (Model) capModel;
			IDesign des = logModel.getDesign();
			if (des == null) {
				return false;
			}
			IReleaseLevel releaseLevel = des.getReleaseLevel();
			if (releaseLevel == null) {
				return false;
			}
			return releaseLevel.isPrintingAllowed();
		}
		return false;
	}

	@Override public void setStatusPanels(IStatusBar statusBar)
	{
		super.setStatusPanels(statusBar);
		m_statusBar.addPanel(createAutoRouteStatusPanel());
		m_statusBar.addPanel(m_snapToGridPanel);
		m_statusBar.addPanel(m_snapToObjectPanel);
		m_statusBar.addPanel(m_gridvisibilityPanel);
		resetAutoGenerateConnectorToggle();
		if (m_autoGenerateConnectorToggle != null) {
			m_statusBar.addPanel(m_autoGenerateConnectorToggle);
		}
	}

	private void resetAutoGenerateConnectorToggle()
	{
		m_model.resetAutoGenerateConnectorState();
		m_autoGenerateConnectorToggle = getAutoGenerateConnectorToggle();
	}

	private StatedButton createAutoRouteStatusPanel()
	{
		if (m_model.getDesign() instanceof IFunctionLogicDesign) {
			return new FunctionAutoRouteStatusPanel();
		}
		return new LogicAutoRouteStatusPanel();
	}

	public void setSnapToGrid(boolean isSnapToGrid)
	{
		m_snapToGridPanel.updateDrawingMode(isSnapToGrid);
	}

	public void setSnapToObjectMode(boolean mode)
	{
		m_snapToObjectPanel.updateDrawingMode(mode);
	}

	public void setGridVisible(boolean isVisible)
	{
		m_gridvisibilityPanel.updateDrawingMode(isVisible);
	}

	public void setAutoGenerateConnectorMode(boolean mode)
	{
		m_model.setAutoGenerateConnectorToggleState(mode);
		if (m_autoGenerateConnectorToggle != null) {
			m_autoGenerateConnectorToggle.updateDrawingMode(mode);
		}
	}

	@Nullable protected TransferHandler constructTransferTreeHandler()
	{
		return new DropOnDiagramActionInvocationTransferHandler()
		{
			@Override protected Point transformDropLocation(@NotNull Point mousePt)
			{
				return deviceToWorld(mousePt);
			}
		};
	}

	private class LogicStatusBarController implements DrawingModePanel.IStatusBarObjectsController
	{

		public void actionPerformed(DrawingModePanel ui)
		{
			boolean isOn = ui.getCurrentState();
			if (ui == m_snapToGridPanel) {
				m_model.setDrawingGridSnap(isOn);
			}
			else if (ui == m_snapToObjectPanel) {
				m_model.setDrawingObjectSnap(isOn);
			}
			else if (ui == m_gridvisibilityPanel) {
				IGridConfig gridConf = getGridConfig();
				gridConf.setVisible(isOn);
				invalidate(IViewInvalidationEnum.eFull);
			}
			else if (m_autoGenerateConnectorToggle != null && ui == m_autoGenerateConnectorToggle) {
				toggleAutoGenerateConnectorModeAcrossAllViews(isOn);
			}
		}

		private void toggleAutoGenerateConnectorModeAcrossAllViews(boolean isOn)
		{
			setAutoGenerateConnectorMode(isOn);
			IWindowMgr windowMgr = CAFUtils.getInstance().getWindowMgr();
			if (windowMgr != null) {
				for (ICAFWindow window : windowMgr.getWindows()) {
					List<ICapletView> views = window instanceof ICapletWindow ?
							((ICapletWindow) window).getViewsList() : Collections.emptyList();
					for (ICapletView view : views) {
						if (view instanceof View) {
							View logicView = (View) view;
							logicView.setAutoGenerateConnectorMode(isOn);
						}
					}
				}
			}
		}
	}
}
