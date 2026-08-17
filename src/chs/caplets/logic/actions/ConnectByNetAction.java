/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2026 Siemens
 */
package chs.caplets.logic.actions;
import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.cof.logical.ISystemLogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.INetConductor;
import chs.common.IDesignContainer;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.Nullable;

@ApplicationSpecification(immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_CONNECT_BY_NET_ACTION")
public class ConnectByNetAction extends ConnectAction
{

	public ConnectByNetAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return ConnectByNetActionUI.class.getName();
	}

	@Nullable protected Class<? extends IConductor> getConductorClass()
	{
		return INetConductor.class;
	}

	public boolean isEnabled()
	{
		return super.isEnabled() && isLogicDesign();
	}

	private boolean isLogicDesign()
	{
		IDesignContainer design = getLogicDesign();
		return design instanceof ISystemLogicDesign;
	}

	@Nullable private IDesignContainer getLogicDesign()
	{
		ICapletController controller = getController();
		ICapletModel model = controller != null ? controller.getCapletModel() : null;
		if (model instanceof ILogicModel) {
			return ((ILogicModel) model).getDesign();
		}
		return null;
	}
}
