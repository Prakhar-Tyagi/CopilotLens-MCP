/*
 * Copyright 2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.helpers.BlockAssignmentControl;
import chs.caplets.logic.merge.Mergeable;
import chs.caplets.logic.merge.Merger;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.concurrency.ILogicConcurrencyController;
import chs.common.IDesignContainer;
import chs.utilities.CollectionUtils;
import chs.utility.helpers.NamedObjectComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LogicBlockAssignmentControl extends BlockAssignmentControl
{

	public LogicBlockAssignmentControl(@NotNull IDesignContainer design)
	{
		super(design);
	}

	protected void doBlockAssignment(IDesignContainer currentDesign, Set<IBlockDevice> blkDevsToProcess,
			@Nullable IDesignContainer selectedDesign)
	{
		IBlockDevice existingBlockDevice = null;
		if (currentDesign instanceof ILogicDesign && selectedDesign != null) {
			IConnectivity connectivity = ((IDesign) currentDesign).getConnectivity();
			assert connectivity != null;
			existingBlockDevice =
					connectivity.findBlockDeviceByAssociatedDesignId(selectedDesign.getBaseId());
			if (existingBlockDevice == null && blkDevsToProcess.size() > 1) {
				Set<IBlockDevice> sortedBlkDevices = CollectionUtils.createAndSortSet(
						blkDevsToProcess.iterator(), NamedObjectComparator.caseSensitiveComparator());
				assert !sortedBlkDevices.isEmpty() : "Invalid number of candidate block devices!!!";
				existingBlockDevice = sortedBlkDevices.iterator().next();
			}
		}
		if (existingBlockDevice != null) {
			existingBlockDevice.setAssociatedDesign(selectedDesign);
			blkDevsToProcess.remove(existingBlockDevice);
			for (IBlockDevice blkDev : blkDevsToProcess) {
				Merger merger = Merger.getMerger(blkDev, existingBlockDevice);
				assert merger != null;
				merger.merge();
			}
		}
		else {
			super.doBlockAssignment(currentDesign, blkDevsToProcess, selectedDesign);
		}
	}

	protected Set<IDesignContainer> getDesignsToExclude(@Nullable IPropertiedSet propSet)
	{
		if (propSet == null) {
			return Collections.emptySet();
		}

		Set<IDesignContainer> excludedDesigns = new HashSet<IDesignContainer>();
		if (m_design instanceof ILogicDesign) {
			IConnectivity connectivity = ((IDesign) m_design).getConnectivity();
			assert connectivity != null;
			for (IBlockDevice selectedBlockDevice : getBlockDevicestoProcess(propSet)) {
				for (IBlockDevice existingBlockDevice : connectivity.getBlockDevices()) {
					if (selectedBlockDevice != existingBlockDevice) {
						if (ILogicConcurrencyController.isUnderLogicConcurrencyLimitation(m_design) ||
								Merger.areMergeable(selectedBlockDevice, existingBlockDevice) != Mergeable.Possible) {
							excludedDesigns.add(existingBlockDevice.getAssociatedDesign(null));
						}
					}
				}
			}
		}
		return excludedDesigns;
	}
}
