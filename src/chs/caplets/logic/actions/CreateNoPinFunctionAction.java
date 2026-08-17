package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.utilities.ResourceMgr;

import java.awt.Cursor;

public class CreateNoPinFunctionAction extends CreateFunctionAction
{

	public CreateNoPinFunctionAction(ICapletController controller)
	{
		super(controller);
	}

	protected boolean shouldAddPins()
	{
		return isCtrlDown();
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return CreateNoPinFunctionActionUI.class.getName();
	}

	public String getStatusbarText()
	{
		return ResourceMgr
				.getString(CreateNoPinDeviceAction.class, "CreateParameterizedObjectAction.StatusBar.NoPorts.text");
	}

	public Cursor getCursor()
	{
		return cursor;
	}

}
