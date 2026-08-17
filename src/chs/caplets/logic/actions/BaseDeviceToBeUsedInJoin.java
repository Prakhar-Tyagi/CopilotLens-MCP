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

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.IBaseDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDeviceOwnedConnector;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.common.Side;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Resultant device to be used for join
 */
class BaseDeviceToBeUsedInJoin extends ObjectsToBeUsedInJoin
{

	private IBaseDevice m_device;

	private IPinList m_anchorPinlist;
	private boolean isPlugAnchor = false;

	private IPinList sameDeviceSchem = null;

	BaseDeviceToBeUsedInJoin(IBaseDevice device, int gridSpacing)
	{
		m_device = device;
		m_matedPairs = new ArrayList<Pair<IPinList, IPinList>>();
		this.gridSpacing = gridSpacing;
	}

	@Override protected boolean addSchemInstances(IPinList currentSchem, @Nullable IPinList mateSchem)
	{
		if (!super.addSchemInstances(currentSchem, mateSchem)) {
			return false;
		}
		IPinList deviceSchem = currentSchem.getConnectivity() instanceof IBaseDevice ? currentSchem : mateSchem;
		if (deviceSchem == null || !(deviceSchem.getConnectivity() instanceof IBaseDevice)) {

			m_errorMessage = ResourceMgr
					.getString(JoinPinlistsAction.class, "JoinPinlistAction.notenabled.NonUniqueInstances",
							"device",
							JoinPinlistsHelper.getType(currentSchem));

			return false;
		}

		IPinList plugSchem = mateSchem;
		if (deviceSchem == mateSchem) {
			plugSchem = currentSchem;
		}

		if (plugSchem != null && plugSchem.getConnectivity() instanceof IDeviceConnector) {
			return true;
		}

		if (plugSchem != null && !(plugSchem.getConnectivity() instanceof IPlugConnector))    // scenario?
		{
			m_errorMessage = ResourceMgr
					.getString(JoinPinlistsAction.class, "JoinPinlistAction.OnlyAPartMated", "device",
							JoinPinlistsHelper.getType(plugSchem));
			return false;
		}

		if (deviceSchem.getConnectivity() != m_device) {
			m_errorMessage = "Multiple devices in selection";
			return false;
		}

		if (plugSchem != null) {

			IDeviceOwnedConnector plugConnector =
					CommonUtils.cast(plugSchem.getConnectivity(), IDeviceOwnedConnector.class);
			if (plugConnector != null) {
				if (plugConnector.getOwner() != m_device) {
					m_errorMessage = "Two different device owners in selection";
					return false;
				}
			}
		}

		createConnectedPinPairs(deviceSchem, plugSchem);

		m_matedPairs.add(new Pair<IPinList, IPinList>(deviceSchem, plugSchem));
		return true;
	}

	@Override protected boolean areAttachedPinlistsOfCorrectSize(
			Map<IPinList, JoinPinlistsHelper.LocationAndExtentOfPinlistOnMergedPinlist> locationOnMergedPinlistMap)
	{
		if (!isPlugAnchor) {
			return super.areAttachedPinlistsOfCorrectSize(locationOnMergedPinlistMap);
		}

		return true;
	}

	@Override protected IPinList getSameInstanceInJoin()
	{

		return sameDeviceSchem;
	}

	IPinList getInstanceOfAnchorObject(Pair<IPinList, IPinList> pinListPair)
	{
		if (isPlugAnchor) {
			return pinListPair.getSecond();
		}
		return pinListPair.getFirst();
	}

	@Nullable IPinList getAnchorObject()
	{

		if (m_anchorPinlist != null) {
			return m_anchorPinlist;
		}
		if (m_matedPairs.size() > 1) {
			m_anchorPinlist = getInstanceOfAnchorObject(m_matedPairs.iterator().next());
			for (Pair<IPinList, IPinList> aPair : m_matedPairs) {
				if (m_anchorPinlist != getInstanceOfAnchorObject(aPair)) {
					if (m_anchorPinlist.getParameterized() != null) {
						return m_anchorPinlist;
					}
				}
			}
			sameDeviceSchem = m_matedPairs.iterator().next().getFirst();
			isPlugAnchor = true;
			m_anchorPinlist = m_matedPairs.iterator().next().getSecond();
			Side side = m_sideOfAttachedPinlist.get(m_anchorPinlist);
			if (side == null) {
				return null;
			}

			if (m_anchorPinlist != null) {
				for (Pair<IPinList, IPinList> aPair : m_matedPairs) {
					if (aPair.getSecond() == null) {
						return null;
					}
					if (m_anchorPinlist.getConnectivity() != aPair.getSecond().getConnectivity()) {
						m_errorMessage = ResourceMgr.getString(JoinPinlistsAction.class,
								"JoinPinlistAction.notenabled.NonUniqueInstances");
						m_anchorPinlist = null;
						return null;
					}
					else if (m_sideOfAttachedPinlist.get(aPair.getSecond()) != side) {
						m_errorMessage = ResourceMgr
								.getString(JoinPinlistsAction.class,
										"JoinPinlistAction.AnchorsNotOnSameSide",
										JoinPinlistsHelper.getType(m_anchorPinlist));
						m_anchorPinlist = null;
						return null;
					}
				}
			}
		}

		return m_anchorPinlist;
	}

	protected boolean areAttachedPinlistsHandledInResize()
	{
		return isPlugAnchor;
	}

	void preMovePin(IAbstractSchemPin pin)
	{
		if (isPlugAnchor) {
			IPinList deviceSchem = m_matedPairs.iterator().next().getFirst();
			IDiagramObject diagramObject = pin.getParent();
			if (diagramObject instanceof IPinList) {
				deviceSchem.removeAttachedObject((IPinList) diagramObject);
			}
		}
	}

	boolean arePinsOnBothSidesOfAnchor()
	{
		return !isPlugAnchor;
	}

	public boolean verifyPinlistPairs()
	{
		if (!addMissingMatedPairs()) {
			return false;
		}
		return super.verifyPinlistPairs();
	}

	@Override protected boolean addMissingMatedPairs()
	{
		Collection<IPinList> anchorSchemInstances = getAnchorSchemInstances();
		// There could be missing mated pairs if we select attached plugs of two device instances to join device instances.
		// if more than one devices are selected, then only we want to consider all other mated plugs.
		// Else, intension is to join two instances of mated plugs on same device. Hence don't consider other mated plugs
		if (anchorSchemInstances.size() > 1) {
			for (IPinList anchorPinlistInstance : anchorSchemInstances) {
				for (IPinList attachedPinList : anchorPinlistInstance.getAttachedPinListObjects()) {
					Pair<IPinList, IPinList> matePair = new Pair<>(anchorPinlistInstance, attachedPinList);
					if (!m_matedPairs.contains(matePair)) {
						if (!addSchemInstances(anchorPinlistInstance, attachedPinList)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
}
