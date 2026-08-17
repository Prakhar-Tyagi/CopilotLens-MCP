package chs.caplets.shared.properties;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.symbol.ISymbolRef;
import chs.common.IUIDObject;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.HybridSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class SymbolControlHelper
{

	private SymbolControlHelper()
	{
	}

	public static boolean doesSelectionIncludeAMixOfSchemAndConnPinlists(@NotNull Collection<IUIDObject> validObjects)
	{
		if (validObjects.size() < 1) {
			return false;
		}

		List<IUIDObject> nonMatchingClassObjects = validObjects.stream()
				.filter(obj -> !IPinList.class.isAssignableFrom(obj.getClass()) &&
						!chs.cof.logical.cable.IPinList.class.isAssignableFrom(obj.getClass()))
				.collect(Collectors.toList());
		if (!nonMatchingClassObjects.isEmpty()) {
			return false;
		}

		List<chs.cof.logical.cable.IPinList> cablePinlists =
				CollectionUtils.getListOfType(validObjects, chs.cof.logical.cable.IPinList.class);
		if (cablePinlists.size() > 1) {
			return false;
		}

		List<IPinList> schemPinlists = CollectionUtils.getListOfType(validObjects, IPinList.class);
		return hasSameConnectivity(schemPinlists);
	}

	@Nullable public static ISymbolRef getSymbolRefFromSelectedObjects(@NotNull Collection<IUIDObject> symboledObjects)
	{
		Collection<IPinList> schemCollection = new ArrayList<>();
		Collection<chs.cof.logical.cable.IPinList> cableCollection = new ArrayList<>();

		for (IUIDObject symboledObject : symboledObjects) {
			if (symboledObject instanceof IPinList) {
				schemCollection.add((IPinList) symboledObject);
			}
			else if (symboledObject instanceof chs.cof.logical.cable.IPinList) {
				cableCollection.add((chs.cof.logical.cable.IPinList) symboledObject);
			}
		}

		ISymbolRef symbolRef = getSymbolRefFromSchem(schemCollection);
		if (symbolRef != null) {
			return symbolRef;
		}
		return getSymbolRefFromConnectivity(cableCollection);
	}

	@Nullable private static ISymbolRef getSymbolRefFromSchem(@NotNull Collection<IPinList> schemPinlists)
	{
		if (!hasSameConnectivity(schemPinlists)) {
			return null;
		}

		Set<ISymbolRef> symbolRefs = schemPinlists.stream().map(obj -> obj.getSymbolRef())
				.filter(ref -> ref != null)
				.collect(Collectors.toCollection(HybridSet::new));

		return symbolRefs.size() == 1 ? symbolRefs.iterator().next() : null;
	}

	@Nullable private static ISymbolRef getSymbolRefFromConnectivity(@NotNull Collection<chs.cof.logical.cable.IPinList> cableObjects)
	{
		if (cableObjects.size() != 1) {
			return null;
		}
		chs.cof.logical.cable.IPinList cablePinlist = cableObjects.iterator().next();
		ISymbolRef symref;
		if (cablePinlist.canMaintainMultipleSymbols()) {
			ISharedPinList sharedPinList = cablePinlist.getSharedPinList();
			if (sharedPinList == null) {
				symref = getSymbolRefFromConnectivityForNonSharedPinlists(cablePinlist);
			}
			else {
				return getSymbolRefFromConnectivityForSharedPinlists(cablePinlist, sharedPinList);
			}
		}
		else {
			symref = cablePinlist.getSymbolRef();
		}
		return symref;
	}

	@Nullable
	private static ISymbolRef getSymbolRefFromConnectivityForNonSharedPinlists(
			@NotNull chs.cof.logical.cable.IPinList cablePinlist)
	{
		Set<ISymbolRef> symbolRefs = cablePinlist.getSymbolReferences();
		ISymbolRef symref = symbolRefs.size() != 1 ? null : symbolRefs.iterator().next();
		return symref;
	}

	@Nullable
	private static ISymbolRef getSymbolRefFromConnectivityForSharedPinlists(
			@NotNull chs.cof.logical.cable.IPinList cablePinlist,
			@NotNull ISharedPinList sharedPinList)
	{
		if (sharedPinList.getNumSymbols() != 1) {
			return null;
		}
		else {
			ILogicDesign logicDesign = cablePinlist.getLogicDesign();
			if (logicDesign != null) {
				Collection<IPinList> sharedSchemPinlists =
						logicDesign.getSharedUsageMgr().getUsages(sharedPinList).stream()
								.map(usage -> usage.getDiagramObject())
								.filter(diagramObject -> diagramObject instanceof IPinList)
								.map(diagramObject -> (IPinList) diagramObject)
								.collect(Collectors.toList());
				return getSymbolRefFromSchem(sharedSchemPinlists);
			}
			return null;
		}
	}

	private static boolean hasSameConnectivity(@NotNull Collection<IPinList> schemPinlists)
	{
		return schemPinlists.stream().map(obj -> obj.getConnectivityUID()).distinct().count() <= 1;
	}
}
