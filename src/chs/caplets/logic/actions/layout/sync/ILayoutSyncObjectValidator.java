/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync;

import chs.cof.logical.cable.ILogicObject;
import org.jetbrains.annotations.NotNull;

public interface ILayoutSyncObjectValidator
{

	@NotNull ILayoutSyncValidationResult validate(@NotNull ILogicObject logicObject);

	boolean accepts(@NotNull ILogicObject logicObject);
}
