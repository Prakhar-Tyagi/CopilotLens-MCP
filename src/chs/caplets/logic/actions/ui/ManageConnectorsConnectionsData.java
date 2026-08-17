/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.ui;

import chs.caplets.logic.actions.ManageConnectorConnectionsInfo;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IDesignDescriptor;
import chs.common.IUID;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.SetMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * This class is responsible for processing a connector's connection data, including
 * populating pin diagram usages for a given set of connector connection information.
 */
public class ManageConnectorsConnectionsData
{

	/**
	 * Populates {@code pinDiagramUsages} so that for any original pin in all of the {@code data} we
	 * know the UIDs of the diagrams the pin is used on.
	 * <p>
	 *
	 * @param data Row data
	 */
	public void populatePinDiagramUsages(@NotNull Collection<ManageConnectorConnectionsInfo> data,
			@Nullable ISharedPinList sharedPinList,
			@NotNull SetMap<IPinProxy, IUID> pinDiagramUsages)
	{
		pinDiagramUsages.clear();
		for (ManageConnectorConnectionsInfo info : data) {
			final IDesignDescriptor design = info.getDesign();
			if (design == null) {
				//design could be null for unplaced library cavity pins, unplaced shared pins.
				continue;
			}

			final ILogicDesign logicDesign = (ILogicDesign) design.getDesignContainer();
			if (logicDesign == null) {
				continue;
			}
			final IPinProxy originalPin = info.getOriginalPin();
			final IAbstractPin cablePin = PinProxyHelper.getCablePin(originalPin, logicDesign, sharedPinList);
			if (cablePin != null) {
				final List<IDesignSharedUsage> usages = logicDesign.usagesOf(cablePin);
				for (IDesignSharedUsage usage : usages) {
					pinDiagramUsages.add(originalPin, usage.getDiagramUID());
				}
			}
		}
	}
}
