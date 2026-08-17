/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface IActionChange
{

	@NotNull String getKey();

	@Nullable String getInitialValue();

	@Nullable String getTransformedValue();

	enum ComparisonField
	{

		Attribute,
		Property,
	}

	@NotNull ComparisonField getKeyType();
}
