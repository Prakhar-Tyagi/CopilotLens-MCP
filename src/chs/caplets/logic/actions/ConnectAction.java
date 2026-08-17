/*
 * Copyright 2006-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.IActionStateWatcher;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionUtils;
import chs.caplets.logic.actions.connection.SelectedConnectablePinsGroupProvider;
import chs.caplets.logic.commands.ConnectCommand;
import chs.caplets.logic.commands.ConnectPinsCommand;
import chs.caplets.shared.Finder;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.IJackConnector;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISystemLogicDiagram;
import chs.cofUtils.logical.concurrency.IConcurrentEditReporter;
import chs.cofUtils.logical.concurrency.LogicConcurrencyController;
import chs.cofUtils.logical.concurrency.LogicConcurrencyLogger;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.services.gfx.GfxView;
import chs.utilities.CommonUtils;
import chs.utilities.ListSet;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utility.DiagramHelper;
import chs.utility.GfxObjectUtils;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Common base class for the various types of Connect actions
 */
@SuppressWarnings({"NoopMethodInAbstractClass"}) // nothing wrong with no-ops in this abstract class?
public abstract class ConnectAction extends ControllerActionRT implements ICtxMenuProvider
{
	protected boolean hasOverlappingPins = false;

	/**
	 * Get all selected pins or pins of selected pinlists.
	 * <p>
	 * Pins that have a conductor of the "wrong" type attached for this action are omitted.
	 *
	 * @param sel The selection
	 *
	 * @return The set of pins
	 */
	public Set<IPin> findSelectedPins(SelectSet sel)
	{
		Set<IPin> pins = new ListSet<>();
		for (SelectedUIDObjectIterator it = sel.getSelectedUIDObjects(); it.hasNext(); ) {
			IUIDObject obj = it.getNext();
			if (obj instanceof IPin) {
				IPin pin = (IPin) obj;
				if (ConnectCommand.connectionAllowed(pin, getConductorClass())) {
					pins.add(pin);
				}
			}
			else if (obj instanceof IPinList) {
				for (Object pobj : ((IPinList) obj).getPins()) {
					IPin pin = (IPin) pobj;
					if (ConnectCommand.connectionAllowed(pin, getConductorClass())) {
						pins.add(pin);
					}
				}
			}
		}
		return pins;
	}

	protected ConnectAction(ICapletController controller)
	{
		super(controller);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		// just do it all in onTerminate
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		if(!successful){
			return true;
		}

		ILogicDesign logicDesign = getDesign();
		ISchemDiagram diagram = (ISchemDiagram) getController().getCapletModel().getModelRoot();
		if (logicDesign == null || diagram == null){
			return false;
		}

		if (hasOverlappingPins) {
			// get all the sets of overlapping pins that are to be connected
			Set<IPin> pins = findSelectedPins(getController().getSelectMgr().getPreSelections());
			Map<ILocation, Set<IPin>> inputPins = findInputPins(pins);
			assert !inputPins.isEmpty(); // isEnabled has already checked that we have at least one set
			String failureMsg = ResourceMgr.getString(ConnectAction.class, "ConnectAction.error.unableToLock",
					getActionUI().getValue(Action.NAME));
			if (isDesignUnderConcurrentEdit() && !attemptLockingRequiredObjects(getLocables(inputPins), logicDesign, failureMsg)) {
				return false;
			}

			// connect each set of overlapping pins together
			Class<? extends IConductor> condClass = getConductorClass();
			for (Map.Entry<ILocation, Set<IPin>> entry : inputPins.entrySet()) {
				ConnectCommand.connectPins(entry.getValue(), diagram, condClass);
			}
		}
		else{
			Action actionUI = getActionUI();
			if(getConductorClass() == null || actionUI == null){
				return false;
			}

			SelectedConnectablePinsGroupProvider pinsProvider = new SelectedConnectablePinsGroupProvider();
			ConnectPinsCommand connectPinsCommand =
					new ConnectPinsCommand(pinsProvider, getConductorClass(), logicDesign, diagram, actionUI);
			connectPinsCommand.execute();
		}
		return true;
	}

