/**
 * Copyright 2008 Mentor Graphics Corporation.
 *            All Rights Reserved.
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.cof.logical.schem.IPinList;

import java.awt.event.ActionEvent;


/**
 * Created by IntelliJ IDEA. User: msoliman Date: Jul 31, 2008 Time: 1:31:25 PM To change this template use File |
 * Settings | File Templates.
 */
public class EditHarnessUnitTestAction extends EditHarnessAction
{

	public EditHarnessUnitTestAction(IPinList pinlst, String stt, ICapletController controller)
	{
		this(controller);
		harness = stt;
		pinlist = pinlst;
	}

	/**
	 * @param controller .
	 */
	public EditHarnessUnitTestAction(ICapletController controller)
	{
		super(controller);
	}

	private String harness = "";
	private IPinList pinlist = null;

	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		pinlist.getConnectivity().setHarness(harness);
		return true;
	}
}
