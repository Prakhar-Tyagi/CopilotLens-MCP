/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic;

import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import chs.system.UIDMgr;

/**
 * @author chandras on 20-09-2022.
 */
public class IndicatorRefreshModelChangeListener implements IModelChangeListener
{

	private final IUID m_design;

	public IndicatorRefreshModelChangeListener(IUID design)
	{
		m_design = design;
	}

	@Override public void modelPreChanged(ModelChangeEvent e)
	{
		ILogicDesign design = UIDMgr.getObjectOfType(m_design, ILogicDesign.class);
		if (design != null) {
			//do process the loaded diagrams. however process the already registered diagrams also.
			for (ISchemDiagram diagram : design.getDiagrams()) {
				if (diagram.isLoadedInMemory()) {
					IndicatorRefresher.getIndicatorRefresher(diagram).refreshIndicators(e);
				}
			}
		}
	}

	@Override public void modelChanged(ModelChangeEvent e)
	{

	}

	@Override public void removedFromModel()
	{
		ILogicDesign design = UIDMgr.getObjectOfType(m_design, ILogicDesign.class);
		if (design != null) {
			for (ISchemDiagram diagram : design.getDiagrams()) {
				IndicatorRefresher.removeInstance(diagram);
			}
		}
	}
}
