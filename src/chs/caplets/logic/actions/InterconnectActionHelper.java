/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.logic.Model;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.schem.IPin;
import chs.cof.parts.ILibraryBaseConnector;
import chs.cof.parts.ILibraryConnector;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.parts.ILibraryDeviceFootprintConnectorDetail;
import chs.cof.parts.ILibraryDeviceFootprintObject;
import chs.cof.parts.ILibraryDeviceFootprintPinMapping;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.Library;
import chs.cof.parts.LibraryCriteriaHelper;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.parts.partselector.ILibraryPartSelector;
import chs.cof.parts.partselector.PartSelectionContext;
import chs.cof.project.IProject;
import chs.common.IPreferenceMgr;
import chs.common.IUIDObject;
import chs.common.attr.IAttributeTypes;
import chs.common.criteria.ICriteria;
import chs.common.criteria.Restrictions;
import chs.common.query.IQueryXMLKeys;
import chs.utilities.CHSConstants;
import chs.utility.helpers.LibraryHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Contains implementations common to CreateInterconnectConnectorAction and CreateInlineInterconnectConnectorAction.
 */

public class InterconnectActionHelper
{

	/**
	 * Prompts the user to select a part.
	 *
	 * @param model
	 * @param controller
	 *
	 * @return the chosen part or null if no part was chosen.
	 */
	@Nullable public static ILibraryPartSelection loadPart(Model model, ICapletController controller)
	{
		IDesign des = model.getDesign();
		IProject proj = des.getProject();
		IPreferenceMgr prefMgr = proj.getPreferences();

		//The DeviceConnector of the selected device pin to be interconnected
		ILibraryDeviceFootprintObject deviceConnector = null;
		//
		// Default is >= 1 pin. May override...
		//
		int pinCount = 1;
		String strCompareType = IQueryXMLKeys.GREATER_THAN_EQUAL;

		//
		// Is there one and only one ICX Pin selected? If so, then use it to constrin our choices
		//
		IPin icxPin = getSelectedInterconnectPin(controller.getSelectMgr().getCurrentSelections());
		if (icxPin != null) {
			IAbstractPin apin = icxPin.getConnectivity();
			//
			// Need to get the Device Connector pin count
			//
			IDevice dev = (IDevice) apin.getOwner();
			ILibraryDeviceFootprint fp = dev.getFootprint();
			if (fp != null) {
//				Collection<ILibraryDeviceFootprintPinMapping> c = fp.getFootprintPinMappings();
				int pc = 0;
				for (ILibraryDeviceFootprintConnectorDetail connDetail : fp.getLibraryDeviceFootprintConnectorDetails()) {
				for (ILibraryDeviceFootprintPinMapping lfpm : connDetail.getFootprintPinMappings()) {
					if (connDetail.getConnectorName().equals(apin.getName())) {
						pc++;
						//
						// As we are scanning the pins - if we get a match, get the part number
						// of the device connector - used as an added constraint.
						//
						if (deviceConnector == null && apin instanceof IDevicePin) {
							IDeviceConnPin dcp = ((IDevicePin) apin).getDeviceConnectorPin();
							if (dcp != null) {
								deviceConnector = connDetail.getConnector();
							}
						}
					}
				}
			}
				//
				// Some pins - use that as a constraint
				//
				if (pc != 0) {
					pinCount = pc;
					strCompareType = IQueryXMLKeys.EQUALS;
				}
			}
		}

		ICriteria<ILibraryConnector> criteria = LibraryCriteriaHelper.createCriteria(ILibraryConnector.class);

		if (IQueryXMLKeys.GREATER_THAN_EQUAL.equals(strCompareType)) {
			criteria.restriction(Restrictions.GE(IAttributeTypes.NUM_CAVITIES, pinCount));
		}
		else {
			criteria.restriction(Restrictions.EQ(IAttributeTypes.NUM_CAVITIES, pinCount));
		}
		//Add the PartNumber of the mated connector to the restrictions
		if (deviceConnector != null && deviceConnector instanceof ILibraryBaseConnector) {
			LibraryCriteriaHelper.addMatingRestriction((ILibraryBaseConnector)deviceConnector, criteria, true);
		}

		ILibraryPartSelector partSelector =
				Library.getInstance().getLibraryPartSelector(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
		PartSelectionContext partSelectionContext = new PartSelectionContext();

//		Symbol preference selection is not required for interconnectors
		partSelectionContext.setSelectionFilter(LibraryCriteriaHelper.getSelectionFilterForNoSymbols(
				null,null,LibraryCriteriaHelper.getCustomerDetailsFromScopes(CAFUtils.getInstance().getActiveDesignContainer(), CAFUtils.getInstance().getCurrentProject())
		));
		//@todo used library configuration context directly, needs confirmation - kjuthi
		return partSelector.selectPart(criteria, proj, partSelectionContext, ConfigurationTypeEnum.LOGICAL,des);
	}

	/**
	 * Finds an interconnect pin within the current selection set.
	 *
	 * @param selections
	 *
	 * @return the pin or null if there is no suitable pin.
	 */
	public static IPin getSelectedInterconnectPin(SelectSet selections)
	{
		SelectionIterator sitr = selections.getSelected();
		if (selections.getSelectCount() != 1) {
			return null;
		}
		IUIDObject uobj = sitr.getNext().getObject();
		if (uobj instanceof IPin) {
			IAbstractPin apin = ((IPin) uobj).getConnectivity();
			if (apin.isInterconnect() && apin.getOwner() instanceof IDevice) {
				return ((IPin) uobj);
			}
		}
		return null;
	}

	public static int verticalOffset(int height)
	{
		return (height / (2 * CHSConstants.PIN_SPACING) * CHSConstants.PIN_SPACING);
	}

	/**
	 * Convert a list of pins for a generic connector into one appropriate for an interconnect connector.
	 *
	 * @param addedPins List of pins suitable for generic connector
	 * @param selection Library selection, could be null - used to set pin name for interconnect connector added from
	 * library part
	 *
	 * @return List of pins suitable for inconnect connector
	 */
	public static List<IPin> convertPins(List<IPin> addedPins, ILibraryPartSelection selection)
	{
		if (addedPins.isEmpty()) {
			assert false : "No pins for interconnect connector";
			return Collections.emptyList();
		}

		// Force only 1 pin
		IPin spin = addedPins.iterator().next();

		// FEAT3271 - no longer force removal of the pin attributes, it is up to styling to
		//decide what texts should be present.

		// make the pin interconnect
		IAbstractPin apin = spin.getConnectivity();
		apin.setInterconnect(true);

		// use the name of the first available cavity on the library part
		if (selection != null) {
			ILibraryObject libObj = selection.getSelectedObject();
			if (libObj != null && libObj.getNumCavities() > 0) {
				String cavityName = LibraryHelper.getCavities(libObj).iterator().next().getName();
				apin.setName(cavityName);
			}
		}

		return Collections.singletonList(spin);
	}
}