	@NotNull private Set<IUIDObject> getLocables(@NotNull Map<ILocation, Set<IPin>> inputPins)
	{
		List<IPin> allPins = new ArrayList<>();
		inputPins.values().stream().forEach(pinset -> allPins.addAll(pinset));
		Set<IUIDObject> lockables = new HashSet<>();
		for (IPin pin: allPins) {
			lockables.add(pin.getConnectivity().getOwner());
		}
		return lockables;
	}

	public static boolean attemptLockingRequiredObjects(@NotNull Collection<IUIDObject> lockables, @NotNull ILogicDesign design,
			String failureMsg)
	{
		Set<IUID> lockFailedObjects = LogicObjectLockFinder.tryEdit(design, lockables);
		if (!lockFailedObjects.isEmpty()) {
			IConcurrentEditReporter reporter =
					LogicConcurrencyController.getInstance().getCAFView().getConcurrentEditReporter();
			LogicConcurrencyLogger.getInstance()
					.reportLockFailure(design, failureMsg, lockFailedObjects, reporter);
			return false;
		}
		return true;
	}

	@Nullable private ILogicDesign getDesign()
	{
		ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
		if (controller != null) {
			ICapletModel capletModel = controller.getCapletModel();
			if (capletModel instanceof ILogicModel) {
				return ((ILogicModel) capletModel).getDesign();
			}
		}
		return null;
	}

	/**
	 * Derived classes define the class of conductor with which to connect overlapping pins.
	 *
	 * @return The conductor class
	 */
	@Nullable protected abstract Class<? extends IConductor> getConductorClass();

	public boolean isEnabled()
	{
		// first check if the overall isEnabled result was cached from a previous isEnabled (with the same model + selection)
		IActionStateWatcher actionStateWatcher = getController().getActionStateWatcher();
		Boolean cached = actionStateWatcher.checkEnabled(this);
		if (cached != null) {
			return cached;
		}

		hasOverlappingPins = false;
		if (getController().getCapletModel().isEditable()) {
			// does the selection provide imply a pin that could be connected (overlaps another pin)
			SelectSet sel = getController().getSelectMgr().getPreSelections();
			if (!sel.isEmpty()) {
				// make sure we dont connect pins that happen to overlap on another diagram!  (dts0100519523)
				IBaseDiagram activeDiagram = CAFUtils.getInstance().getActiveDiagram();
				if (!SelectionUtils.hasOtherDiagramSelection(sel, activeDiagram)) {
					if (getConductorClass() == null) {
						hasOverlappingPins = !findInputPins(findSelectedPins(sel)).isEmpty();
					}
					else {
						hasOverlappingPins = hasInputPins(sel);
					}
				}
			}
		}
		boolean enabled;
		if (hasOverlappingPins) {
			enabled = super.isEnabled();
		}
		else {
			enabled = getConductorClass() != null && checkHasAtleastTwoPinlistInSelection()  &&
					super.isEnabled();
		}

		actionStateWatcher
				.setEnabled(this, enabled); // cache to workaround multiple isEnabled calls with same model/selection
		return enabled;
	}

