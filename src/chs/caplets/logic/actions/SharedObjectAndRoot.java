/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions;

import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedObject;
import org.jetbrains.annotations.NotNull;

public record SharedObjectAndRoot(@NotNull ISharedObject sharedObject, @NotNull ISharedLockableUpdateableObject root)
{

}
