/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.helpers.backshell;

import chs.caplets.logic.backshell.IBackshellBuilder;
import chs.cof.logical.ICopyableAttributes;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedBackshellOwner;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IProject;
import chs.common.IPropertiedObject;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.utility.Replicator;
import chs.utility.helpers.PropertyTemplateHelper;
import org.jetbrains.annotations.NotNull;

/**
 * Factory class for creating and managing backshell and backshell terminations.
 */
public class BackshellBuilder implements IBackshellBuilder
{

	private Replicator replicator = new Replicator();
	/**
	 * Creates or retrieves an existing backshell for the given targetConnector.
	 * Handles both shared and non-shared connectors.
	 *
	 * @param sourceBackshell
	 * @param targetConnector       the targetConnector to get/create backshell for
	 * @return existing or newly created backshell, never null
	 */

	@NotNull public IBackshell getOrCreateBackshell(@NotNull IBackshell sourceBackshell,
			@NotNull IConnector targetConnector)
	{
		IBackshell existingBackshell = targetConnector.getBackshell();
		if (existingBackshell != null) {
			return existingBackshell;
		}
		IBackshell newBackshell = null;
		ISharedPinList sharedPinList = targetConnector.getSharedPinList();
		if (sharedPinList instanceof ISharedBackshellOwner sharedConnector) {
			newBackshell = createSharedBackshell(sourceBackshell, sharedConnector, targetConnector);
		}
		else {
			newBackshell = createNonSharedBackshell(sourceBackshell, targetConnector);
		}
		copySymbol(sourceBackshell, newBackshell);
		return newBackshell;
	}

	/**
	 * Creates a backshell with shared configuration.
	 * Creates both the shared backshell (if it doesn't exist) and the instance backshell.
	 *
	 * @param sourceBackshell
	 * @param sharedConnector the shared connector
	 * @param connector       the instance connector
	 * @return newly created instance backshell linked to shared backshell
	 */

	@NotNull private IBackshell createSharedBackshell(@NotNull IBackshell sourceBackshell, @NotNull ISharedBackshellOwner sharedConnector,
			@NotNull IConnector connector)
	{
		// Create the instance backshell
		IBackshell backshell = createNonSharedBackshell(sourceBackshell, connector);
		ISharedBackshell sharedBackshell = sharedConnector.getBackshell();
		if (sharedBackshell == null) {
			sharedBackshell = createNewSharedBackshell();
			autoAssignPropertiesFromCurrentProject(sharedBackshell);
			sharedBackshell.setIncludeOnBOM(true);
			sharedBackshell.setName(backshell.getName());
			replicator.replicateCopyableObject(getSourceForCopy(sourceBackshell), sharedBackshell);
			sharedConnector.setBackshell(sharedBackshell);
		}

		backshell.setSharedPinList(sharedBackshell);
		copySymbol(sourceBackshell, backshell);
		return backshell;
	}

	/**
	 * Creates a non-shared backshell for the given connector.
	 *
	 * @param sourceBackshell
	 * @param connector       the connector to create backshell for
	 * @return newly created backshell
	 */

	@NotNull
	private IBackshell createNonSharedBackshell(@NotNull IBackshell sourceBackshell, @NotNull IConnector connector)
	{
		IUID uid = createUID();
		IBackshell backshell = FactoryMgr.getCablePropertiedFactory().createBackshell(uid);
		backshell.setName(sourceBackshell.getName());
		replicator.replicateCopyableObject(getSourceForCopy(sourceBackshell), backshell);
		connector.setBackshell(backshell);
		return backshell;
	}

	@NotNull
	private ICopyableAttributes getSourceForCopy(@NotNull ILogicObject logicObject)
	{
		ISharedObject sharedObject = logicObject.getSharedObject();
		return sharedObject == null ? logicObject : sharedObject;
	}

	@NotNull
	public  IBackshellTermination getOrCreateTargetTermination(@NotNull IBackshellTermination sourceTermination,
			@NotNull IBackshell targetBackshell)
	{
		// First try exact name match (reuse existing)
		IBackshellTermination existing = targetBackshell.findBackshellTerminationByName(sourceTermination.getName());
		if (existing != null) {
			return existing;
		}

		// Create new termination using BackshellFactory to ensure correct handling of shared vs non-shared backshells
		IBackshellTermination newTermination = createBackshellTermination(sourceTermination, targetBackshell);
		return newTermination;
	}

