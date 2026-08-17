package chs.caplets.logic.merge;

import chs.cof.drawplus.ICompositeTextDecorationText;
import chs.cof.drawplus.IConnected;
import chs.cof.helpers.SegmentContainerHelper;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPhysicalConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.utility.helpers.ConductorHelper;
import chs.utility.helpers.HighwayHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 16-Mar-2010 Time: 17:50:42
 */
public class ConductorMerger extends SegmentContainerMerger
{

	public ConductorMerger(ILogicObject sourceObject, ILogicObject targetObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceObject, targetObject, reporter);
	}

	void mergeChildrenConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		IConductor sourceConductor = (IConductor) sourceLogicObject;
		IConductor targetConductor = (IConductor) targetLogicObject;

		IAbstractPinIterator pinsIterator = sourceConductor.getPins();
		while (pinsIterator.hasNext()) {
			IAbstractPin pin = pinsIterator.getNext();
			movePin(sourceConductor, targetConductor, pin);
		}
		if (sourceConductor instanceof IHighwayConductor) {
			Set<IGeneralHighway> highways = ((IHighwayConductor) sourceConductor).getHighways();
			for (IGeneralHighway highway : highways) {
				highway.removeConductor((IHighwayConductor) sourceConductor);
				highway.addConductor((IHighwayConductor) targetConductor);
			}
		}
		// Source conductor needs to be deleted, so remove this object from stacked highway representations
		removeSourceConductorFromStackedHighways();
	}

	protected void movePin(IConductor sourceConductor, IConductor targetConductor, IAbstractPin pin)
	{
		ConductorHelper.PhysicalConductorHandler physicalConductorHandler =
				sourceConductor instanceof IPhysicalConductor ?
						new ConductorHelper.PhysicalConductorHandler((IPhysicalConductor) sourceConductor) : null;

		pin.removeConductor(sourceConductor);
		pin.addConductor(targetConductor);
		if (physicalConductorHandler != null) {
			physicalConductorHandler.updateWireEndDetails((IPhysicalConductor) targetConductor);
		}
	}

	protected void mergeSchematic(IConnectivityRef sourceSchemObject, ILogicObject targetlogicObject)
	{
		super.mergeSchematic(sourceSchemObject, targetlogicObject);
		if (sourceSchemObject instanceof chs.cof.logical.schem.IConductor) {
			chs.cof.logical.schem.IConductor schemConductor = (chs.cof.logical.schem.IConductor) sourceSchemObject;
			processCompositeDecorationTexts(schemConductor);
			schemConductor.setConnectivity((IConductor) targetlogicObject);
		}
	}

	public static void processCompositeDecorationTexts(chs.cof.logical.schem.IConductor schemConductor)
	{
		for (IConnected segment : schemConductor.getSegments()) {
			for (ICompositeTextDecorationText object : segment.getObjects(ICompositeTextDecorationText.class)) {
				if (object.getAttributePropertyProvider() == schemConductor.getConnectivity()) {
					// the connectivity object is going to be deleted, so reset the attribute provider of composite text objects appropriately
					object.setAttributePropertyProvider(schemConductor);
				}
			}
		}
	}

	@Override protected void postSchematicMerge(IConnectivityRef schemSourceObject)
	{
		SegmentContainerHelper.updatePortGfx(schemSourceObject);
		super.postSchematicMerge(schemSourceObject);
	}

	private void removeSourceConductorFromStackedHighways()
	{
		ILogicObject srcObj = getSourceLogicObject();
		if (srcObj instanceof IHighwayConductor) {
			IHighwayConductor highwayCond = (IHighwayConductor) srcObj;
			for (IGeneralHighway highway : HighwayHelper.getStackedHighways(highwayCond)) {
				highway.removeStackPinConductor(highwayCond);
			}
		}
	}
}
