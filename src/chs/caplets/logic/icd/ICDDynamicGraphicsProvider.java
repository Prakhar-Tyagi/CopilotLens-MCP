/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.icd;

import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.draw.LineStyle;
import chs.common.ILocation;
import chs.services.dynamicgfx.DynamicGfxFactoryHelper;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxFactory;
import chs.system.FactoryMgr;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.List;

/**
 * Provide dynamic graphics to show signals between pins while placing ICDs or pins on ICD
 */

public class ICDDynamicGraphicsProvider
{

	private ICDDynamicGraphicsProvider()
	{
	}

	public static void showTrace(@NotNull ILocation placingPinLoc, @NotNull ILocation placedPinLoc,
			@NotNull List<IDynamicGfx> dynamicGfxs)
	{
		IDynamicGfx iDynamicGfx = constructTransientLine(placedPinLoc, placingPinLoc);
		dynamicGfxs.add(iDynamicGfx);
	}

	public static void showSplitTrace(@NotNull ILocation startLoc, @NotNull ILocation endLoc,
			@NotNull ILocation breakLoc, @NotNull List<IDynamicGfx> dynamicGfxs)
	{
		IDynamicGfx iDynamicGfx1 = drawTransientLine(startLoc, breakLoc);
		IDynamicGfx iDynamicGfx2 = drawTransientLine(endLoc, breakLoc);
		dynamicGfxs.add(iDynamicGfx1);
		dynamicGfxs.add(iDynamicGfx2);
	}

	@NotNull private static IDynamicGfx drawTransientLine(@NotNull ILocation startLoc, @NotNull ILocation breakLoc)
	{
		IDynamicGfx iDynamicGfx = constructTransientLine(startLoc, breakLoc);
		final IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
		IWritableGfxAttribute lineAttr = FactoryMgr.getDrawFactory().constructGfxAttribute(
				drawFactory.lookupColor("transient"), 1, LineStyle.DASHED);
		iDynamicGfx.setAttribute(lineAttr);
		return iDynamicGfx;
	}

	@NotNull private static IDynamicGfx constructTransientLine(@NotNull ILocation startLoc, @NotNull ILocation breakLoc)
	{
		IDynamicGfxFactory factory = new DynamicGfxFactoryHelper(FactoryMgr.getDrawFactory());
		return factory.constructLine(
				new Point(startLoc.getX(), startLoc.getY()),
				new Point(breakLoc.getX(), breakLoc.getY()),
				new Point(0, 0), true);
	}
}
