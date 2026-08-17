/*
 * Copyright 2003-2012 Mentor Graphics Corporation
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
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.schem.IBaseShareableDiagramObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShareableDiagramObject;
import chs.common.IUIDObject;
import chs.utility.DiagramHelper;
import chs.utility.helpers.SchemPinListHelper;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class ToggleHomeAction extends ControllerActionRT implements ICtxMenuProvider
{

	public ToggleHomeAction(ICapletController controller, String instanceName)
	{
		super(controller, instanceName);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		List<IBaseShareableDiagramObject> operands = getOperands(getController());
		if (operands != null) {
			return IActionEnum.eCompleted;
		}
		else {
			return IActionEnum.eCanceled;
		}
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			List<IBaseShareableDiagramObject> operands = getOperands(getController());
			if (operands != null && !operands.isEmpty()) {
				toggleHome(operands);
			}
		}
		return successful;
	}

	private void toggleHome(List<IBaseShareableDiagramObject> operands)
	{
		// if the action is MARK_HOME instance that means we would like to make the object as home object
		// other wise if the action is MARK_NOT_HOME then we would like to make the object as non home
		boolean isHome = ToggleHomeActionUI.MARK_HOME.equalsIgnoreCase(getActionInstanceName());

		// invoked action should be either mark home or Remove Home
		assert ToggleHomeActionUI.MARK_HOME.equalsIgnoreCase(getActionInstanceName()) ||
				ToggleHomeActionUI.REMOVE_HOME.equalsIgnoreCase(getActionInstanceName());

		for (IBaseShareableDiagramObject operand : operands) {
			toggleHomeForShareableDiagramObject(isHome, operand);
		}
		getController().getSelectMgr().notifySelectionChanged();
	}

	public static void toggleHomeForShareableDiagramObject(boolean isHome, IBaseShareableDiagramObject object)
	{
		object.setHome(isHome);

		// the other half of an inline or a mated inline pin should also be set to the same home condition
		if (object instanceof IShareableDiagramObject) {
			IShareableDiagramObject mate =
					SchemPinListHelper.getInlineMateObject((IShareableDiagramObject) object);
			if (mate != null) {
				mate.setHome(isHome);
			}
		}
	}

	public boolean isEnabled()
	{
		if (!super.isEnabled()) {
			return false;
		}

		List<IBaseShareableDiagramObject> operands = getOperands(getController());
		String instanceName = getActionInstanceName();
		// invoked action should be either mark home or Remove Home
		assert ToggleHomeActionUI.MARK_HOME.equalsIgnoreCase(instanceName) ||
				ToggleHomeActionUI.REMOVE_HOME.equalsIgnoreCase(instanceName);

		if (operands != null && !operands.isEmpty()) {
			if (ToggleHomeActionUI.MARK_HOME.equals(instanceName)) {
				return shouldEnableIsHome(operands);
			}
			else if (ToggleHomeActionUI.REMOVE_HOME.equals(instanceName)) {
				return shouldEnableRemoveHome(operands);
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}

	public String getActionUIClass()
	{
		return ToggleHomeActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		List<IBaseShareableDiagramObject> operands = getOperands(getController());
		if (operands != null && !operands.isEmpty()) {
			String instanceName = getActionInstanceName();
			// invoked action should be either mark home or Remove Home
			assert ToggleHomeActionUI.MARK_HOME.equalsIgnoreCase(instanceName) ||
					ToggleHomeActionUI.REMOVE_HOME.equalsIgnoreCase(instanceName);

			boolean shouldAddToCtxMenu = false;
			if (ToggleHomeActionUI.MARK_HOME.equals(instanceName)) {
				// if there is atleast one selcted object which is non home object then we need to add "Set Home" to ctxt menu
				shouldAddToCtxMenu = shouldEnableIsHome(operands);
			}
			else if (ToggleHomeActionUI.REMOVE_HOME.equals(instanceName)) {
				shouldAddToCtxMenu = shouldEnableRemoveHome(operands);
			}

			if (shouldAddToCtxMenu) {
				container.add(new ActionEntry(getActionUI()));
			}
		}
	}

	private boolean shouldEnableIsHome(List<IBaseShareableDiagramObject> operands)
	{
		// if there is atleast one selcted object which is non home object then we need to enable "Set Home"
		for (IBaseShareableDiagramObject operand : operands) {
			if (!operand.isHome()) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldEnableRemoveHome(List<IBaseShareableDiagramObject> operands)
	{
		// if there is atleast one selected object which is home object then we need to enable "Remove Home"
		for (IBaseShareableDiagramObject operand : operands) {
			if (operand.isHome()) {
				return true;
			}
		}
		return false;
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	@Nullable public static List<IBaseShareableDiagramObject> getOperands(ICapletController controller)
	{
		if (controller == null) {
			return null;
		}

		List<IBaseShareableDiagramObject> result = new ArrayList<IBaseShareableDiagramObject>();
		for (SelectedUIDObjectIterator iter = controller.getSelectMgr().getCurrentSelections().getSelectedUIDObjects();
				iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();
			if (!(uidObj instanceof IRepresentedObject)) {
				// FEAT00013786: stack pins are also filtered out here. so do not need to check again
				continue;
			}

			if (uidObj instanceof ISegment) {
				uidObj = ((ISegment) uidObj).getConductor();
			}

			if (uidObj instanceof IBaseShareableDiagramObject) {
				// FEAT00013786: IBaseShareableDiagramObject is introduced.
				// StackPin and HighwaySchem are implementing. These code changes are to handle this case for highway
				// Actually, this action is not enabled for stack pin which is taken care above
				IBaseShareableDiagramObject sdo = (IBaseShareableDiagramObject) uidObj;
				ISchemDiagram diagram = DiagramHelper.getDiagram(sdo);
				if (diagram != null && diagram.isEditable()) {
					result.add(sdo);
				}
			}
			else {
				// todo-vishwam I think we should not return null here, we need to just continue
				return null;
			}
		}
		return result;
	}
}
