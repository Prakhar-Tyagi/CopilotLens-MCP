package chs.caplets.logic.merge;

import chs.cof.drawplus.IConnected;
import chs.cof.helpers.SegmentContainerHelper;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IHighwaySegment;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 16-Mar-2010 Time: 17:41:42
 */
public class HighwayMerger extends SegmentContainerMerger
{

	public HighwayMerger(ILogicObject sourceObject, ILogicObject targetObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceObject, targetObject, reporter);
	}

	void mergeChildrenConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		IGeneralHighway sourceHighway = (IGeneralHighway) sourceLogicObject;
		IGeneralHighway targetHighway = (IGeneralHighway) targetLogicObject;

		IConductorIterator conductorsIterator = sourceHighway.getConductors();
		while (conductorsIterator.hasNext()) {
			IConductor conductor = conductorsIterator.getNext();
			targetHighway.addConductor((IHighwayConductor) conductor);
		}

		IConductorIterator stackPinCondIter = sourceHighway.getStackPinConductors();
		while (stackPinCondIter.hasNext()) {
			IConductor conductor = stackPinCondIter.getNext();
			targetHighway.addStackPinConductor((IHighwayConductor) conductor);
		}
	}

	protected void mergeSchematic(IConnectivityRef sourceSchemObject, ILogicObject targetlogicObject)
	{
		super.mergeSchematic(sourceSchemObject, targetlogicObject);
		IHighwaySchematic highwaySchematic = (IHighwaySchematic) sourceSchemObject;
		highwaySchematic.setConnectivity((IHighway) targetlogicObject);

		highwaySchematic.reassignConnectivityforCompositeTextDecorations();
	}

	@Override
	protected void postSchematicMerge(IConnectivityRef schemSourceObject)
	{
		SegmentContainerHelper.updatePortGfx(schemSourceObject);
		super.postSchematicMerge(schemSourceObject);

		IHighwaySchematic highwaySchematic = (IHighwaySchematic) schemSourceObject;
		for (IConnected connected : highwaySchematic.getSegments()) {
			IHighwaySegment highwaySegment = ((IHighwaySegment) connected);
			highwaySegment.refreshTables();
		}
	}
}
