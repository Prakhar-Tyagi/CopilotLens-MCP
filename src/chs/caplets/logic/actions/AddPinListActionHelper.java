/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.helpers.ControllerActionRT;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevicePin;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.symbol.ISymbolDef;
import chs.cofUtils.CreationUtils;
import chs.ctf.caf.utils.IBlockPinProxy;
import chs.ctf.caf.utils.IPinProxy;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.helpers.PinListHelper;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.MouseInputAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Used for interactive addition of a schematic pinlist to a diagram, based on an existing connectivity pinlist.
 * <p>
 * Intended for use by actions to handle mouse events for drawing/placing a pinlist and adding pins
 * <p>
 * TODO jacobt FEAT13040 : javadoc these action helpers more
 */
public class AddPinListActionHelper extends MouseInputAdapter
		// NOTE: actually we don't *have* to extend MouseInputAdapter or even implement MouseInputListener here
		// 		 because clients generally listen for events and delegate them to this object
		//       however it seems convenient to do so to get all our NO-OP implementations
		//       and there might be a scenario where this object is added as a mouse listener in it's own right
{

	private State state = State.PLACING_PARAMETERIZED;

	private ControllerActionRT action;
	private List<IPinProxy> proxies;
	@Nullable private ISymbolDef symDef;
	private boolean autogenerate;
	private boolean reference = false;
	private boolean placeAsStack = false;
	private boolean placeAsGroup = false;
	@Nullable private AddPinActionHelper addPinActionHelper;
	@Nullable private IPinList displayObject;

	public void keyPressed(KeyEvent event)
	{
		if (addPinActionHelper != null) {
			addPinActionHelper.keyPressed(event);
		}
	}

	public enum State
	{

		/**
		 * Start of parameterized instance placemet - e.g. moving the preview graphic around the diagram before clicking
		 * to place the first corner
		 */
		PLACING_PARAMETERIZED,
		/**
		 * Drawing a parameterised pinlist - e.g. drawing the shape of the object(s) after clicking once for the
		 * location
		 */
		DRAWING_PARAMETERISED,
		/**
		 * Start of symbol instance placement - e.g. moving the preview graphic around the diagram before clicking to
		 * place
		 */
		PLACING_SYMBOL,
		/**
		 * Placing pins on a parameterized instance - detailed state handled by AddPinActionHelper
		 */
		PLACING_PINS,
		/**
		 * All UI interaction complete.
		 */
		COMPLETE
	}

	/**
	 * Constructor accepts required parameters common to all types of interactive placement of a schematic pinlist.
	 * <p>
	 * Use setup methods below for further, optional/alternative parameters
	 *
	 * @param action Used by lower level helper for adding pins
	 */
	public AddPinListActionHelper(ControllerActionRT action)
	{
		this.action = action;
	}

	/**
	 * Setup this helper for interactively drawing a parameterized schematic pinlist instance with pins on a diagram
	 *
	 * @param proxies The connectivity pins for which we will add schematic pins
	 * @param autogenerate Option to automatically add schematic pins to the schematic instance once interactively
	 * placed
	 */
	public void setup(List<IPinProxy> proxies, boolean autogenerate)
	{
		setup(proxies, autogenerate, false, false, false);
	}

	/**
	 * Setup this helper for interactively drawing a parameterized schematic pinlist instance with pins on a diagram
	 *
	 * @param proxies The connectivity pins for which we will add schematic pins
	 * @param autogenerate Option to automatically add schematic pins to the schematic instance once interactively
	 * placed
	 * @param referencePins Option to generate pins as reference pins.
	 */
	public void setup(List<IPinProxy> proxies, boolean autogenerate, boolean referencePins, boolean placeAsStack,
			boolean placeAsGroup)
	{
		assert proxies != null;
		this.proxies = proxies;
		this.autogenerate = autogenerate;
		reference = referencePins;
		this.placeAsStack = placeAsStack;
		this.placeAsGroup = placeAsGroup;
		symDef = null;
	}

	/**
	 * Setup this helper for interactively placing a symbolled schematic pinlist instance on a diagram
	 *
	 * @param symDef The symbol to place
	 */
	public void setup(ISymbolDef symDef)
	{
		assert symDef != null;
		this.symDef = symDef;
		proxies = null;
		autogenerate = false;
	}

	/**
	 * Clear the internal state of this object
	 */
	public void clear()
	{
		state = State.COMPLETE;
		action = null;
		proxies = null;
		symDef = null;
		displayObject = null;
		if (addPinActionHelper != null) {
			// TODO jacobt FEAT13040 : use .cleanup here?
			addPinActionHelper.clearTransientGraphics();
		}
		addPinActionHelper = null;
	}

	/**
	 * Acceess method for setting the display object used when interactively adding pins.
	 *
	 * @param displayObject A schematic pinlist, typically a temp
	 */
	public void setDisplayObject(IPinList displayObject)
	{
		this.displayObject = displayObject;
	}

	/**
	 * Access method for current state of UI interaction on this helper
	 *
	 * @return The current state of UI interaction
	 */
	public State getState()
	{
		return state;
	}

	/**
	 * Add the pins created interactively here to the diagram.
	 * <p>
	 * These should be called from action onTerminate implementations when all interaction is done
	 * <p>
	 *
	 * @param diagram The diagram on which to create the pins
	 * @param schemPinlist The schem pinlist on which to create the pins
	 *
	 * @return true if the objects were added successfully - false here means a coding error + assert
	 */
	public boolean addPins(ISchemDiagram diagram, IPinList schemPinlist,
			CompositePinConnectivityFinder connectivityFinder)
	{
		if (schemPinlist != null && diagram != null) {
			if (autogenerate) {
				// TODO jacobt FEAT13040 : There's no reason not to create the missing cable pins from the proxies here
				// actually it might be better to pass the proxies down to the autogen method
				List<IAbstractPin> pins = getPinsFromProxies(proxies);

				PinListAddPinHelper
						.autogenerateSchematicPins(diagram, schemPinlist, null, pins, reference, connectivityFinder);
			}
			else if (addPinActionHelper != null) { // may be null if there were no pins
				addPinActionHelper.addPins(schemPinlist, diagram, connectivityFinder);
			}
			return true;
		}
		assert false : "addPins is not implemented in this case";
		return false;
	}

	public void regenerateGraphics(IPinList pinList)
	{
		if (addPinActionHelper != null) {
			addPinActionHelper.regenerateGraphics(pinList);
		}
	}

	/**
	 * Get a list of pins from a list of pin proxies.
	 * <p>
	 * Don't create any cable pins, only add the cable pin to the list if the proxy has one.
	 *
	 * @param pinProxies The pin proxies - which may have a cable pin
	 *
	 * @return The existing connectivity pins
	 */
	public static List<IAbstractPin> getPinsFromProxies(List<IPinProxy> pinProxies)
	{
		List<IAbstractPin> pins = new ArrayList<IAbstractPin>(pinProxies.size());
		for (IPinProxy pp : pinProxies) {
			IAbstractPin pin = pp.getCablePin();
			if (pin != null) {
				pins.add(pin);
			}
		}
		return pins;
	}

	/**
	 * Iterate all proxies..check if there exists a cable pin corresponding to the proxy. If not, create a cable pin
	 * <p>
	 * and add it to the pinlist
	 *
	 * @param pinlist The cable object to which the pins have to be added
	 */
	public void createMissingCablePinsFromProxies(chs.cof.logical.cable.IPinList pinlist)
	{
		for (IPinProxy pp : proxies) {
			IGenericPin pin = pp.getCablePin();
			if (pin == null) {
				IInternalPin internalPin = pp.getInternalPin();
				if (internalPin != null) {
					pin = PinListHelper.replicateInternalPinAsDevicePin(internalPin);
					pp.setInternalPin(null);
				}
				else {
					pin = CreationUtils.createPin(pinlist);
				}
				pinlist.addPin((IAbstractPin) pin);

				if (internalPin != null) {
					internalPin.delete();
				}
				if (pin instanceof IBlockDevicePin && pp instanceof IBlockPinProxy) {
					IBlockDevicePin blockpin = (IBlockDevicePin) pin;
					blockpin.setAssociatedPinName(((IBlockPinProxy) pp).getAssociatedPinName());
					blockpin.setAssociatedObject(((IBlockPinProxy) pp).getAssociatedObject());
					blockpin.setAssociatedObjectType(((IBlockPinProxy) pp).getAssociatedObjectType());
					blockpin.setName(((IBlockPinProxy) pp).getNameOfActualBlockPin());
				}
				else {
					pin.setName(pp.getName());
				}
				pp.setCablePin(pin);
			}
			LogicUtils.setMatchingShortDescriptionFromOTI(pin, pin.getProject());
		}
	}

	/**
	 * Variant of the above method for inlines
	 *
	 * @param diagram The diagram on which to create the pinlist
	 * @param schemPlug The newly created schem plug
	 * @param schemJack The newly created schem jack
	 *
	 * @return true of the objects were successfully added - false here means a coding error + assert
	 */
	public boolean addPins(ISchemDiagram diagram, IPinList schemPlug, IPinList schemJack)
	{
		// TODO jacobt FEAT13040 : clean this up - some clients must pass pinlist + mate, some must pass plug + jack!!
		if (diagram != null && schemPlug != null) {
			if (autogenerate) {
				// client must pass plug + jack (?)
				if (schemJack != null) {
					autogenerateInlinePins(diagram, schemPlug, schemJack);
					return true;
				}
			}
			else if (addPinActionHelper != null) {
				// client must pass pinlist + possibly null mate
				addPinActionHelper.addPins(schemPlug, schemJack, diagram);
				return true;
			}
		}
		assert false : "addPins is not implemented in this case";
		return false;
	}

	private void autogenerateInlinePins(ISchemDiagram diagram, IPinList schemPlug, IPinList schemJack)
	{
		if (proxies.isEmpty()) {
			return;
		}

		// the connectivity pins should already be setup for an inline plug or jack
		// partition the pins into plug/jack pins
		// if for some reason the specified pins are not all inline pins with connected pin then give up on pin generation
		List<IAbstractPin> plugPins = new ArrayList<IAbstractPin>();
		List<IAbstractPin> jackPins = new ArrayList<IAbstractPin>();
		if (!partitionInlinePins(plugPins, jackPins)) {
			return; // can't partition the mated pins
		}

		// autogenerate of the plug pins will take care of the jack pins
		PinListAddPinHelper.autogenerateSchematicPins(diagram, schemPlug, schemJack, plugPins, reference);

		// it seems that we now allow pins missing on inline halves
		// e.g. if these halves have library parts then we don't generate the pin on the other side
		// if no mating info found in the library part
		//FEAT00013786: At this point we do not expect pinlist to have stack pins
		assert schemPlug.getPins().size() == proxies.size();
		assert schemJack.getPins().size() <= proxies.size();
	}

	private boolean partitionInlinePins(List<IAbstractPin> plugPins, List<IAbstractPin> jackPins)
	{
		List<IAbstractPin> abstractPins = new ArrayList<IAbstractPin>(proxies.size());
		for (IPinProxy pp : proxies) {
			IAbstractPin pin = pp.getCablePin();
			if (pin == null) {
				continue;
			}
			abstractPins.add(pin);
		}
		return partitionInlinePins(plugPins, jackPins, abstractPins);
	}

	public static boolean partitionInlinePins(List<IAbstractPin> plugPins, List<IAbstractPin> jackPins,
			List<IAbstractPin> abstractPins)
	{
		for (IAbstractPin pin : abstractPins) {
			chs.cof.logical.cable.IPinList owner = pin.getOwner();
			if (!(owner instanceof IGenericInlineConnector)) {
				assert false;
				return false;
			}

			Collection<IAbstractPin> pins = pin.getConnectedPins();
			IAbstractPin matePin = !pins.isEmpty() ? pins.iterator().next() : null;
			if (matePin == null) {
				// might happen in future functionality? or for CAVAI?
				return false;
			}

			if (owner instanceof IInlinePlugConnector) {
				plugPins.add(pin);
				jackPins.add(matePin);
			}
			else {
				plugPins.add(matePin);
				jackPins.add(pin);
			}
		}
		return true;
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		if (state == State.PLACING_PINS) {
			if (addPinActionHelper != null) {
				addPinActionHelper.mouseMoved(e);
			}
			else {
				assert false;
			}
		}
	}

	@Override public void mouseReleased(MouseEvent e)
	{
		if (state == State.PLACING_PARAMETERIZED) {
			if (symDef == null) {
				state = State.DRAWING_PARAMETERISED;
			}
			else {
				state = State.PLACING_SYMBOL;
			}
		}
		else if (state == State.DRAWING_PARAMETERISED) {
			// finished drawing parameterized pinlist
			// may now add pins manually or autogenerate
			if (proxies.isEmpty() || autogenerate) {
				state = State.COMPLETE;
			}
			else {
				// manual placement of pins using AddPinActionHelper
				addPinActionHelper = new AddPinActionHelper(action, requirePlacement(), useBoundaryExensions());
				addPinActionHelper.setPlaceAsStack(placeAsStack);
				addPinActionHelper.setPlaceAsGroup(placeAsGroup);
				addPinActionHelper.setUp(displayObject, proxies);
				addPinActionHelper.setIsReference(reference);
				state = State.PLACING_PINS;
			}
		}
		else if (state == State.PLACING_PINS) {
			// all further state management is done by AddPinActionHelper, which may also terminate the action!
			if (addPinActionHelper != null) {
				addPinActionHelper.mouseReleased(e);
			}
			else {
				assert false;
			}
		}
	}

	private boolean requirePlacement()
	{
		// TODO jacobt FEAT13040
		return true;
	}

	private boolean useBoundaryExensions()
	{
		// TODO jacobt FEAT13040
		return true;
	}
}