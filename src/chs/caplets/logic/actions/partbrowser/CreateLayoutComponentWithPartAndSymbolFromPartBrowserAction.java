/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.partbrowser;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.partbrowser.PartBrowserAction;
import chs.caf.caplet.action.IAction;
import chs.caplets.logic.actions.CreateOtherComponentWithPartAndSymbolAction;

import javax.swing.Icon;

/**
 * @author chandras on 3-10-2019.
 */
public abstract class CreateLayoutComponentWithPartAndSymbolFromPartBrowserAction extends PartBrowserAction
{

	protected CreateLayoutComponentWithPartAndSymbolFromPartBrowserAction(
			String name, String shortDesc, String longDesc, int mnemonic, Icon icon)
	{
		super(name, shortDesc, longDesc, mnemonic, icon);
	}

	public IAction getActionToPerform()
	{
		return CAFUtils.getInstance().getActiveCapletController()
				.getAction(CreateOtherComponentWithPartAndSymbolAction.class);
	}
}