	public String getActionUIClass()
	{
		return ConnectActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		container.add(new ActionEntry(getActionUI()));
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	/**
	 * Input pins are all pins that overlap and can be connected with one of the specified pins.
	 *
	 * @param pins The specified pins
	 *
	 * @return The input pins, partitioned into each overlapping group (of 2 or more)
	 */
	private Map<ILocation, Set<IPin>> findInputPins(Set<IPin> pins)
	{
		// construct a map ILocation --> overlapping pins
		// TODO jacobt FEAT2081 : HashMap could be faster here?
		// ILocation does not implement equals() so need to use a TreeMap with our own comparator for ILocations
		// Could a HashMap (e.g. Point --> pins) be faster here?
		// Or could we somehow setup a HashMap ILocation --> pins?
		Map<ILocation, Set<IPin>> inputPins =
				new TreeMap<ILocation, Set<IPin>>(GfxObjectUtils.getLocationComparator());
		if (pins.isEmpty()) {
			return inputPins;
		}

		// store locations of selected pins
		for (IPin pin : pins) {
			storePinLocation(pin, inputPins);
		}

		final Set<ILocation> fixedInputPins = new TreeSet<>(GfxObjectUtils.getLocationComparator());
		fixedInputPins.addAll(getFinalizedLocations(inputPins));

		// check all diagram pins
		// add locations into the map if they overlap the location of an input pin
		IUIDObject root = getController().getCapletModel().getModelRoot();
		if (root instanceof ISchemDiagram) {
			for (Object plobj : ((ISchemDiagram) root).getPinLists().getCollection()) {
				IPinList pl = (IPinList) plobj;
				for (Object pobj : pl.getPins()) {
					IPin pin = (IPin) pobj;
					if (ConnectCommand.connectionAllowed(pin, getConductorClass())) {
						ILocation loc = pin.getAbsLocation();
						if (!fixedInputPins.contains(loc) && inputPins.containsKey(loc)) {
							storePinLocation(pin, inputPins);
						}
					}
				}
			}
		}

		// all sets of >1 pins are now valid to be connected
		// remove any set of <2 pins (all input pins were added to the map)
		List<ILocation> nonPairs = new ArrayList<ILocation>();
		for (Map.Entry<ILocation, Set<IPin>> entry : inputPins.entrySet()) {
			if (!isValidInputPinsEntry(entry)) {
				nonPairs.add(entry.getKey());
			}
		}
		for (ILocation loc : nonPairs) {
			inputPins.remove(loc);
		}

		return inputPins;
	}

	protected boolean isValidInputPinsEntry(@NotNull Map.Entry<ILocation, Set<IPin>> entry)
	{
		Set<IPin> pinset = entry.getValue();
		if (pinset.size() < 2) {
			return false;
		}
		// we can't mate a pin with itself in case of pin mating
		if (getConductorClass() == null) {
			IAbstractPin cpin = pinset.iterator().next().getConnectivity();
			boolean samePin = true;
			for (IPin p : pinset) {
				samePin &= (p.getConnectivity() == cpin);
			}
			if (samePin) {
				return false;
			}
			return !allPinsAreFromSamePinlist(pinset);
		}

		return true;
	}

	protected Set<ILocation> getFinalizedLocations(@NotNull Map<ILocation, Set<IPin>> inputPins)
	{
		return Collections.emptySet();
	}

	protected boolean allPinsAreFromSamePinlist(@NotNull Set<IPin> pins)
	{
		return pins.stream()
				.map(p -> p.getParent())
				.distinct()
				.count() == 1;
	}

	/**
	 * Does the selection imply any "input pins"?
	 * <p>
	 * Pins are obtained from all selected pins or pinlists. An input pin is a pin that overlaps some other pin, where
	 * neither pin has a conductor of the "wrong" type.
	 *
	 * @param sel The selection to examine
	 *
	 * @return The result of the check described above
	 */
	protected boolean hasInputPins(SelectSet sel)
	{
		assert !sel.isEmpty();

		// we can do this check a bit quicker if we have a view
		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			return hasInputPins(sel, view);
		}

		// no view - this can still happen for the unit test + perhaps for some future client?
		Set<IPin> selPins = findSelectedPins(sel);
		return hasInputPins(selPins);
	}

