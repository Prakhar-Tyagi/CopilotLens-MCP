/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IBackshellTerminationIterator;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryBackshell;
import chs.cof.parts.ILibraryCavity;
import chs.cof.project.IProject;
import chs.common.IPropertiedObject;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.utility.helpers.PropertyTemplateHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Factory class for creating and managing backshell and backshell terminations.
 */
public class BackshellFactory implements IBackshellProvider, IBackshellTerminationProvider
{

	/**
	 * Creates or retrieves an existing backshell for the given connector.
	 * Handles both shared and non-shared connectors.
	 *
	 * @param connector the connector to get/create backshell for
	 * @return existing or newly created backshell, never null
	 */
	@Override
	@NotNull
	public IBackshell getOrCreateBackshell(@NotNull IConnector connector)
	{
		IBackshell existingBackshell = connector.getBackshell();
		if (existingBackshell != null) {
			return existingBackshell;
		}

		ISharedPinList sharedPinList = connector.getSharedPinList();
		if (sharedPinList instanceof ISharedConnector sharedConnector) {
			return createSharedBackshell(sharedConnector, connector);
		}

		return createNonSharedBackshell(connector);
	}

	/**
	 * Creates a backshell with shared configuration.
	 * Creates both the shared backshell (if it doesn't exist) and the instance backshell.
	 *
	 * @param sharedConnector the shared connector
	 * @param connector       the instance connector
	 * @return newly created instance backshell linked to shared backshell
	 */
	@Override
	@NotNull
	public IBackshell createSharedBackshell(@NotNull ISharedConnector sharedConnector,
			@NotNull IConnector connector)
	{
		// Create the instance backshell
		IBackshell backshell = createNonSharedBackshell(connector);
		ISharedBackshell sharedBackshell = sharedConnector.getBackshell();
		if (sharedBackshell == null) {
			sharedBackshell = createNewSharedBackshell();
			autoAssignPropertiesFromCurrentProject(sharedBackshell);
			sharedBackshell.setIncludeOnBOM(true);
			sharedBackshell.setName(backshell.getName());
			sharedConnector.setBackshell(sharedBackshell);
		}

		backshell.setSharedPinList(sharedBackshell);

		return backshell;
	}

	/**
	 * Creates a non-shared backshell for the given connector.
	 *
	 * @param connector the connector to create backshell for
	 * @return newly created backshell
	 */
	@Override
	@NotNull
	public IBackshell createNonSharedBackshell(@NotNull IConnector connector)
	{
		IUID uid = createUID();
		IBackshell backshell = FactoryMgr.getCablePropertiedFactory().createBackshell(uid);
		connector.setBackshell(backshell);
		return backshell;
	}

	/**
	 * Creates or retrieves an existing backshell termination for the given backshell.
	 * Handles both shared and non-shared connectors.
	 *
	 * @param backshell the backshell to add termination to
	 * @return existing or newly created backshell termination, never null
	 */
	@Override
	@NotNull
	public IBackshellTermination getOrCreateBackshellTermination(@NotNull IBackshell backshell)
	{
		Set<String> existingNames = new HashSet<>();
		IBackshellTerminationIterator existingTerminations = backshell.getBackshellTerminations();
		if (existingTerminations != null) {
			for (IBackshellTermination termination : existingTerminations) {
				existingNames.add(termination.getName());
				if (termination.getNumConductors() == 0) {
					return termination;
				}
			}
		}

		// If backshell has a library part, only create a termination for a free library cavity
		if (backshell.getLibraryObject() instanceof ILibraryBackshell libraryBackshell) {
			Set<ILibraryCavity> cavities = libraryBackshell.getCavities();
			if (cavities != null) {
				Optional<ILibraryCavity> freeCavity = cavities.stream()
						.sorted(Comparator.comparing(ILibraryCavity::getName))
						.filter(cavity -> !existingNames.contains(cavity.getName()))
						.findFirst();
				if (freeCavity.isPresent()) {
					IBackshellTermination newTermination = createBackshellTermination(backshell);
					newTermination.setName(freeCavity.get().getName());
					return newTermination;
				}
			}
		}

		return createBackshellTermination(backshell);
	}

	/**
	 * Creates a backshell termination. Handles both shared and non-shared cases.
	 *
	 * @param backshell the backshell to add termination to
	 * @return newly created backshell termination
	 */
	@Override
	@NotNull
	public IBackshellTermination createBackshellTermination(@NotNull IBackshell backshell)
	{
		ISharedPinList sharedPinList = backshell.getSharedPinList();

		if (sharedPinList instanceof ISharedBackshell sharedBackshell) {
			return createSharedBackshellTermination(sharedBackshell, backshell);
		}

		return createNonSharedBackshellTermination(backshell);
	}

	/**
	 * Creates a backshell termination with shared configuration.
	 *
	 * @param sharedBackshell the shared backshell
	 * @param backshell       the instance backshell
	 * @return newly created instance backshell termination linked to shared termination
	 */
	@Override
	@NotNull
	public IBackshellTermination createSharedBackshellTermination(@NotNull ISharedBackshell sharedBackshell,
			@NotNull IBackshell backshell)
	{
		IBackshellTermination backshellTermination = createNonSharedBackshellTermination(backshell);

		IUID sharedTermUid = createUID();
		ISharedBackshellTermination sharedTermination =
				FactoryMgr.getSharedFactory().createSharedBackshellTermination(sharedTermUid);

		final String terminationName = backshellTermination.getName();
		sharedTermination.setName(terminationName);

		backshellTermination.setSharedPin(sharedTermination);
		sharedBackshell.addBackshellTermination(sharedTermination);

		return backshellTermination;
	}

	/**
	 * Creates a non-shared backshell termination.
	 *
	 * @param backshell the backshell to add termination to
	 * @return newly created backshell termination
	 */
	@Override
	@NotNull
	public IBackshellTermination createNonSharedBackshellTermination(@NotNull IBackshell backshell)
	{
		IUID uid = createUID();
		IBackshellTermination backshellTermination =
				FactoryMgr.getCablePropertiedFactory().createBackshellTermination(uid);
		backshell.addBackshellTermination(backshellTermination);
		return backshellTermination;
	}

	/**
	 * Gets the shared backshell from a backshell instance, if it exists.
	 *
	 * @param backshell the backshell instance
	 * @return the shared backshell, or null if the backshell is not shared
	 */
	@Override
	@Nullable
	public ISharedBackshell getSharedBackshell(@NotNull IBackshell backshell)
	{
		ISharedPinList sharedPinList = backshell.getSharedPinList();
		return sharedPinList instanceof ISharedBackshell ? (ISharedBackshell) sharedPinList : null;
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

	/**
	 * Auto-assigns properties from the current project to a propertied object.
	 *
	 * @param propertiedObject the object to assign properties to
	 */
	private void autoAssignPropertiesFromCurrentProject(@NotNull IPropertiedObject propertiedObject)
	{
		final IProject currentProject = CAFUtils.getInstance().getCurrentProject();
		PropertyTemplateHelper.AssociateAutoAssignProperties(propertiedObject, currentProject, false);
	}
}
