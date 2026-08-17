/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.cof.draw.IGrid;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.parts.partselector.ILibraryPartSelection;

import java.awt.event.ActionEvent;
import java.util.List;

public class CreateInlineInterconnectConnectorAction extends CreateInlineConnectorAction
{

	private ILibraryPartSelection m_librarySelection;

	/**
	 * Constructor for CreatePlugConnectorAction.
	 *
	 * @param controller
	 */
	public CreateInlineInterconnectConnectorAction(ICapletController controller)
	{
		super(controller);
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		// TODO-FEAT2882 - confirm that this is the requirement and for non-inline interconnects.
		//                 If so we can remove InterconnectHelper.loadPart and this function.

		//m_librarySelection = InterconnectHelper.loadPart((Model)getModel(),getController());
		//if (m_librarySelection == null) {
		//	return IActionEnum.eCanceled;
		//}
		//
		// Continue on to regular placement.
		//
		return super.onActivate(e);
	}

	public ILibraryPartSelection getLibrarySelectedObject()
	{
		return m_librarySelection;
	}

	protected List<IPin> addPins(int width, int height, IGrid grid, IPinList schem_conn, IConnector connector,
			boolean topdown)
	{
		List<IPin> addedPins = super.addPins(width, 0, InterconnectActionHelper.verticalOffset(height), grid,
				schem_conn, connector, topdown);
		return InterconnectActionHelper.convertPins(addedPins, m_librarySelection);
	}

	protected void setJackSubType()
	{
		setSubType(INLINE_INTERCONNECT_JACK_CONNECTOR);
	}

	protected void setPlugSubType()
	{
		setSubType(INLINE_INTERCONNECT_PLUG_CONNECTOR);
	}

	public String getActionUIClass()
	{
		return CreateInlineInterconnectConnectorActionUI.class.getName();
	}
}
