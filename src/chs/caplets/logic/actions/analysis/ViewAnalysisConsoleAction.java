/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.analysis;

// caf imports

import chs.analysis.AnalysisServices;
import chs.caf.AppAction;
import chs.caf.IFIB;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.cof.logical.ILogicDesign;
import chs.common.IDesignContainer;
import chs.images.CHSImageLoader;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import java.awt.event.ActionEvent;

/**
 * * Called when the user attempts to cut some object(s) * * The method is undoable, becuase it makes changes to the
 * caplet model
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalSystemsIntegrator, Application.CapitalCapture,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class ViewAnalysisConsoleAction extends AppAction
{

	public ViewAnalysisConsoleAction(IFIB fib)
	{
		super(fib);
		String name = ResourceMgr.getString(ViewAnalysisConsoleAction.class, "ViewAnalysisConsoleAction.String.name");
		String shortDesc =
				ResourceMgr.getString(ViewAnalysisConsoleAction.class, "ViewAnalysisConsoleAction.String.name");
		String longDesc =
				ResourceMgr.getString(ViewAnalysisConsoleAction.class, "ViewAnalysisConsoleAction.String.name");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_C);
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/as_console.gif");

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(SMALL_ICON, icon);
	}

	/**
	 * * Use the analysis interface to fire off the launcher.
	 */
	public void actionPerformed(ActionEvent ae)
	{
		if (LogicAnalysisServices.getAnalysisServices().getControlPanel().isShowing()) {
			LogicAnalysisServices.getAnalysisServices().getControlPanel().ensureConsoleIsVisible();
		}
	}

	public void updateUI()
	{
	}

	public boolean isEnabled()
	{

		IDesignContainer design = FactoryMgr.getCAFUtils().getActiveDesignContainer();
		ILogicDesign logicDesign = design instanceof ILogicDesign ? (ILogicDesign) design : null;

		return AnalysisServices.isActionSupportedInMUMode(logicDesign);
	}
}