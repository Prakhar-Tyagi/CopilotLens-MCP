/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.caplets.logic.actions.serviceDocumentation.offPage.messages.MessageReporterWithContext;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IMultipleConnectivityRef;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IRevisionedObject;
import chs.publisher.offPage.IDesignContentToBeCopied;
import chs.publisher.offPage.IDiagramContentToBeCopied;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class goes through each object in the signal that is going to be fetched into the target diagram and runs
 * checks
 */
class PreFetchChecks
{

	private final List<IDesignContentToBeCopied> copyableDesignContents;
	private final MessageReporterWithContext messageReporter;
	private boolean allChecksPassed = true;

	PreFetchChecks(List<IDesignContentToBeCopied> copyableDesignContents,
			MessageReporterWithContext messageReporter)
	{
		this.copyableDesignContents = copyableDesignContents;
		this.messageReporter = messageReporter;
	}

	boolean runChecks()
	{
		copyableDesignContents
				.forEach(this::checkDesignContent);
		return allChecksPassed;
	}

	private void checkDesignContent(IDesignContentToBeCopied designContentToBeCopied)
	{
		designContentToBeCopied
				.getDiagramContentToBeCopied()
				.forEach(this::checkDiagramContent);
	}

	private void checkDiagramContent(IDiagramContentToBeCopied diagramContentToBeCopied)
	{
		Set<IDiagramObject> diagramObjects = diagramContentToBeCopied.getDiagramObjects();
		Set<ILogicObject> logicObjects = new LinkedHashSet<>();
		SetMap<ISharedObject, ILogicObject> sharedObjects = new SetMap<>();
		diagramObjects
				.forEach((obj) -> {
					populate(obj, logicObjects, sharedObjects);
				});
		checkSharedObject(sharedObjects);
	}

	private void populate(IDiagramObject diagramObject, Set<ILogicObject> logicObjects,
			SetMap<ISharedObject, ILogicObject> sharedObjects)
	{
		getConnectivity(diagramObject)
				.stream()
				.forEach(co -> {
					logicObjects.add(co);
					populateSharedObjects(co, sharedObjects);
				});
	}

	private void populateSharedObjects(ILogicObject logicObject, SetMap<ISharedObject, ILogicObject> sharedObjects)
	{
		Optional
				.ofNullable(logicObject.getSharedObject())
				.ifPresent(so -> {
					sharedObjects.add(so, logicObject);
				});
	}

	private void checkSharedObject(SetMap<ISharedObject, ILogicObject> sharedObjects)
	{
		Set<ISharedObject> allSOs = sharedObjects.keySet();
		Set<ISharedObject> editableSharedObjects = LogicUtils.getEditableSharedObjects(allSOs.iterator());
		if (allSOs.size() == editableSharedObjects.size()) {
			return;
		}
		for (ISharedObject sharedObject : allSOs) {
			if (!editableSharedObjects.contains(sharedObject)) {
				String fullName =
						sharedObject instanceof IRevisionedObject ? ((IRevisionedObject) sharedObject).getFullName() :
								sharedObject.getName();
				messageReporter.report(PromptSeverity.ERROR, ResourceMgr.getString(PreFetchChecks.class,
						"PreFetchChecks.error.sharedobject.notwritable", fullName),
						IMessageContext.createContext(sharedObject));
				allChecksPassed = false;
//				Set<ILogicObject> logicObjects = sharedObjects.get(sharedObject);
//				Iterator<ILogicObject> iterator = logicObjects.iterator();
//				if (iterator.hasNext()) {
//				}
			}
		}
	}

	@NotNull private Set<ILogicObject> getConnectivity(IDiagramObject diagramObject)
	{
		if (diagramObject instanceof IConnectivityRef) {
			ILogicObject connectivity = ((IConnectivityRef) diagramObject).getConnectivity();
			Set<ILogicObject> keys = new LinkedHashSet<>();
			keys.add(connectivity);
			return keys;
		}
		if (IMultipleConnectivityRef.class.isInstance(diagramObject)) {
			return (IMultipleConnectivityRef.class.cast(diagramObject))
					.getAllConnectivity()
					.stream()
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}
		return Collections.emptySet();
	}
}
