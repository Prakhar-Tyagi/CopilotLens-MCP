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

import chs.caplets.topology.inlineassist.IInlineAssistConductor;
import chs.cof.draw.ILine;
import chs.cof.drawplus.IConnected;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.LogicSegment;
import chs.common.ILocation;
import chs.utilities.CHSConstants;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Implementation of advanced inline insertion algorithm.
 */
public class AdvancedConnectorGraphicsCalculator extends LongestSegmentConnectorGraphicsCalculator
{
	
	private static final LogicSegment[] logicSegmentZeroArray = new LogicSegment[0];

	//FIXME this algorithm is not fully implemented yet.
	private AdvancedConnectorGraphicsCalculator()
	{
	}

	@Override
	public Collection<NewConnectorData> getNewConnectorsData(@NotNull Collection<IInlineAssistConductor> conductors,
			@NotNull Collection<IConductor> targetConductors,
			@NotNull Collection<IgnoredConductorInformation> ignoredConductors)
	{
		List<LogicSegment> horizontalSegments = new ArrayList<>();
		List<LogicSegment> verticalSegments = new ArrayList<>();

		processConductors(targetConductors, horizontalSegments, verticalSegments);

		Map<IConductor, InsertionCandidate> horizontalCandidates =
				processLogicSegmentList(targetConductors, horizontalSegments, this::verticalPointSeeker);
		Map<IConductor, InsertionCandidate> verticalCandidates =
				processLogicSegmentList(targetConductors, verticalSegments, this::horizontalPointSeeker);

		Collection<NewConnectorData> result = new HashSet<>();

		return result;
	}

	private Map<IConductor, InsertionCandidate> processLogicSegmentList(Collection<IConductor> targetConductors,
			List<LogicSegment> logicSegments, Consumer<Point> lookupPointMutator)
	{
		Map<IConductor, InsertionCandidate> result = new HashMap<>();
		
		for (int i = 0; i < logicSegments.size(); i++) {
			LogicSegment currentSegment = logicSegments.get(i);
			if (!isSegmentNotLongEnough(currentSegment)) {
				continue;
			}

			ILine line = currentSegment.getLineHolder();
			ILocation currentSegmentStartLocation = line.getStartPoint();
			ILocation currentSegmentEndLocation = line.getEndPoint();
			
			for (int j = i+1; j < logicSegments.size(); j++) {
				LogicSegment currentCandidateSegment = logicSegments.get(i);
				if (!isSegmentNotLongEnough(currentCandidateSegment)) {
					continue;
				}
				
				
			}
		}
		
		return result;
	}

	private void processConductors(Collection<IConductor> targetConductors, List<LogicSegment> horizontalSegments,
			List<LogicSegment> verticalSegments)
	{
		for (IConductor targetConductor : targetConductors) {
			for (IConnected connected : targetConductor.getSegments()) {
				if (connected instanceof LogicSegment) {
					if (connected.isVertical()) {
						verticalSegments.add((LogicSegment) connected);
					}
					else {
						horizontalSegments.add((LogicSegment) connected);
					}
				}
			}
		}

		horizontalSegments.sort(this::compareHorizontal);
		verticalSegments.sort(this::compareVertical);
	}

	private int compareHorizontal(LogicSegment segment1, LogicSegment segment2)
	{
		ILocation left1 = getLeftPoint(segment1.getLineHolder());
		ILocation left2 = getLeftPoint(segment2.getLineHolder());
		
		int y1 = left1.getY();
		int y2 = left2.getY();

		if (y1 > y2) {
			return 1;
		}

		if (y2 > y1) {
			return -1;
		}
		return 0;
	}

	private ILocation getLeftPoint(ILine lineHolder)
	{
		ILocation startLocation = lineHolder.getStartPoint();
		ILocation endLocation = lineHolder.getEndPoint();
		
		if (startLocation.getX() < endLocation.getX()) {
			return startLocation;
		}
		
		return endLocation;
	}

	private int compareVertical(LogicSegment segment1, LogicSegment segment2)
	{
		ILocation top1 = getTopPoint(segment1.getLineHolder());
		ILocation top2 = getTopPoint(segment2.getLineHolder());
		
		int x1 = top1.getX();
		int x2 = top2.getX();

		if (x1 < x2) {
			return 1;
		}

		if (x2 < x1) {
			return -1;
		}
		return 0;
	}

	private ILocation getTopPoint(ILine lineHolder)
	{
		ILocation startLocation = lineHolder.getStartPoint();
		ILocation endLocation = lineHolder.getEndPoint();

		if (startLocation.getY() > endLocation.getY()) {
			return startLocation;
		}

		return endLocation;
	}

	private void verticalPointSeeker(Point point)
	{
		point.translate(0, -CHSConstants.PIN_SPACING);
	}

	private void horizontalPointSeeker(Point point)
	{
		point.translate(CHSConstants.PIN_SPACING, 0);
	}
}
