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
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.IInterconnectMember;
import chs.cof.logical.cable.IInterconnectMemberIterator;
import chs.cof.logical.cable.IInterconnectObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignSharedUsageMgr;
import chs.cof.logical.shared.IWriteableDSUM;
import chs.cof.project.IProject;
import chs.common.IUIDObject;
import chs.common.validation.ValidationHelper;
import chs.system.FactoryMgr;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.persist.DesignPersistenceUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: jamesmw Date: 28-Jun-2006 Time: 10:09:07 To change this template use File | Settings
 * | File Templates.
 */
public class GenerateWiringDiagramHelper
{

	private List<ICXHarness> m_harnessList;

	private ILogicDesign m_generatedDesign;
	private ISchemDiagram m_generatedDiagram;

	public boolean missingRepresentations()
	{
		for (ICXHarness harness : getHarnessList()) {
			if (harness.getCableConnectors().size() > harness.getSchemConnectors().size()) {
				return true;
			}
		}
		return false;
	}

	public boolean missingMembers()
	{
		for (Iterator<ICXHarness> hItr = getHarnessList().iterator(); hItr.hasNext(); ) {
			ICXHarness harness = hItr.next();
			for (Iterator cItr = harness.getCableConductors().iterator(); cItr.hasNext(); ) {
				IInterconnectConductor icxConductor = (IInterconnectConductor) cItr.next();
				if (!icxConductor.getMembers().hasNext()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Attempts to load all of the library parts referenced by the connectors and conductors on the harnesses being
	 * generated. Stops on the first unavailable part.
	 *
	 * @return true if a missing part was encountered, false if all parts were successfully loaded.
	 */
	public boolean missingLibraryData()
	{
		for (Iterator<ICXHarness> hItr = getHarnessList().iterator(); hItr.hasNext(); ) {
			ICXHarness harness = hItr.next();
			for (Iterator<IConnector> cItr = harness.getCableConnectors().iterator(); cItr.hasNext(); ) {
				IConnector icxConnector = cItr.next();
				if (icxConnector.getLibraryObject() == null) {
					return true;
				}
			}
			for (Iterator cItr = harness.getCableConductors().iterator(); cItr.hasNext(); ) {
				IInterconnectConductor icxConductor = (IInterconnectConductor) cItr.next();
				IInterconnectMemberIterator mItr = icxConductor.getMembers();
				while (mItr.hasNext()) {
					IInterconnectMember member = mItr.getNext();
					if (member.getLibraryObject() == null) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean findHarnesses(SelectSet sset, IDesign design, ISchemDiagram diagram, String icxName)
	{
		setHarnessList(new ArrayList<ICXHarness>());
		List<IInterconnectObject> icxList = new ArrayList<IInterconnectObject>();
		SelectedUIDObjectIterator objIter = sset.getSelectedUIDObjects();
		while (objIter.hasNext()) {
			IUIDObject obj = ReferenceHelper.reduceToLogicObject(objIter.getNext());
			if (!icxList.contains(obj)) {
				// Interconnect connectors and conductors only - we don't want interconnect devices.
				if (obj instanceof IInterconnectObject &&
						(obj instanceof IConductor || obj instanceof IConnector)) {
					icxList.add((IInterconnectObject) obj);
				}
			}
		}

		Iterator<IInterconnectObject> iter = icxList.iterator();
		while (iter.hasNext()) {
			IInterconnectObject ico = iter.next();
			if (!harnessListContains(getHarnessList(), ico)) {
				if (ico instanceof IInterconnectConductor) {
					ICXHarnessSet harnessSet = new ICXHarnessSet((IInterconnectConductor) ico);
					getHarnessList().add(new ICXHarness(design, diagram, harnessSet));
				}
				else if (ico instanceof IConnector) {
					IConnector icx = (IConnector) ico;
					ICXHarnessSet harnessSet = new ICXHarnessSet(icx);
					if (harnessSet.size() > 0) {
						getHarnessList().add(new ICXHarness(design, diagram, harnessSet));
					}
					else {
						icxName = icx.getName();
						return false;
					}
				}
			}
		}
		return !getHarnessList().isEmpty();
	}

	public void generateDiagram(ICXHarness harness, IProject project)
	{
		ICXConnectivityGenerator connectivityGenerator = new ICXConnectivityGenerator(getGeneratedDesign(),
				harness.getCableConnectors());
		Map<IConnector, IConnector> connectorMap = connectivityGenerator.generateConnectivity();
		ICXDiagramGenerator diagramGenerator = new ICXDiagramGenerator(getGeneratedDiagram(),
				harness.getSchemConnectors(), connectorMap, CAFUtils.getInstance().getCurrentProjectPreferences());
		diagramGenerator.generateDiagram();

		// Need to generate usages for the new design.
		IDesignSharedUsageMgr usageMgr = m_generatedDesign.getSharedUsageMgr();
		((IWriteableDSUM) usageMgr).regenerateUsages();

		// FEAT12834 Validation in customer environment - Need to validate before data is saved to DB
		ValidationHelper.validateAfterGeneration(m_generatedDesign);
		DesignPersistenceUtils.flushChanges(project, m_generatedDesign, false, false);
	}

	public void setInterconnectSourceInfo(ILogicDesign design, ISchemDiagram diagram, ICXHarness harness,
			IDesign srcDesign, ISchemDiagram srcDiagram)
	{
		design.setInterconnectSourceInfo(
				FactoryMgr.getLogicalFactory().constructInterconnectSourceInfo(FactoryMgr.createUID(),
						diagram.getUID(), srcDesign.getUID(), srcDiagram.getUID(),
						diagram.getName(), harness.getCableConductors()));

		m_generatedDesign = design;
		m_generatedDiagram = diagram;
	}

	private boolean harnessListContains(List<ICXHarness> harnessList, IInterconnectObject icx)
	{
		for (ICXHarness harness : harnessList) {
			ICXHarnessSet hset = harness.getHarnessSet();
			if (hset.contains(icx)) {
				return true;
			}
		}
		return false;
	}

	public List<ICXHarness> getHarnessList()
	{
		return m_harnessList;
	}

	public void setHarnessList(List<ICXHarness> harnessList)
	{
		m_harnessList = harnessList;
	}

	public ILogicDesign getGeneratedDesign()
	{
		return m_generatedDesign;
	}

	public void setGeneratedDesign(ILogicDesign generatedDesign)
	{
		m_generatedDesign = generatedDesign;
	}

	public ISchemDiagram getGeneratedDiagram()
	{
		return m_generatedDiagram;
	}

	public void setGeneratedDiagram(ISchemDiagram generatedDiagram)
	{
		m_generatedDiagram = generatedDiagram;
	}
}

