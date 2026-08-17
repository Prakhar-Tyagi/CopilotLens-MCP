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

import chs.caf.caplet.ICapletController;
import org.jetbrains.annotations.NotNull;

public class AddBackshellAction extends AbstractBackshellAction implements IICDProviderAction
{

	public AddBackshellAction(ICapletController controller)
	{
		super(controller);
	}

	@Override protected boolean hasCollectedAllRequiredDataForActivation()
	{
		//Show dialog and collect required backshll and termination details
		return showDialog();
	}

	protected boolean showDialog()
	{
		final AddBackshellDialog dialog = createAddBackshellDialog();
		dialog.setVisible(true);

		//dts0100531800 Issue: The input wasn't set as invalid when pressing "Escape"
		//Because the windowclosing action wasn't fired.
		//A check for "isCancelled" (Press the close button, cancel or escape) will work
		return !dialog.isCancelled();
	}

	@NotNull protected AddBackshellDialog createAddBackshellDialog()
	{
		return new AddBackshellDialog(getFrame(), getTitle(), this, true);
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return AddBackshellActionUI.class.getName();
	}
}
