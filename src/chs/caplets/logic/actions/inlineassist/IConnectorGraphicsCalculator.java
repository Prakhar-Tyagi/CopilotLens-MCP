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
import chs.cof.logical.schem.IConductor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * This interface defines methods to be implemented by classes responsible for inline insertion graphics calculation.
 */
public interface IConnectorGraphicsCalculator
{

	/**
	 * Calculates graphics data needed for creation of new connector.
	 *
	 * @param conductors Collection of connectivity conductors with pin that points to the side that the plug
	 * should be on.
	 * @param targetConductors Collection of conductors to insert connectors into
	 * @param ignoredConductors Collection of conductors that were ignored in the process.
	 * @return Collection of new connector parameters.
	 */
	Collection<NewConnectorData> getNewConnectorsData(@NotNull Collection<IInlineAssistConductor> conductors,
			@NotNull Collection<IConductor> targetConductors,
			@NotNull Collection<IgnoredConductorInformation> ignoredConductors);

}
