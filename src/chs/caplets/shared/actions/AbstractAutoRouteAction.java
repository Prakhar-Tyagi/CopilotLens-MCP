package chs.caplets.shared.actions;

import chs.caf.AppAction;
import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.caplet.ICaplet;
import chs.caplets.logic.ILogicCaplet;
import com.mentor.capital.javafx.interfaces.IRibbonConstants;
import com.mentor.capital.ui.IToggleAction;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.ICapletController;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;

abstract class AbstractAutoRouteAction extends AppAction implements IToggleAction
{
	AbstractAutoRouteAction(IFIB fib)
	{
		super(fib);
		initMenu();
	}

	void initMenu(){

	}


	@Override public boolean isOn()
	{
		return ConductorRouteAction.getInstance().isEnableAutoRouting();
	}

	@Override public void actionPerformed(ActionEvent e)
	{
		ICapletController activeController = getActiveCapletController();
		if(activeController!= null) {
			ICaplet activeCaplet = getCapletForActiveController();
			if (activeCaplet instanceof ILogicCaplet) {
				boolean oldToggled = isOn();
				firePropertyChange(IRibbonConstants.PROPERTY_TOGGLED, oldToggled, !oldToggled);
				setEnableRouting(!oldToggled);

			}
		}
	}

	protected void setEnableRouting(boolean togglingValue)
	{
		ConductorRouteAction.getInstance().setEnableAutoRouting(togglingValue);
	}


	@Override public boolean isEnabled()
	{
		// disabled if a background save is currently active, or if we are in local mode
		if (getFIB().isTaskActive(IFIB.TASK_SAVE)) {
			return false;
		}
		//Make sure that there is a diagram open by ensuring that the active caplet controller is not null
		return (CAFUtils.getInstance().getActiveCapletController() != null && super.isEnabled());
	}

	@Nullable protected ICaplet getCapletForActiveController()
	{
		ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
		return controller != null? controller.getCaplet(): null;
	}

	@Override public void updateUI()
	{
	}
}
