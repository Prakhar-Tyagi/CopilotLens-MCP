/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.bridges;

import chs.caf.AppAction;
import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.IWindowMgr;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.cof.logical.IDesign;

import java.awt.Component;

/*
 ************************************************************************************************
 *   (c) Copyright 2003, by Mentor Graphics Harness Systems Ltd..  All rights reserved.  This
 *   data and information is proprietary to,  and a valuable trade secret of, Mentor Graphics
 *   Harness Systems Ltd.  It is given in confidence by Mentor Graphcis Harness Systems Ltd.,
 *   and may only be  used as  permitted under the license agreement  under which it has been
 *   distributed, and in no other way.
 *
 *   Mentor Graphics Harness Systems Ltd., Mentor House, Edward Court,
 *   Altrincham Business Park, Altrincham, WA14 5GL, UK.
 *
 *   Filename :   BridgeLogicAppAction
 *   User     :   jmyvon
 *   Created  :   21-Apr-2004
 *
 *   @author      Jean-Marc Yvon
 *   @version     1.0
 *
 ************************************************************************************************
 */

/**
 * Base Action for Bridges integration in Logic: no model modification (e.g. export, cross-highlight...)
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public abstract class BridgeLogicAppAction extends AppAction
{

	public BridgeLogicAppAction(IFIB fib)
	{
		super(fib);
	}

	/**
	 * Retrieves current loaded Design
	 *
	 * @return IDesign
	 */
	protected IDesign getDesign()
	{
		IDesign oDesign = null;
		ICapletController ctrl = CAFUtils.getInstance().getActiveCapletController();

		if (ctrl != null) {
			ICapletModel model = ctrl.getCapletModel();
			if (model != null && model instanceof chs.caplets.logic.Model) {
				oDesign = ((chs.caplets.logic.Model) model).getDesign();
			}
		}

		return oDesign;
	}

	protected Component getMainWindow()
	{
		Component window = null;
		IWindowMgr mgr = CAFUtils.getInstance().getWindowMgr();
		window = mgr.getDialogFrame();
		return window;
	}
}
