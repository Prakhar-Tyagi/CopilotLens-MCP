package chs.caplets.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ModelChangeEvent;
import chs.cof.logical.ILogicDesign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.util.Collections;

class LogicDesignUpdateBrowserRefresh extends AbstractDesignUpdateListener
{

	LogicDesignUpdateBrowserRefresh(@NotNull ILogicDesign design, @NotNull ICapletController capletController)
	{
		super(design, capletController);
	}

	@Override protected boolean shouldHandleUpdateFromRemoteOncePerAction()
	{
		return true;
	}

	protected void handleDesignDataUpdateFromRemote()
	{
		ICapletModel model = mController.getCapletModel();
		//If the action is cancelled and the connectivity is refreshed then there is a need for refreshing browser tree.
		if (model != null) {
			//if logic browser tree is expanded this may eventually load a diagram and thus might implicitly refresh
			//the design. that would cause this event firing and eventually refreshing the browser tree but that would
			//un-install the UI tree which makes the old treeUI object where the node expansion triggered this cycle
			//would become stale and would cause NPE delayed exception. and browser tree won't get expanded also.
			//so scheduling this for next execution in the event queue.
			if (SwingUtilities.isEventDispatchThread()) {
				SwingUtilities.invokeLater(() -> {
					doHandleDesignDataUpdateFromRemote(model);
				});
			}
			else {
				doHandleDesignDataUpdateFromRemote(model);
			}
		}
	}

	private void doHandleDesignDataUpdateFromRemote(ICapletModel model)
	{
		if (model.isDestroyed()) {
			//this model can be destroyed before coming here during save at close.
			return;
		}
		boolean modified = model.isModified();
		try {
			model.notifyModelChange(new ModelChangeEvent(model, Collections.emptyList()));
		}
		finally {
			model.setModified(modified);
		}
	}

	@Override protected boolean shouldNotify(@Nullable ILogicDesign context)
	{
		//listen only for the related design only.
		return isContextMatching(context) && super.shouldNotify(context);
	}
}
