package chs.caplets;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IUndoableContainer;
import chs.cof.logical.shared.ISharedObject;
import chs.cog.ICOGManaged;
import chs.cog.IPersistenceSessionListener;
import org.jetbrains.annotations.NotNull;

public class UndoDisableForSharedObjectSave implements IPersistenceSessionListener
{

	private boolean bDisableUndo = false;

	public void clearUndo()
	{
		if (bDisableUndo) {
			ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
			IUndoableContainer undoableContainer = CAFUtils.getInstance().getCurrentUndoableContainer();
			if (undoableContainer != null) {
				undoableContainer.endEdit();
				controller.clearUndoQueue();
			}
		}
		bDisableUndo = false;
	}

	@Override public void loaded(@NotNull ICOGManaged managed)
	{

	}

	@Override public void updated(@NotNull ICOGManaged managed)
	{

	}

	@Override public void deleted(@NotNull ICOGManaged managed)
	{

	}

	@Override public void saved(@NotNull ICOGManaged managed)
	{
		if(managed instanceof ISharedObject) {
			bDisableUndo = true;
		}
	}
}
