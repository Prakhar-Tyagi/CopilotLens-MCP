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

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.utilities.ResourceMgr;

import javax.swing.Action;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.List;

public class CreateInterconnectConnectorAction extends CreateConnectorAction
{

	private static Cursor m_connectorCursor = null;
	private ILibraryPartSelection m_librarySelection;
	private String m_ctxCommand;

	/**
	 * Constructor for CreatePlugConnectorAction.
	 *
	 * @param controller
	 */
	public CreateInterconnectConnectorAction(ICapletController controller)
	{
		super(controller);
		//
		// This is trure for now - will add dummy pin.
		//
		//setShouldAddPins(false);
		setSubType(INTERCONNECT_CONNECTOR);
		if (m_connectorCursor == null) {
			m_connectorCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_connector.gif", new Point(7, 7));
		}
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_librarySelection = PartBrowserActionHelper.getSelectedBrowserPart();
		if (m_librarySelection == null) {
			m_librarySelection = InterconnectActionHelper.loadPart((Model) getModel(), getController());
		}
		if (m_librarySelection == null) {
			return IActionEnum.eCanceled;
		}
		//
		// Continue on to regular placement.
		//
		return super.onActivate(e);
	}

	protected IGfxObject createParamObject(Point p1, Point p2)
	{
		IGfxObject gobj = super.createParamObject(p1, p2);
		IPinList pl = (IPinList) gobj;
		chs.cof.logical.cable.IPinList cpl = pl.getConnectivity();
		return gobj;
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

	protected boolean isCtrlDown()
	{
		return false; // Cannot swap to 'no pin interconnect'
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_connectorCursor;
	}

	//
	// Explicitly don't return anything, as this is used for the tooltip.
	//
	public String getFeedbackText()
	{
		return null;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (InterconnectActionHelper.getSelectedInterconnectPin(selections) != null) {
			String shortDesc = (String) getActionUI().getValue(Action.SHORT_DESCRIPTION);
			if (m_ctxCommand == null || !m_ctxCommand.equalsIgnoreCase(shortDesc)) {
				// Make a private copy for command name
				m_ctxCommand = shortDesc;
			}
			//
			// Add this action - will pick up in onActivate..
			//
			container.add(new ActionEntry(getActionUI(), m_ctxCommand));
		}
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateInterconnectConnectorActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(CreateInterconnectConnectorAction.class,
				"CreateInterconnectConnectorActionUI.StatusBar.text");
	}
}