package chs.caplets.logic.actions;

import chs.cof.draw.IGfxObject;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPinPlaceholder;
import chs.common.ILocation;
import chs.common.Location;
import chs.utilities.ListMap;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.logic.ModularConnectorHelper;
import chs.utility.logic.PinUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 4 Mar, 2013
 */
public class MovePinActionUtils
{

	private MovePinActionUtils()
	{
	}

	/**
	 * Given a pinlist & a location, find the nearest attached pinlist of the expected Type  to given location
	 *
	 * @param pinList - current pinlist whose attached pinlist we are interested to find
	 * @param expectedMatePinListType - expected attached pinlist type
	 * @param x - x coordinate of the location from where we want the nearest pinlist
	 * @param y - y coordinate of the location from where we want the nearest pinlist
	 *
	 * @return - the nearest attached pinlist of given type
	 */
	@Nullable public static IPinList getNearestMateOfGivenType(IPinList pinList,
			chs.cof.logical.cable.IPinList expectedMatePinListType,
			int x,
			int y)
	{
		Map<IPinList, ModularSchemPinListInfo> attachedModularGroup = new LinkedHashMap<>();
		ModularSchemPinListInfo srcGroup = new ModularSchemPinListInfo(pinList);
		ModularConnectorHelper.generateAttachedModularGrouping(srcGroup, attachedModularGroup);

		//do ordered processing for consistency.
		chs.cof.logical.cable.IPinList expectedMateModularRoot = determineModularRoot(expectedMatePinListType);

		double min = Integer.MAX_VALUE;
		ILocation point = new Location(x, y);
		IPinList nearestMate = null;

		for (ModularSchemPinListInfo schemPinListInfo : new LinkedHashSet<>(attachedModularGroup.values())) {
			IPinList anchor = schemPinListInfo.getAnchor();
			chs.cof.logical.cable.IPinList modularRoot = determineModularRoot(anchor.getConnectivity());
			if (modularRoot == expectedMateModularRoot) {
				double distance = anchor.getLocation().distance(point);
				if (distance < min) {
					min = distance;
					nearestMate = anchor;
				}
			}
		}
		return nearestMate;
	}

