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
import chs.caplets.logic.Model;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IBaseSegment;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.ISegment;
import chs.common.ILocation;
import chs.utility.EndLineStyleUtils;
import chs.utility.logic.LogicConnectionUtils;
import chs.utility.ui.ArrowPropertiesComponent;
import chs.view.route.RoutableUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
Used for the insertion of grippoints into conductors
*/
//FEAT 2287.3.23.8.11 - Manage GripPoints

public class InsertConductorSegmentModifier extends ConductorSegmentModifier implements IInsertConductorSegmentModifier
{

	public InsertConductorSegmentModifier(ICapletController cont)
	{
		super(cont);
	}

	@Nullable public IBaseSegment modifySegment(ILocation location, IBaseSegment segment)
	{
		Model model = (Model) getControllerForNonHeadlessMode().getCapletModel();
		location.setX(model.getDiagram().getGrid().snap(location.getX()));
		location.setY(model.getDiagram().getGrid().snap(location.getY()));

		// gdh 12/26/03 re: 6468 have to be able to restore arrowheads after inserting grip point (breaking the conductor)
		ArrowPropertiesComponent arrows = new ArrowPropertiesComponent(null);
		IDiagramObject conductor = segment.getParent();
		String left = null;
		String right = null;
		if (conductor instanceof IConductor) {
			IConductor cond = (IConductor) conductor;
			left = arrows.getEndStyle(cond, EndLineStyleUtils.LEFT_STYLE);
			right = arrows.getEndStyle(cond, EndLineStyleUtils.RIGHT_STYLE);
		}

		Map<IGfxObject, ILocation>
				decorativesAbsLocation =
				getDecorativesAbsoluteLocations(segment);

		IBaseSegment newSegment = LogicConnectionUtils.insertGripPoint(segment, location);
		if (newSegment != null) {

			if (conductor instanceof IConductor) {
				arrows.applyStyles((IConductor) conductor, (ISegment) newSegment, left, right);
			}
			/*dts0101312537 - Adding a grip point to a wire sometimes causes the option expression to get placed far away from the wire.
			Now the segment's location is modified. so on decoratives update the location which is relative to modified segment*/
			setRelativeLocationOnObjects(segment, decorativesAbsLocation);

			refresh(newSegment);
		}

		return newSegment;
	}

	@NotNull
	public List<ILogicSegment> modifySegment(@NotNull List<ILocation> locations, @NotNull ILogicSegment segment)
	{
		List<ILogicSegment> segments = new ArrayList<>(locations.size());
		ILogicSegment prevSegment = segment;

		for (ILocation location : locations) {
			ILogicSegment newSegment = (ILogicSegment) modifySegment(location, prevSegment);
			if (newSegment == null) {
				ILogicObject logicObject =
						RoutableUtils.getCommonRouteObjectUtils().getSegmentOwnerConnectivity(segment);
				assert logicObject != null;
				assert false : "New segment can not be null when inserting grip point at " + location.toString() +
						" for segment from " + prevSegment.getStartJoint().toString() + " to " +
						prevSegment.getEndJoint().toString() + " of conductor " + logicObject;
				continue;
			}
			segments.add(newSegment);
			prevSegment = newSegment;
		}

		return segments;
	}

	public void setController(@NotNull ICapletController controller)
	{
		m_controller = controller;
	}
}
