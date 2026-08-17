/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IBaseSegment;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISegment;
import chs.common.ILocation;
import chs.utility.DiagramHelper;
import chs.utility.EndLineStyleUtils;
import chs.utility.logic.LogicConnectionUtils;
import chs.utility.ui.ArrowPropertiesComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/*
Used for the deleteion of grippoints from conductors
*/
//FEAT 2287.3.23.8.11 - Manage GripPoints

public class DeleteConductorSegmentModifier extends ConductorSegmentModifier
{

	public DeleteConductorSegmentModifier(ICapletController cont)
	{
		super(cont);
	}

	@Nullable public IBaseSegment modifySegment(ILocation location, IBaseSegment segment)
	{
		// gdh 12/26/03 re: 6468 have to be able to restore arrowheads after inserting grip point (breaking the conductor)
		ArrowPropertiesComponent arrows = new ArrowPropertiesComponent(null);
		IDiagramObject conductor = segment.getParent();

		IJoint gripPointJointToDelete = getJointFromLocation(location, segment, DiagramHelper.getDiagram(segment));

		if (gripPointJointToDelete != null) {
			Map<IGfxObject, ILocation> decorativesAbsLocations =
					getDecorativesAbsLocations(segment, gripPointJointToDelete);

			String left = null;
			String right = null;
			if (conductor instanceof IConductor) {
				IConductor cond = (IConductor) conductor;
				left = arrows.getEndStyle(cond, EndLineStyleUtils.LEFT_STYLE);
				right = arrows.getEndStyle(cond, EndLineStyleUtils.RIGHT_STYLE);
			}
			if (LogicConnectionUtils.removeGripPoint(segment, gripPointJointToDelete)) {

				if(!decorativesAbsLocations.isEmpty()){
					/*dts0101312537 - Adding a grip point to a wire sometimes causes the option expression to get placed far away from the wire.
					Now the segments are merged and its location is modified. so on decoratives update the location which is relative to merged segment*/
					setRelativeLocationOnObjects(segment, decorativesAbsLocations);
				}

				refresh(segment);
			}

			if (conductor instanceof IConductor) {
				arrows.applyStyles((IConductor) conductor, (ISegment) segment, left, right);
			}
			return segment;
		}

		return null;
	}

	@NotNull private Map<IGfxObject, ILocation> getDecorativesAbsLocations(IBaseSegment segment,
			IJoint gripPointJointToDelete)
	{
		Map<IGfxObject, ILocation>
				decorativesAbsLocations = new HashMap<>();

		IBaseSegment adjoiningSegment =
				LogicConnectionUtils.getAdjoiningSegment(segment, gripPointJointToDelete);

		if(adjoiningSegment != null) {
			decorativesAbsLocations.putAll(
					getDecorativesAbsoluteLocations(segment));

			decorativesAbsLocations.putAll(
					getDecorativesAbsoluteLocations(adjoiningSegment));
		}
		return decorativesAbsLocations;
	}
}