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

import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IInterconnectConnector;
import chs.cof.logical.cable.IInterconnectSourceInfo;
import chs.cof.logical.schem.ISchemDiagram;
import chs.system.FactoryMgr;

import java.util.ArrayList;
import java.util.Collection;

public class GeneratedConnectorHyperlink extends AbstractInterconnectChildHyperlink
{

	IConnector m_parent;

	GeneratedConnectorHyperlink(ISchemDiagram currentDiagram, IDesign design, ISchemDiagram diagram,
			IConnector icxConnector)
	{
		super(currentDiagram, design, diagram);
		m_parent = icxConnector;
	}

	GeneratedConnectorHyperlink(ISchemDiagram currentDiagram, IDesign design, ISchemDiagram diagram,
			IConnectorPin icxPin) throws IllegalArgumentException
	{
		super(currentDiagram, design, diagram);
		assert (icxPin.getOwner() instanceof IInterconnectConnector);
		if (!(icxPin.getOwner() instanceof IInterconnectConnector)) {
			throw new IllegalArgumentException("pin must belong to an Interconnect Connector");
		}
		m_parent = (IInterconnectConnector) icxPin.getOwner();
	}

	public IDiagramObjectIterator getDiagramObjects()
	{
		Collection diagramObjects = new ArrayList(1);
		if (ensureDiagramLoaded()) {
			IInterconnectSourceInfo isi = getDesign().getInterconnectSourceInfo();
			diagramObjects.addAll(getDiagram().getRepresentationsCollection(isi.getDerivedConnectorUID(m_parent)));
		}
		return FactoryMgr.getDrawPlusFactory().createDiagramObjectIterator(diagramObjects);
	}

	public double getConfidence()
	{
		return 0.9;
	}
}
