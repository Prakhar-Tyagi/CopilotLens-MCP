/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared;

import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.cable.ISingleLineEnd;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.IUID;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.SingleLineHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper to support un-share behavior for Single Line
 */
public class UnshareSingleLineActionHelper extends UnshareHighwayActionHelper
{

	public UnshareSingleLineActionHelper(IDesign theDesign)
	{
		super(theDesign);
	}

	@Override public boolean doEdit()
	{
		if (SingleLineHelper.isSingleLine(m_highway)) {
			ISingleLine singleLine = (ISingleLine) m_highway;
			String failureMsg = ResourceMgr.getString(UnshareHighwayActionHelper.class,
					"UnshareConductorActionHelper.UnshareFailureInMU.Message.text");
			ILogicDesign logicDesign = singleLine.getLogicDesign();
			assert logicDesign != null;
			if (!ShareConcurrencyHelper.attemptLockOnSourceSingleLineForShare(singleLine, logicDesign, failureMsg)) {
				return false;
			}
		}
		return super.doEdit();
	}

	/**
	 * This method updates the ends in the single line objects after unsharing an instance of single line.
	 *
	 * @param singleLineSchematic The schematic instance object unshared from existing shared single line object.
	 * @param singleLine          The existing single line associated with shared single line.
	 */
	public static void updateEnds(@NotNull IHighwaySchematic singleLineSchematic, @NotNull ISingleLine singleLine)
	{
		Function<ISingleLine, Set<IUID>> getDesignWideEnds = sl -> {
			return SingleLineHelper.getDesignWideSingleLineEnds(sl)
					.stream()
					.map(IDevice::getUID)
					.collect(Collectors.toSet());
		};

		ISingleLine unsharedSingleLine = (ISingleLine) singleLineSchematic.getConnectivity();
		Set<IUID> unsharedSLEnds = getDesignWideEnds.apply(unsharedSingleLine);
		Set<IUID> existingSLEnds = getDesignWideEnds.apply(singleLine);

		List<ISingleLineEnd> slEnds = singleLine.getEnds().stream().collect(Collectors.toList());
		for (ISingleLineEnd end : slEnds) {
			if (end.getRefID() != null && unsharedSLEnds.contains(end.getRefID())) {
				unsharedSingleLine.addEnd(end.getRefID(), end.getType());
				if (!existingSLEnds.contains(end.getRefID())) {
					singleLine.removeEnd(end.getRefID());
				}
			}
		}
	}
}
