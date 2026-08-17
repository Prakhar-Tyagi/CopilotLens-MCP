/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.ISharedPin;
import chs.common.IDesignDescriptor;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ManageConnectorPinDuplicationFinder
{

	private ManageConnectorPinSelections manageConnectorPinSelections;
	private BiConsumer<Collection<String>, DuplicateReason> duplicatePinSelected;
	private Map<IDesignDescriptor, InstanceCountPerDesign> instanceCount;
	//only the changes in dialog can result in automatic pins in two different designs.
	//Example change PIN1 to PIN3 using combobox selection.
	//Now Swap PIN3 with PIN5 in another design. This will result in PIN3 in both the designs.
	private Map<ISharedPin, SharedPinCount> automaticPinNumDesigns;

	public enum DuplicateReason
	{

		None(""),
		DuplicatePins(ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.duplicatepins")),

		AutomaticPinsInMultipleDesigns(ResourceMgr.getString(ManageConnectorsAction.class,
				"ManageConnectorsAction.dialog.AutomaticPinsInDifferentDesigns"));

		private String disableReason;

		DuplicateReason(String value)
		{
			disableReason = value;
		}

		public String toString()
		{
			return disableReason;
		}

	}

	public ManageConnectorPinDuplicationFinder(ManageConnectorPinSelections manageConnectorPinSelections,
			BiConsumer<Collection<String>, DuplicateReason> duplicatePinSelected)
	{
		this.manageConnectorPinSelections = manageConnectorPinSelections;

		instanceCount = new HashMap<>();
		automaticPinNumDesigns = new HashMap<>();
		this.duplicatePinSelected = duplicatePinSelected;
	}

	public boolean isDuplicate(IPinProxy pin, IDesignDescriptor designDescriptor)
	{

		InstanceCountPerDesign instanceCountPerDesign = instanceCount.get(designDescriptor);

		boolean isDuplicatePin = (instanceCountPerDesign != null && instanceCountPerDesign.isDuplicate(pin));
		if (isDuplicatePin) {
			return true;
		}

		ISharedPin sharedPin = pin.getSharedPin();
		SharedPinCount sharedPinCount = automaticPinNumDesigns.get(sharedPin);
		if (sharedPinCount != null) {
			return sharedPinCount.isDuplicate();
		}
		return false;
	}

	public void updateActivePins(ManageConnectorConnectionsInfo item, Object oldValue, Object newValue)
	{

		IDesignDescriptor thisRowDesign = item.getDesign();
		if (oldValue == null || newValue == null || thisRowDesign == null ||
				oldValue.toString().equals(newValue.toString())) {
			return;
		}
		IPinProxy originalPin =
				manageConnectorPinSelections.getPinByName(oldValue.toString(), thisRowDesign);
		IPinProxy currentPin = manageConnectorPinSelections.getPinByName(newValue.toString(), thisRowDesign);
		if (originalPin == null || currentPin == null) {
			return;
		}

		IDesignDescriptor design = item.getDesign(); //we update only those pins that have a conductor
		//so these rows should all have a design returned.
		if (design != null) {
			if (currentPin.getSharedPin() != null) {
				ISharedPin originalSharedPin = originalPin.getSharedPin();
				ISharedPin currentPinSharedPin = currentPin.getSharedPin();
				if (currentPinSharedPin.getReservationType() == ISharedPin.ReservationType.AUTOMATIC) {
					SharedPinCount currentSharedPinDesigns =
							automaticPinNumDesigns.get(currentPinSharedPin);
					if (currentSharedPinDesigns == null) {
						currentSharedPinDesigns = new SharedPinCount();
						automaticPinNumDesigns.put(currentPinSharedPin, currentSharedPinDesigns);
					}
					currentSharedPinDesigns
							.add(design, manageConnectorPinSelections.isSharedPinPresent(newValue.toString(), design));
				}
				if (originalSharedPin.getReservationType() == ISharedPin.ReservationType.AUTOMATIC) {
					SharedPinCount originalSharedPinDesigns =
							automaticPinNumDesigns.get(originalSharedPin);
					if (originalSharedPinDesigns != null) {
						originalSharedPinDesigns.remove(design);
					}
				}
			}
			InstanceCountPerDesign instanceCountPerDesign = instanceCount.get(design);
			if (instanceCountPerDesign == null) {
				instanceCountPerDesign = new InstanceCountPerDesign(design, manageConnectorPinSelections);
				instanceCount.put(design, instanceCountPerDesign);
			}
			instanceCountPerDesign.updateInstance(item, originalPin, currentPin);

			if (duplicatePinSelected != null) {
				Collection<String> sharedPinNames = new ArrayList<>();
				for (ISharedPin aSharedPin : automaticPinNumDesigns.keySet()) {
					if (automaticPinNumDesigns.get(aSharedPin).isDuplicate()) {
						sharedPinNames.add(aSharedPin.getName());
					}
				}
				DuplicateReason duplicateReason = DuplicateReason.None;
				if (!sharedPinNames.isEmpty()) {
					duplicateReason = DuplicateReason.AutomaticPinsInMultipleDesigns;
					duplicatePinSelected.accept(sharedPinNames, duplicateReason);
				}
				else {

					List<String> duplicatePins = new ArrayList<>();
					instanceCount.values().forEach(anInstanceCountOfDesign -> {
						duplicatePins.addAll(anInstanceCountOfDesign.getDuplicatePins());
					});
					if (!duplicatePins.isEmpty()) {
						duplicateReason = DuplicateReason.DuplicatePins;

						Collections.sort(duplicatePins);
						duplicatePinSelected.accept(new LinkedHashSet<>(duplicatePins), duplicateReason);
					}
				}
				if (duplicateReason.equals(DuplicateReason.None)) {
					duplicatePinSelected.accept(Collections.emptyList(), duplicateReason);
				}
			}
		}
	}

	private static class SharedPinCount
	{

		private Map<IDesignDescriptor, Integer> instanceCout;

		SharedPinCount()
		{
			instanceCout = new HashMap<>();
		}

		boolean isDuplicate()
		{
			int numDesignsPresent = 0;
			for (IDesignDescriptor aDesign : instanceCout.keySet()) {
				if (instanceCout.get(aDesign) > 0) {
					numDesignsPresent++;
				}
			}
			return numDesignsPresent > 1;
		}

		void add(IDesignDescriptor designDescriptor, boolean isSharedPinPresentInDesign)
		{
			Integer countInDesign = instanceCout.get(designDescriptor);
			if (countInDesign == null) {
				if (isSharedPinPresentInDesign) {
					instanceCout.put(designDescriptor, 2);
				}
				else {
					instanceCout.put(designDescriptor, 1);
				}
			}
			else {
				instanceCout.put(designDescriptor, countInDesign + 1);
			}
		}

		void remove(IDesignDescriptor designDescriptor)
		{
			Integer countInDesign = instanceCout.get(designDescriptor);
			if (countInDesign != null) {
				int reqCount = countInDesign > 0 ? countInDesign - 1 : 0;
				instanceCout.put(designDescriptor, reqCount);
			}
		}
	}

	private static class InstanceCountPerDesign
	{

		private Map<IPinProxy, Integer> instanceCount;
		private ILogicDesign logicDesign;
		private boolean disallowPinDuplication;
		private List<String> duplicatePins;
		private ManageConnectorPinSelections manageConnectorPinSelections;

		InstanceCountPerDesign(IDesignDescriptor designDescriptor, ManageConnectorPinSelections manageConnectorPinSelections)
		{
			logicDesign = (ILogicDesign) designDescriptor.getLoadedDesignContainer();
			disallowPinDuplication = logicDesign.getProject().getPreferences().getDisallowLogicPinDuplication();
			instanceCount = new LinkedHashMap<>();
			duplicatePins = new LinkedList<>();
			this.manageConnectorPinSelections = manageConnectorPinSelections;
		}

		void updateInstance(ManageConnectorConnectionsInfo item, IPinProxy originalPin, IPinProxy currentPin)
		{

			if (originalPin == null || currentPin == null) {
				return;
			}
			Integer originalPinCount =
					instanceCount.get(originalPin) != null ? instanceCount.get(originalPin) : 0;
			Integer currentPinInstanceCount =
					instanceCount.get(currentPin) != null ? instanceCount.get(currentPin) :
							getNumInstances(currentPin);

			Integer numInstancesUpdated =
					getNumInstances(manageConnectorPinSelections.getPinByName(item.getOriginalValue(), logicDesign));
			Integer updatedOriginalPinCount = originalPinCount - numInstancesUpdated;

			instanceCount.put(currentPin, currentPinInstanceCount + numInstancesUpdated);
			instanceCount.put(originalPin, (updatedOriginalPinCount > 0 ? updatedOriginalPinCount : 0));
			duplicatePins =
					instanceCount.keySet().stream().filter(aPin -> isDuplicate(aPin)).map(aPin -> aPin.getName())
							.collect(
									Collectors.toList());
		}

		private Integer getNumInstances(@Nullable  IPinProxy pinProxy)
		{

			int numUsages = 0;
			if (pinProxy != null && disallowPinDuplication) {
				IAbstractPin cablePin = pinProxy.getCablePin();
				ISharedPin sharedPin = pinProxy.getSharedPin();

				if (cablePin != null) {
					List<IDesignSharedUsage> usages =
							logicDesign.getDesignWideUsageMgr().getUsages(cablePin);
					numUsages = usages.size();
				}
				else if (sharedPin != null) {
					numUsages = logicDesign.getSharedUsageMgr().getUsages(sharedPin).size();
				}
			}
			return numUsages;
		}

		boolean isDuplicate(IPinProxy pin)
		{
			Integer updatedInstanceCount = instanceCount.get(pin);
			return updatedInstanceCount != null && updatedInstanceCount > 1;
		}

		List<String> getDuplicatePins()
		{

			return duplicatePins;
		}
	}
}
