/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.AbstractContextAction;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.IApplicationSpecificationAction;
import chs.caf.IBasicDrawingActivityHandler;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.creation.SnapPoint;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.layout.DeviceLayoutHelper;
import chs.caplets.logic.actions.layout.DevicePlacementController;
import chs.caplets.logic.actions.layout.DevicePlacementDataModel;
import chs.caplets.logic.actions.layout.DevicePlacementInfo;
import chs.caplets.logic.actions.layout.DeviceSymbolController;
import chs.caplets.logic.actions.layout.IComponentPhysicalDetails;
import chs.caplets.logic.actions.layout.IDevicePlacementController;
import chs.caplets.logic.actions.layout.IDevicePlacementDataModel;
import chs.caplets.logic.actions.layout.IDevicePlacementInfo;
import chs.caplets.logic.actions.layout.PlacementAxisRotation;
import chs.cof.draw.IColor;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IGfxView;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISourceObjectRef;
import chs.cof.logical.ISourceObjectRefIterator;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.symbol.ISymbolDef;
import chs.common.ICommonFactory;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IUID;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ListSet;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.GfxUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chandras on 10-10-2019.
 */
public class BatchDevicePlacementAction extends ControllerActionRT implements IBasicDrawingActivityHandler
{

	private Point m_currentMouse_point = new Point();
	private Map<Integer, KeyEventHandler> m_keyHandlers = new HashMap<>(8);
	private IDevicePlacementController m_placementController;
	private IDevicePlacementController m_deviceController;
	private IDevicePlacementController m_symbolController;
	private boolean m_symbolSelection = false;
	private boolean m_areAdvancedFeaturesEnabled = false;

	public BatchDevicePlacementAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override public boolean isEnabled()
	{
		return super.isEnabled() && isModelEditable() && !getOperands().isEmpty();
	}

	@Override protected boolean shouldEnableGfxDimmerable()
	{
		return true;
	}

	@NotNull private List<IDevice> getOperands()
	{
		return getOperands(getController().getSelectMgr().getCurrentSelections());
	}

