/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */
package chs.caplets.logic.helpers.backshell;

import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.schem.IPinList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Context object that flow-specific call sites provide to the migration service
 * to convey placement hints, target connector references, and flow-specific behavior flags.
 */
public class BackshellTransferContext
{

	@NotNull private final IConnector targetPlugConnector;
	@NotNull private final Map<IBackshellTermination, IBackshellTermination> sourceTermToTargetTermMap;
	@NotNull private final IDeviceConnector sourceDeviceConnector;
	@NotNull private final IPinList schemDevice;
	@NotNull private final IBackshell sourceBackshell;
	@NotNull private final IBackshell targetBackshell;
	private final boolean processAllInstance;

	public BackshellTransferContext(@NotNull IPinList schemDevice,
			@NotNull IConnector targetPlugConnector,
			@NotNull IBackshell sourceBackshell,
			@NotNull IBackshell targetBackshell,
			@NotNull Map<IBackshellTermination, IBackshellTermination> sourceTermToTargetTermMap,
			boolean processAllInstance)
	{
		//source objects
		this.schemDevice = schemDevice;
		this.sourceBackshell = sourceBackshell;
		sourceDeviceConnector = (IDeviceConnector) sourceBackshell.getOwner();

		//target objects
		this.targetPlugConnector = targetPlugConnector;
		this.sourceTermToTargetTermMap = sourceTermToTargetTermMap;
		this.targetBackshell = targetBackshell;
		this.processAllInstance = processAllInstance;
	}

	@NotNull public IConnector getTargetPlugConnector()
	{
		return targetPlugConnector;
	}

	@NotNull public IPinList getSchemDevice()
	{
		return schemDevice;
	}

	@NotNull public IDeviceConnector getSourceDeviceConnector()
	{
		return sourceDeviceConnector;
	}

	@NotNull public IBackshell getTargetBackshell()
	{
		return targetBackshell;
	}

	@Nullable public IBackshellTermination getTargetTermination(@NotNull IBackshellTermination sourceTerm)
	{
		return sourceTermToTargetTermMap.getOrDefault(sourceTerm, null);
	}

	@NotNull public IBackshell getSourceBackshell()
	{
		return sourceBackshell;
	}

	public boolean isProcessAllInstance()
	{
		return processAllInstance;
	}
}
