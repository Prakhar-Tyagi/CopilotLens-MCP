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

import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.IInterconnectObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IIncLoadable;
import chs.utilities.CollectionUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ICXHarness
{

	private ICXHarnessSet m_harnessSet;
	private Set<IDiagramObject> m_schemConnectors;
	private Set<IConnector> m_cableConnectors;
	private Set<IInterconnectConductor> m_cableConductors;
	private String m_name;

	public ICXHarness(IDesign design, ISchemDiagram activeDiagram, ICXHarnessSet harnessSet)
	{
		String icx_name = null;

		m_harnessSet = harnessSet;
		m_name = null;

		m_schemConnectors = new HashSet<IDiagramObject>();
		m_cableConnectors = new HashSet<IConnector>();
		m_cableConductors = new HashSet<IInterconnectConductor>();

		for (Iterator itr = m_harnessSet.iterator(); itr.hasNext();) {
			IInterconnectObject ico = (IInterconnectObject) itr.next();
			if (ico instanceof IConnector) {
				IConnector icx = (IConnector) ico;
				m_cableConnectors.add(icx);

				// Try to find a schem connector in an open diagram.
				List<ISchemDiagram> diagrams = CollectionUtils.<ISchemDiagram>createList(((ILogicDesign)design).getDiagrams());
				// Give the active diagram precedence
				diagrams.remove(activeDiagram);
				diagrams.add(0, activeDiagram);
				for (int i = 0; i < diagrams.size(); i++) {
					ISchemDiagram diagram = (ISchemDiagram) diagrams.get(i);
					if (diagram instanceof IIncLoadable && ((IIncLoadable) diagram).isSkeleton()) {
						continue;
					}
					for (IDiagramObjectIterator doItr = diagram.getRepresentations(icx.getUID()); doItr.hasNext();) {
						IDiagramObject dObj = doItr.getNext();
						if (dObj instanceof IPinList) {
							m_schemConnectors.add(dObj);
							break; // Can only use one.
						}
					}
				}
			}
			else if (ico instanceof IInterconnectConductor) {
				IInterconnectConductor cond = (IInterconnectConductor) ico;

				// Save the lexically smallest interconnect name.
				// This will be used as the harness name if the harness
				// attribute is not set.
				if (icx_name == null) {
					icx_name = cond.getName();
				}
				else if (cond.getName().compareTo(icx_name) < 0) {
					icx_name = cond.getName();
				}

				if (m_name == null || "".equals(m_name)) {
					// Get the first conductor's harness attribute as the harness name.
					m_name = cond.getHarness();
				}
				m_cableConductors.add(cond);
			}
			else {
				assert true : "Illegal object in ICXHarnessSet";
			}
		}

		// If harness attribute not set on any interconnect then use the interconnect name.
		// Since there can be more than one interconnect in a harness (multitermed at connector)
		// the icx_name will be the the first one lexically.
		if (m_name == null || "".equals(m_name)) {
			m_name = icx_name;
		}
	}

	public String getName()
	{
		return m_name;
	}

	public Set<IDiagramObject> getSchemConnectors()
	{
		return m_schemConnectors;
	}

	public Set<IConnector> getCableConnectors()
	{
		return m_cableConnectors;
	}

	public Set<IInterconnectConductor> getCableConductors()
	{
		return m_cableConductors;
	}

	public ICXHarnessSet getHarnessSet()
	{
		return m_harnessSet;
	}
}
