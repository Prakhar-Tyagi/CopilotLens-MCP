/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic;

import chs.caf.caplet.action.IAction;
import chs.caplets.logic.actions.shared.AddSharedDeviceAction;
import chs.caplets.logic.actions.shared.AddSharedFunctionAction;
import chs.caplets.logic.actions.shared.AddSharedInlineConnectorAction;
import chs.caplets.logic.actions.shared.AddSharedInterconnectConnectorAction;
import chs.caplets.logic.actions.shared.AddSharedInterconnectDeviceAction;
import chs.caplets.logic.actions.shared.AddSharedJackConnectorAction;
import chs.caplets.logic.actions.shared.AddSharedPlugConnectorAction;
import chs.caplets.logic.actions.shared.AddSharedRingTerminalAction;
import chs.caplets.logic.actions.shared.AddSharedSpliceAction;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility to provide action level help for shared objects
 */
public class SharedObjectActionUtil
{

	private static final Map<PinListTypeEnum, Class<? extends IAction>> m_sharedPLActionClasses = new HashMap<>();

	static {
		m_sharedPLActionClasses.put(PinListTypeEnum.TypeSplice, AddSharedSpliceAction.class);
		m_sharedPLActionClasses.put(PinListTypeEnum.TypeDevice, AddSharedDeviceAction.class);
		m_sharedPLActionClasses.put(PinListTypeEnum.TypeJack, AddSharedJackConnectorAction.class);
		m_sharedPLActionClasses.put(PinListTypeEnum.TypePlug, AddSharedPlugConnectorAction.class);
		m_sharedPLActionClasses.put(PinListTypeEnum.TypeInlinePlug, AddSharedInlineConnectorAction.class);
		m_sharedPLActionClasses.put(PinListTypeEnum.TypeInlineJack, AddSharedInlineConnectorAction.class);
		m_sharedPLActionClasses.put(PinListTypeEnum.TypeRingTerminal, AddSharedRingTerminalAction.class);
		m_sharedPLActionClasses.put(PinListTypeEnum.TypeFunction, AddSharedFunctionAction.class);
		m_sharedPLActionClasses.put(PinListTypeEnum.TypeInterconnectDevice, AddSharedInterconnectDeviceAction.class);
		m_sharedPLActionClasses
				.put(PinListTypeEnum.TypeInterconnectConnector, AddSharedInterconnectConnectorAction.class);
	}

	private SharedObjectActionUtil()
	{
	}

	@Nullable public static Class<? extends IAction> determinePinListActionClass(@NotNull ISharedPinList object)
	{
		return m_sharedPLActionClasses.get(object.getType());
	}
}
