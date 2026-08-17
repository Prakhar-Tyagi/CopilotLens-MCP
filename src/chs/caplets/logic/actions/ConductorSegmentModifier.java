/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.graphics.SegmentModifier;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IBaseSegment;
import chs.cof.drawplus.ICompoundDiagramObject;
import chs.cof.drawplus.IDecorative;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.schem.ISegment;
import chs.common.ILocation;
import chs.utility.helpers.CoordinateHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/*
Used for the management of grippoints in conductors
*/
//FEAT 2287.3.23.8.11 - Manage GripPoints

public abstract class ConductorSegmentModifier extends SegmentModifier
{

	protected ConductorSegmentModifier(ICapletController cont)
	{
		super(cont, ISegment.class);
	}

	public void refresh(IBaseSegment segment)
	{
		super.refresh(segment);

		IDiagramObject diagObj = segment.getParent();

		if (diagObj instanceof ICompoundDiagramObject) {
			ICompoundDiagramObject segmentOwnerBundle = (ICompoundDiagramObject) diagObj;
			segmentOwnerBundle.regenerateDiagramObject(true);
		}
	}

	@NotNull protected Map<IGfxObject, ILocation>
			getDecorativesAbsoluteLocations(@NotNull IBaseSegment segment)
	{
		Collection<? extends IGfxObject>
				decoratives =
				segment.getObjects(IDecorative.class);

		Map<IGfxObject, ILocation> decorativesAbsLocations =
				new HashMap<>(decoratives.size());

		for(IGfxObject eachObj : decoratives) {
			ILocation objAbsLoc = CoordinateHelper.getAbsGfxLocation(
					segment, eachObj.getLocation().getX(), eachObj.getLocation().getY());

			decorativesAbsLocations.put(eachObj, objAbsLoc);
		}

		return decorativesAbsLocations;
	}

	protected void setRelativeLocationOnObjects(
			@NotNull IBaseSegment segment, @NotNull Map<IGfxObject, ILocation> gfxObjsAbsLocations)
	{
		for(IGfxObject eachObj : gfxObjsAbsLocations.keySet()) {
			ILocation objAbsLoc = gfxObjsAbsLocations.get(eachObj);

			ILocation relLocFromSeg =
					CoordinateHelper.getRelativeLocation(segment, objAbsLoc.getX(), objAbsLoc.getY());

			eachObj.setLocation(relLocFromSeg);
		}
	}

}