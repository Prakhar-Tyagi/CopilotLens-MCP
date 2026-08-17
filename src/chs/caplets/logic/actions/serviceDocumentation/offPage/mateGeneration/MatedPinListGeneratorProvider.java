/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utility.IMessageCollectorAndReporter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * abstraction of mated pin list generator provider for pin lists.
 * expects implementations to implement schematics generator
 */
public class MatedPinListGeneratorProvider implements IMatedPinListGeneratorProvider
{

	private ILogicDesign m_design;
	protected ISchemDiagram m_diagram;
	protected IMessageCollectorAndReporter m_messageReporter;

	public MatedPinListGeneratorProvider(IMessageCollectorAndReporter messageReporter, ISchemDiagram activeDiagram)
	{
		ILogicDesign design = activeDiagram.getDesign();
		assert design != null;
		m_design = design;
		m_diagram = activeDiagram;
		m_messageReporter = messageReporter;
	}

	@NotNull @Override public IMatedPinListGenerator getMatedPinListGenerator(Map<IPin, IPin> pinPairs)
	{
		IMatedPinListReplicator replicator = getReplicator();
		IMatedPinListSchematicsGenerator schematicsGenerator = getSchematicsGenerator();
		IShareIntoExecutor shareIntoExecutor = getShareIntoExecutor();
		return new MatedPinListGenerator(m_design, replicator, schematicsGenerator, shareIntoExecutor, pinPairs);
	}

	@NotNull protected IMatedPinListReplicator getReplicator()
	{
		return new MatedPinListReplicator(m_diagram, m_messageReporter);
	}

	@NotNull protected IMatedPinListSchematicsGenerator getSchematicsGenerator()
	{
		return new SchematicsGenerator();
	}

	@NotNull protected IShareIntoExecutor getShareIntoExecutor()
	{
		return new ShareIntoExecutor(m_diagram, m_messageReporter);
	}
}
