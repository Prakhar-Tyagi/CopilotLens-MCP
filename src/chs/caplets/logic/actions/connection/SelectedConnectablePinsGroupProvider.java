/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.connection;

import chs.caf.CAFUtils;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caplets.logic.actions.CriteriaMatchType;
import chs.caplets.logic.actions.IConnectivityMatchCriteria;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevicePin;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISystemLogicDiagram;
import chs.cof.project.IProject;
import chs.cofUtils.parameterized.PinPlacementHelper;
import chs.common.IDesignAbstraction;
import chs.common.IProperty;
import chs.common.attr.IAttribute;
import chs.utilities.CapabilityHelper;
import chs.utilities.CommonUtils;
import chs.utilities.HybridSet;
import chs.utilities.ListMap;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utilities.SupportedFeatureInfo;
import chs.utility.DiagramHelper;
import chs.utility.logic.PinUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * provide all possible candidate pin groups for generating connection from selected objects
 */
public class SelectedConnectablePinsGroupProvider implements IConnectablePinGroupProvider
{

	private CriteriaProvider criteriaProvider;

	public SelectedConnectablePinsGroupProvider()
	{
		if (CapabilityHelper.supports(SupportedFeatureInfo.Feature.DESIGN_ABSTRACTION)) {
			criteriaProvider = new CriteriaProvider();
		}
		else {
			criteriaProvider = new DerivativeCriteriaProvider();
		}
	}

	@NotNull public Set<IAbstractSchemPin> getSelectedSchemPins()
	{
		ISystemLogicDiagram
				currentDiagram = CommonUtils.cast(CAFUtils.getInstance().getActiveDiagram(), ISystemLogicDiagram.class);
		Set<IAbstractSchemPin> selectedSchemPins = new HashSet<>();
		ISelectMgr selectMgr = CAFUtils.getInstance().getActiveSelectMgr();
		if (currentDiagram == null || selectMgr == null) {
			return selectedSchemPins;
		}

		SelectSet selectSet = selectMgr.getCurrentSelections();
		for (Selection selection : selectSet.getSelected()) {
			IDiagramObject diagramObject = CommonUtils.cast(selection.getObject(), IDiagramObject.class);
			if (diagramObject != null && currentDiagram.equals(DiagramHelper.getDiagram(diagramObject))) {
				addPinsFromSelectedObject(selectedSchemPins, diagramObject);
			}
		}
		return selectedSchemPins;
	}

	private void addPinsFromSelectedObject(@NotNull Set<IAbstractSchemPin> selectedSchemPins,
			@NotNull IDiagramObject diagramObject)
	{
		if (IPinList.class.isAssignableFrom(diagramObject.getClass())) {
			IPinList pinList = (IPinList) diagramObject;
			addPinsFromPinList(selectedSchemPins, pinList);
		}
		if (IAbstractSchemPin.class.isAssignableFrom(diagramObject.getClass())) {
			IAbstractSchemPin pin = (IAbstractSchemPin) diagramObject;
			addSchemPins(selectedSchemPins, pin);
		}
	}

	private void addSchemPins(@NotNull Set<IAbstractSchemPin> selectedSchemPins, @NotNull IAbstractSchemPin pin)
	{
		if (!isValidPinType(pin)) {
			return;
		}
		List<IAbstractSchemPin> connectedAbstractSchemPins =
				PinPlacementHelper.getConnectedAbstractSchemPins(pin);
		if (pin.isDevicePin() && !connectedAbstractSchemPins.isEmpty()) {
			selectedSchemPins.addAll(connectedAbstractSchemPins);
			return;
		}
		selectedSchemPins.add(pin);
	}

	private void addPinsFromPinList(@NotNull Set<IAbstractSchemPin> selectedSchemPins, @NotNull IPinList pinList)
	{
		ListMap<IAbstractSchemPin, IAbstractSchemPin> connectedPins =
				PinPlacementHelper.getConnectedAbstractSchemPins(pinList);
		for (IAbstractSchemPin pin : connectedPins.keySet()) {
			if (!isValidPinType(pin)) {
				continue;
			}
			if (pin.isDevicePin() && connectedPins.contains(pin) && !connectedPins.get(pin).isEmpty()) {
				selectedSchemPins.addAll(connectedPins.get(pin));
				continue;
			}
			selectedSchemPins.add(pin);
		}
	}

	private boolean isValidPinType(@NotNull IAbstractSchemPin pin)
	{
		IPin iPin = CommonUtils.cast(pin, IPin.class);
		if (iPin != null) {
			return !IBlockDevicePin.class.isAssignableFrom(iPin.getConnectivity().getClass()) &&
					PinUtils.isValidPinForConductorConnection(iPin);
		}
		ISchemStackPin stackPin = CommonUtils.cast(pin, ISchemStackPin.class);
		if (stackPin != null) {
			return stackPin.getAllConnectivity().stream()
					.filter(cablePin -> cablePin instanceof IBlockDevicePin)
					.distinct().count() == 0;
		}
		return false;
	}

