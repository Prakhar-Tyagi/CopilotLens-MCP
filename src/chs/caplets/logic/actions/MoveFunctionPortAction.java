package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

public class MoveFunctionPortAction extends MovePinAction
{

	public MoveFunctionPortAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return MoveFunctionPortActionUI.class.getName();
	}

	@Override @NotNull protected String getNameForTooltip()
	{
		return ResourceMgr.getString(MoveFunctionPortAction.class, "MoveFunctionPortAction.tooltipName.text");
	}

	@Override public String getStatusbarText()
	{
		return ResourceMgr.getString(MoveFunctionPortAction.class, "MoveFunctionPortAction.StatusBar.Msg");
	}
}
