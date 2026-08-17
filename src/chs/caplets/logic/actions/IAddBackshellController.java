/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedBackshellOwner;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryBackshell;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryCavity;
import chs.cof.symbol.ISymbolRef;
import chs.common.INamedPropertiedObject;
import chs.common.IUID;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.system.FactoryMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Controller interface that provides required data for add backshell views
 */
public interface IAddBackshellController extends IBackshellUtils
{

	@Nullable default ILibraryBaseObject getLibraryBackshell()
	{
		final IConnector connector = getConnector();
		ISharedBackshellOwner sharedBackshellOwner = (ISharedBackshellOwner) connector.getSharedPinList();
		ILibraryBaseObject libraryObject = null;
		if (sharedBackshellOwner != null) {
			ISharedBackshell sharedBackshell = sharedBackshellOwner.getBackshell();
			if (sharedBackshell != null) {
				libraryObject = sharedBackshell.getLibraryObject();
			}
		}
		else {
			IBackshell backshell = connector.getBackshell();
			if (backshell != null) {
				libraryObject = backshell.getLibraryObject();
			}
		}
		return libraryObject;
	}

	default Set<ILibraryCavity> getLibraryTerminations()
	{
		Set<ILibraryCavity> libraryCavities = new LinkedHashSet<>();

		ILibraryBaseObject libraryObject = getLibraryBackshell();

		if (libraryObject instanceof ILibraryBackshell) {
			ILibraryBackshell libshell = (ILibraryBackshell) libraryObject;
			libraryCavities.addAll(libshell.getCavities());
			return libraryCavities;
		}

		return libraryCavities;
	}

	default Set<ISharedBackshellTermination> getSharedBackshellTerminations()
	{
		Set<ISharedBackshellTermination> sharedBackshellTerminations = new LinkedHashSet<>();

		final ISharedBackshell sharedBackshell = getSharedBackshell();

		if (sharedBackshell != null) {
			for (ISharedBackshellTermination backshellTermination : sharedBackshell.getBackshellTerminations()) {
				sharedBackshellTerminations.add(backshellTermination);
			}
		}

		return sharedBackshellTerminations;
	}

	@Nullable default ISymbolRef getBackshellSymbolRef()
	{
		IBackshell bs = getExistingBackshell();
		ISharedBackshell sbs = getSharedBackshell();
		//
		// If we have a backshell, then get the values.. .
		//
		ISymbolRef symbolRef = null;
		if (bs != null || sbs != null) {

			// get any shared terminations that we may want to add
			if (hasNonDeletedSharedBackshell(sbs)) {
				// Get the shared backshell off the shared connector again, as the refresh wont actually refresh this
				// object, and it may now be out of date
				sbs = getSharedBackshell();
				assert sbs != null;
				symbolRef = sbs.getPinSymbolRef();
			}
			else {
				assert bs != null;
				symbolRef = bs.getSymbolRef();
			}
		}

		return symbolRef;
	}

	default boolean isSharedPinListFrozen()
	{
		final ISharedPinList sharedPinList = getConnector().getSharedPinList();
		return sharedPinList != null && sharedPinList.isFrozen();
	}

	void selectedBackshellTerminations(List<IPinProxy> selectedBackshellTerminations);

	void selectedBackshellName(String backshellName);

	void selectedBackshellSymbol(@Nullable ISymbolRef backshellSymbol);

	default ISharedBackshell createNewSharedBackshell()
	{
		return FactoryMgr.getSharedFactory().createSharedBackshell(getNewUID());
	}

	default ISharedBackshellTermination createnewSharedBackshellTermination()
	{
		return FactoryMgr.getSharedFactory().createSharedBackshellTermination(getNewUID());
	}

	default IBackshell createNewBackshell()
	{
		return FactoryMgr.getCablePropertiedFactory().createBackshell(getNewUID());
	}

	default IBackshellTermination createNewBackshellTermination()
	{
		return FactoryMgr.getCablePropertiedFactory().createBackshellTermination(getNewUID());
	}

	default IUID getNewUID()
	{
		return FactoryMgr.getCommonFactory().createUID();
	}

	@NotNull IBackshell createTemporaryBackshell();

	@NotNull IBackshellTermination createNewTemporaryBackshellTermination(@NotNull IBackshell backshell);

	@NotNull IBackshellTermination createNewTemporaryBackshellTermination();

	@Nullable default String getUnusedDefaultBackshellTerminationName()
	{
		// The modifier ‘Ctrl+B’ will always present the user with the default backshell pin name.
		// Similarly, GUI too should always show default name if there is no instance with the same name
		final String terminationName = getNextDefaultBackshellTerminationName();
		final Optional<INamedPropertiedObject> alreadyExisting = findExistingBackshellTermination(terminationName);

		if (!alreadyExisting.isPresent()) {
			return terminationName;
		}
		return null;
	}

	@NotNull default Object getNextBackshellTermination()
	{
		final String terminationName = getNextDefaultBackshellTerminationName();
		final Optional<INamedPropertiedObject> alreadyExisting = findExistingBackshellTermination(terminationName);

		if (alreadyExisting.isPresent()) {
			//Reuse the existing termination instead of new one
			return alreadyExisting.get();
		}
		return terminationName;
	}

	default String getNextDefaultBackshellTerminationName()
	{
		IBackshellTermination termination = createNewTemporaryBackshellTermination();
		return termination.getName();
	}

	Optional<INamedPropertiedObject> findExistingBackshellTermination(String name);

	void clearTemporaryObjects();

	void removeTemporaryBackshellAndTerminations();

	@NotNull default IBackshell getExistingOrTemporaryBackshell()
	{
		IBackshell backshell = getExistingBackshell();

		if (backshell == null) {

			// No existing backshell, create one temporarily
			backshell = createTemporaryBackshell();

			//And resuse the name of shared backshell, if exists
			final ISharedBackshell sharedBackshell = getSharedBackshell();
			if (sharedBackshell != null) {
				backshell.setName(sharedBackshell.getName());
			}
		}
		return backshell;
	}

	void clearTempoaryBackshell();

	void clearTemporaryTerminations();

	default IPinProxy createPinProxy(Object element)
	{
		IPinProxy pinProxy;
		if (element instanceof IBackshellTermination) {
			pinProxy = new PinProxy((IAbstractPin) element);
		}
		else if (element instanceof ISharedBackshellTermination) {
			pinProxy = new PinProxy((ISharedPin) element);
		}
		else {
			pinProxy = new PinProxy(element.toString(), false);
		}
		pinProxy.setCablePinList(getConnector());

		return pinProxy;
	}
}
