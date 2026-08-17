/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.merge;

import chs.caplets.logic.actions.JoinPinlistsHelper;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utility.DiagramHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 *  Join two schem pinlists together.
 */
public class PinListSticher
{

	public void stichPinLists(@NotNull IPinList pinlist1, @NotNull IPinList pinList2)
	{
		ISchemDiagram diagram1 = DiagramHelper.getDiagram(pinlist1);
		ISchemDiagram diagram2 = DiagramHelper.getDiagram(pinList2);
		assert (diagram1 == diagram2);

		JoinPinlistsHelper helper = new JoinPinlistsHelper(){
			@Nullable @Override protected IBaseDiagram getDiagram()
			{
				//Use pinlist diagram as applicable diagram
				return diagram1;
			}
		};

		if (helper.isStitchPossibleOnPinlistsSelected(List.of(pinlist1, pinList2)) && helper.hasValidOperand()) {
			helper.completeEdits();
		}
	}
}
