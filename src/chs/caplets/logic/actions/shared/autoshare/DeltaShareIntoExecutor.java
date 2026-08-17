/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.BaseShareActionOperands;
import chs.caplets.logic.actions.shared.DeltaShareConductorGroupActionHelper;
import chs.caplets.logic.actions.shared.IShareActionHelper;
import chs.caplets.logic.actions.shared.IShareIntoActionHelper;
import chs.caplets.logic.actions.shared.IShareOperandStrategy;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.project.IProject;
import chs.common.INamedUIDObject;
import chs.utilities.Pair;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeltaShareIntoExecutor extends FetchOffPageAutoShareIntoExecutor
{

	public DeltaShareIntoExecutor(@NotNull IProject project, @NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram, @NotNull ISharedObject sharedObject,
			@NotNull IMessageReporterWithContext reporter, @NotNull AutoShareParams params, boolean isNewlyCreatedObj)
	{
		super(project, design, diagram, sharedObject, reporter, params, isNewlyCreatedObj);
	}
	@NotNull @Override
	public IShareOperandStrategy getShareOperandStrategy()
	{
		return new DeltaShareIntoOperandStrategy();
	}

	@Nullable @Override
	protected Pair<INamedUIDObject, IShareActionHelper> determineActionHelper(@NotNull BaseShareActionOperands operands)
	{
		ILogicObject logicObject = operands.getLogicObject();
		assert logicObject != null;
		ILogicDesign logicDesign = logicObject.getLogicDesign();
		assert logicDesign != null;
		if (logicObject instanceof IMulticore){
			IShareIntoActionHelper conductorGrpActionHelper = new DeltaShareConductorGroupActionHelper(
					logicDesign,
					getMessageReporter(),
					m_multicoreHierarchyMap,
					true,
					true);
			if (conductorGrpActionHelper.acceptSharedObject(mSharedObject)) {
				return new Pair<>(logicObject, conductorGrpActionHelper);
			}
			else {
				return null;
			}
		}
		return super.determineActionHelper(operands);
	}
}