	/**
	 * Are any "input pins" implied by this set of pins?
	 * <p>
	 * Input pins are those groups of pins that could be connected by this action.
	 *
	 * @param pins The pins implied by the selection (*not* necessarily just the selected pins)
	 *
	 * @return true iff at least one pin on the diagram overlaps one of the specified pins and is valid for connection.
	 */
	protected boolean hasInputPins(Set<IPin> pins)
	{
		if (pins.isEmpty()) {
			return false;
		}

		// store the location versus pins on same location
		final SetMap<Point, IPin> selPinsPoints = new SetMap<>();
		for (IPin pin : pins) {
			if (checkPinOverlap(pin, selPinsPoints)) {
				return true;
			}
		}

		// none of the selected pins overlap eachother - check all diagram pins for overlap with selected pins
		IUIDObject root = getController().getCapletModel().getModelRoot();
		if (root instanceof ISchemDiagram) {
			for (Object plobj : ((ISchemDiagram) root).getPinLists().getCollection()) {
				IPinList pl = (IPinList) plobj;
				for (Object pobj : pl.getPins()) {
					IPin pin = (IPin) pobj;
					ILocation loc = pin.getAbsLocation();
					final Point point = new Point(loc.getX(), loc.getY());
					if (selPinsPoints.contains(point)) {
						final Set<IPin> pinsOnSamePoint = selPinsPoints.pull(point);
						if (pinsOnSamePoint != null && !pinsOnSamePoint.contains(pin) &&
								hasDifferentParent(pin, pinsOnSamePoint)) {
							// found a pin that overlaps one of the input pins - can we connect it?
							if (ConnectCommand.connectionAllowed(pin, getConductorClass())) {
								return true;
							}
						}
					}
				}
			}
		}

		return false;
	}

	/**
	 * A faster version of hasInputPins that relies on using the VHTree to cut down on pins to test for overlap.
	 *
	 * @param sel The selection to check
	 * @param view The view
	 *
	 * @return The result of the check
	 */
	private boolean hasInputPins(SelectSet sel, GfxView view)
	{
		assert !sel.isEmpty();
		// try to use a Finder to cut down on the number of pins to which we compare the selected pins

		// check for overlap among the pins implied by the selection
		SetMap<Point, IPin> inputPins = new SetMap<>();
		Class<? extends IConductor> condCls = getConductorClass();
		for (SelectedUIDObjectIterator it = sel.getSelectedUIDObjects(); it.hasNext(); ) {
			IUIDObject obj = it.getNext();
			if (obj instanceof IPin) {
				IPin pin = (IPin) obj;
				if (ConnectCommand.connectionAllowed(pin, condCls) && checkPinOverlap(pin, inputPins)) {
					return true;
				}
			}
			else if (obj instanceof IPinList) {
				for (Object pobj : ((IPinList) obj).getPins()) {
					IPin pin = (IPin) pobj;
					if (ConnectCommand.connectionAllowed(pin, condCls) && checkPinOverlap(pin, inputPins)) {
						return true;
					}
				}
			}
		}

		final Set<IPin> pins =
				inputPins.values().stream()
						.flatMap(s -> s.stream())
						.collect(Collectors.toSet());

		if (pins.isEmpty()) {
			return false;
		}

		// no overlap amongst selected pins - check for overlap with other diagram pins
		// restrict check to diagram pins that are within the combined extent of the selected pins
		IExtent ext = null;
		for (IPin pin : pins) {
			ILocation loc = pin.getAbsLocation();
			if (ext == null) {
				ext = CAFUtils.getInstance().getCommonFactory().createExtent();
				ext.setBounds(loc.getX(), loc.getY(), 0, 0);
			}
			else {
				ext.addUnionLocation(loc);
			}
		}
		// make the extent a bit bigger so pins are actually contained
		assert ext != null; // because pins are non-empty - stops IntelliJ moaning about the next line...
		ext.setBounds(ext.getX() - 1, ext.getY() - 1, ext.getWidth() + 2, ext.getHeight() + 2);

		// now we're ready to do our "fast-find"!
		PinOverlapFinder finder = new PinOverlapFinder(view.getGfxContext(), ext, inputPins, condCls);
		finder.visitRoot(view.getSheet(), 0, 0);
		return finder.getFound();
	}

