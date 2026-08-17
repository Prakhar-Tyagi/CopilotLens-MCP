/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.graphics.GfxFinder;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IUnconstrainedFrame;
import chs.cof.drawplus.table.ITableGroup;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IInternalLinkPolyline;
import chs.cof.logical.schem.IInternalSchemPin;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPort;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.logical.schem.ISchemStackPin;
import chs.common.IExtent;
import chs.common.ILocation;

public class Finder extends GfxFinder
{

	public Finder(IGfxContext dc, SelectSet selSet, IExtent loc, boolean selectInvisibleObjects)
	{
		super(dc, selSet, loc, selectInvisibleObjects);
	}

	protected boolean postDescend(IDiagramObject sobj)
	{
		return sobj instanceof ILogicSegment || sobj instanceof IPort ||
				sobj instanceof IInternalLinkPolyline || sobj instanceof ISchemInternalLink ||
				(sobj instanceof ITableGroup) || sobj instanceof IUnconstrainedFrame;
	}

	protected boolean preDescend(IGfxObject gobj)
	{
		return gobj instanceof IPin || gobj instanceof IInternalSchemPin || gobj instanceof ISchemStackPin;
	}

	protected void adjustChildHitExtent(IDiagramObject sobj)
	{
		ILocation absLoc = sobj.getAbsLocation(sobj.getLocation().getX(), sobj.getLocation().getY());
		int worldHit = CAFUtils.getInstance().getWorldValue(10, CAFUtils.getInstance().getActiveCapletView());
		m_childHitExtent.setWidth(worldHit);
		m_childHitExtent.setHeight(worldHit);

		m_childHitExtent.setX(absLoc.getX() - (worldHit / 2));
		m_childHitExtent.setY(absLoc.getY() - (worldHit / 2));
	}

	protected boolean excludeFromSelection(IGfxObject obj)
	{
		return obj instanceof IConductor || obj instanceof ISchemInternalLink;
	}
}

