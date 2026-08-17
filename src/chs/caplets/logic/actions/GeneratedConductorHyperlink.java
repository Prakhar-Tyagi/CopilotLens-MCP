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
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.IInterconnectSourceInfo;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUIDObject;
import chs.common.IUIDObjectIterator;
import chs.common.UIDObjectIterator;
import chs.common.UIDUtils;
import chs.system.FactoryMgr;

import java.util.ArrayList;
import java.util.Collection;

public class GeneratedConductorHyperlink extends AbstractInterconnectChildHyperlink
{

	IInterconnectConductor m_parent;

	GeneratedConductorHyperlink(ISchemDiagram currentDiagram, IDesign design, ISchemDiagram diagram,
			IInterconnectConductor icxConductor)
	{
		super(currentDiagram, design, diagram);
		m_parent = icxConductor;
	}

	/**
	 * Like MulticoreUtils.findDescendents() but less constrained, allowing traversal into descendents inside overbraids.
	 * <p/>
	 * Could be moved somewhere more generic but are there other clients needing this functionality?
	 */
	private static void findDescendants(IMulticore multicore, Collection<ILogicObject> descendants)
	{
		IShieldConductor shield = multicore.getShield();
		if (shield != null) {
			descendants.add(shield);
		}

		IShieldBody sb = multicore.getShieldBody();
		if (sb != null) {
			descendants.add(sb);
		}

		for (IConductorIterator condIt = multicore.getConductors(); condIt.hasNext();) {
			IConductor cond = condIt.getNext();
			descendants.add(cond);
		}

		for (IMulticoreIterator mcIt = multicore.getMulticores(); mcIt.hasNext();) {
			IMulticore mc = mcIt.getNext();
			descendants.add(mc);
			findDescendants(mc, descendants);
		}
	}

	public IDiagramObjectIterator getDiagramObjects()
	{
		Collection diagramObjects = new ArrayList(1);
		if (ensureDiagramLoaded()) {
			IInterconnectSourceInfo isi = getDesign().getInterconnectSourceInfo();
			for (IUIDObjectIterator itr =
					new UIDObjectIterator(UIDUtils.convertToUIDObject(isi.getDerivedConductorUIDs(m_parent)));
					itr.hasNext();) {
				IUIDObject uo = itr.getNext();
				if (uo instanceof IConductor) {
					diagramObjects.addAll(getDiagram().getRepresentationsCollection(uo.getUID()));
				}
				else if (uo instanceof IMulticore) {
					Collection<ILogicObject> descendants = new ArrayList<ILogicObject>();
					findDescendants((IMulticore) uo, descendants);
					for (ILogicObject descendant : descendants) {
						diagramObjects.addAll(getDiagram().getRepresentationsCollection(descendant.getUID()));
					}
				}
			}
		}
		return FactoryMgr.getDrawPlusFactory().createDiagramObjectIterator(diagramObjects);
	}

	public double getConfidence()
	{
		return 0.8;
	}
}
