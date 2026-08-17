/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.AppAction;
import chs.caf.IFIB;
import chs.caf.action.immersed.ImmersedAction;
import chs.caplets.logic.actions.immersed.strategy.DeviceCreationContext;
import chs.caplets.logic.actions.immersed.strategy.DeviceCreationStrategyRegistry;
import chs.caplets.logic.actions.immersed.strategy.IDeviceCreationStrategy;
import chs.cof.logical.ILogicDesign;
import chs.common.IDesignContainer;
import chs.subsystem.immersed.impl.object.devicemodel.CreateDeviceInfo;
import chs.subsystem.immersedapp.ImmersedActionEvent;
import chs.subsystem.immersedapp.ImmersedAppServices;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.suite.DesignType;
import chs.utilities.ui.messaging.IMessageContent;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.ui.DialogExceptionDisplay;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;

/**
 * Action to add a new 3d device to the design based on a CreateDeviceInfo provided in the action context.
 */
@ImmersedAction(actionId = "CAPITAL_ADD_3D_DEVICE_ACTION")
public class Add3dDeviceAction extends AppAction
{

	@NotNull private final DeviceCreationStrategyRegistry m_strategyRegistry;

	public Add3dDeviceAction(IFIB fib)
	{
		super(fib);
		m_strategyRegistry = DeviceCreationStrategyRegistry.buildDefaultChain();
		putValue(NAME, "Add Device");
	}

	@Override public void updateUI()
	{
		setEnabled(isEnabled());
	}

	@Override public boolean isEnabled()
	{
		return super.isEnabled() && ImmersedAppServices.getImmersedDesign() == null;
	}

	@Override public void actionPerformed(ActionEvent e)
	{
		if (!(e instanceof ImmersedActionEvent)) {
			return;
		}
		ImmersedActionEvent immersedActionEvent = (ImmersedActionEvent) e;
		CreateDeviceInfo deviceNode = (CreateDeviceInfo) immersedActionEvent.getContext().get("AddDevice");
		CreateDeviceInfo m_deviceInfo = deviceNode;

		IDesignContainer activeDesignContainer = FactoryMgr.getCAFUtils().getActiveDesignContainer();
		if (activeDesignContainer == null) {
			IMessageContent messageContent = new ResourceBasedMessageContent(Add3dDeviceAction.class,
					"Add3dDeviceAction.Error.DesignNotOpen");
			Message.show(PromptSeverity.ERROR, messageContent);
			return;
		}
		if (activeDesignContainer.getDesignType() != DesignType.LOGICAL) {
			IMessageContent messageContent = new ResourceBasedMessageContent(Add3dDeviceAction.class,
					"Add3dDeviceAction.Error.DesignTypeNotSupported");
			Message.show(PromptSeverity.ERROR, messageContent);
			return;
		}
		ILogicDesign m_logicDesign =
				(ILogicDesign) UIDMgr.getObject(activeDesignContainer.getUID());
		if (m_logicDesign != null) {
			DeviceCreationContext context = new DeviceCreationContext(m_deviceInfo, m_logicDesign);
			IDeviceCreationStrategy strategy = m_strategyRegistry.resolve(context);

			if (strategy == null) {
				IMessageContent messageContent = new ResourceBasedMessageContent(DialogExceptionDisplay.class,
						"Add3dDeviceAction.Error.UnableToPerformAction");
				Message.show(PromptSeverity.ERROR, messageContent);
				return;
			}

			strategy.execute(context);
		}
	}
}
