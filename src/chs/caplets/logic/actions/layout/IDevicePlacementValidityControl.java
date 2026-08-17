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
import chs.cof.logical.schem.ISchemOtherComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author chandras on 19-10-2019.
 */
public interface IDevicePlacementValidityControl
{

	boolean isValidPlacement(@NotNull IDevice device, @NotNull IDevicePlacementItem placementItem,
			@Nullable ISchemOtherComponent snappedMount);
}