	@NotNull public List<IConnectablePinGroup> getConnectionCandidates(@NotNull ILogicDesign design)
	{
		List<IConnectablePinGroup> connectionCandidates = new ArrayList<>();

		IDesignAbstraction designAbstraction = design.getDesignAbstraction();
		Set<IConnectablePin> selectedPins = getSelectedConnectablePins();
		List<IConnectivityMatchCriteria> criteria =
				criteriaProvider.getCriteriaForAbstraction(designAbstraction);
		Set<IConnectablePin> pinsAlreadyGrouped = new HashSet<>();
		for (IConnectivityMatchCriteria criterion : criteria) {
			SetMap<String, IConnectablePin> matchingPinGroups = selectedPins.stream()
					.filter(pin -> !pinsAlreadyGrouped.contains(pin) &&
							StringUtils.isNotBlank(getGroupingKey(pin, criterion)))
					.collect(Collectors.groupingBy(pin -> getGroupingKey(pin, criterion), SetMap::new,
							Collectors.toCollection(HybridSet::new)));
			List<String> sortedGroupKeys = matchingPinGroups.keySet().stream().sorted().collect(Collectors.toList());
			for (String groupKey : sortedGroupKeys) {
				Set<IConnectablePin> connectablePinSet = matchingPinGroups.get(groupKey);
				if (connectablePinSet.size() > 1) {
					connectionCandidates.add(new ConnectablePinGroup(connectablePinSet));
					pinsAlreadyGrouped.addAll(connectablePinSet);
				}
			}
		}
		return connectionCandidates;
	}

	@Nullable private String getGroupingKey(IConnectablePin pin, IConnectivityMatchCriteria criterion)
	{
		IAbstractPin applicablePin = getApplicablePin(pin.getPin());
		String criteriaName = criterion.getCriteriaMatchItem().getName();
		if (criterion.getCriteriaMatchType().equals(CriteriaMatchType.ATTRIBUTE)) {
			IAttribute attribute = applicablePin.getAttribute(criteriaName);
			return attribute != null ? attribute.getAsUnformattedString() : null;
		}
		if (criterion.getCriteriaMatchType().equals(CriteriaMatchType.PROPERTY)) {
			IProperty property = applicablePin.findPropertyByName(criteriaName);
			return property != null ? property.getAsUnformattedString() : null;
		}
		return null;
	}

	@NotNull private IAbstractPin getApplicablePin(@NotNull IAbstractPin cablePin)
	{
		if (!(cablePin instanceof IDevicePin) && shouldConsiderDevicePin()) {
			Collection<IAbstractPin> connectedPins = cablePin.getConnectedPins();
			if (connectedPins.size() == 1) {
				IAbstractPin connectedPin = connectedPins.iterator().next();
				return connectedPin instanceof IDevicePin ? connectedPin : cablePin;
			}
		}
		return cablePin;
	}

	private boolean shouldConsiderDevicePin()
	{
		IProject project = CAFUtils.getInstance().getCurrentProject();
		assert project != null;
		return project.getPreferences().getLogicConnectivityMatchBasedOnDevicePin();
	}

	@NotNull private Set<IConnectablePin> getSelectedConnectablePins()
	{
		Set<IAbstractSchemPin> selectedSchemPins = getSelectedSchemPins();
		SetMap<IAbstractPin, IAbstractSchemPin> selectedSchemPinsToConnectivityMap = new SetMap<>();
		for (IAbstractSchemPin selectedSchemPin : selectedSchemPins) {
			for (IAbstractPin cablePin : getCablePins(selectedSchemPin)) {
				selectedSchemPinsToConnectivityMap.add(cablePin, selectedSchemPin);
			}
		}
		Set<IConnectablePin> connectablePins = new HashSet<>();
		for (IAbstractPin pin : selectedSchemPinsToConnectivityMap.keySet()) {
			Set<IAbstractSchemPin> schemPins = selectedSchemPinsToConnectivityMap.get(pin);
			if (!schemPins.isEmpty()) {
				connectablePins.add(new ConnectablePin(pin, schemPins));
			}
		}
		return connectablePins;
	}

	@NotNull private Set<IAbstractPin> getCablePins(@NotNull IAbstractSchemPin selectedSchemPin)
	{
		if (selectedSchemPin instanceof ISchemStackPin) {
			return ((ISchemStackPin) selectedSchemPin).getAllConnectivity().stream().collect(Collectors.toSet());
		}
		if (selectedSchemPin instanceof IPin) {
			return Set.of(((IPin) selectedSchemPin).getConnectivity());
		}
		return Collections.emptySet();
	}
}