/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.cof.logical.shared.ISharedObject;
import org.jetbrains.annotations.NotNull;

public interface IShareIntoActionHelper extends IShareActionHelper
{

	boolean acceptSharedObject(@NotNull ISharedObject sharedObject);
}
