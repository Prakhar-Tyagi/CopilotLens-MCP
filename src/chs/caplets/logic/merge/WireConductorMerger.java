package chs.caplets.logic.merge;

/*
 * Copyright 2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.ISpliceIterator;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.cof.drawplus.IJoint;
import chs.common.IUIDObjectCollection;
import chs.system.FactoryMgr;
import chs.utilities.CollectionUtils;
import chs.utility.helpers.NodeHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 18-Mar-2010 Time: 16:27:24
 */
public class WireConductorMerger extends ConductorMerger
{

	private boolean m_disconnectSources = false;
	private Set<IAbstractPin> m_movedPins = new HashSet<IAbstractPin>();

	public WireConductorMerger(ILogicObject sourceObject, ILogicObject targetObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceObject, targetObject, reporter);
	}

	@Override void mergeChildrenConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		super.mergeChildrenConnectivity(sourceLogicObject, targetLogicObject);

		IWireConductor targetWireConductor =
				(IWireConductor) targetLogicObject;
		IWireConductor sourceWireConductor =
				(IWireConductor) sourceLogicObject;
		ISpliceIterator centerStripSplicesIterator = sourceWireConductor.getCenterStripSplices();
		//noinspection LoopConditionNotUpdatedInsideLoop
		while (centerStripSplicesIterator.hasNext()) {
			ISplice splice = centerStripSplicesIterator.getNext();
			splice.removeCenterStrippedWire(sourceWireConductor);
			targetWireConductor.addCenterStripSplice(splice);
		}
	}

	@Override
	protected void movePin(chs.cof.logical.cable.IConductor sourceConductor,
			chs.cof.logical.cable.IConductor targetConductor, IAbstractPin pin)
	{

		if (!targetConductor.containsPin(pin) && targetConductor.getNumPins() == 2) {
			m_disconnectSources = true;
			return;
		}
		if (!targetConductor.containsPin(pin)) {
			// If that pin didn't exist on the target, and it's coming from source
			// keep track of it, we may need to disconnect it from the target if number of terminations exceeded 2
			m_movedPins.add(pin);
		}
		super.movePin(sourceConductor, targetConductor, pin);
	}

	@Override
	protected void postSchematicMerge(IConnectivityRef schemSourceObject)
	{
		if (m_disconnectSources && schemSourceObject instanceof IConductor) {
			IConductor schemWireConductor = (IConductor) schemSourceObject;
			IUIDObjectCollection<IPin> pins = schemWireConductor.getPins();
			for (IPin pin : pins) {
				IAbstractPin cablePin = pin.getConnectivity();
				IPinList pinOwner = cablePin.getOwner();
				if (!((IWireConductor) schemWireConductor.getConnectivity()).getCenterStripSplicesAsSet()
						.contains(pinOwner)) {
					IJoint pinJoint = pin.getJoint();
					if (pinJoint != null) {
						NodeHelper.separateConductorAtNode(schemWireConductor, pinJoint, FactoryMgr.getCommonFactory(),
								FactoryMgr.getSchemFactory());
					}
				}
			}
		}
		super.postSchematicMerge(schemSourceObject);
	}

	@Override protected void postMergingComplete()
	{
		super.postMergingComplete();

		// fix for dts0100692670
		// now remove pins from the conductor that were moved earlier, in case we are disconnected all sources
		if (m_disconnectSources) {
			for (IAbstractPin movedPin : m_movedPins) {
				IWireConductor targetWire = (IWireConductor) getTargetLogicObject();
				targetWire.removePin(movedPin);
			}
		}
		m_movedPins.clear();
	}

	public static boolean areMergeable(IWireConductor sourceObject, IWireConductor targetObject)
	{
		IWireConductor sourceWire = sourceObject;
		IWireConductor targetWire = targetObject;

		Set<ISplice> spliceSet = CollectionUtils
				.coalesce(sourceWire.getCenterStripSplicesAsSet(), targetWire.getCenterStripSplicesAsSet());
		Set<IAbstractPin> pins = CollectionUtils.coalesce(sourceWire.getPinSet(), targetWire.getPinSet());
		for (IAbstractPin pin : pins) {
			if (spliceSet.contains(pin.getOwner())) {
				return true;
			}
		}
		return false;
	}

}
