package chs.caplets.logic.actions.icdbrowser;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

public abstract class AbstactICDBrowserAction extends AbstractAction
{

	@Nullable protected IAction getActionToPerform()
	{
		final ICapletController controller = getActiveCapletController();
		return controller != null ? getAction(controller) : null;
	}

	@Nullable protected abstract IAction getAction(@NotNull ICapletController controller);

	public boolean isEnabled()
	{
		IAction act = getActionToPerform();
		if (act != null) {
			IActionMgr actMgr = CAFUtils.getInstance().getActiveActionMgr();
			if (actMgr != null) {
				if (!actMgr.getController().getCaplet().getActionDispatcher().isEnabled(act)) {
					return false;
				}
			}
		}
		return true;
	}

	protected ICapletController getActiveCapletController()
	{
		return CAFUtils.getInstance().getActiveCapletController();
	}

	@Override public void actionPerformed(ActionEvent e)
	{
		IAction action = getActionToPerform();
		if (action != null) {
			IActionMgr actMgr = CAFUtils.getInstance().getActiveActionMgr();
			if (actMgr != null) {
				actMgr.actionPerformed(action, e);
			}
		}
	}
}
