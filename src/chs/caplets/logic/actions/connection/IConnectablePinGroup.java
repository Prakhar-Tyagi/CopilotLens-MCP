/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.connection;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Represents pins to be connected
 */
public interface IConnectablePinGroup
{

	@NotNull List<IConnectablePin> getConnectablePins();
}