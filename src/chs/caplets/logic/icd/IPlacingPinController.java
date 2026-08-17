package chs.caplets.logic.icd;

import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISegment;
import chs.common.ILocation;

import java.util.Set;

interface IPlacingPinController
{

	boolean proceedToJoin(Set<IPin> connectedPins);

	ILocation getPlacingPinReferenceLocation();

	boolean canConnect(IPin placedPin);

	boolean canConnect(ISegment segment);

	boolean canSplit(ISegment segment);

	void registerDanglingConnection(IPin placedPin);
}
