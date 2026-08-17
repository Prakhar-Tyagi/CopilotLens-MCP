/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

/**
 * Fixed set of columns in harness propagate window
 */
public enum HarnessPropagateColumn
{
	Propagate("Propagate"),
	Severity("Severity"),
	Object("Object"),
	ObjectType("ObjectType"),
	Message("Message"),
	Before("Before"),
	After("After"),
	Design("Design");

	private String name;

	@NotNull public String getName()
	{
		return name;
	}

	@NotNull public String getDisplayName()
	{
		return ResourceMgr.getString(HarnessUpdateStatusMessageTableModel.class,
				"HarnessPropagateColumn." + name + ".title");
	}

	HarnessPropagateColumn(String name)
	{
		this.name = name;
	}
}