	/**
	 * Special-purpose Finder looks for pins that overlap a specified set of points. Only finds pins that can be
	 * connected by this action.
	 * <p>
	 * Uses VHTree to cut down on the number of pins that must be checked.
	 * <p>
	 * TODO jacobt FEAT2081 : This could be generally useful - and faster if required
	 */
	private static class PinOverlapFinder extends Finder
	{

		@NotNull private SetMap<Point, IPin> selPinsPoints;
		private boolean found = false;
		@Nullable private Class<? extends IConductor> condCls;

		/**
		 * Construct a PinOverlapFinder, use visit... method before calling getFound() to see if we found something.
		 *
		 * @param context GfxContext e.g. for the view in which we're searching
		 * @param ext Extent within which to search
		 * @param pinsPoints The set map having points versus set of pins on same location. The pins that we are
		 * checking for overlap (any pin found must not be in this set) The points that we are checking for overlap
		 * (can't use ILocations in a HashSet)
		 * @param cls The type of conductor that we must be able to connect to the overlapping pin
		 */
		PinOverlapFinder(IGfxContext context, IExtent ext, @NotNull SetMap<Point, IPin> pinsPoints,
				@Nullable Class<? extends IConductor> cls)
		{
			super(context, new SelectSet(), ext, false); // we don't use the selection here
			selPinsPoints = pinsPoints;
			condCls = cls;
		}

		/**
		 * Was an overlapping pin found during the last visit... call?
		 *
		 * @return The result described above
		 */
		public boolean getFound()
		{
			return found;
		}

		/**
		 * Overridden here to stop the traversal when we find an overlapping pin
		 */
		protected boolean prune(IGfxObject gobj)
		{
//			assert !(gobj instanceof IPin);
			if (gobj instanceof IPin) {
				return found || super.prune(gobj);
			}
			else {
				return super.prune(gobj);
			}
		}

		/**
		 * Overridden  here to check if this object is our overlapping pin
		 */
		protected boolean preDescend(IGfxObject gobj)
		{
			if (!found && gobj instanceof IPin) {
				IPin pin = (IPin) gobj;
				ILocation loc = pin.getAbsLocation();
				final Point point = new Point(loc.getX(), loc.getY());
				if (selPinsPoints.contains(point)) {
					final Set<IPin> pins = selPinsPoints.pull(point);
					if (pins != null && !pins.contains(pin) && hasDifferentParent(pin, pins)) {
						// found a pin that overlaps one of the input pins - can we connect it?
						if (ConnectCommand.connectionAllowed(pin, condCls)) {
							found = true;
						}
					}
				}
			}
			return false;
		}
	}

