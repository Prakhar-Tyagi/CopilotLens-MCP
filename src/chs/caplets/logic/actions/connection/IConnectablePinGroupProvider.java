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

import chs.cof.logical.ILogicDesign;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Implementation of this class should provide all possible candidate pin groups for generating connection.
 */
public interface IConnectablePinGroupProvider
{

	@NotNull List<IConnectablePinGroup> getConnectionCandidates(@NotNull ILogicDesign design);
}
