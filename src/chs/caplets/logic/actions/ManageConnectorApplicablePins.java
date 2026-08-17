package chs.caplets.logic.actions;

import chs.capitalmanager.appserver.UserSessionException;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.IDesignSharedPinUsage;
import chs.cof.logical.shared.ISharedBackshellTermination;
import chs.cof.logical.shared.ISharedConnectorPin;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.SharedPinHelper;
import chs.common.DesignUtils;
import chs.common.IDesignDescriptor;
import chs.common.IUID;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.subsystem.logic.manageconnections.ISharedPinProvider;
import chs.subsystem.logic.manageconnections.ManageConnectionsServices;
import chs.utilities.SetMap;
import chs.utility.ui.PinConductorConnectionSortHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ManageConnectorApplicablePins
{

	private Supplier<PinConductorConnectionSortHelper> sortHelperSupplier;

	private ISharedPinList sharedPinList;
	private ManageConnectorDesignScope designScope;

	private Map<ISharedPin, IPinProxy> sharedPinProxies = new HashMap<>();
	private Map<IDesignDescriptor, SharedPinAndItsNotationInContainer> sharedPinProxiesApplicableForDesign;
	private Map<ISharedPin, Collection<IUID>> designsWithSharedPins = null;
	private Collection<IDesignDescriptor> designsForWhichSharedUsageCalculated = new LinkedHashSet<>();

	private static class SharedPinAndItsNotationInContainer
	{

		private List<Comparable<?>> sharedPinNotation;

		private ISharedPinList sharedPinlist;
		private Collection<ISharedPin> sharedPinAvailable;
		private IDesignDescriptor designDescriptor;
		private Map<ISharedPin, IPinProxy> sharedPinProxies;

		SharedPinAndItsNotationInContainer(ISharedPinList sharedPinList, IDesignDescriptor designDescriptor,
				Map<ISharedPin, IPinProxy> sharedPinProxies)
		{
			this.designDescriptor = designDescriptor;
			sharedPinlist = sharedPinList;
			this.sharedPinProxies = sharedPinProxies;
		}

		boolean isSharedPinApplicableForDesign(ISharedPin sharedPin,
				BiFunction<ISharedPin, IUID, Boolean> checkForAvailability)
		{
			if (sharedPinAvailable == null) {
				sharedPinAvailable = getAvailablePins(designDescriptor, checkForAvailability);
			}

			return sharedPinAvailable.contains(sharedPin);
		}

		Collection<Comparable<?>> getAllNotations(Supplier<PinConductorConnectionSortHelper> sortSupplier,
				BiFunction<ISharedPin, IUID, Boolean> checkForAvailability)
		{

			if (sharedPinNotation == null) {
				if (sharedPinAvailable == null) {
					sharedPinAvailable = getAvailablePins(designDescriptor, checkForAvailability);
				}

				sharedPinNotation = sharedPinAvailable.stream()
						.map(aSharedPin -> sortSupplier.get()
								.createComparableForPin(sharedPinProxies.get(aSharedPin))).collect(
								Collectors.toList());
			}
			return sharedPinNotation;
		}

		private Collection<ISharedPin> getAvailablePins(IDesignDescriptor descriptor,
				BiFunction<ISharedPin, IUID, Boolean> checkSharedPinAvailableForDesign)
		{

			ILogicDesign logicDesign = (ILogicDesign) descriptor.getDesignContainer();
			if (logicDesign == null) {
				return Collections.emptySet();
			}

			Collection<ISharedPin> availablePinsForThisDesign = new LinkedHashSet<>();
			List<ISharedPin> sharedPins =
					ManageConnectionsServices.requireExtension(sharedPinlist, ISharedPinProvider.class).getAllSharedPins();
			for (ISharedPin sharedPin : sharedPins) {
				if (sharedPin instanceof ISharedConnectorPin) {
					if (((ISharedConnectorPin) sharedPin).isBlockedCavity()) {
						continue;
					}
				}
				Boolean isSharedPinAvailableToDesign =
						sharedPin.getReservationType().equals(ISharedPin.ReservationType.Unrestricted);
				if (!isSharedPinAvailableToDesign) {
					if (sharedPin.getReservationType().equals(ISharedPin.ReservationType.MANUAL)) {
						isSharedPinAvailableToDesign =
								sharedPin.getDesignReservationList().contains(logicDesign.getUID());
					}
					else if (logicDesign.isLocked() && logicDesign.isLoadedInMemory()) {
						isSharedPinAvailableToDesign = SharedPinHelper.isUsable(sharedPin, logicDesign) ||
								SharedPinHelper.isUsed(sharedPin, logicDesign);

						List<IDesignSharedPinUsage> usages =
								logicDesign.getSharedUsageMgr().getPinUsages(sharedPin, false);
						if (!usages.isEmpty()) {
							//make sure that all the pin instances in the design are not just reference pins
							isSharedPinAvailableToDesign =
									(usages.stream().filter(anUsage -> !anUsage.isReference()).findFirst().isPresent());
						}
					}
				}

				if (!isSharedPinAvailableToDesign) {
					if (sharedPin.getReservationType() == ISharedPin.ReservationType.AUTOMATIC) {
						isSharedPinAvailableToDesign =
								checkSharedPinAvailableForDesign.apply(sharedPin, logicDesign.getUID());
					}
				}

				if (isSharedPinAvailableToDesign) {
					availablePinsForThisDesign.add(sharedPin);
				}
			}

			return availablePinsForThisDesign;
		}
	}

	private Map<ISharedPin, Collection<IUID>> getDesignsWithSharedPins()
	{

		if (designsWithSharedPins == null ||
				!designsForWhichSharedUsageCalculated.containsAll(designScope.getDesignsInScope())) {
			try {
				designsForWhichSharedUsageCalculated.addAll(designScope.getDesignsInScope());
				designsWithSharedPins = new HashMap<>();
				Collection<IUID> designUIDsInScope =
						designScope.getDesignsInScope().stream().map(designDescriptor -> designDescriptor.getUID())
								.collect(Collectors.toSet());

				Collection<IUID> loadedAndLockedDesigns = new LinkedHashSet<>();
				for (IDesignDescriptor aDesign : designScope.getDesignsInScope()) {
					ILogicDesign logicDesign = DesignUtils.getDesign(aDesign.getUID(), ILogicDesign.class);
					if (logicDesign != null && logicDesign.isLoadedInMemory() && logicDesign.isLocked()) {
						loadedAndLockedDesigns.add(logicDesign.getUID());
						Collection<ISharedPin> usagesInLoadedAndLockedDesign =
								getUsagesInLoadedndLockedDesign(logicDesign);
						for (ISharedPin sharedPin : usagesInLoadedAndLockedDesign) {
							if (designsWithSharedPins.get(sharedPin) == null) {
								designsWithSharedPins.put(sharedPin, new LinkedHashSet<>());
							}
							designsWithSharedPins.get(sharedPin).add(aDesign.getUID());
						}
					}
				}

				SetMap<ISharedPin, IUID> allDesignWithSharedPins = SharedPinHelper
						.getSharedPinReservationsFromDB(Collections.singleton(sharedPinList), sharedPinList.getType());

				for (ISharedPin aSharedPin : allDesignWithSharedPins.keySet()) {
					Set<IUID> designsWithSharedPin = allDesignWithSharedPins.get(aSharedPin);
					Collection<IUID> designsWithSharedPinInScope =
							designsWithSharedPin.stream().filter(aDesignUID -> designUIDsInScope.contains(aDesignUID) &&
									!loadedAndLockedDesigns.contains(aDesignUID))
									.collect(Collectors.toSet());
					if (designsWithSharedPins.get(aSharedPin) == null) {
						designsWithSharedPins.put(aSharedPin, new LinkedHashSet<>());
					}
					designsWithSharedPins.get(aSharedPin).addAll(designsWithSharedPinInScope);
				}
			}
			catch (UserSessionException e) {
				e.printStackTrace();
			}
		}
		return designsWithSharedPins;
	}

	public ManageConnectorApplicablePins(
			@NotNull ISharedPinList sharedPinList, @NotNull ManageConnectorDesignScope designScope)
	{

		this.sharedPinList = sharedPinList;
		this.designScope = designScope;
		List<ISharedPin> sharedPins =
				ManageConnectionsServices.requireExtension(sharedPinList, ISharedPinProvider.class).getAllSharedPins();
		for (ISharedPin aSharedPin : sharedPins) {
			IPinProxy pin = new PinProxy(aSharedPin);
			if (aSharedPin instanceof ISharedBackshellTermination) {
				pin.setName(aSharedPin.getOwner().getName() + ":" + aSharedPin.getName());
			}
			sharedPinProxies.put(aSharedPin, pin);
		}
		sharedPinProxiesApplicableForDesign = new HashMap<>();
	}

	public List<Comparable<?>> getAllPossibleValues()
	{
		List<Comparable<?>> sharedPinProxiesApplicable = new ArrayList<>();
		for (ISharedPin sharedPin : sharedPinList.getPins()) {
			IPinProxy aPinProxy = sharedPinProxies.get(sharedPin);
			Comparable<?> comparable = sortHelperSupplier.get().createComparableForPin(aPinProxy);
			sharedPinProxiesApplicable.add(comparable);
		}
		return sharedPinProxiesApplicable;
	}

	public Collection<Comparable<?>> getNotationsForSharedPinsApplicableInDesign(
			@NotNull IDesignDescriptor designDescriptor)
	{
		SharedPinAndItsNotationInContainer sharedPinProxiesApplicable =
				getNotationsForSharedPinInDesign(designDescriptor);

		return sharedPinProxiesApplicable
				.getAllNotations(sortHelperSupplier, checkPinAvailabilityInDesign());
	}

	public boolean canUsePinInCurrentDesign(ISharedPin sharedPin, IDesignDescriptor design)
	{
		SharedPinAndItsNotationInContainer sharedPinProxiesApplicable =
				getNotationsForSharedPinInDesign(design);
		return sharedPinProxiesApplicable.isSharedPinApplicableForDesign(sharedPin,
				checkPinAvailabilityInDesign());
	}

	private Collection<ISharedPin> getUsagesInLoadedndLockedDesign(ILogicDesign logicDesign)
	{
		Collection<ISharedPin> usedPinsInLoadedAndLockedDesign = new LinkedHashSet<>();
		for (ISharedPin sharedPin : ManageConnectionsServices.requireExtension(sharedPinList, ISharedPinProvider.class)
				.getAllSharedPins()) {
			boolean isSharedPinAvailableToDesign = SharedPinHelper.isUsable(sharedPin, logicDesign) ||
					SharedPinHelper.isUsed(sharedPin, logicDesign);

			List<IDesignSharedPinUsage> usages =
					logicDesign.getSharedUsageMgr().getPinUsages(sharedPin, false);
			if (!usages.isEmpty()) {
				//make sure that all the pin instances in the design are not just reference pins
				isSharedPinAvailableToDesign =
						(usages.stream().filter(anUsage -> !anUsage.isReference()).findFirst().isPresent());
			}
			if (isSharedPinAvailableToDesign) {
				usedPinsInLoadedAndLockedDesign.add(sharedPin);
			}
		}
		return usedPinsInLoadedAndLockedDesign;
	}

	private BiFunction<ISharedPin, IUID, Boolean> checkPinAvailabilityInDesign()
	{
		return new BiFunction<ISharedPin, IUID, Boolean>()
		{
			@Override public Boolean apply(ISharedPin sharedPin, IUID designUID)
			{

				Map<ISharedPin, Collection<IUID>> pinUsages = getDesignsWithSharedPins();
				Collection<IUID> designUIDs = pinUsages.get(sharedPin);

				return designUIDs == null || designUIDs.isEmpty() || designUIDs.contains(designUID);
			}
		};
	}

	public Collection<String> getPinSharedPinNamesApplicableInDesign(IDesignDescriptor designDescriptor)
	{
		SharedPinAndItsNotationInContainer sharedPinProxiesApplicable =
				getNotationsForSharedPinInDesign(designDescriptor);
		return sharedPinProxiesApplicable
				.getAvailablePins(designDescriptor, checkPinAvailabilityInDesign())
				.stream()
				.map(aSharedPin -> (aSharedPin instanceof ISharedBackshellTermination && aSharedPin.getOwner() != null)
						? aSharedPin.getOwner().getName() + ":" + aSharedPin.getName()
						: aSharedPin.getName())
				.collect(Collectors.toSet());
	}

	private SharedPinAndItsNotationInContainer getNotationsForSharedPinInDesign(IDesignDescriptor designDescriptor)
	{
		SharedPinAndItsNotationInContainer sharedPinProxiesApplicable =
				sharedPinProxiesApplicableForDesign.get(designDescriptor);
		if (sharedPinProxiesApplicable == null) {

			sharedPinProxiesApplicable =
					new SharedPinAndItsNotationInContainer(sharedPinList, designDescriptor, sharedPinProxies);
			sharedPinProxiesApplicableForDesign.put(designDescriptor, sharedPinProxiesApplicable);
		}
		return sharedPinProxiesApplicable;
	}

	public void updateDataUsingSortHelper(Supplier<PinConductorConnectionSortHelper> sortHelper)
	{
		sortHelperSupplier = sortHelper;
	}
}