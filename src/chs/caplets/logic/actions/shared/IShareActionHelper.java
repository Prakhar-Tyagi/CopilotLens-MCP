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

import chs.caf.caplet.action.IActionEnum;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Apr 14, 2004 Time: 9:19:46 AM
 */
public interface IShareActionHelper
{

	@NotNull IActionEnum setup(@NotNull BaseShareActionOperands operands, @Nullable String dialogTitle,
			@Nullable ISchemDiagram diagram);

	boolean doEdit();

	void cleanup();

	/**
	 * Use after edit to check if the action resulted in a new object being created (simple share), or an existing
	 * object being modified (share into)
	 *
	 * @return true if a new shared object was created
	 */
	boolean isNewSharedObject();

	@Nullable IUID getSharedObjectUID();

	/**
	 * Used before edit to determine if an object is newly shared or shared into
	 *
	 * @return true if the object is shared into an object
	 */
	boolean isShareInto();
}
