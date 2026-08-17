/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout;

import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemOtherComponent;
import chs.common.attr.IAttributeTypes;
import chs.utilities.AppInfo;
import chs.utilities.StringUtils;
import chs.utility.attr.custom.CustomAttributesControl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author chandras on 19-10-2019.
 */
public class DevicePlacementValidityControl implements IDevicePlacementValidityControl
{

	@NotNull private final ISchemDiagram m_diagram;

	public DevicePlacementValidityControl(@NotNull ISchemDiagram diagram)
	{
		m_diagram = diagram;
	}

	@Override public boolean isValidPlacement(@NotNull IDevice device, @NotNull IDevicePlacementItem placementItem,
			@Nullable ISchemOtherComponent snappedMount)
	{
		final ILogicOtherComponent mountConnectivity = snappedMount != null ? snappedMount.getConnectivity() : null;
		if (mountConnectivity != null && !AppInfo.isSEElectrical()) {
			final String mSpec1 = StringUtils.trim(extractMountSpecAsNonNullString(mountConnectivity));
			final String mSpec2 = StringUtils.trim(extractMountSpecAsNonNullString(device));
			return (StringUtils.isBlank(mSpec1) && StringUtils.isBlank(mSpec2)) ||
					StringUtils.equalsIgnoreCase(mSpec1, mSpec2);
		}
		return true;
	}

	@NotNull private String extractMountSpecAsNonNullString(@NotNull ILogicObject logicObject)
	{
		CustomAttributesControl snappedMountControl = new CustomAttributesControl(logicObject);
		return snappedMountControl.getCustomAttributeValueAsNonNullString(IAttributeTypes.LAYOUT_COMP_MOUNT_SPEC);
	}
}
