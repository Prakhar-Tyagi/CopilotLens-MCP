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

import chs.cof.draw.IGrid;
import chs.cof.draw.ITransform;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDrawPlusFactory;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IJackConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.CreationUtils;
import chs.common.ICommonFactory;
import chs.common.IExtent;
import chs.common.IProjectPreferenceMgr;
import chs.common.PreferenceContext;
import chs.common.preferencesets.IPreferenceSet;
import chs.system.FactoryMgr;
import chs.utilities.CHSConstants;
import chs.utilities.CollectionMap;
import chs.utility.DiagramHelper;
import chs.utility.GfxObjectUtils;
import chs.utility.gfx.Compactor;
import chs.utility.gfx.SeparatingCompactor;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.placement.IOneDimensionalTranslationUnit;
import chs.utility.placement.PlacementConstants;
import chs.utility.preferences.PreferenceSetHelper;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ICXDiagramGenerator
{

	private ISchemDiagram m_diagram;
	private Set m_icxSchemConnectors;
	private Map m_icxToWiringConnectorMap;
	private IProjectPreferenceMgr m_prefMgr;
	private int m_scale;
	private IGrid m_grid;
	private int m_connectorWidth;
	private ICommonFactory m_commFact;
	private IDrawPlusFactory m_drawplusFact;
	private Map m_derivedToICXMap;

	public ICXDiagramGenerator(ISchemDiagram diagram, Set icxSchemConnectors, Map cxToWiringConnectorMap,
			IProjectPreferenceMgr prefMgr)
	{
		m_diagram = diagram;
		m_grid = m_diagram.getGrid();
		m_icxSchemConnectors = icxSchemConnectors;
		m_icxToWiringConnectorMap = cxToWiringConnectorMap;
		m_prefMgr = prefMgr;
		int maxPins = 0;
		for (Iterator itr = m_icxToWiringConnectorMap.values().iterator(); itr.hasNext(); ) {
			IConnector icxConnector = (IConnector) itr.next();
			maxPins = Math.max(maxPins, icxConnector.getNumPins());
		}
		m_scale = Math.max(maxPins / 2, 2);

		m_commFact = FactoryMgr.getCommonFactory();
		m_drawplusFact = FactoryMgr.getDrawPlusFactory();
	}

	public void generateDiagram()
	{
		m_connectorWidth = m_prefMgr.getGeneratedConnectorWidth(PreferenceContext.LOGIC);
		final int defaultSeparation = m_connectorWidth * CHSConstants.PIN_SPACING;

		// Partition the connectors by diagram.
		Map diagramToSchemMap = partitionSchemConnectorsByDiagram();

		m_derivedToICXMap = new HashMap();
		Map prev = null;
		for (Iterator iterator = diagramToSchemMap.values().iterator(); iterator.hasNext(); ) {
			Collection connectors = (Collection) iterator.next();
			Map schemConnectors = generateSchemConnectors(connectors);
			if (prev != null) {
				// Push this partition down the diagram so there is no vertical overlap with the previous group
				int deltaY = (CoordinateHelper.minY(prev.keySet()) - CoordinateHelper.maxY(schemConnectors.keySet())) -
						defaultSeparation;
				CoordinateHelper.addY(schemConnectors.keySet(), deltaY);
			}
			m_derivedToICXMap.putAll(schemConnectors);
			prev = schemConnectors;
		}

		Collection generatedSchemConnectors = new ArrayList(m_derivedToICXMap.keySet());

		// Generate schem for all of the cable connectors that weren't represented on any open diagram
		Collection cableOnly = getOrphanedConnectivity();
		int yOffset = prev != null ? CoordinateHelper.minX(prev.keySet()) : 0;
		for (Iterator itr = cableOnly.iterator(); itr.hasNext(); ) {
			IConnector wiringConnector = (IConnector) itr.next();
			chs.cof.logical.schem.IPinList schemConnector = generateSchemConnector(wiringConnector, null, 0, 0);
			yOffset -=
					(defaultSeparation + schemConnector.getLocation().getY() + schemConnector.getExtent().getHeight());
			schemConnector.getLocation().setY(yOffset);
			generatedSchemConnectors.add(schemConnector);
		}

		Compactor compactor = new SeparatingCompactor(generatedSchemConnectors, PlacementConstants.VERTICAL)
		{
			protected int getMinimumHorizontalDistance(IOneDimensionalTranslationUnit cu1,
					IOneDimensionalTranslationUnit cu2)
			{
				int minHorizDist = Integer.MAX_VALUE;
				List dList1 = new ArrayList(cu1.elements());
				List dList2 = new ArrayList(cu2.elements());
				for (int i = 0; i < dList1.size(); i++) {
					IDiagramObject conn1 = (IDiagramObject) dList1.get(i);
					IDiagramObject icxConn1 = (IDiagramObject) m_derivedToICXMap.get(conn1);
					if (icxConn1 != null) {
						for (int j = 0; j < dList2.size(); j++) {
							IDiagramObject conn2 = (IDiagramObject) dList2.get(j);
							IDiagramObject icxConn2 = (IDiagramObject) m_derivedToICXMap.get(conn2);
							if (icxConn2 != null) {
								minHorizDist = Math.min(minHorizDist,
										GfxObjectUtils.calculateHorizontalDistance(icxConn1, icxConn2));
							}
							else {
								return defaultSeparation;
							}
						}
					}
					else {
						return defaultSeparation;
					}
				}
				return minHorizDist;
			}

			protected int getMinimumVerticalDistance(IOneDimensionalTranslationUnit cu1,
					IOneDimensionalTranslationUnit cu2)
			{
				int minVertDist = Integer.MAX_VALUE;
				List dList1 = new ArrayList(cu1.elements());
				List dList2 = new ArrayList(cu2.elements());
				for (int i = 0; i < dList1.size(); i++) {
					IDiagramObject conn1 = (IDiagramObject) dList1.get(i);
					IDiagramObject icxConn1 = (IDiagramObject) m_derivedToICXMap.get(conn1);
					if (icxConn1 != null) {
						for (int j = 0; j < dList2.size(); j++) {
							IDiagramObject conn2 = (IDiagramObject) dList2.get(j);
							IDiagramObject icxConn2 = (IDiagramObject) m_derivedToICXMap.get(conn2);
							if (icxConn2 != null) {
								minVertDist = Math.min(minVertDist,
										GfxObjectUtils.calculateVerticalDistance(icxConn1, icxConn2));
							}
							else {
								return defaultSeparation;
							}
						}
					}
					else {
						return defaultSeparation;
					}
				}
				return minVertDist;
			}
		};
		compactor.compact();
		compactor.shiftToZero();
	}

	private Map partitionSchemConnectorsByDiagram()
	{
		CollectionMap diagramToSchemMap = new CollectionMap();
		for (Iterator itr = m_icxSchemConnectors.iterator(); itr.hasNext(); ) {
			chs.cof.logical.schem.IPinList pinList = (chs.cof.logical.schem.IPinList) itr.next();
			ISchemDiagram diagram = DiagramHelper.getDiagram(pinList);
			diagramToSchemMap.add(diagram, pinList);
		}

		// We want to process the partitions in order by diagram name
		Map map = new TreeMap(NamedObjectComparator.caseSensitiveComparator());
		map.putAll(diagramToSchemMap);
		return map;
	}

	private Collection getOrphanedConnectivity()
	{
		Set orphans = new HashSet(m_icxToWiringConnectorMap.values());
		for (Iterator itr = m_icxSchemConnectors.iterator(); itr.hasNext(); ) {
			chs.cof.logical.schem.IPinList pinList = (chs.cof.logical.schem.IPinList) itr.next();
			orphans.remove(m_icxToWiringConnectorMap.get(pinList.getConnectivity()));
		}
		return orphans;
	}

	/**
	 * Derive schem Plug Connectors for the wiring diagram from a collection of schem Interconnect Connectors
	 *
	 * @param icxSchemConnectors
	 *
	 * @return a map from the derived connectors to their source interconnect connectors
	 */
	private Map generateSchemConnectors(Collection icxSchemConnectors)
	{
		Map derivedToICXMap = new HashMap(icxSchemConnectors.size());
		for (Iterator schemItr = icxSchemConnectors.iterator(); schemItr.hasNext(); ) {
			chs.cof.logical.schem.IPinList icxSchem = (chs.cof.logical.schem.IPinList) schemItr.next();
			IConnector icxConnector = (IConnector) icxSchem.getConnectivity();
			IPinList wiringConnector = (IPinList) m_icxToWiringConnectorMap.get(icxConnector);
			int x = icxSchem.getLocation().getX() * m_scale;
			int y = icxSchem.getLocation().getY() * m_scale;
			derivedToICXMap.put(generateSchemConnector(wiringConnector, icxSchem.getTransform(), x, y), icxSchem);
		}
		return derivedToICXMap;
	}

	private chs.cof.logical.schem.IPinList generateSchemConnector(IPinList wiringConnector, ITransform transform, int x,
			int y)
	{
		// for jacks, we must calculate the extent here and pass it down to the generator,
		// otherwise they are too thin (1 grid)
		Map<IAbstractPin, Point> pinMap = createPinMap(wiringConnector);
		IExtent ext = null; // null extent means the generator calculates the extent
		if (wiringConnector instanceof IJackConnector) {
			ext = calculateExtent(pinMap);
		}

		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(m_diagram);
		chs.cof.logical.schem.IPinList wiringSchem = CreationUtils
				.createSchemPinList(wiringConnector, x, y, pinMap, m_diagram, ext, styleSet);

		if (transform != null) {
			wiringSchem.getTransform().setTransform(transform);
		}

		// DR 454143: don't generate pin texts here, it happened when we created the pinlist.

		return wiringSchem;
	}

	private IExtent calculateExtent(Map<IAbstractPin, Point> pinMap)
	{
		// our extent starts at 0,0 and encompasses all pins, together with the width of the connector
		IExtent ext = FactoryMgr.getCommonFactory().createExtent();
		ext.setWidth(getConnectorWidth());
		for (Map.Entry<IAbstractPin, Point> e : pinMap.entrySet()) {
			int y = e.getValue().y;
			ext.setHeight(Math.max(y, ext.getHeight()));
		}
		return ext;
	}

	private int getConnectorWidth()
	{
		return m_connectorWidth * CHSConstants.PIN_SPACING;
	}

	private Map createPinMap(IPinList wiringConnector)
	{
		Map pinMap = new HashMap(wiringConnector.getNumPins());
		int y = 0;

		// Jack connectors on left.
		int xPos = 0;
		if (wiringConnector instanceof IPlugConnector) {
			// Plug connectors on right.
			xPos = getConnectorWidth();
		}

		for (IAbstractPinIterator pinsItr = wiringConnector.getPins(); pinsItr.hasNext(); ) {
			IAbstractPin pin = (IAbstractPin) pinsItr.next();
			pinMap.put(pin, new Point(xPos, y * CHSConstants.PIN_SPACING));
			y++;
		}
		return pinMap;
	}
}
