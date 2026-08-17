/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.commands;

import chs.cof.logical.IConvertFilteredNetsToWiresCmd;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.project.IProject;
import chs.common.IDesignContainer;
import chs.common.IUID;
import chs.utilities.SetMap;
import chs.utility.logic.SchemGraphUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A specialized command that converts a specific subset of nets to wires within a project.
 * <p>
 * Unlike the parent {@link ConvertNetsToWiresCmd} which processes all nets, this command
 * operates only on a pre-selected set of net conductors. This is intended for use in
 * delta application scenarios where selective net-to-wire conversion is required as part
 * of the logic-to-wiring abstraction delta application process.
 */
public class ConvertFilteredNetsToWiresCmd extends ConvertNetsToWiresCmd implements IConvertFilteredNetsToWiresCmd
{

	@NotNull private final Set<IUID> mNetsToBeConverted;
	@NotNull private final SetMap<IUID, IUID> mNetToWiresMap = new SetMap<>();

	public ConvertFilteredNetsToWiresCmd(@NotNull IProject proj, @NotNull Set<IUID> selectedNetsForConversion)
	{
		super(proj);
		mNetsToBeConverted = selectedNetsForConversion;
	}

	@Override
	protected boolean isObjectExcluded(@NotNull ILogicObject logicObject)
	{
		return !(logicObject instanceof IMulticore) && !mNetsToBeConverted.contains(logicObject.getBaseId());
	}

	@Override
	protected boolean shouldExitSharedMulticoreConversion()
	{
		return getSharedMulticores().isEmpty();
	}

	// Don't show status messages for this specialized command since it is intended to be used as part of delta
	// application and the user doesn't need the nets conversion details during that.
	@Override
	protected void outputStatusMessage(@NotNull String message, @NotNull String... resourceParameters)
	{
	}

	@Override
	protected void outputNetToWiresMessage(@NotNull final IDesignContainer design, @NotNull final String netName,
			@NotNull final Set<IConductor> schemWireConductors, @NotNull final Set<IPinList> schemSplices)
	{
	}

	@Override
	protected void outputNetToSingleWireMessage(IDesignContainer design, String netName, IConductor schemWireConductor)
	{
	}

	@Override
	protected void outputNetToSingleWireMessage(IDesignContainer design, String netName, String wireName,
			IHighwaySchematic schemWireConductor)
	{
	}

	@NotNull
	@Override
	protected IWireConductor createWireConnectivity(IDesignContainer design, INetConductor netConductor, boolean bShare)
	{
		IWireConductor createdWire = super.createWireConnectivity(design, netConductor, bShare);
		registerCreatedWire(netConductor, createdWire);
		return createdWire;
	}

	@NotNull
	@Override
	protected IWireConductor createWireConductor(@NotNull SchemGraphUtils.SchemEdge edge, @NotNull IConnectivity connectivity,
			@NotNull INetConductor netConductor,
			@NotNull Set<SchemGraphUtils.SchemEdge> edges,
			boolean bAssignMC)
	{
		IWireConductor createdWire = super.createWireConductor(edge, connectivity, netConductor, edges, bAssignMC);
		registerCreatedWire(netConductor, createdWire);
		return createdWire;
	}

	private void registerCreatedWire(@NotNull INetConductor sourceNet, @NotNull IWireConductor createdWire)
	{
		mNetToWiresMap.add(sourceNet.getUID(), createdWire.getUID());
	}

	@NotNull
	@Override
	public SetMap<IUID, IUID> getNetToWiresMap()
	{
		return mNetToWiresMap;
	}

	@Override
	protected boolean handleSharedNetsInSingleDesignScope(@NotNull Set<ISharedConductor> sharedNets)
	{
		return true;
	}
}