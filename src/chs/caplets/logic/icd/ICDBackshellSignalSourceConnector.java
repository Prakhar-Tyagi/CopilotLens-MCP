/*
* Copyright 2017 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.
*/

package chs.caplets.logic.icd;

import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICDConnector;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceOwnedConnector;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.common.ILocation;
import chs.utilities.CommonUtils;
import chs.utilities.LazyEvaluatedOptional;
import chs.utilities.SetMap;
import chs.utility.ICDUtils;
import chs.utility.IDeviceICDBackshellSignalAssociation;
import chs.utility.IDeviceICDSignalsContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author pbhawsar on 04-04-2017
 */

public class ICDBackshellSignalSourceConnector implements IICDSignalSourceSchemPinlist
{

	@NotNull private IPinList mSchemConnector;
	@NotNull private LazyEvaluatedOptional<Collection<IDeviceICDBackshellSignalAssociation>> mBackshellAssocSignals =
			new LazyEvaluatedOptional<>();
	@NotNull private LazyEvaluatedOptional<IPinList> mOwnerDeviceSchem = new LazyEvaluatedOptional<>();

	public ICDBackshellSignalSourceConnector(@NotNull IPinList schemConn)
	{
		mSchemConnector = schemConn;
	}

	@Nullable public IPin getConnectedSchemHarnConnectorPin(@Nullable IPin pin)
	{
		return pin;
	}

	@Nullable @Override public IPin getSignalMatchingDevicePin(@Nullable String pinName)
	{
		return null;
	}

	@Nullable public IDevice getCableDevice()
	{
		IPinList schemDevice = getSchemDevice();
		return schemDevice != null ? CommonUtils.cast(schemDevice.getConnectivity(), IDevice.class) : null;
	}

	@NotNull @Override
	public Collection<? extends IDeviceICDSignalsContainer> getICDSignalContainers(@NotNull IDeviceICD icd)
	{
		final Collection<IDeviceICDBackshellSignalAssociation> signalAssociations =
				mBackshellAssocSignals.getValue(() -> evaluateBackshellSignalsFromConnector(icd));
		return signalAssociations != null ? signalAssociations : Collections.emptySet();
	}

	@NotNull private Collection<IDeviceICDBackshellSignalAssociation> evaluateBackshellSignalsFromConnector(@NotNull
			IDeviceICD icd)
	{
		final Collection<IDeviceICDBackshellSignalAssociation> backshellAssocSignals = new HashSet<>();
		final IDeviceOwnedConnector connector =
				CommonUtils.cast(mSchemConnector.getConnectivity(), IDeviceOwnedConnector.class);
		if (connector != null) {
			IDevice ownerDevice = CommonUtils.cast(connector.getOwner(), IDevice.class);
			if (ownerDevice != null) {
				SetMap<IICDConnector, IConnector> icdToLogicConnectors = new SetMap<>();
				SetMap<IConnector, IICDConnector> logicToIcdConnectors = new SetMap<>();
				ICDUtils.generateICDvsLogicHarnessConnectorMapping(ownerDevice, icd, icdToLogicConnectors,
						logicToIcdConnectors);
				final Set<IICDConnector> icdConnectors = logicToIcdConnectors.get(connector);
				if (icdConnectors.size() == 1) {
					IICDConnector icdConnector = icdConnectors.iterator().next();
					for (IDeviceICDBackshellSignalAssociation signalAssociation : icd.getICDUsageDefinition()
							.getBackshellSignalAssociations()) {
						if (icdConnector == signalAssociation.getTermination().getBackshell().getConnector()) {
							backshellAssocSignals.add(signalAssociation);
						}
					}
				}
			}
		}
		return backshellAssocSignals;
	}

	@Nullable @Override public ILocation getPinLocation(@Nullable String pinName)
	{
		return null;
	}

	@NotNull @Override public IPinList getSchemPinlist()
	{
		return mSchemConnector;
	}

	@Nullable @Override public IPinList getSchemDevice()
	{
		return mOwnerDeviceSchem.getValue(this::getOwnerICDSchemDevice);
	}

	@Nullable @Override public IPin getEquivalentICDMatchingSignalPin(@Nullable IPin pin)
	{
		return pin;
	}

	@Nullable private IPinList getOwnerICDSchemDevice()
	{
		final IDeviceOwnedConnector connector =
				CommonUtils.cast(mSchemConnector.getConnectivity(), IDeviceOwnedConnector.class);
		if (connector != null) {
			final IDevice ownerDevice = CommonUtils.cast(connector.getOwner(), IDevice.class);
			if (ownerDevice != null) {
				final Optional<IPinList> attachedDevice =
						mSchemConnector.getAttachedPinListObjects().stream()
								.filter(p -> p.getConnectivity() == ownerDevice)
								.findFirst();
				return attachedDevice.isPresent() ? attachedDevice.get() : null;
			}
		}

		return null;
	}
}
