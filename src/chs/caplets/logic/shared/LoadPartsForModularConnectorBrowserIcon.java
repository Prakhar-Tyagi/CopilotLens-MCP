/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.caplets.logic.shared;

import chs.cof.library.ILibrariedObject;
import chs.cof.logical.cable.IInternalPositionedObject;
import chs.cof.logical.shared.ISharedInternalPosition;
import chs.cof.logical.shared.ISharedModularConnector;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.parts.IInternalLibraryObject;
import chs.cof.parts.ILibraryHousingDefinition;
import chs.cofUtils.parts.LibraryBatchLoader;
import chs.common.IUID;
import chs.common.attr.IAttributeTypes;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Batch load library parts and housing definitions required for shared browser tree icon calculation.
 * Modular connectors do not hold the blocked position information and needs library part to calculate this information.
 * This upfront loading of library parts avoids sequential loading while building the browser tree.
 */
class LoadPartsForModularConnectorBrowserIcon
{

	private final SharedObjectsByAbstraction sharedObjectsByAbstraction;

	LoadPartsForModularConnectorBrowserIcon(@NotNull SharedObjectsByAbstraction sharedObjectsByAbstraction)
	{
		this.sharedObjectsByAbstraction = sharedObjectsByAbstraction;
	}

	public void loadParts()
	{
		Set<IUID> libraryPartsToLoad = new HashSet<>();
		Set<IUID> housingDefinitionOwners = new HashSet<>();

		for (ISharedModularConnector sharedConnector : getModularConnectors()) {
			Set<IUID> positionedObjectsToLoad = new HashSet<>();
			for (ISharedInternalPosition position : sharedConnector.getPositions()) {
				for (IInternalPositionedObject<?> positionedObject : position.getPositionedObjects()) {
					addLibraryRefToCollection(positionedObjectsToLoad, positionedObject);
				}
			}
			if (!positionedObjectsToLoad.isEmpty()) {
				libraryPartsToLoad.addAll(positionedObjectsToLoad);
				addLibraryRefToCollection(libraryPartsToLoad, sharedConnector);
				addLibraryRefToCollection(housingDefinitionOwners, sharedConnector);
			}
		}

		loadLibraryParts(libraryPartsToLoad);
		loadHousingDefinitions(housingDefinitionOwners);
	}

	protected void loadLibraryParts(@NotNull Collection<IUID> libraryPartsToLoad)
	{
		FactoryMgr.getSystemFactory().getCHSSystem().getPartsLibrary().getLibraryObjects(libraryPartsToLoad);
	}

	private void loadHousingDefinitions(@NotNull Set<IUID> housingDefinitionOwners)
	{
		Set<IUID> housingDefinitionsOwnersToLoad = getHousingDefinitionOwnersToLoad(housingDefinitionOwners);
		LibraryBatchLoader.createInstance().loadChildren(housingDefinitionsOwnersToLoad, ILibraryHousingDefinition.class, IAttributeTypes.OWNER);
	}

	@NotNull protected Set<IUID> getHousingDefinitionOwnersToLoad(@NotNull Set<IUID> housingDefinitionOwners)
	{
		Set<IUID> housingDefinitionsOwnersToLoad = new HashSet<>();
		for (IUID owner : housingDefinitionOwners) {
			IInternalLibraryObject internalLibraryObject = UIDMgr.getObjectOfType(owner, IInternalLibraryObject.class);
			if (internalLibraryObject != null) {
				Collection<IUID> housingDefinitionsUIDs = internalLibraryObject.getHousingDefinitionsUIDs();
				if (housingDefinitionsUIDs == null) {
					housingDefinitionsOwnersToLoad.add(owner);
					continue;
				}
				for (IUID housingDefinitionsUID : housingDefinitionsUIDs) {
					ILibraryHousingDefinition housingDefinition =
							UIDMgr.getObjectOfType(housingDefinitionsUID, ILibraryHousingDefinition.class);
					if (housingDefinition == null) {
						housingDefinitionsOwnersToLoad.add(owner);
						break;
					}
				}
			}
		}
		return housingDefinitionsOwnersToLoad;
	}

	private void addLibraryRefToCollection(@NotNull Set<IUID> collection, @NotNull ILibrariedObject libraryObject)
	{
		IUID libraryRef = libraryObject.getLibraryRef();
		if (libraryRef != null) {
			collection.add(libraryRef);
		}
	}

	@NotNull private Collection<ISharedModularConnector> getModularConnectors()
	{
		Set<PinListTypeEnum> types = Set.of(PinListTypeEnum.TypePlug, PinListTypeEnum.TypeJack);
		List<ISharedPinList> sharedConnectors = sharedObjectsByAbstraction.getAllSharedPinListsOfType(types);
		return sharedConnectors.stream().filter(conn -> conn instanceof ISharedModularConnector)
				.map(ISharedModularConnector.class::cast).collect(Collectors.toList());
	}
}
