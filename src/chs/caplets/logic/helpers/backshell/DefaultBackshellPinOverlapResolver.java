/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.helpers.backshell;

import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caplets.logic.DeleteHelper;
import chs.cof.drawplus.IJoint;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.utility.DiagramHelper;
import chs.utility.helpers.NodeHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A default implementation of IBackshellPinOverlapResolver that resolves pin overlaps
 * by transferring conductors from the backshell pin to the overlapped pin if the overlapped pin is a target termination
 * , and then deletes the backshell pin.
 */
public class DefaultBackshellPinOverlapResolver implements IBackshellPinOverlapResolver
{

	@Override public void resolveOverlappedPins(@NotNull IPin backshellPin, @NotNull IPin overlappedPin)
	{
		// If the coLocated pin is target termination, we will re-use it i.e. transfer conductors to it & we will delete the source targetPin.
		if(overlappedPin.getConnectivity() instanceof IBackshellTermination) {
			transferSchemConductors(backshellPin, overlappedPin);
			deleteSchemPin(backshellPin, false);
		}
	}

	protected static void deleteSchemPin(@NotNull IPin backshellPin, boolean deleteConn)
	{
		ISchemDiagram diagram = DiagramHelper.getDiagram(backshellPin);
		assert diagram != null : "Diagram of the Backshell Pin should not be null";
		DeleteHelper.getInstance().delete(diagram, Set.of(backshellPin), deleteConn);
	}

	protected void transferSchemConductors(@NotNull IPin sourcePin, @NotNull IPin targetPin)
	{
		IJoint sourcePinJoint = sourcePin.getJoint();
		if (sourcePinJoint == null) {
			return;
		}

		IJoint targetPinJoint = targetPin.getJoint();
		if (targetPinJoint == null) {
			targetPinJoint = NodeHelper.createJointAtLocation(targetPin.getAbsolutionLocation());
			targetPin.setJoint(targetPinJoint);
			targetPinJoint.addAssociation(targetPin);
		}

		for (ISegment segment : sourcePinJoint.getAssociations(ISegment.class)) {
			targetPinJoint.addAssociation(segment);
			NodeHelper.replaceEquivSegmentNode(segment, sourcePinJoint, targetPinJoint);
			sourcePinJoint.removeAssociation(segment);
			ConductorRouteAction.getInstance().addConductorForRoute(segment.getConductor());
		}
	}
}