	/**
	 * Creates a backshell termination. Handles both shared and non-shared cases.
	 *
	 * @param sourceTermination
	 * @param backshell         the backshell to add termination to
	 * @return newly created backshell termination
	 */

	@NotNull
	private IBackshellTermination createBackshellTermination(@NotNull IBackshellTermination sourceTermination, @NotNull IBackshell backshell)
	{
		ISharedPinList sharedPinList = backshell.getSharedPinList();

		if (sharedPinList instanceof ISharedBackshell sharedBackshell) {
			return createSharedBackshellTermination(sourceTermination, sharedBackshell, backshell);
		}

		return createNonSharedBackshellTermination(sourceTermination, backshell);
	}

	/**
	 * Creates a backshell termination with shared configuration.
	 *
	 * @param sourceTermination
	 * @param sharedBackshell   the shared backshell
	 * @param backshell         the instance backshell
	 * @return newly created instance backshell termination linked to shared termination
	 */

	@NotNull
	private IBackshellTermination createSharedBackshellTermination(@NotNull IBackshellTermination sourceTermination, @NotNull ISharedBackshell sharedBackshell,
			@NotNull IBackshell backshell)
	{
		IBackshellTermination backshellTermination = createNonSharedBackshellTermination(sourceTermination, backshell);

		// Obtain or create ISharedBackshellTermination on the shared backshell
		ISharedBackshellTermination sharedTermination = null;
		for (ISharedPin existingSharedTerm : sharedBackshell.getPins()) {
			if (existingSharedTerm.getName().equals(backshellTermination.getName())) {
				sharedTermination = (ISharedBackshellTermination) existingSharedTerm;
				break;
			}
		}

		if (sharedTermination == null) {
			sharedTermination = FactoryMgr.getSharedFactory()
					.createSharedBackshellTermination(FactoryMgr.getCommonFactory().createUID());
			sharedTermination.setName(backshellTermination.getName());
			replicator.replicateCopyableObject(getSourceForCopy(sourceTermination), sharedTermination);
			sharedBackshell.addBackshellTermination(sharedTermination);
		}

		backshellTermination.setSharedPin(sharedTermination);
		return backshellTermination;
	}

	/**
	 * Creates a non-shared backshell termination.
	 *
	 * @param sourceTermination
	 * @param backshell         the backshell to add termination to
	 * @return newly created backshell termination
	 */

	@NotNull
	private IBackshellTermination createNonSharedBackshellTermination(@NotNull IBackshellTermination sourceTermination, @NotNull IBackshell backshell)
	{
		IUID uid = createUID();
		IBackshellTermination backshellTermination =
				FactoryMgr.getCablePropertiedFactory().createBackshellTermination(uid);
		backshellTermination.setName(sourceTermination.getName());
		replicator.replicateCopyableObject(getSourceForCopy(sourceTermination), backshellTermination);
		backshell.addBackshellTermination(backshellTermination);
		return backshellTermination;
	}

	/**
	 * Creates a new shared backshell.
	 *
	 * @return newly created shared backshell
	 */
	@NotNull
	private ISharedBackshell createNewSharedBackshell()
	{
		return FactoryMgr.getSharedFactory().createSharedBackshell(createUID());
	}

	/**
	 * Creates a new UID.
	 *
	 * @return newly created UID
	 */
	@NotNull
	private IUID createUID()
	{
		return FactoryMgr.getCommonFactory().createUID();
	}

	private void copySymbol(@NotNull IBackshell sourceBackshell, @NotNull IBackshell targetBackshell)
	{
		if (sourceBackshell.getSymbolRef() != null) {
			targetBackshell.setSymbolRef(
					FactoryMgr.getSymbolFactory()
							.constructSymbolRefTimestamped(sourceBackshell.getSymbolRef().getSymbolUID(),
									sourceBackshell.getSymbolRef().getTimestamp()));
		}
	}

	/**
	 * Auto-assigns properties from the current project to a propertied object.
	 *
	 * @param propertiedObject the object to assign properties to
	 */
	private void autoAssignPropertiesFromCurrentProject(@NotNull IPropertiedObject propertiedObject)
	{
		final IProject currentProject = FactoryMgr.getCAFUtils().getCurrentProject();
		PropertyTemplateHelper.AssociateAutoAssignProperties(propertiedObject, currentProject, false);
	}

}