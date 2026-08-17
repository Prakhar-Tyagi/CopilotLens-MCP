/*
 * Copyright 2002-2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.actions;

// caf imports

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.creation.AbstractDeleteAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.SelectionUtils;
import chs.caplets.logic.helpers.DeletabilityChecker;
import chs.caplets.logic.helpers.IDeletabilityChecker;
import chs.cof.drawplus.IBaseDiagram;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

public abstract class DeleteAction extends AbstractDeleteAction
{

	private Set m_updatedSymbols = new HashSet();

	protected DeleteAction(ICapletController controller)
	{
		super(controller);
	}

	/**
	 * Gets the Enabled attribute of the Delete Action
	 *
	 * @return The Enabled value
	 */
	public boolean isEnabled()
	{
		// enable this thing only if there are items selected
		SelectSet sset = getPreSelections();
		// enable this thing only if there are items selected
		if (sset.getSelectCount() == 0) {
			return false;
		}

		// gdh 10/07/03 4014
		// do not allow delete in non-editable diagram
		ICapletModel icm = getController().getCapletModel();
		if (icm != null && !icm.isEditable()) {
			return false;
		}

		// FEAT13040 : Disabled for now if the selection spans multiple Logic diagrams - might be able to relax this later
		IBaseDiagram currentDiagram = CAFUtils.getInstance().getActiveDiagram();
		if (SelectionUtils.hasOtherDiagramSelection(sset, currentDiagram)) {
			return false;
		}

		return isDeletable(sset) && super.isEnabled();
	}

	protected boolean isDeletable(@NotNull SelectSet sset)
	{
		// If everything in the selection is deletable, the selection is deletable
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (!isDeletable(obj, sset)) {
				return false;
			}
		}

		return true;
	}

	protected boolean isDeletable(IUIDObject obj, @NotNull SelectSet sset)
	{
		m_updatedSymbols.clear();
		IDeletabilityChecker deletabilityChecker = new DeletabilityChecker();
		return deletabilityChecker.canDelete(obj, sset);
	}

	/**
	 * Description of the Method
	 *
	 * @param e The Action Event
	 *
	 * @return Return eCompleted so the action will complete
	 */
	@Override @NotNull public IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	/**
	 * If successful delete the selected objects
	 */
	public boolean onTerminate(boolean successful)
	{
		boolean bEditOk = true;

		// Delete all of the selected objects
		if (successful) {
			bEditOk = editModel();
		}

		return bEditOk;
	}

	// Do the model edit
	protected abstract boolean editModel();

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// If there is something selected we can delete it
		if (isDeletable(selections)) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	/**
	 * @see ActionRT#destroy()
	 */
	public void destroy()
	{
		super.destroy();
		m_model = null;
	}
}