	@NotNull private List<IDevice> getOperands(SelectSet currentSelections)
	{
		//disbale action on read-only design.
		if (!getController().getCapletModel().isEditable()) {
			return Collections.emptyList();
		}
		ListSet<IDevice> selectedDevices = new ListSet<>();
		for (Object uidObject : currentSelections.getUIDObjects()) {
			final IDevice device = CommonUtils.cast(ReferenceHelper.reduceToLogicObject(uidObject), IDevice.class);
			if (device != null) {
				selectedDevices.add(device);
			}
		}
		return selectedDevices;
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		final List<IDevice> operands = getOperands();
		if (operands.isEmpty()) {
			return IActionEnum.eCanceled;
		}
		setupKeyHandlers();
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		IGfxView gview = (IGfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gview.getDiagram();
		final IDevicePlacementDataModel devicePlacementDataModel = new DevicePlacementDataModel(diagram);
		final IDynamicGfxService dynamicGfxService = getModel().getDynamicGfxService();
		m_symbolController = new DeviceSymbolController(dynamicGfxService, devicePlacementDataModel);
		m_deviceController = new DevicePlacementController(diagram, dynamicGfxService, devicePlacementDataModel);
		m_placementController = m_deviceController;
		m_symbolSelection = false;
		devicePlacementDataModel.setupController(m_placementController);
		//do this construction in the last because this might ask for info from above controls.
		DeviceLayoutHelper layoutHelper = new DeviceLayoutHelper(diagram.getProject());
		List<IDevicePlacementInfo> placementInfos = new ArrayList<>(operands.size());
		for (IDevice operand : operands) {
			final Collection<ISymbolDef> deviceSymbols = layoutHelper.getDeviceSymbols(operand);
			if (deviceSymbols.isEmpty()) {
				final IComponentPhysicalDetails componentPhysicalDetails =
						DeviceLayoutHelper.getComponentPhysicalDetails(operand);
				final IGrid grid = diagram.getGrid();
				//this is a placeholder. the placeholder should be such
				//that it will fit the actual one without overflowing.
				final int length = componentPhysicalDetails.getLength(diagram);
				final int width = componentPhysicalDetails.getWidth(diagram);
				final int gridSpacing = grid.getGridSpacing();
				//cosidering border of a grid on top and bottom.
				if (length < gridSpacing || width < 2 * gridSpacing || length % gridSpacing != 0 ||
						width % gridSpacing != 0) {
					final ILogicDesign design = diagram.getDesign();
					assert design != null;
					String gridDetails = diagram.getGrid().getRealMapping().getValue() + " " +
							diagram.getGrid().getRealMapping().getType().toDisplayString();
					LogHelper.appMsgSafe(HTMLHelper.color(IColor.RED, ResourceMgr.getString(
							BatchDevicePlacementAction.class, "BatchDevicePlacementAction.place.fail.msg",
							getDeviceLink(operand, design), gridDetails, gridDetails)));
				}
				else {
					placementInfos.add(new DevicePlacementInfo(operand, length, width, devicePlacementDataModel));
				}
			}
			else {
				placementInfos.add(new DevicePlacementInfo(operand, deviceSymbols, devicePlacementDataModel));
			}
		}
		if (placementInfos.isEmpty()) {
			return IActionEnum.eCanceled;
		}
		devicePlacementDataModel.setupOerands(placementInfos);
		return IActionEnum.eActivated;
	}

	@NotNull private String getDeviceLink(IDevice operand, ILogicDesign design)
	{
		if (design instanceof ILayoutLogicDesign) {
			final ILayoutDesignMgr layoutDesignMgr = ((ILayoutLogicDesign) design).getLayoutDesignMgr();
			final ISourceObjectRefIterator sourceObjectRefs = layoutDesignMgr.getSourceObjectRefs(operand.getUID());
			List<IUID> sourceRefs = new ArrayList<>();
			List<IUID> sourceDesignRefs = new ArrayList<>();
			for (ISourceObjectRef sourceObjectRef : sourceObjectRefs) {
				final IUID sourceObjectUID = sourceObjectRef.getSourceObjectUID();
				if (sourceObjectUID != null) {
					sourceRefs.add(sourceObjectUID.getPersistentUID());
				}
				final IUID sourceDesignUID = sourceObjectRef.getSourceDesignUID();
				if (sourceDesignUID != null) {
					sourceDesignRefs.add(sourceDesignUID);
				}
			}
			if (!sourceRefs.isEmpty() && sourceDesignRefs.size() == 1) {
				Collections.sort(sourceRefs);
				return HTMLHelper.link(sourceDesignRefs.get(0), sourceRefs.get(0), operand.getName());
			}
		}
		return operand.getName();
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		try {
			m_placementController.terminate(successful);
		}
		finally {
			getController().getSelectMgr().removeSelectSet();
			final IDynamicGfxService dynamicGfxService = getModel().getDynamicGfxService();
			dynamicGfxService.removeAllDynamicGfx();
			dynamicGfxService.removeAllTransientGfx();
			updateTransientView(true);
			reset();
		}
		return successful;
	}

	@SuppressWarnings("ConstantConditions")
	private void reset()
	{
		m_placementController = null;
		m_symbolController = null;
		m_deviceController = null;
		m_keyHandlers.clear();
		m_symbolSelection = false;
		m_areAdvancedFeaturesEnabled = false;
	}

	private void invalidateTransientView()
	{
		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.refreshPhysicalStateInformation();
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	private void updateTransientView(boolean forceUpdateTooltip)
	{
		updateTooltip(forceUpdateTooltip);
		invalidateTransientView();
	}

	private void updateTooltip(boolean forceUpdateTooltip)
	{
		GfxView view = CommonUtils.cast(CAFUtils.getInstance().getActiveCapletView(), GfxView.class);
		if (view == null) {
			return;
		}
		String tooltipText = m_placementController.getTooltipText(m_areAdvancedFeaturesEnabled);
		if (StringUtils.isBlank(tooltipText)) {
			view.clearPopupTooltip();
			return;
		}

		if (!forceUpdateTooltip) {
			//lot of flicker on tooltip. regenerate only when needed.
			return;
		}

		view.clearPopupTooltip();
		IExtent transGfxBoundary = m_placementController.computeTransientGraphicsBoundary();
		ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		IExtent viewExtentWorld = view.getGfxContext().getViewExtentWorld();
		IExtent transGfxExt = commonFactory.createExtent();
		viewExtentWorld.intersect(transGfxBoundary, transGfxExt);
		if (!transGfxExt.isValid()) {
			final ILocation loc = commonFactory.constructLocation(m_currentMouse_point.x, m_currentMouse_point.y);
			transGfxExt.addUnionLocation(loc);
		}
		if (transGfxExt.isValid()) {
			final PlacementAxisRotation placementRotation = m_placementController.getPlacementRotation();
			Point tooltipPoint = new Point(transGfxExt.getRight(), transGfxExt.getBottom());
			switch (placementRotation) {
				case Zero:
					tooltipPoint = new Point(transGfxExt.getLeft(), transGfxExt.getBottom());
					break;
				case Ninety:
					tooltipPoint = new Point(transGfxExt.getRight(), transGfxExt.getTop());
					break;
				case OneEighty:
					break;
				case TwoSeventy:
					break;
				default:
					break;
			}
			Point devicePoint = view.worldToDevice(tooltipPoint);
			int tooltipShift = GfxUtils.TOOLTIP_SHIFT;
			int x = devicePoint.x + tooltipShift;
			int y = devicePoint.y + -tooltipShift;
			view.showTooltipAtLocation(tooltipText, new Point(x, y));
		}
		else {
			assert false;
		}
	}

	private void backup()
	{
		m_placementController.undo();
		updateTransientView(true);
	}

	@Override public IDynamicGfxService getDynamicGfxService()
	{
		return getModel().getDynamicGfxService();
	}

	private abstract static class KeyEventHandler
	{

		private KeyEventHandler()
		{
		}

		protected abstract void doProcess();

		public final void process(@SuppressWarnings("unused") int keyCode)
		{
			doProcess();
		}
	}

	private void setupKeyHandlers()
	{
		m_keyHandlers.put(KeyEvent.VK_BACK_SPACE, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				backup();
			}
		});

		m_keyHandlers.put(KeyEvent.VK_ENTER, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				terminateAction();
			}
		});

		m_keyHandlers.put(KeyEvent.VK_O, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				m_placementController.toggleOrderOfPlacement();
				updateTransientView(true);
			}
		});

		m_keyHandlers.put(KeyEvent.VK_F, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				m_placementController.toggleFlip();
				updateTransientView(true);
			}
		});

		m_keyHandlers.put(KeyEvent.VK_R, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				m_placementController.setupNextAxis();
				updateTransientView(true);
			}
		});

		m_keyHandlers.put(KeyEvent.VK_T, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				m_placementController.setupPrevAxis();
				updateTransientView(true);
			}
		});

		m_keyHandlers.put(KeyEvent.VK_P, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				if (m_symbolSelection) {
					switchToPlacementModeFromSymbolSelectionMode();
				}
				else {
					m_placementController.setupNextPlacementMode();
					updateTransientView(true);
				}
			}
		});

		m_keyHandlers.put(KeyEvent.VK_C, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				if (!m_symbolSelection) {
					m_placementController.setupAbutPlacementMode();
					updateTransientView(true);
				}
			}
		});

		m_keyHandlers.put(KeyEvent.VK_S, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				if (!m_symbolSelection) {
					switchToSymbolSelectionModeFromPlacementMode();
				}
			}
		});

		m_keyHandlers.put(KeyEvent.VK_1, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				if (m_symbolSelection) {
					m_placementController.handleNextSymbolSelection();
				}
				else {
					m_placementController.incrementGroupCustomAdditionalGap();
				}
				updateTransientView(true);
			}
		});

		m_keyHandlers.put(KeyEvent.VK_2, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				if (m_symbolSelection) {
					m_placementController.handlePreviousSymbolSelection();
				}
				else {
					m_placementController.decrementGroupCustomAdditionalGap();
				}
				updateTransientView(true);
			}
		});

		m_keyHandlers.put(KeyEvent.VK_NUMPAD1, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				if (m_symbolSelection) {
					m_placementController.handleNextSymbolSelection();
				}
				else {
					m_placementController.incrementGroupCustomAdditionalGap();
				}
				updateTransientView(true);
			}
		});

		m_keyHandlers.put(KeyEvent.VK_NUMPAD2, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				if (m_symbolSelection) {
					m_placementController.handlePreviousSymbolSelection();
				}
				else {
					m_placementController.decrementGroupCustomAdditionalGap();
				}
				updateTransientView(true);
			}
		});

		m_keyHandlers.put(KeyEvent.VK_H, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				m_placementController.handleHorizontalJustification();
				updateTransientView(true);
			}
		});

		m_keyHandlers.put(KeyEvent.VK_V, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				m_placementController.handleVerticalJustification();
				updateTransientView(true);
			}
		});

		m_keyHandlers.put(KeyEvent.VK_F1, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				m_areAdvancedFeaturesEnabled = !m_areAdvancedFeaturesEnabled;
				updateTransientView(true);
			}
		});

		m_keyHandlers.put(KeyEvent.VK_Q, new KeyEventHandler()
		{
			@Override protected void doProcess()
			{
				m_placementController.toggleOriginAligned();
				updateTransientView(true);
			}
		});
	}

	@Override public String getActionUIClass()
	{
		return BatchDevicePlacementActionUI.class.getName();
	}

	@Override public void keyTyped(KeyEvent e)
	{

	}

	@Override public void keyPressed(KeyEvent e)
	{
		int keyCode = e.getKeyCode();
		KeyEventHandler keyHandler = m_keyHandlers.get(keyCode);
		if (keyHandler != null) {
			keyHandler.process(keyCode);
		}
	}

	@Override public void keyReleased(KeyEvent e)
	{

	}

	@Override public void mouseClicked(MouseEvent e)
	{
		m_currentMouse_point = getMouseWorldPoint(e);
		if (e.getClickCount() > 1) {
			terminateAction();
		}
		else {
			m_placementController.mouseClicked(m_currentMouse_point);
			if (m_placementController.hasPendingPlacements()) {
				updateTransientView(true);
			}
			else {
				terminateAction();
			}
		}
	}

	protected void terminateAction()
	{
		if (m_symbolSelection) {
			switchToPlacementModeFromSymbolSelectionMode();
		}
		else {
			getController().getActionMgr().terminateActiveAction(true);
		}
	}

	private void switchToPlacementModeFromSymbolSelectionMode()
	{
		if (m_symbolSelection) {
			//when trying for ending the action in symbol selection mode, it would
			//return to placement mode. otherwise its feeling a loss of work.
			m_placementController.endProcessing();
			m_symbolSelection = false;
			m_placementController = m_deviceController;
			m_placementController.beginProcessing();
			updateTransientView(true);
		}
	}

	private void switchToSymbolSelectionModeFromPlacementMode()
	{
		if (!m_symbolSelection) {
			m_symbolSelection = true;
			m_placementController.endProcessing();
			final int currentPlacementIdx = m_placementController.getCurrentPlacementIdx();
			final Point currentPlacementPoint = m_placementController.getCurrentPlacementPoint();
			m_placementController = m_symbolController;
			m_placementController.moveToNewPlacementPoint(currentPlacementPoint);
			m_placementController.setupPlacementIndex(currentPlacementIdx);
			m_placementController.beginProcessing();
			updateTransientView(true);
		}
	}

	@Override public void mousePressed(MouseEvent e)
	{
		m_currentMouse_point = getMouseWorldPoint(e);
		final boolean placementLocationChanged = isPlacementLocationChanged();
		m_placementController.mousePressed(m_currentMouse_point);
		updateTransientView(placementLocationChanged);
	}

	@Override public void mouseReleased(MouseEvent e)
	{
		m_currentMouse_point = getMouseWorldPoint(e);
		final boolean placementLocationChanged = isPlacementLocationChanged();
		m_placementController.mouseReleased(m_currentMouse_point);
		updateTransientView(placementLocationChanged);
	}

	@Override public void mouseEntered(MouseEvent e)
	{

	}

	@Override public void mouseExited(MouseEvent e)
	{

	}

	@Override public void mouseDragged(MouseEvent e)
	{
		m_currentMouse_point = getMouseWorldPoint(e);
		final boolean placementLocationChanged = isPlacementLocationChanged();
		m_placementController.mouseDragged(m_currentMouse_point);
		updateTransientView(placementLocationChanged);
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		m_currentMouse_point = getMouseWorldPoint(e);
		final boolean placementLocationChanged = isPlacementLocationChanged();
		m_placementController.mouseMoved(m_currentMouse_point);
		updateTransientView(placementLocationChanged);
	}

	private boolean isPlacementLocationChanged()
	{
		final Point currentPlacementPoint = m_placementController.getCurrentPlacementPoint();
		final Point gridSnappedPoint = m_placementController.getGridSnappedPoint(m_currentMouse_point);
		return currentPlacementPoint == null || !gridSnappedPoint.equals(currentPlacementPoint);
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		final Action actionUI = getActionUI();
		if (actionUI != null && !getOperands(selections).isEmpty()) {
			container.add(new ActionEntry(actionUI));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{
		if (m_placementController == null || m_placementController.isSymbolPreviewMode()) {
			return;
		}
		AbstractAction act = new BackupAction(this);

		act.putValue(Action.NAME, ResourceMgr.getString(BatchDevicePlacementAction.class,
				"BatchDevicePlacementAction.backup.action.name"));
		act.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getString(BatchDevicePlacementAction.class,
				"BatchDevicePlacementAction.backup.action.name"));
		act.putValue(Action.LONG_DESCRIPTION, ResourceMgr.getString(BatchDevicePlacementAction.class,
				"BatchDevicePlacementAction.backup.action.description"));

		//putValue(SMALL_ICON, icon);
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
		act.putValue(Action.ACCELERATOR_KEY, accel);
		container.add(new ActionEntry(act));

		// Put an entry on the menu to increment the radius by 10
		act = new CommitAction(this);
		act.putValue(Action.NAME, ResourceMgr.getString(BatchDevicePlacementAction.class,
				"BatchDevicePlacementAction.commit.action.name"));
		act.putValue(Action.SHORT_DESCRIPTION, ResourceMgr.getString(BatchDevicePlacementAction.class,
				"BatchDevicePlacementAction.commit.action.name"));
		act.putValue(Action.LONG_DESCRIPTION, ResourceMgr.getString(BatchDevicePlacementAction.class,
				"BatchDevicePlacementAction.commit.action.description"));
		//putValue(SMALL_ICON, icon);
		accel = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		act.putValue(Action.ACCELERATOR_KEY, accel);
		container.add(new ActionEntry(act));
	}

	@NotNull protected Model getModel()
	{
		return (Model) getCapletModel();
	}

	@Override public SnapPoint snapWorldPoint(MouseEvent e, Point worldPt)
	{
		return SnapPoint.toSnapPoint(m_placementController.getGridSnappedPoint(getMouseWorldPoint(e)), worldPt, true);
	}

	private class BackupAction extends AbstractContextAction
	{

		protected BackupAction(IApplicationSpecificationAction parent)
		{
			super(parent, ResourceMgr.getString(CreateMultipleConductorsAction.class,
					"CreateMultipleConductorsAction.backup.action.name"));
		}

		public void actionPerformed(ActionEvent e)
		{
			backup();
		}

		public boolean isEnabled()
		{
			return m_placementController.canUndo();
		}
	}

	private class CommitAction extends AbstractContextAction
	{

		protected CommitAction(IApplicationSpecificationAction parent)
		{
			super(parent, ResourceMgr.getString(CreateMultipleConductorsAction.class,
					"CreateMultipleConductorsAction.commit.action.name"));
		}

		public void actionPerformed(ActionEvent e)
		{
			terminateAction();
		}

		public boolean isEnabled()
		{
			return true;
		}
	}
}
