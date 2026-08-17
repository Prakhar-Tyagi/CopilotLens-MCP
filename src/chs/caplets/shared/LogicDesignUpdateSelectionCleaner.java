package chs.caplets.shared;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.ILogicDesign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class LogicDesignUpdateSelectionCleaner extends AbstractDesignUpdateListener
{

	LogicDesignUpdateSelectionCleaner(@NotNull ILogicDesign design, @NotNull ICapletController capletController)
	{
		super(design, capletController);
	}

	@Override protected boolean shouldHandleUpdateFromRemoteOncePerAction()
	{
		return false;
	}

	@Override protected void handleDesignDataUpdateFromRemote()
	{
		mController.getSelectMgr().removeNonExistentObjects();
	}

	@Override protected boolean shouldNotify(@Nullable ILogicDesign context)
	{
		//listen only for the related design only.
		return isContextMatching(context);
	}
}
