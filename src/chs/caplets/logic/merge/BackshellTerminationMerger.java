/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.merge;

import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.ISchemDiagram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implements the IBackshellTerminationMerger interface by delegating to a {@link PinlistMerger} instance.
 * This allows the same pin mapping logic to be reused for merging backshell terminations as is used for
 * merging connectivity pins of pinLists.
 * <p>
 *
 * The PinlistMerger is initialized with the source and target backshells, and
 * {@link IMergeActionChangeReporter#NULL_REPORTER}.
 * Method calls are delegated to the PinlistMerger, allowing it to maintain the necessary state for
 * tracking processed schematics and mapping logic objects during the merge process.
 */
public class BackshellTerminationMerger implements IBackshellTerminationMerger
{

	@NotNull private final PinlistMerger pinlistMerger;

	public BackshellTerminationMerger(@NotNull IBackshell sourceBackshell, @NotNull IBackshell targetBackshell)
	{
		pinlistMerger =
				new PinlistMerger(sourceBackshell, targetBackshell, IMergeActionChangeReporter.NULL_REPORTER);
	}

	@Override
	public void mergeBackshellTerminations(@NotNull IBackshell sourceBackshell, @NotNull IBackshell targetBackshell)
	{
		pinlistMerger.mergeAbstractPins(sourceBackshell, targetBackshell);
	}

	@Nullable @Override public ILogicObject getMappedValue(ILogicObject key)
	{
		return pinlistMerger.getMappedValue(key);
	}

	@Override public void addProcessedSchematic(@NotNull IConnectivityRef schemSourceObject)
	{
		pinlistMerger.addProcessedSchematic(schemSourceObject);
	}

	@Override
	public void processSchematicsFor(@NotNull ILogicObject logicObject, @NotNull ISchematicProcessor processor)
	{
		pinlistMerger.processSchematicsFor(logicObject, processor);
	}

	@Override
	public void processSchematicsForDiagram(@NotNull ILogicObject logicObject, @NotNull ISchematicProcessor processor,
			@NotNull ISchemDiagram diagram)
	{
		pinlistMerger.processSchematicsForDiagram(logicObject, processor, diagram);
	}
}
