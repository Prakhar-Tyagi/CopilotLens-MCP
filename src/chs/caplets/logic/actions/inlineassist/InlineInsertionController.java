/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.caf.ActionEntry;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionMgr;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.schem.ISystemLogicDiagram;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedPin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * This class provides functionality to insert an inline on the logic diagram using logic actions.
 */
public class InlineInsertionController implements IInlineInsertionController
{

	private static final NewConnectorData[] newConnectorDataZeroArray = new NewConnectorData[0];

	private ActionEntry.ActionUIWedge insertInlineActionUIWedge;
	private InsertInlineConnectorAction insertInlineAction;
	private AutoAddPinListAction autoAddPinListAction;

	private ActionEntry.ActionUIWedge insertSharedInlineActionUIWedge;
	private InsertSharedInlineConnectorAction insertSharedInlineAction;
	private ISystemLogicDiagram mSystemLogicDiagram;

	@NotNull private ShieldAdder shieldAdder;

	public InlineInsertionController(@NotNull ICapletController capletController)
	{
		mSystemLogicDiagram = null;
		insertInlineActionUIWedge = new ActionEntry.ActionUIWedge(
				new InsertInlineConnectorAction.UI(capletController));
		InsertInlineConnectorAction.UI insertInlineActionUI
				= (InsertInlineConnectorAction.UI) insertInlineActionUIWedge.getRealAction();
		insertInlineAction = (InsertInlineConnectorAction) insertInlineActionUI.getAction();

		autoAddPinListAction = new AutoAddPinListAction(capletController);

		insertSharedInlineActionUIWedge = new ActionEntry.ActionUIWedge(
				new InsertSharedInlineConnectorAction.UI(
						capletController));
		InsertSharedInlineConnectorAction.UI insertSharedInlineActionUI
				=
				(InsertSharedInlineConnectorAction.UI) insertSharedInlineActionUIWedge
						.getRealAction();
		insertSharedInlineAction = (InsertSharedInlineConnectorAction) insertSharedInlineActionUI.getAction();

		shieldAdder = new ShieldAdder();
	}

	@Override
	@NotNull public InsertInlineResult insertInline(@NotNull IDiagramConnectorData diagramConnectorData,
			String jackName, String plugName)
	{
		Collection<NewConnectorData> connectorData = getConnectorData(diagramConnectorData);

		NewConnectorData[] newConnectorDataArray = connectorData.toArray(newConnectorDataZeroArray);

		//No valid new connector data found
		if (newConnectorDataArray.length < 1) {
			return new InsertInlineResult(new HashSet<>(), diagramConnectorData.getIgnoredConductors(),
					mSystemLogicDiagram);
		}

		//Insert the new inline initially
		Collection<InsertInlineResult.ResultInlineConnector> insertedConnectors = new HashSet<>();
		InsertInlineResult.ResultInlineConnector newInline =
				insertNewInline(newConnectorDataArray[0], jackName, plugName);
		insertedConnectors.add(newInline);

		//Then insert instances of this new inline if there is data for them
		if (newConnectorDataArray.length > 1) {
			for (int i = 1; i < newConnectorDataArray.length; i++) {
				NewConnectorData newConnectorData = newConnectorDataArray[i];
				InsertInlineResult.ResultInlineConnector resultInlineConnector =
						insertExistingInline(newInline.getLogicalJack(), newConnectorData);
				if (resultInlineConnector != null) {
					insertedConnectors.add(resultInlineConnector);
				}
			}
		}
		return new InsertInlineResult(insertedConnectors, diagramConnectorData.getIgnoredConductors(),
				mSystemLogicDiagram);
	}

