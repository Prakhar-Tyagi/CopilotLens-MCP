/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.cof.draw.ITransform;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBackshellTermination;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.PinListConnectionHelper;
import chs.utility.helpers.TransformHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Resultant inline to be used for join
 */
class InlinePlugJackToBeUsedInJoin extends ObjectsToBeUsedInJoin
{

	private IInlinePlugConnector m_plugConnector;
	private IInlineJackConnector m_jackConnector;
	private ITransform anchorTransform;

	InlinePlugJackToBeUsedInJoin(IConnector current, IConnector mate, int gridSpacing)
	{
		this.gridSpacing = gridSpacing;
		IInlineJackConnector inlineJack;
		IInlinePlugConnector inlinePlug = CommonUtils.cast(current, IInlinePlugConnector.class);
		if (inlinePlug == null) {
			inlinePlug = CommonUtils.cast(mate, IInlinePlugConnector.class);
			inlineJack = CommonUtils.cast(current, IInlineJackConnector.class);
		}
		else {
			inlineJack = CommonUtils.cast(mate, IInlineJackConnector.class);
		}

		m_jackConnector = inlineJack;
		m_plugConnector = inlinePlug;
		m_matedPairs = new ArrayList<Pair<IPinList, IPinList>>();
	}

	@Override protected boolean addSchemInstances(@Nullable IPinList currentSchem, @Nullable IPinList mateSchem)
	{

		if (!super.addSchemInstances(currentSchem, mateSchem)) {
			return false;
		}
		IPinList jackSchem =
				(currentSchem != null && currentSchem.getConnectivity() instanceof IInlineJackConnector) ?
						currentSchem :
						mateSchem;
		IPinList plugSchem =
				(currentSchem != null && currentSchem.getConnectivity() instanceof IInlinePlugConnector) ?
						currentSchem :
						mateSchem;
		IInlineJackConnector jackConnector = jackSchem != null ? CommonUtils.cast(jackSchem.getConnectivity(),
				IInlineJackConnector.class) : null;
		IInlinePlugConnector plugConnector =
				plugSchem != null ? CommonUtils.cast(plugSchem.getConnectivity(), IInlinePlugConnector.class) :
						null;
		if ((jackSchem != null && jackConnector == null) || (plugSchem != null && plugConnector == null)) {
			return false;
		}
		if (jackConnector != m_jackConnector || plugConnector != m_plugConnector) {
			m_errorMessage =
					ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.DifferentInlineNamePairs");
			return false;
		}
		if (plugSchem == null || jackSchem == null) {
			m_errorMessage = ResourceMgr
					.getString(JoinPinlistsAction.class, "JoinPinlistAction.OnlyAPartMated", "inlineplug",
							"inlinejack");
			return false;
		}

		if (anchorTransform == null) {
			anchorTransform = plugSchem.getTransform();
		}

		if (anchorTransform != plugSchem.getTransform()) {
			Double anchorTransformDeg = Math.toDegrees(TransformHelper.getRotation(anchorTransform));
			Double currentTransformDeg = Math.toDegrees(TransformHelper.getRotation(plugSchem.getTransform()));
			if (anchorTransformDeg.intValue() != currentTransformDeg.intValue()) {
				m_errorMessage =
						ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.DifferentOrientation");
				return false;
			}
		}

		if (getNumInstancePins(plugSchem) != getNumInstancePins(jackSchem)) {
			m_errorMessage = ResourceMgr
					.getString(JoinPinlistsAction.class, "JoinPinlistAction.NoOneToOnePinMapping", "inlineplug",
							"inlinejack");
			return false;
		}

		createConnectedPinPairs(plugSchem, jackSchem);

		if (!verifyOneToOnePinMappingOnInline(plugSchem)) {
			m_errorMessage = ResourceMgr
					.getString(JoinPinlistsAction.class, "JoinPinlistAction.NoOneToOnePinMapping", "inlineplug",
							"inlinejack");
			return false;
		}

		m_matedPairs.add(new Pair<IPinList, IPinList>(plugSchem, jackSchem));
		return true;
	}

	private long getNumInstancePins(@NotNull IPinList plugSchem)
	{
		return plugSchem.getCablePins(true).stream()
				.filter(pin -> !isBackshellTermination(pin))
				.count();
	}

	private boolean isBackshellTermination(@NotNull IAbstractPin pin)
	{
		return pin instanceof IBackshellTermination;
	}

	private boolean verifyOneToOnePinMappingOnInline(@NotNull IPinList plugSchem)
	{
		for (IAbstractSchemPin pin : plugSchem.getAllPins(false)) {
			if (!isBackshellTermination(pin) && m_connectedPins.get(pin) == null) {
				return false;
			}
		}
		return true;
	}

	void moveConnectedPins(Collection<IAbstractSchemPin> pinsMovedOnPlug, IPinList mergedPlug, IPinList mergedJack,
			MovePinHandler theMovePinHandler)
	{

		PinListConnectionHelper connectionHelper = ConnectionHelper.createInstance(mergedPlug, mergedJack);

		if (connectionHelper != null) {
			for (IAbstractSchemPin pin : pinsMovedOnPlug) {

				IAbstractSchemPin connectedPinToMove = m_connectedPins.get(pin);
				if (connectedPinToMove != null) {
					connectionHelper.moveConnectedPin(pin, connectedPinToMove, mergedJack);
					theMovePinHandler.moveJoint(connectedPinToMove, connectedPinToMove.getAbsLocation().getX(),
							connectedPinToMove.getAbsLocation().getY());
				}
			}
		}
	}

	@Nullable IPinList getAnchorObject()
	{
		if (!m_matedPairs.isEmpty()) {
			return m_matedPairs.iterator().next().getFirst();
		}
		return null;
	}

	IPinList getInstanceOfAnchorObject(Pair<IPinList, IPinList> pairOfPinlists)
	{
		return pairOfPinlists.getFirst();
	}

	protected boolean areAttachedPinlistsHandledInResize()
	{
		return true;
	}
}
