/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISystemLogicDesign;
import chs.common.DesignAbstractionType;
import chs.common.IDesignAbstraction;
import chs.utilities.AppInfo;
import chs.utilities.CommonUtils;
import chs.utility.UserPreferenceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * This class stores the logic for visibility & applicability  of AutoGenerateConnector Icon/Action
 */
public class AutoGenerateConnectorSupport
{

	public static final String PREF_KEY_AUTO_GENERATE_CONNECTOR = "AutoGenerateConnector";
	@Nullable private Boolean m_autoGenerateConnectorActive = null;

	public boolean isAutoGenerateConnectorActive(@NotNull ILogicDesign design)
	{
		if (m_autoGenerateConnectorActive == null) {
			m_autoGenerateConnectorActive = checkAutoGenerateConnectorSupport(design) &&
					UserPreferenceUtils.getClassPreferences(Model.class).getBoolean(PREF_KEY_AUTO_GENERATE_CONNECTOR,
							AutoGenerateConnectorPreferenceDefault.getDefault(design));
		}
		return m_autoGenerateConnectorActive;
	}

	public boolean checkAutoGenerateConnectorSupport(@Nullable ILogicDesign design)
	{
		ISystemLogicDesign systemLogicDesign = CommonUtils.cast(design, ISystemLogicDesign.class);
		return systemLogicDesign != null && isDesignAbstractionSupported(systemLogicDesign.getDesignAbstraction()) &&
				!AppInfo.isSvcDoc();
	}

	private boolean isDesignAbstractionSupported(@Nullable IDesignAbstraction designAbstraction)
	{
		if (designAbstraction == null) {
			return true;
		}
		Set<DesignAbstractionType> unSupportedTypes = getUnSupportedAbstractionTypes();
		return !unSupportedTypes.contains(designAbstraction.getType());
	}

	@NotNull
	protected Set<DesignAbstractionType> getUnSupportedAbstractionTypes()
	{
		Set<DesignAbstractionType> unSupportedTypes = EnumSet.noneOf(DesignAbstractionType.class);
		unSupportedTypes.add(DesignAbstractionType.LOGICAL);
		unSupportedTypes.add(DesignAbstractionType.SYTEM_BLOCK);
		unSupportedTypes.add(DesignAbstractionType.FLUID);
		unSupportedTypes.add(DesignAbstractionType.SMART_FLOWS);
		return unSupportedTypes;
	}

	public void setAutoGenerateConnectorToggleState(boolean value)
	{
		m_autoGenerateConnectorActive = null;
		UserPreferenceUtils.getClassPreferences(Model.class).putBoolean(PREF_KEY_AUTO_GENERATE_CONNECTOR, value);
	}

	public void resetAutoGenerateConnectorState()
	{
		m_autoGenerateConnectorActive = null;
	}
}