	@Override @NotNull
	public InsertInlineResult insertSharedInline(@NotNull IDiagramConnectorData diagramConnectorData,
			@NotNull ISharedConnector sharedConnector, @NotNull Queue<ISharedPin> sharedPins)
	{
		Collection<InsertInlineResult.ResultInlineConnector> insertedConnectors = new HashSet<>();

		Collection<NewConnectorData> connectorData = getConnectorData(diagramConnectorData);
		for (NewConnectorData newConnectorData : connectorData) {
			insertSharedInlineAction.enable();
			insertSharedInlineAction.setSharedConnector(sharedConnector);
			final int numberOfPins = newConnectorData.getExtent().getNumberOfPins();
			// Collect the number of pins that will be added for this instance
			List<ISharedPin> pinsForThisInstance = new ArrayList<>(numberOfPins);
			for (int i = 0; i < numberOfPins; i++) {
				final ISharedPin sharedPin = Objects.requireNonNull(sharedPins.poll());
				pinsForThisInstance.add(sharedPin);
			}
			insertSharedInlineAction.setPins(pinsForThisInstance);
			insertSharedInlineActionUIWedge.actionPerformed(
					new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getClass().getName(), 0));

			insertSharedInlineAction.createConnectorInstance(newConnectorData);
			insertSharedInlineAction.getController().getActionMgr().terminateActiveAction(true, false);
			final InsertInlineResult.ResultInlineConnector resultConnector =
					insertSharedInlineAction.getResultConnector();
			insertedConnectors.add(resultConnector);

			shieldAdder.processShieldConnections(resultConnector, newConnectorData, mSystemLogicDiagram,
					Optional.of(sharedPins));
		}
		return new InsertInlineResult(insertedConnectors, diagramConnectorData.getIgnoredConductors(),
				mSystemLogicDiagram);
	}

	@NotNull
	public InsertInlineResult insertExistingNonSharedInline(@NotNull IDiagramConnectorData diagramConnectorData,
			@NotNull IGenericInlineConnector nonSharedInlineHalf)
	{
		if (nonSharedInlineHalf.getSharedPinList() != null) {
			List<IgnoredConductorInformation> ignoredConductorInformationList = diagramConnectorData.getConductors()
					.stream()
					.map(IgnoredConductorInformation::new)
					.collect(Collectors.toList());
			return new InsertInlineResult(new HashSet<>(), ignoredConductorInformationList, mSystemLogicDiagram);
		}

		Collection<InsertInlineResult.ResultInlineConnector> insertedConnectors = new HashSet<>();
		Collection<NewConnectorData> connectorsData = getConnectorData(diagramConnectorData);
		for (NewConnectorData newConnectorData : connectorsData) {
			InsertInlineResult.ResultInlineConnector resultInlineConnector =
					insertExistingInline(nonSharedInlineHalf, newConnectorData);
			if (resultInlineConnector != null) {
				insertedConnectors.add(resultInlineConnector);
			}
		}
		return new InsertInlineResult(insertedConnectors, diagramConnectorData.getIgnoredConductors(),
				mSystemLogicDiagram);
	}

	@NotNull private Collection<NewConnectorData> getConnectorData(@NotNull IDiagramConnectorData diagramConnectorData)
	{
		return diagramConnectorData.getConnectorData()
				.stream()
				.map(NewConnectorData.class::cast)
				.collect(Collectors.toList());
	}

	@Override public void setDiagram(@NotNull ISystemLogicDiagram diagram)
	{
		mSystemLogicDiagram = diagram;
	}

	@NotNull
	private InsertInlineResult.ResultInlineConnector insertNewInline(@NotNull NewConnectorData newConnectorData,
			String jackName, String plugName)
	{
		insertInlineAction.enable();
		insertInlineActionUIWedge.actionPerformed(
				new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getClass().getName(), 0));

		insertInlineAction
				.setPointsForPlacement(newConnectorData.getExtent().getPoints(), newConnectorData.getDirection());
		insertInlineAction.setPlugName(plugName);
		insertInlineAction.setJackName(jackName);
		insertInlineAction.getController().getActionMgr().terminateActiveAction(true, false);

		InsertInlineResult.ResultInlineConnector resultConnector = insertInlineAction.getResultConnector();

		//create schematic shields on identified pin Points
		shieldAdder.processShieldConnections(resultConnector, newConnectorData, mSystemLogicDiagram,
				Optional.empty());
		return resultConnector;
	}

	@Nullable
	private InsertInlineResult.ResultInlineConnector insertExistingInline(
			@NotNull IGenericInlineConnector nonSharedInlineHalf, @NotNull NewConnectorData newConnectorData)
	{
		if (nonSharedInlineHalf.getSharedPinList() != null) {
			return null;
		}

		autoAddPinListAction.enable();
		autoAddPinListAction.setInlineHalf(nonSharedInlineHalf);

		ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "doubleclick", 0);
		final ICapletController controller = autoAddPinListAction.getController();
		final IActionMgr actionMgr = controller.getActionMgr();
		if (actionMgr == null) {
			return null;
		}
		actionMgr.actionPerformed(autoAddPinListAction, ae);

		autoAddPinListAction.setPoints(newConnectorData.getExtent().getPoints(), newConnectorData.getDirection());
		actionMgr.terminateActiveAction(true, false);

		InsertInlineResult.ResultInlineConnector resultConnector = autoAddPinListAction.getResultConnector();

		if (resultConnector != null) {
			shieldAdder.processShieldConnections(resultConnector, newConnectorData, mSystemLogicDiagram,
					Optional.empty());
		}

		return resultConnector;
	}
}
