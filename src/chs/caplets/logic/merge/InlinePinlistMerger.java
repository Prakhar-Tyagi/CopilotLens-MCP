package chs.caplets.logic.merge;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IConnectivityRef;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 22-Mar-2010 Time: 18:39:16 To change this template use File | Settings
 * | File Templates.
 */
public class InlinePinlistMerger extends ConnectorPinlistMerger
{

	private IConnector m_sourceMate = null;
	private IConnector m_targetMate = null;

	private boolean m_mergingMate = false;

	public InlinePinlistMerger(IGenericInlineConnector sourceLogicObject, IGenericInlineConnector targetLogicObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceLogicObject, targetLogicObject, reporter);

		m_sourceMate = sourceLogicObject.getMate();
		m_targetMate = targetLogicObject.getMate();
		assert m_sourceMate != null && m_targetMate != null;
	}

	protected List<ILogicObject> getAllTargetObjects()
	{
		return Arrays.asList(getTargetLogicObject(), m_targetMate);
	}

	@Override protected void mergeConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		super.mergeConnectivity(sourceLogicObject, targetLogicObject);

		m_mergingMate = true;
		super.mergeConnectivity(m_sourceMate, m_targetMate);
	}

	@Override protected void mergePin(IPinList sourceParent, IPinList targetParent, IAbstractPin sourcePin,
			IAbstractPin targetPin)
	{
		if (sourcePin instanceof IConnectorPin) {
			if (!m_mergingMate) {
				IConnectorPin sourceMatedPin = ((IConnectorPin) sourcePin).getMatedPin();
				IConnectorPin targetMatedPin = targetPin == null ? null : ((IConnectorPin) targetPin).getMatedPin();

				super.mergePin(sourceParent, targetParent, sourcePin, targetPin);
				super.mergePin(m_sourceMate, m_targetMate, sourceMatedPin, targetMatedPin);
			}
		}
		else {
			// dts0100692671 fix: in case of merging backshell terminations, these are not IConnectorPins
			super.mergePin(sourceParent, targetParent, sourcePin, targetPin);
		}
	}

	@Override protected void mergeSchematic(IConnectivityRef sourceSchemObject, ILogicObject targetlogicObject)
	{
		Collection<chs.cof.logical.schem.IPinList> attachedPinListObjects =
				((chs.cof.logical.schem.IPinList) sourceSchemObject).getAttachedPinListObjects();
		assert attachedPinListObjects.size() == 1 : "Inline Connector can have only one attached pinlist";
		chs.cof.logical.schem.IPinList schemSourceMate = attachedPinListObjects.iterator().next();
		super.mergeSchematic(sourceSchemObject, targetlogicObject);
		super.mergeSchematic(schemSourceMate, m_targetMate);
	}

	@Override protected void postMergingComplete()
	{
		m_sourceMate.delete();
		super.postMergingComplete();
	}

	@Override protected void fixupConnectorMatings(IConnectivityRef sourceSchemObject)
	{
		//suppress super!
	}

	@Override protected void postSchematicMerge(IConnectivityRef schemSourceObject)
	{
		Collection<chs.cof.logical.schem.IPinList> attachedPinListObjects =
				((chs.cof.logical.schem.IPinList) schemSourceObject).getAttachedPinListObjects();
		assert attachedPinListObjects.size() == 1 : "Inline Connector can have only one attached pinlist";

		chs.cof.logical.schem.IPinList schemSourceMate = attachedPinListObjects.iterator().next();
		super.postSchematicMerge(schemSourceObject);
		super.postSchematicMerge(schemSourceMate);
	}

	@Override
	protected void fixupIncompatiblePins(IPinList sourcePinList, IPinList targetPinList, IAbstractPin sourcePin)
	{
	}
}
