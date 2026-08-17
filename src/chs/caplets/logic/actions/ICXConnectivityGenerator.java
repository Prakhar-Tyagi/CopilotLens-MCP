/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.ICableFactory;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnPin;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IInterconnectSourceInfo;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryObject;
import chs.common.INamedObject;
import chs.common.IProperty;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.utility.attr.AttributeUtils;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.PropertyCopier;
import chs.utility.modular.ModuleCodesHelper;
import chs.utility.ui.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ICXConnectivityGenerator
{

	private IDesign m_wiringDesign;
	private Set m_icxConnectors;
	private static final Comparator<ILibraryCavity> CavityComparator = new Comparator<ILibraryCavity>()
	{
		public int compare(ILibraryCavity cav1, ILibraryCavity cav2)
		{
			return UIUtils.compareAlphaNumStrings(cav1.getName(),
					cav2.getName());
		}
	};

	public ICXConnectivityGenerator(IDesign wiringDesign, Set icxConnectors)
	{
		m_wiringDesign = wiringDesign;
		m_icxConnectors = icxConnectors;
	}

	/**
	 * Produces a mapping from interconnect connectors to simple connectors.
	 *
	 * @return the mapping
	 */
	public Map<IConnector, IConnector> generateConnectivity()
	{
		IInterconnectSourceInfo sourceInfo = m_wiringDesign.getInterconnectSourceInfo();
		Map<IConnector, IConnector> icxToWiringConnectorMap = new HashMap<IConnector, IConnector>();
		IConnectivity gConnectivity = m_wiringDesign.getConnectivity();
		for (Iterator opItr = m_icxConnectors.iterator(); opItr.hasNext();) {
			IConnector icxConnector = (IConnector) opItr.next();

			ICableFactory factory = FactoryMgr.getCableFactory();
			IUID uid = FactoryMgr.getCommonFactory().createUID();
			IConnector wConnector;
			if (icxConnector.isPlug()) {
				wConnector = factory.createPlugConnector(uid);
			}
			else {
				wConnector = factory.createJackConnector(uid);
			}

			gConnectivity.addConnector(wConnector);
			sourceInfo.addConnectorDerivation(icxConnector, wConnector);
			AttributeUtils.copyDataModelAttributes(icxConnector, wConnector, null);
			ModuleCodesHelper.copyFunctionalModuleCodes(icxConnector, wConnector);
			PropertyCopier.copyAllProperties(wConnector, icxConnector);
			wConnector.setOptionExpression(icxConnector.getOptionExpression());
			ILibraryObject libConnector = (ILibraryObject) icxConnector.getLibraryObject();
			if (libConnector != null) {
				wConnector.assignLibraryPart(libConnector);

				List<ILibraryCavity> cavities = new ArrayList<ILibraryCavity>(LibraryHelper.getCavities(libConnector));
				Collections.sort(cavities, CavityComparator);
				Collections.reverse(cavities);
				for (ILibraryCavity cavity : cavities) {
					IAbstractPin pin = FactoryMgr.getCableFactory()
							.createPinForOwner(FactoryMgr.getCommonFactory().createUID(), wConnector);
					wConnector.addPin(pin);
					pin.setName(cavity.getName());
					pin.assignLibraryCavity(cavity);
				}
			}

			icxToWiringConnectorMap.put(icxConnector, wConnector);

			//
			// Build up assembly name for property
			//
			List nesting = new ArrayList();
			//
			// 308053 - Need to include the interconnect connector itself.
			//
			nesting.add(icxConnector.getName());
			INamedObject nobj = null;
			IDevice dev = null;
			if (icxConnector instanceof IDeviceOwned) {
				IDeviceOwned owned = (IDeviceOwned) icxConnector;
				dev = owned.getOwner(IDevice.class);
			}
			IAbstractPinIterator apitr = icxConnector.getPins();
			if (dev != null) {
				//
				// Is there a DC - if so, add this?
				//
				if (apitr.getSize() != 0) {
					IAbstractPin ap = apitr.getNext();
					IAbstractPin devpin = ap.getConnectedPin(dev);
					if (devpin instanceof IDevicePin) {
						IDeviceConnPin dcp = ((IDevicePin) devpin).getDeviceConnectorPin();
						if (dcp != null) {
							nesting.add(dcp.getOwner().getName());
						}
					}
				}
				//
				// Add the device.
				//
				nesting.add(dev.getName());
				nobj = dev;
			}
			else {
				nobj = icxConnector;
			}

			while ((nobj = ((ILogicObject) nobj).getAssembly()) != null) {
				nesting.add(nobj.getName());
			}
			if (!nesting.isEmpty()) {
				//
				// 308055 - Build up the list from inner to outer (not the reverse which was spec.d)
				//
				StringBuffer nestedAssembly = new StringBuffer();
				for (int i = 0; i < nesting.size(); i++) {
					if (i != 0) {
						nestedAssembly.append("|");
					}
					nestedAssembly.append(nesting.get(i));
				}

				IProperty prop = FactoryMgr.getCommonFactory().constructProperty(
						"ASSEMBLY_HIERARCHY",
						FactoryMgr.getCommonFactory().createdStringValue(nestedAssembly.toString()), wConnector);
				wConnector.addProperty(prop);
			}
		}
		return icxToWiringConnectorMap;
	}
}
