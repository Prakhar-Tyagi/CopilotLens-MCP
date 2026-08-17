/*
 * Copyright 2003-2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.cof.logical.cable.ICableFactory;
import chs.cof.logical.cable.IConnector;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.common.ICommonFactory;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.LogicObjectUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;

/**
 * Overrides {@link CreateJackConnectorAction} to not add pins by default.
 *
 * @author Matt Boyd
 */
public class CreateNoPinJackConnectorAction extends CreateJackConnectorAction
{

	private ILibraryPartSelection m_librarySelection;
	private static Cursor m_cursor = null;

	public CreateNoPinJackConnectorAction(ICapletController controller)
	{
		super(controller);
		if (m_cursor == null) {
			m_cursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_connector_jack_nopin.gif", new Point(7, 7));
		}
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_librarySelection = PartBrowserActionHelper.getSelectedBrowserPart();
		if (m_librarySelection != null &&
				shouldCreateModularConnectorWithOutInstance(m_librarySelection.getSelectedObject())) {
			ICableFactory cblFactory = FactoryMgr.getCablePropertiedFactory();
			ICommonFactory commonFactory =
					FactoryMgr.getCommonFactory();
			IUID uid = commonFactory.createUID();
			IConnector connector = cblFactory.createJackConnector(uid);
			if (((ILogicModel) getModel()).getDesign() != null
					&& (((ILogicModel) getModel()).getDesign().getConnectivity() != null)) {
				((ILogicModel) getModel()).getDesign().getConnectivity().addConnector(connector);
			}

			connector.assignLibraryDetails(m_librarySelection);
			//TODO Bala : is this redundant?
			if (LogicObjectUtils.isValidPositionContainer(connector)) {
				connector.createPositionsFromLibrary((ILibraryObject) connector.getLibraryObject());
			}
			connector.removeAllSymbolRef();
			return IActionEnum.eCompleted;
		}
		return super.onActivate(e);
	}

	protected boolean shouldAddPins()
	{
		return isCtrlDown();
	}

	/**
	 * Gets the class attribute of the {@link CreateNoPinInlineConnectorAction} object.
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateNoPinJackConnectorActionUI.class.getName();
	}

	public boolean onPostTerminate(boolean onTerminateResult)
	{
		enableDesignTreeExpansion(false);
		return true;
	}

	/**
	 * @see chs.caplets.logic.actions.CreateJackConnectorAction#getCursor()
	 */
	public Cursor getCursor()
	{
		return m_cursor;
	}

	@Nullable
	public ILibraryPartSelection getLibrarySelectedObject()
	{
		return m_librarySelection;
	}

	public boolean onTerminate(boolean successful)
	{
		if (m_librarySelection != null &&
				shouldCreateModularConnectorWithOutInstance(m_librarySelection.getSelectedObject())) {
			enableDesignTreeExpansion(true);
			return true;
		}
		return super.onTerminate(successful);
	}
}