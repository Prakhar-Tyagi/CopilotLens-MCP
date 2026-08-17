/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.inlineassist;

import org.jetbrains.annotations.NotNull;

/**
 * This class provides data for creation of new inline connector as part of Place Inline flow.
 */
public class NewConnectorData implements INewConnectorData
{

	private InlineExtent extent;
	private InlineDirection direction;

	public NewConnectorData(@NotNull InlineExtent extent, @NotNull InlineDirection direction)
	{
		this.extent = extent;
		this.direction = direction;
	}

	@NotNull public InlineExtent getExtent()
	{
		return extent;
	}

	@NotNull public InlineDirection getDirection()
	{
		return direction;
	}

	@Override public boolean isVertical()
	{
		return getDirection().isVertical();
	}
}
