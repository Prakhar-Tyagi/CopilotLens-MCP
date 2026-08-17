/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-22 Siemens
 */
package chs.caplets.shared;

import chs.caf.caplet.ICapletModel;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.CommonUtils;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author chandras on 08-10-2022.
 */
public class CAFSharedConductorCrossingsUpdater extends SharedConductorCrossingsUpdater
{

	@Nullable protected final ICapletModel m_model;

	public CAFSharedConductorCrossingsUpdater(@NotNull ILogicModel model)
	{
		super(model.getDesign());
		m_model = CommonUtils.cast(model, ICapletModel.class);
	}

	protected void diagramsUpdated(@NotNull Set<ISchemDiagram> newlyChangedDiagrams)
	{
		//check whether the design is editable. In MU mode diagram could be read-only but not the design.
		if (m_model != null && !newlyChangedDiagrams.isEmpty() && m_model.isEditable(null)) {
			m_model.setModified(true);
		}
	}
}
