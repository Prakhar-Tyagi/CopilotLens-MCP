/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024-2026 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caf.helpers.ConnectSegmentToSBHookup;
import chs.cof.drawplus.IConnected;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IPrivilegedSegment;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.cable.SpliceTypeEnum;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cofUtils.cmd.CreateSchemConductorCmd;
import chs.common.ILocation;
import chs.view.assist.AbstractConnectionCreator;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class is responsible for terminating the shield conductor via pigtail wires to the inline half pin or backshell pin.
 */
public class PigTailConnector implements IShieldConnector
{

	@Override public void connectShield(@NotNull InlineHalfShieldTerminationParams terminationParams)
	{
		IPinList schemInlineHalf = terminationParams.getSchemInlineHalf();
		IShieldConductor inlineHalfShield = terminationParams.getInlineHalfShield();
		IPin schemInlineHalfPin = terminationParams.getSchemInlineHalfPin();
		ISchemDiagram diagram = schemInlineHalf.getDiagram();
		ILogicDesign design = diagram.getDesign();
		assert design != null;

		ILocation pinAbsLocation = schemInlineHalfPin.getAbsLocation();
		Point inlineHalfPinPoint = new Point(pinAbsLocation.getX(), pinAbsLocation.getY());

		IShieldBodyHookup sbHookup =
				AbstractConnectionCreator.getNearestShieldHookup(inlineHalfShield, pinAbsLocation, diagram);
		if (sbHookup == null) {
			return;
		}
		ILocation sbHookupAbsLocation = sbHookup.getAbsLocation();
		Point hookupPoint = new Point(sbHookupAbsLocation.getX(), sbHookupAbsLocation.getY());

		List<ISegment> segments = new ArrayList<>();
		IConductor schemPigTailWire = CreateSchemConductorCmd
				.createConductor(design, diagram, List.of(inlineHalfPinPoint, hookupPoint),
						IWireConductor.class, segments, null, null, true);

		if (schemPigTailWire != null) {
			Set<IConnected> schemWireSegments = schemPigTailWire.getSegments();
			if (!schemWireSegments.isEmpty()) {
				IPrivilegedSegment pigTailWireSegment = (IPrivilegedSegment) schemWireSegments.iterator().next();
				pigTailWireSegment.connectPin(schemInlineHalfPin, inlineHalfPinPoint);

				ConnectSegmentToSBHookup connectSegmentToSBHookup = new ConnectSegmentToSBHookup();
				connectSegmentToSBHookup
						.handleShieldBodyHookup(sbHookup, pigTailWireSegment, hookupPoint,
								SpliceTypeEnum.TYPE_SOLDER_SLEEVE);
			}
		}
	}
}
