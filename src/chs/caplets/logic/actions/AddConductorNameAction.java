/*
 * Copyright 2005-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

// caf imports

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramText;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.utility.attr.AttributeUtils;
import chs.utility.helpers.TextHelper;

import java.awt.Cursor;
import java.awt.event.ActionEvent;

public class AddConductorNameAction extends ControllerActionRT implements ICtxMenuProvider
{

	private Model m_model = null;

	static private Cursor m_deleteCursor = CAFUtils.getInstance().loadCursor(Cursor.DEFAULT_CURSOR);

	public AddConductorNameAction(ICapletController controller)
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
		if (!getController().getCapletModel().isEditable()) // eg. read-only model
		{
			return false;
		}

		// enable this thing only if there are items selected
		for (IDiagramObject obj : LogicMultiUserSelectionFilter
				.getValidDiagramObjectOperands(getController().getSelectMgr().getPreSelections())) {
			if (obj instanceof ISegment || obj instanceof IHighwaySegment) {
				return super.isEnabled();
			}
		}

		return false;
	}

	/**
	 * Description of the Method
	 *
	 * @param e The Action Event
	 *
	 * @return Return eCompleted so the action will complete
	 */
	public IActionEnum onActivate(ActionEvent e)
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

	public String getActionUIClass()
	{
		return AddConductorNameActionUI.class.getName();
	}

	private boolean segmentHasNameText(ISegment seg)
	{
		IGfxObjectIterator iter = seg.getObjects();
		while (iter.hasNext()) {
			IGfxObject obj = iter.getNext();
			if (AttributeUtils.isNameText(obj)) {
				return true;
			}
		}

		return false;
	}

	private boolean highwaySegmentHasNameText(IHighwaySegment highwaySegment)
	{
		IGfxObjectIterator iter = highwaySegment.getObjects();
		while (iter.hasNext()) {
			IGfxObject obj = iter.getNext();
			if (AttributeUtils.isNameText(obj)) {
				return true;
			}
		}

		return false;
	}

	// Do the model edit
	private boolean editModel()
	{
		// loop through select segments amd add nametext to them
		SelectSet preSelections = getController().getSelectMgr().getPreSelections();
		ISchemDiagram diagram = m_model.getDiagram();

		//
		for (IDiagramObject obj : LogicMultiUserSelectionFilter
				.getValidDiagramObjectOperands(getController().getSelectMgr().getPreSelections())) {

			if (obj instanceof ISegment) {
				ISegment segment = (ISegment) obj;

				if (segmentHasNameText(segment)) {
					continue;
				}

				chs.cof.logical.schem.IConductor cond = segment.getConductor();
				IDiagramText text =
						TextHelper.createMiddleJustLogicNameText(cond.getConnectivity(), (ISchemDiagram) diagram);
				TextHelper.addTextToSegment(segment, text);
			}
			if (obj instanceof IHighwaySegment) {
				IHighwaySegment highwaySegment = (IHighwaySegment) obj;

				if (highwaySegmentHasNameText(highwaySegment)) {
					continue;
				}

				chs.cof.logical.schem.IHighwaySchematic highwayCond = highwaySegment.getHighway();
				IDiagramText text = TextHelper
						.createMiddleJustLogicNameText(highwayCond.getConnectivity(), (ISchemDiagram) diagram);
				TextHelper.addTextToSegment((IHighwaySegment) obj, text);
			}
		}

		return true;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		// If there is something selected we can delete it

		if (isEnabled()) {
			setFullNamedContextMenu(container);
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
		return m_deleteCursor;
	}

	/**
	 * @see chs.caf.caplet.helpers.ActionRT#destroy()
	 */
	public void destroy()
	{
		super.destroy();
		m_model = null;
	}
}

