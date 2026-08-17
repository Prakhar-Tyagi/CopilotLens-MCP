/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caf.caplet.helpers.snapping;

import chs.caf.caplet.helpers.creation.ISnapController;
import chs.cof.logical.cable.ILogicObject;
import chs.services.dynamicgfx.IDynamicSnap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Control snapping conductor through connector
 */
public interface ISnapThroughConnectorController extends ISnapController
{

	boolean isSnapThroughConnectorEnabled();

	@Nullable ILogicObject getSnapSourceObject();

	boolean overrideLastSnapped();

	default boolean isConnectionAvailable(@NotNull SchemConnectorPlaceholder connectorSnap)
	{
		return true;
	}

	default boolean checkWireCanBeSnapped(@NotNull IDynamicSnap dynamicSnap, @Nullable ILogicObject logicObject)
	{
		return true;
	}

	default void updateCursor(boolean valid)
	{

	}
}