/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.modular;

import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.IMatedPinListGeneratorProvider;
import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.IMatedPinListSchematicsGenerator;
import chs.caplets.logic.actions.serviceDocumentation.offPage.mateGeneration.MatedPinListGeneratorProvider;
import chs.caplets.logic.actions.serviceDocumentation.offPage.modular.ModularConnectorHierarchyBasedComparator;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUIDObject;
import chs.utility.IMessageCollectorAndReporter;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * mated connector generator provider for modular connectors
 * pin pair transformer will sort the pin pairs in the order in which the modular connectors need to be shared
 * schematic generator implementation has a post processing step needed for modular connectors schematic generation
 */
public class MatedModularConnectorGeneratorProvider extends MatedPinListGeneratorProvider
{

	public MatedModularConnectorGeneratorProvider(IMessageCollectorAndReporter messageReporter,
			ISchemDiagram activeDiagram)
	{
		super(messageReporter, activeDiagram);
	}

	@NotNull @Override protected IMatedPinListSchematicsGenerator getSchematicsGenerator()
	{
		return new ModularConnectorSchematicsGenerator();
	}

	@NotNull @Override public Function<Map<IPin, IPin>, Map<IPin, IPin>> getPinPairTransformer()
	{
		return (params) -> {
			return params
					.entrySet()
					.stream()
					.sorted(getComparator(params))
					.collect(Collectors
							.toMap(Map.Entry::getKey, Map.Entry::getValue, (u, v) -> u, LinkedHashMap::new));
		};
	}

	@NotNull private Comparator<Map.Entry<IPin, IPin>> getComparator(Map<IPin, IPin> params)
	{
		Set<IPinList> owners = params
				.entrySet()
				.stream()
				.map(IMatedPinListGeneratorProvider::getOwner)
				.collect(Collectors.toSet());
		Map<IUIDObject, ISharedObject> map = owners
				.stream()
				.filter(owner -> owner.getSharedPinList() != null)
				.collect(Collectors.toMap(Function.identity(), IPinList::getSharedPinList));
		ModularConnectorHierarchyBasedComparator comparator = new ModularConnectorHierarchyBasedComparator(map);
		return new Comparator<Map.Entry<IPin, IPin>>()
		{
			@Override public int compare(Map.Entry<IPin, IPin> o1, Map.Entry<IPin, IPin> o2)
			{
				IPin fetchedSchematicPin1 = o1.getKey();
				IPin fetchedSchematicPin2 = o2.getKey();
				IAbstractPin connectivity1 = fetchedSchematicPin1.getConnectivity();
				IAbstractPin connectivity2 = fetchedSchematicPin2.getConnectivity();
				IPinList owner1 = connectivity1.getOwner();
				IPinList owner2 = connectivity2.getOwner();
				assert owner1 != null;
				assert owner2 != null;
				return comparator.compare(owner1, owner2);
			}
		};
	}
}
