package chs.caplets.logic.actions.shared;

import chs.analysis.IVHDLModelMapping;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedPinList;
import chs.utility.helpers.SchemPinListHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 29 Apr, 2013 Time: 12:40:55 PM To change this template use File |
 * Settings | File Templates.
 */
public class InlineConnectorUnshareHelper extends ConnectorUnshareHelper
{

	public InlineConnectorUnshareHelper(IDesign theDesign, @Nullable ISchemDiagram diagram)
	{
		super(theDesign, diagram);
	}

	@Override protected void buildOldUsagesMap()
	{
		super.buildOldUsagesMap();
		IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
		if (cablePinListMate != null) {
			for (IAbstractPin pin : getPinsForUsages(cablePinListMate)) {
				for (IDesignSharedUsage usage : dwum.getUsages(pin)) {
					PinUsageInfo pinUsageInfo = new PinUsageInfo(usage.getDiagramObjectUID(), usage.getDiagramUID());
					oldPinUsageInfoMap.add(pin.getUID(), pinUsageInfo);
				}
			}
		}
	}

	@Override protected void populatePinStackPinInfo()
	{
		super.populatePinStackPinInfo();
		if (cablePinListMate != null) {
			populatePinStackPinRepsMap(cablePinListMate.getPins());
		}
	}

	protected void unshareThisPinList()
	{
		super.unshareThisPinList();
		//moved to inline
		if (cablePinListMate != null) {
			cablePinListMate.setSharedPinList(null);
			unshareBackShells(cablePinListMate);
		}
	}

	protected void transferModelMappingToCablePinList(ISharedPinList spl)
	{
		super.transferModelMappingToCablePinList(spl);
		//moved to inline
		if (cablePinListMate != null && cablePinListMate.getSharedPinList() != null) {
			IVHDLModelMapping mapping = cablePinListMate.getSharedPinList().getModelMapping();
			if (mapping != null) {
				cablePinListMate.setModelMapping(
						getModelMappingToBeAppliedOnUnshared(cablePinListMate, cablePinListMate.getSharedPinList(),
								mapping));
			}
		}
	}

	protected Set<ILogicObject> getLockableCableObjects()
	{
		Set<ILogicObject> lockables = new HashSet<>();
		lockables.addAll(super.getLockableCableObjects());
		if (cablePinListMate != null) {
			lockables.add(cablePinListMate);
		}
		return lockables;
	}

	protected void unsharePins()
	{
		super.unsharePins();
		//moved to inline
		if (cablePinListMate != null) {
			unsharePins(cablePinListMate);
		}
	}

	@Override protected void copyInfoFromSharedToLogicObject(ISharedPinList spl)
	{
		super.copyInfoFromSharedToLogicObject(spl);
		if (cablePinListMate != null) {
			ISharedConnector splMate = ((ISharedConnector) spl).getMate();
			replicator.replicateCopyableObject(splMate, cablePinListMate);
		}
	}

	@Override protected Collection<? extends IPinList> getAdditionalSchemObjectsToProcess()
	{
		Collection<IPinList> mates = new HashSet<IPinList>();
		for (IPinList schempl : schemPinLists) {
			mates.add((IPinList) SchemPinListHelper.getInlineMateObject(schempl));
		}
		return mates;
	}
}
