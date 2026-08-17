/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout;

import chs.caf.caplet.ICapletController;
import chs.cof.harness.diagram.DimensionType;
import chs.common.ILocation;
import chs.common.Location;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;

public class LayoutYDimensionAction extends AbstractAxisDimensionAction
{

	public LayoutYDimensionAction(ICapletController controller)
	{
		super(controller);
	}

	@NotNull @Override protected DimensionType getDimensionType()
	{
		return DimensionType.YAXIS;
	}

	@NotNull @Override protected ILocation getRefAxisPoint(@NotNull ILocation loc)
	{
		return new Location(0, loc.getY());
	}

	@Override protected double getDistance(@NotNull Point firstPoint, @NotNull Point secondPoint)
	{
		return Math.abs(secondPoint.getY() - firstPoint.getY());
	}

	@Override public String getActionUIClass()
	{
		return LayoutYDimensionActionUI.class.getName();
	}
}