	private static boolean hasDifferentParent(@NotNull IPin pin, @NotNull Set<IPin> pins)
	{
		for (IPin aPin : pins) {
			if (aPin.getParent() != pin.getParent()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if this pin overlaps some other pin.  Add it to the set of pins/locations
	 *
	 * @param pin The pin to test
	 * @param donePins The set map having points versus set of pins on same location Pin in the set: The pins we've
	 * already tested Point: The locations of the pins, must use Points not ILocations in a hashset
	 *
	 * @return True if the pin is not in the donePins, but it's location is in the pts set
	 */
	private boolean checkPinOverlap(@NotNull IPin pin, @NotNull SetMap<Point, IPin> donePins)
	{
		ILocation loc = pin.getAbsLocation();
		final Point point = new Point(loc.getX(), loc.getY());
		donePins.add(point, pin);

		final Set<IPin> pins = donePins.pull(point);
		return pins != null && pins.size() > 1 && !allPinsAreFromSamePinlist(pins);
	}

	/**
	 * Find and store the location of a pin in the map of overlapping pins.
	 *
	 * @param pin The pin
	 * @param pinLocations The map of overlapping pins
	 */
	private static void storePinLocation(IPin pin, Map<ILocation, Set<IPin>> pinLocations)
	{
		ILocation loc = pin.getAbsLocation();
		Set<IPin> pins = pinLocations.get(loc);
		if (pins == null) {
			pins = new ListSet<>();
			pinLocations.put(loc, pins);
		}
		pins.add(pin);
	}

	private boolean checkHasAtleastTwoPinlistInSelection()
	{
		ISystemLogicDiagram currentDiagram = CommonUtils.cast(CAFUtils.getInstance().getActiveDiagram(), ISystemLogicDiagram.class);

		ISelectMgr selectMgr = CAFUtils.getInstance().getActiveSelectMgr();
		if (currentDiagram == null || selectMgr == null) {
			return false;
		}

		SelectSet selectSet = selectMgr.getCurrentSelections();

		Set<IPinList> pinlists = new HashSet<>();
		for (Selection selection : selectSet.getSelected()) {
			IDiagramObject diagramObject = CommonUtils.cast(selection.getObject(), IDiagramObject.class);
			if (diagramObject != null && currentDiagram.equals(DiagramHelper.getDiagram(diagramObject))) {
				addPinlistsInSelection(pinlists, diagramObject);
			}

			if (cablePinlistCount(pinlists) > 1) {
				return true;
			}
		}
		return false;
	}

	private long cablePinlistCount(@NotNull Set<IPinList> pinlists)
	{
		return pinlists.
				stream().
				map(IPinList::getConnectivity).
				map(cablePinList -> LogicUtils.getTopLevelPinList(cablePinList)).
				distinct().
				count();
	}

	private void addPinlistsInSelection(@NotNull Set<IPinList> pinLists, @NotNull IDiagramObject diagramObject)
	{
		if (IAbstractSchemPin.class.isAssignableFrom(diagramObject.getClass())) {
			IDiagramObject pinOwner = diagramObject.getParent();
			if(pinOwner != null && IPinList.class.isAssignableFrom(pinOwner.getClass())){
				addPinlist(pinLists, (IPinList) pinOwner);
			}
		}
		else if (IPinList.class.isAssignableFrom(diagramObject.getClass())) {
			addPinlist(pinLists, (IPinList) diagramObject);
		}
	}

	private void addPinlist(@NotNull Set<IPinList> pinLists, @NotNull IPinList pinList)
	{
		chs.cof.logical.cable.IPinList connectivityPinlist = pinList.getConnectivity();

		if (connectivityPinlist == null) {
			return;
		}

		if (IHarnessPlugConnector.class.isAssignableFrom(connectivityPinlist.getClass())) {
			List<IPinList> connectedDevices = getConnectedDevice(pinList);
			if (!connectedDevices.isEmpty()) {
				pinLists.addAll(connectedDevices);
			}
			else {
				pinLists.add(pinList);
			}
		}
		else if (IJackConnector.class.isAssignableFrom(connectivityPinlist.getClass())) {
			List<IPinList> attPlugConnectors = getConnectedPlugConnector(pinList);
			if (!attPlugConnectors.isEmpty()) {
				pinLists.addAll(attPlugConnectors);
			}
			else {
				pinLists.add(pinList);
			}
		}
		else if (!isIgnoredPinListType(connectivityPinlist)) {
			pinLists.add(pinList);
		}
	}

	@NotNull private List<IPinList> getConnectedPlugConnector(@NotNull IPinList pinList)
	{
		return pinList.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR).stream()
				.filter(aObj -> aObj.getConnectivity() instanceof IPlugConnector).collect(
						Collectors.toList());
	}

	@NotNull private List<IPinList> getConnectedDevice(@NotNull IPinList pinList)
	{
		return pinList.getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR).stream()
				.filter(aObj -> aObj.getConnectivity() instanceof IDevice).collect(
						Collectors.toList());
	}

	private boolean isIgnoredPinListType(@NotNull chs.cof.logical.cable.IPinList connectivityPinlist)
	{
		return IDeviceConnector.class.isAssignableFrom(connectivityPinlist.getClass()) ||
				ISplice.class.isAssignableFrom(connectivityPinlist.getClass()) ||
				IBlockDevice.class.isAssignableFrom(connectivityPinlist.getClass());
	}
}
