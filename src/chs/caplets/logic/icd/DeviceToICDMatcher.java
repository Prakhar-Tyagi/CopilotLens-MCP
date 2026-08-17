/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.icd;

import chs.cof.icd.IDeviceICD;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.ListMap;
import chs.utilities.SubdividedCollection;
import chs.utility.ICDUtils;
import chs.utility.helpers.NamedObjectComparator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Calculate the devices and the corresponding ICDs in a particular diagram
 */
public class DeviceToICDMatcher
{
	@NotNull private ISchemDiagram diagram;

	public DeviceToICDMatcher(@NotNull ISchemDiagram currentDiagram)
	{
		diagram = currentDiagram;
	}

	@NotNull public Map<IDevice, List<IDeviceICD>> getPlacedDeviceToICDsInPrecedence()
	{
		ListMap<IDevice, IDeviceICD> placedDeviceToICDs =
				new ListMap<>(() -> new TreeMap<>(NamedObjectComparator.caseSensitiveComparator()));
		ILogicDesign design = diagram.getDesign();
		if (design == null) {
			return placedDeviceToICDs;
		}
		SubdividedCollection<IDeviceICD> icds = new SubdividedCollection<>(ICDUtils.RoleKeyCollector);
		icds.addAll(design.getDesignICDContainer().getApplicableICDsWithDesignAssociation());
		List<IDeviceICD> icdsWithMatchedRole = new ArrayList<>();
		Set<IDevice> devices = new HashSet<>();
		for (IPinList pinList : diagram.getPinLists()) {
			if (pinList.getConnectivity() instanceof IDevice) {
				IDevice device = (IDevice) pinList.getConnectivity();
				devices.add(device);
				icdsWithMatchedRole.addAll(icds.getSubsetForKey(ICDUtils.getICDMatchName(device)));
			}
		}
		icds = new SubdividedCollection<>(ICDUtils.RoleKeyCollector);
		icds.addAll(ICDUtils.getApplicableICDs(design, icdsWithMatchedRole));
		for (IDevice device : devices) {
			Collection<IDeviceICD> subsetForKey = icds.getSubsetForKey(ICDUtils.getICDMatchName(device));
			for (IDeviceICD icd : subsetForKey) {
				placedDeviceToICDs.add(device, icd);
			}
		}
		return placedDeviceToICDs;
	}
}
