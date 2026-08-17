/*
 * Copyright 2002-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IOutputWindow;
import chs.caf.cafmain.actions.DeveloperHandyActionUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.ILogicSegmentContainer;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.utilities.BuildInfo;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.task.TaskMgr;
import chs.utility.ui.progress.ProgressCancelledException;
import chs.view.route.RoutableUtils;
import chs.view.utils.ConductorRouteActionHelper;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class AutoRouteAction extends ControllerActionRT implements ICtxMenuProvider
{

	private Model m_model = null;
	private static Cursor m_cursor = CAFUtils.getInstance().loadCursor(Cursor.DEFAULT_CURSOR);
	private static final String TASK_ID = "Auto Route";

	private static final boolean m_devExtennsionsEnabled = BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled();
	private static final boolean m_qAExtennsionsEnabled = BuildInfo.getBuildInfo().areQAExtensionsEnabled();
	private static final boolean m_extennsionsEnabled = m_devExtennsionsEnabled || m_qAExtennsionsEnabled;

	public AutoRouteAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
	}

	/**
	 * Gets the Enabled attribute of the Delete Action
	 *
	 * @return The Enabled value
	 */
	public boolean isEnabled()
	{
		// gdh 10/07/03 4014
		// don't allow auto-routing on a non-editable diagram (using keyboard shortcut)
		if (m_model != null && !m_model.isEditable()) {
			return false;
		}
		// dts0100408329 - Cannot have >1 task with the same ID, throws exception
		if (TaskMgr.defaultTaskMgr().getRunningTask(TASK_ID) != null) {
			return false;
		}

		boolean bEnable = false;
		for (SelectionIterator iter = getController().getSelectMgr().getPreSelections().getSelected();
				iter.hasNext(); ) {
			Selection sel = iter.getNext();
			if (ILogicSegmentContainer.class.isAssignableFrom(sel.getSelectionClass())) {
				bEnable = true;
				break;
			}
		}

		return bEnable && super.isEnabled();
	}

	/**
	 * Description of the Method
	 *
	 * @param e The Action Event
	 *
	 * @return Return eCompleted so the action will complete
	 */
	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	/**
	 * If successful delete the selected objects
	 */
	protected boolean onTerminate(boolean successful)
	{
		boolean bEditOk = true;

		// Delete all of the selected objects
		if (successful) {
			if (m_extennsionsEnabled) {
				ConductorRouteActionHelper.printTime("Auto Route started at ");
			}
			bEditOk = editModel();
			if (m_extennsionsEnabled) {
				ConductorRouteActionHelper.printTime("Auto Route ended at ");
			}
		}

		return bEditOk;
	}

	public String getActionUIClass()
	{
		return AutoRouteActionUI.class.getName();
	}

	// Do the model edit

	private boolean editModel()
	{
		return doAutoRoute(getController(), m_model);
	}

	private static boolean doAutoRoute(ICapletController controller, Model model)
	{

		// loop through select objects and delete them
		SelectSet preSelections = controller.getSelectMgr().getPreSelections();
		ISchemDiagram diagram = model.getDiagram();

		try {
			doRun(preSelections, diagram);
		}
		catch (ProgressCancelledException e) {
			// do nothing
		}

		IGfxModel gfxModel = (IGfxModel) controller.getCapletModel();
		IDynamicGfxService dynamics = gfxModel.getDynamicGfxService();
		dynamics.removeAllDynamicGfx();
		dynamics.removeAllTransientGfx();
		dynamics.resetSelections();

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
		return true;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (selections.getSelectCount() >= 1) {
			for (SelectionIterator iter = selections.getSelected(); iter.hasNext(); ) {
				Selection sel = iter.getNext();
				if (ILogicSegmentContainer.class.isAssignableFrom(sel.getSelectionClass())) {
					container.add(new ActionEntry(getActionUI()));
					break;
				}
			}
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_cursor;
	}

	/**
	 * @see ActionRT#destroy()
	 */
	public void destroy()
	{
		super.destroy();
		m_model = null;
	}

	/*
	* @param preSelections The SelectSet of items currently selected
	* @param diagram The diagram
	*/

	private static void doRun(SelectSet preSelections, ISchemDiagram diagram)
	{
		doRun(preSelections, diagram, false);
	}

	/*
	* @param preSelections The SelectSet of items currently selected
	* @param newSelections The items that are selected once the auto route is complete
	* @param diagram The diagram
	*/

	public static void doRun(SelectSet preSelections, ISchemDiagram diagram,
			boolean neverShowProgressBar)
	{
		// Contains schem conductors
		Set<ILogicSegmentContainer> conductors = new LinkedHashSet<>();

		IOutputWindow output = CAFUtils.getInstance().getOutputWindow();
		Set<ILogicSegmentContainer> processedConductors = new HashSet<>();
		for (SelectedUIDObjectIterator iter = preSelections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();

			if (obj instanceof ILogicSegmentContainer) {
				if (!checkIfValidConductor(obj, diagram, output, processedConductors)) {
					continue;
				}

				conductors.add((ILogicSegmentContainer) obj);
			}
		}

		if (!conductors.isEmpty()) {
			try {
				ConductorRouteAction.getInstance().addConductorsForRoute(conductors, true);
				if (neverShowProgressBar) {
					ConductorRouteAction.getInstance().processAction(true, false);
				}
				else {
					ConductorRouteAction.getInstance().processAction();
				}
			}
			catch (ProgressCancelledException e) {
				throw new ProgressCancelledException(e);
			}
			catch (Exception xcpt) {
				Environment.getExceptionDisplay().displayException(xcpt, false);
			}
			finally {
				if (m_extennsionsEnabled) {
					DeveloperHandyActionUtils.printDiagramScore(diagram, "Auto Route");
				}
			}
		}
	}

	private static boolean checkIfValidConductor(IUIDObject obj, ISchemDiagram diagram, IOutputWindow output,
			Set<ILogicSegmentContainer> processedConductors)
	{
		//dts0100589918 Validation Error on Route Selected action from Browser Tree if conductor is in different diagram
		// If the selection is not part of current diagram, we can't route it,
		if (DiagramHelper.getDiagram((IDiagramObject) obj) != diagram) {
			if (output != null) {
				ILogicSegmentContainer conductor = null;
				if (obj instanceof ILogicSegmentContainer) {
					conductor = (ILogicSegmentContainer) obj;
				}
				else if (obj instanceof ILogicSegment) {
					conductor = RoutableUtils.getCommonRouteObjectUtils().getSegmentOwner((ILogicSegment) obj);
				}
				if (conductor != null && processedConductors.add(conductor)) {
					ILogicObject cableObject =
							RoutableUtils.getCommonRouteObjectUtils().getSegmentOwnerConnectivity(conductor);
					if (cableObject != null) {
						output.sendApplicationMessage(
								ResourceMgr.getString(AutoRouteAction.class, "AutoRouteAction.ignoreconductor.text",
										cableObject.getName()));
					}
				}
			}
			return false;
		}
		return true;
	}
}
