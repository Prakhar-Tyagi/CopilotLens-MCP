/*
 * Copyright 2016-2017 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.inlineassist;

import chs.caplets.topology.inlineassist.IInlineAssistConductor;
import chs.caplets.topology.inlineassist.ILongestSegmentFinder;
import chs.caplets.topology.inlineassist.ILongestSegmentFinderClient;
import chs.cof.drawplus.IConnected;
import chs.cof.logical.cable.IPhysicalConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISegment;
import chs.common.IUID;
import chs.utilities.CHSConstants;
import chs.utilities.CommonUtils;
import chs.utility.topology.utils.InlineAssistUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @see chs.caplets.topology.inlineassist.ILongestSegmentFinder
 */
public class LongestSegmentFinder implements ILongestSegmentFinder
{

	@NotNull private final ILongestSegmentFinderClient mClient;
	@NotNull private final Map<IPhysicalConductor, IInlineAssistConductor> mConductors = new HashMap<>();
	@NotNull private final Collection<IConductor> mTargetConductors;
	@NotNull private final Collection<IgnoredConductorInformation> mIgnoredConductors;

	LongestSegmentFinder(@NotNull ILongestSegmentFinderClient client,
			@NotNull Collection<IInlineAssistConductor> conductors, @NotNull Collection<IConductor> targetConductors,
			@NotNull Collection<IgnoredConductorInformation> ignoredConductors)
	{
		mClient = client;
		InlineAssistUtils.populateConductors(conductors, mConductors);
		mTargetConductors = new ArrayList<>(targetConductors);
		mIgnoredConductors = ignoredConductors;
	}

	@Override @NotNull public Map<IInlineAssistConductor, ISegment> invoke()
	{
		Set<IConductor> usedConductors = new HashSet<>();
		Map<IInlineAssistConductor, ISegment> segmentsToUse = new HashMap<>();

		// We need our own representation of conductor segments that merges out any colinear segments
		// Can we use existing views graph representation?
		for (IConductor targetConductor : mTargetConductors) {
			// If the conductor we are starting to look at is marked as already used - just go to the next one
			if (usedConductors.contains(targetConductor)) {
				continue;
			}
			//If conductor is not visible - do not use it.
			//PDV-11079
			if (!targetConductor.isVisible()) {
				IgnoredConductorInformation ignoredConductorInfo = new IgnoredConductorInformation(targetConductor);
				ignoredConductorInfo.setIgnoredReason(IgnoredConductorInformation.Reason.CONDUCTOR_NOT_VISIBLE);
				mIgnoredConductors.add(ignoredConductorInfo);
				continue;
			}
			// Only graphical conductors with at least one graphical pin can be used as we need it to determine the
			// plug-jack direction.
			if (targetConductor.getPins().isEmpty()) {
				continue;
			}

			// Find the longest segment for current conductor
			double longest = Double.MIN_VALUE;
			ISegment longestSegment = null;
			for (ISegment segment : targetConductor.getSegmentsOfType(ISegment.class)) {
				if (!segment.isOrthogonal()) {
					// Insert does not work on diagonal segments
					continue;
				}
				double segmentLength = segment.getLength();
				// Prefer horizontal segments incase of multiple segments of the same length
				if (segmentLength > longest || (CommonUtils.equals(segmentLength, longest) && segment.isHorizontal())) {
					longest = segmentLength;
					longestSegment = segment;
				}
			}

			// See if segment is long enough to accommodate the smallest possible inline
			if (longestSegment == null || isSegmentNotLongEnough(longestSegment)) {
				IgnoredConductorInformation ignoredConductorInfo = new IgnoredConductorInformation(targetConductor);
				ignoredConductorInfo.setIgnoredReason(IgnoredConductorInformation.Reason.NO_ORTHOGONAL_SEGMENT);
				mIgnoredConductors.add(ignoredConductorInfo);
				continue;
			}

			boolean isVertical = longestSegment.isVertical();

			// Mark current conductor as used, since it is ready for insertion of an inline
			usedConductors.add(targetConductor);

			final IInlineAssistConductor logicalConductor = getLogicalConductor(targetConductor);
			if (logicalConductor == null) {
				IgnoredConductorInformation ignoredConductorInfo = new IgnoredConductorInformation(targetConductor);
				ignoredConductorInfo.setIgnoredReason(IgnoredConductorInformation.Reason.NEED_REFRESH);
				mIgnoredConductors.add(ignoredConductorInfo);
			}
			else {
				mClient.handleInsertion(mTargetConductors, usedConductors, longestSegment, isVertical, mIgnoredConductors);
				final ISegment currentLongest = segmentsToUse.get(logicalConductor);
				if (currentLongest == null || longestSegment.getLength() > currentLongest.getLength()) {
					segmentsToUse.put(logicalConductor, longestSegment);
				}
			}
		}
		return segmentsToUse;
	}

	@Nullable private IInlineAssistConductor getLogicalConductor(@NotNull IConductor conductor)
	{
		final IInlineAssistConductor logicalConductor = mConductors.get(conductor.getConnectivity());
		if (logicalConductor == null) {
			// If there is no match for the connectivity of the conductor, it may be that a modification
			// has been made in another session (changing the UIDObject instance). So check whether the
			// UID of the connectivity would match as this indicates the object has been changed.
			final IUID connectivityUID = conductor.getConnectivity().getUID();
			if (mConductors.keySet().stream()
					.map(IPhysicalConductor::getUID)
					.filter(uid -> uid.equals(connectivityUID))
					.findAny()
					.isPresent()) {
				// return null to indicate this conducotr is out of date
				return null;
			}
			throw new IllegalArgumentException("Logical and graphical conductors are mismatched");
		}
		return logicalConductor;
	}

	protected boolean isSegmentNotLongEnough(IConnected segment)
	{
		return segment.getLength() < CHSConstants.PIN_SPACING * 4;
	}


}
