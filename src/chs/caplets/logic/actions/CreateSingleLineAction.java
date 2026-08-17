/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023-2024 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.caf.caplet.helpers.snapping.SingleLineConnectionHelper;
import chs.caf.helpers.ui.std.DesignAbstractionHelper;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.parts.ILibraryMulticore;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cofUtils.cmd.CreateSchemSingleLineCmd;
import chs.common.DesignAbstractionType;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A create tool to make a single line.
 *
 * @created June 14, 2023
 */
public class CreateSingleLineAction extends CreateHighwayAction
{

	private static Cursor m_singleLineCursor = null;
	@Nullable
	private ILibraryMulticore m_libMulticore = null;
	private SingleLineConnectionHelper mSingleLineConnectionHelper;

	public CreateSingleLineAction(ICapletController controller)
	{
		super(controller);

		if (m_singleLineCursor == null) {
			m_singleLineCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/ico_cursor_singleline.png", new Point(7, 7));
		}

		if (mSingleLineConnectionHelper == null) {
			mSingleLineConnectionHelper = new SingleLineConnectionHelper(
					((IGfxModel) getController().getCapletModel()).getDynamicGfxService());
		}
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_singleLineCursor;
	}

	protected Class snappingSource()
	{
		return ISingleLine.class;
	}

	public String getActionUIClass()
	{
		return CreateSingleLineActionUI.class.getName();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		m_libMulticore = getSelectedLibMulticore();
		m_cmd = new CreateSchemSingleLineCmd(ConductorRouteAction.getInstance(), m_libMulticore);

		return super.onActivate(e);
	}

	@Nullable private ILibraryMulticore getSelectedLibMulticore()
	{
		ILibraryMulticore libraryMulticore = null;
		ILibraryPartSelection libPart = PartBrowserActionHelper.getSelectedBrowserPart();
		if (libPart != null && libPart.getSelectedObject() instanceof ILibraryMulticore) {
			libraryMulticore = (ILibraryMulticore) libPart.getSelectedObject();
		}
		return libraryMulticore;
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		mSingleLineConnectionHelper.handleSingleLineEvent(e);
		super.mouseMoved(e);
	}

	@Override public boolean onTerminate(boolean successful)
	{
		mSingleLineConnectionHelper.handleTerminate();
		boolean result = super.onTerminate(successful);
		return result;
	}

	@Override public boolean isEnabled()
	{
		//we need to perform basic checks first to decide availability
		if(!super.isEnabled()){
			return false;
		}
		DesignAbstractionType designAbstraction = DesignAbstractionHelper.getTypeOfDesignAbstraction();
		return designAbstraction == null || (designAbstraction == DesignAbstractionType.UNDEFINED);
	}
}