	/**
	 * Given a schem pinlist & a location (absolute) on the pinlist, find the attached pinlist appropriate for that
	 * location If user specifies the expectedAttachedPinListType, it must be of that type. for example, at a given
	 * location of the pinlist, there can be more than 1 attached pinlists (overlapped). But, user is interested in only
	 * one of those whose type matches with the one specified.
	 *
	 * @param pinList - current pinlist for which we are looking for the attached pinlist
	 * @param x - location on the pinlist whose matching object we are intersted in
	 * @param y - location on the pinlist whose matching object we are intersted in
	 * @param expectedAttachedPinListType - if specified, only that attached pinlist of this type will be returned. If
	 * more than one attachedPinlist is of this type, it will return first one. If expectedAttachedPinListType is null,
	 * any matching pinlist will be returned
	 *
	 * @return - the most appropriate attached pinlist of given pinlist. null, if nothing found.
	 */
	@Nullable public static IPinList getAttachedPinlistCorrespondingToGivenLocation(IPinList pinList,
			int x, int y, @Nullable chs.cof.logical.cable.IPinList expectedAttachedPinListType)
	{
		//Get the X,Y location relative to the destination pinlist
		ILocation newLoc = CoordinateHelper.getDirectParentRelLocation(pinList, x, y);
		//
		// See if there is an object at this location.
		//
		Set<chs.cof.logical.cable.IPinList> filter = expectedAttachedPinListType == null ? Collections.emptySet() :
				Collections.singleton(determineModularRoot(expectedAttachedPinListType));
		for (IGfxObject gfxObject : pinList.getObjects()) {
			if (gfxObject instanceof IAbstractSchemPin || gfxObject instanceof IPinPlaceholder) {
				ILocation location = gfxObject.getLocation();
				if (location.getX() == newLoc.getX() && location.getY() == newLoc.getY()) {
					IPinList match = getAttachedPinlistCorrespondingToGivenPin(gfxObject, pinList, filter);
					if (match != null) {
						return match;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Given a schematic pin, return the most appropriate attached pinlist of its parent. There can be multiple pinlists
	 * attached to its parent. At the location of this pin's match, there can be multiple attached pinlists, which are
	 * overlapped. This function finds the most appropriate of them. If this pin has no connected pin, return the first
	 * attached pinlist. for example, at a given location of the pinlist, there can be more than 1 attached pinlists
	 * (overlapped). But, user is interested in only one of those whose type matches with the pin's mate.
	 *
	 * @param pin - current pin for whose owner we want to find the appropriate attached pinlist
	 *
	 * @return the most appropriate attached pinlist of given pinlist. null, if nothing found.
	 */
	@Nullable public static IPinList getAttachedPinlistCorrespondingToGivenPin(@NotNull IAbstractSchemPin pin)
	{
		IPinList pinlist = (IPinList) pin.getParent();
		assert pinlist != null;
		Set<chs.cof.logical.cable.IPinList> attachedModualrRootPinLists =
				getMatedAeroModularRootPinLists(PinUtils.getAllDesignPins(pin));

		return getAttachedPinlistCorrespondingToGivenPin(pin, pinlist, attachedModualrRootPinLists);
	}

	@Nullable private static IPinList getAttachedPinlistCorrespondingToGivenPin(
			@NotNull IGfxObject pinOrPH, @NotNull IPinList pinlist,
			@NotNull Set<chs.cof.logical.cable.IPinList> modualrRootPinListsFilter)
	{
		Map<IPinList, ModularSchemPinListInfo> attachedModularGroup = new LinkedHashMap<>();
		ModularSchemPinListInfo srcGroup = new ModularSchemPinListInfo(pinlist);
		ModularConnectorHelper.generateAttachedModularGrouping(srcGroup, attachedModularGroup);

		//do ordered processing for consistency.
		for (ModularSchemPinListInfo schemPinListInfo : new LinkedHashSet<>(attachedModularGroup.values())) {
			Set<IPinPlaceholder> validPlaceHolders = new HashSet<>();
			for (Map.Entry<IPinList, Set<IPinPlaceholder>> entry : schemPinListInfo.getPlaceHolders().entrySet()) {
				validPlaceHolders.addAll(entry.getValue());
			}
			for (IPinList attachePL : schemPinListInfo.getCandidates()) {
				IGfxObject match = ConnectionHelper.getSingleMatchingPinOrPlaceholder(pinOrPH, pinlist, attachePL);
				if (match != null && (!(match instanceof IPinPlaceholder) || validPlaceHolders.contains(match))) {
					chs.cof.logical.cable.IPinList modularRoot = determineModularRoot(attachePL.getConnectivity());
					if (modualrRootPinListsFilter.isEmpty() || modualrRootPinListsFilter.contains(modularRoot)) {
						return attachePL;
					}
				}
			}
		}
		return null;
	}

	@NotNull private static Set<chs.cof.logical.cable.IPinList> getMatedAeroModularRootPinLists(
			@NotNull Set<IAbstractPin> cablepins)
	{
		Set<chs.cof.logical.cable.IPinList> attachedPinLists = new HashSet<chs.cof.logical.cable.IPinList>();
		for (IAbstractPin cablepin : cablepins) {
			for (IAbstractPin cablePin : cablepin.getConnectedPins()) {
				chs.cof.logical.cable.IPinList owner = cablePin.getOwner();
				assert owner != null;
				attachedPinLists.add(determineModularRoot(owner));
			}
		}
		return attachedPinLists;
	}

	/**
	 * Given a pin to move & a target schem pinlis to which it is supposed to move, this method will determine if it is
	 * valid to move the pin to the specified target schem pinlist.
	 *
	 * @param destinationPinList - the target schem pinlist
	 * @param movingPins - the pins which are being moved
	 *
	 * @return - true if it is valid to move this pin to specified pinlist
	 */
	public static boolean isMovementAcrossConnectorsValid(IPinList destinationPinList, IAbstractSchemPin... movingPins)
	{
		ListMap<IPinList, IAbstractSchemPin> pinListsToProcess = new ListMap<>();
		for (IAbstractSchemPin movingPin : movingPins) {
			IPinList movingPinParent = (IPinList) movingPin.getParent();
			if (movingPinParent != null) {
				pinListsToProcess.add(movingPinParent, movingPin);
			}
		}

		Set<chs.cof.logical.cable.IPinList> destAttachedCables = new HashSet<>();
		for (IPinList candidate : new ModularSchemPinListInfo(destinationPinList).getCandidates()) {
			for (IPinList attachedConnOnDest : candidate.getAttachedPinListObjects()) {
				destAttachedCables.add(determineModularRoot(attachedConnOnDest.getConnectivity()));
			}
		}

		chs.cof.logical.cable.IPinList destinationModularRoot =
				determineModularRoot(destinationPinList.getConnectivity());
		for (Map.Entry<IPinList, List<IAbstractSchemPin>> entry : pinListsToProcess.entrySet()) {
			IPinList movingPinParent = entry.getKey();
			if (determineModularRoot(movingPinParent.getConnectivity()) != destinationModularRoot) {
				return false;
			}
			for (IAbstractSchemPin movingPin : entry.getValue()) {
				IAbstractSchemPin matedSchemPin = ConnectionHelper.getMatchingPinForConnectorPin(movingPin,
						movingPinParent, chs.cof.logical.cable.IPinList.class);
				if (matedSchemPin != null) {
					IPinList attachedSchemConnector = (IPinList) matedSchemPin.getParent();
					if (attachedSchemConnector != null) {
						chs.cof.logical.cable.IPinList attachedCableConnector =
								attachedSchemConnector.getConnectivity();
						if (attachedCableConnector instanceof IConnector) {
							if (!destAttachedCables.contains(determineModularRoot(attachedCableConnector))) {
								return false;
							}
						}
					}
				}
			}
		}
		return true;
	}

	@NotNull
	public static chs.cof.logical.cable.IPinList determineModularRoot(@NotNull chs.cof.logical.cable.IPinList candidate)
	{
		return ModularConnectorHelper.determineAeroModularRoot(candidate);
	}
}
