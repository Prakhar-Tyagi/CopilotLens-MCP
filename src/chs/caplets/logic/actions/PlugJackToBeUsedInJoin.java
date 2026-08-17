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
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IJackConnector;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.schem.IPinList;
import chs.common.IExtent;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.ExtentHelper;
import chs.utility.helpers.TransformHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Resultant plug/jack to be used for join
 */
class PlugJackToBeUsedInJoin extends ObjectsToBeUsedInJoin
{

	@Nullable private IPlugConnector m_plugConnector;
	@Nullable private IJackConnector m_jackConnector;

	private ITransform anchorTransform;

	@Nullable private IConnector anchorConnector;
	private IPinList determinedAnchorConnector;

	@Nullable private IPinList samePlugSchem = null;
	@Nullable private IPinList sameJackSchem = null;
	protected Collection<IPinList> m_selectedSchems;

	private static final byte NO_ERROR = (byte) 0;
	private static final byte SINGLE_OR_NO_SCHEM = (byte) 1;
	private static final byte DIFF_CONNECTIVITY = (byte) 2;

	PlugJackToBeUsedInJoin(@Nullable IPlugConnector plugConnector, @Nullable IJackConnector jackConnector, int gridSpacing,
			Collection<IPinList> selectedSchems)
	{
		m_plugConnector = plugConnector;
		m_jackConnector = jackConnector;
		m_matedPairs = new ArrayList<Pair<IPinList, IPinList>>();
		this.gridSpacing = gridSpacing;
		m_selectedSchems = selectedSchems;
	}

	@Override protected boolean addSchemInstances(@Nullable IPinList currentSchem, @Nullable IPinList mateSchem)
	{

		if (!super.addSchemInstances(currentSchem, mateSchem)) {
			return false;
		}
		IPinList jackSchem = null;
		IPinList plugSchem = null;

		if (currentSchem != null && currentSchem.getConnectivity() instanceof IPlugConnector ||
				mateSchem != null && mateSchem.getConnectivity() instanceof IJackConnector) {
			plugSchem = currentSchem;
			jackSchem = mateSchem;
		}
		else if (currentSchem != null && currentSchem.getConnectivity() instanceof IJackConnector ||
				mateSchem != null && mateSchem.getConnectivity() instanceof IPlugConnector) {
			plugSchem = mateSchem;
			jackSchem = currentSchem;
		}

		IJackConnector jackConnector = jackSchem != null ? CommonUtils.cast(jackSchem.getConnectivity(),
				IJackConnector.class) : null;
		IPlugConnector plugConnector =
				plugSchem != null ? CommonUtils.cast(plugSchem.getConnectivity(), IPlugConnector.class) : null;

		if (jackSchem != null && jackConnector == null) {
			if (plugConnector == m_plugConnector) {
				m_errorMessage = ResourceMgr
						.getString(JoinPinlistsAction.class, "JoinPinlistAction.OnlyAPartMated", "plug",
								JoinPinlistsHelper.getType(jackSchem));
			}
			else {
				m_errorMessage = ResourceMgr
						.getString(JoinPinlistsAction.class, "JoinPinlistAction.notenabled.NonUniqueInstances",
								"plug",
								JoinPinlistsHelper.getType(jackSchem));
			}

			return false;
		}
		if (plugSchem != null && plugConnector == null) {
			// if else (check if All connectivity objects are same type else)
			if (jackConnector == m_jackConnector) {
				m_errorMessage = ResourceMgr
						.getString(JoinPinlistsAction.class, "JoinPinlistAction.OnlyAPartMated", "jack",
								JoinPinlistsHelper.getType(plugSchem));
			}
			else {
				m_errorMessage = ResourceMgr
						.getString(JoinPinlistsAction.class, "JoinPinlistAction.notenabled.NonUniqueInstances",
								"jack",
								JoinPinlistsHelper.getType(plugSchem));
			}
			return false;
		}
		if (jackConnector != m_jackConnector && plugConnector != m_plugConnector) {
			m_errorMessage = ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.NonUnique");
			return false;
		}

		//anchorconnector should match one of the plug or jack instances.
		if (anchorConnector != null && (anchorConnector != m_plugConnector && anchorConnector != m_jackConnector)) {
			m_errorMessage = ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.NonUnique");
			return false;
		}
		if (m_plugConnector != plugConnector) {
			if (m_jackConnector == null) {
				m_errorMessage = ResourceMgr
						.getString(JoinPinlistsAction.class, "JoinPinlistAction.notenabled.NonUniqueInstances",
								"plug");
				return false;
			}
			anchorConnector = m_jackConnector;
		}
		if (m_jackConnector != jackConnector) {
			if (m_plugConnector == null) {
				m_errorMessage = ResourceMgr
						.getString(JoinPinlistsAction.class, "JoinPinlistAction.NonUniqueInstances", "jack");
				return false;
			}
		}

		if (anchorConnector == null) {
			if (plugConnector != null) {
				anchorConnector = m_plugConnector;
			}
			else {
				anchorConnector = m_jackConnector;
			}
		}
		ITransform currentTransform = null;
		if (anchorConnector != m_plugConnector && jackSchem != null) {
			currentTransform = jackSchem.getTransform();
		}
		else if (plugSchem != null) {
			currentTransform = plugSchem.getTransform();
		}
		if (currentTransform == null) {
			m_errorMessage =
					ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.DifferentOrientation");
			return false;
		}
		if (anchorTransform == null) {
			anchorTransform = currentTransform;
		}

		if (anchorTransform != currentTransform) {
			Double anchorTransformDeg = Math.toDegrees(TransformHelper.getRotation(anchorTransform));
			Double currentTransformDeg = Math.toDegrees(TransformHelper.getRotation(currentTransform));
			if (anchorTransformDeg.intValue() != currentTransformDeg.intValue()) {
				m_errorMessage =
						ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.DifferentOrientation");
				return false;
			}
		}
		createConnectedPinPairs(plugSchem, jackSchem);

		m_matedPairs.add(new Pair<IPinList, IPinList>(plugSchem, jackSchem));
		return true;
	}

	@Nullable IPinList getAnchorObject()
	{
		if (determinedAnchorConnector != null) {
			return determinedAnchorConnector;
		}

		Collection<IPinList> selectedPlugSchems = new HashSet<>();
		Collection<IPinList> selectedJackSchems = new HashSet<>();

		for (Pair<IPinList, IPinList> aPair : m_matedPairs) {
			IPinList aPlugPinlist = aPair.getFirst();
			IPinList aJackPinlist = aPair.getSecond();

			if (aPlugPinlist != null && m_selectedSchems.contains(aPlugPinlist)) {
				selectedPlugSchems.add(aPlugPinlist);
			}

			if (aJackPinlist != null && m_selectedSchems.contains(aJackPinlist)) {
				selectedJackSchems.add(aJackPinlist);
			}
		}

		byte plugCanBeAnchor = satisfiesAnchorCriteria(selectedPlugSchems, m_plugConnector);
		byte jackCanBeAnchor = satisfiesAnchorCriteria(selectedJackSchems, m_jackConnector);

		if (checkErrorCondition(plugCanBeAnchor, jackCanBeAnchor)) {
			return null;
		}

		if (plugCanBeAnchor == NO_ERROR) {
			sameJackSchem = hasSameMatedSchem(selectedPlugSchems, true);
			boolean plugIsAnchor = false;
			if (sameJackSchem != null) {
				plugIsAnchor = reconstructMatedPairs(selectedPlugSchems, sameJackSchem, true, false);
			}
			else if (!isMatePinListLarger(selectedPlugSchems, true)) {
				plugIsAnchor = reconstructMatedPairs(selectedPlugSchems, null, true, true);
			}

			if (plugIsAnchor) {
				anchorConnector = m_plugConnector;
				m_errorMessage = "";

				return getAnchorInstanceFromMatedPairs();
			}
		}

		if (jackCanBeAnchor == NO_ERROR) {
			samePlugSchem = hasSameMatedSchem(selectedJackSchems, false);
			boolean jackIsAnchor = false;
			if (samePlugSchem != null) {
				jackIsAnchor = reconstructMatedPairs(selectedJackSchems, samePlugSchem, false, false);
			}
			else if (!isMatePinListLarger(selectedJackSchems, false)) {
				jackIsAnchor = reconstructMatedPairs(selectedJackSchems, null, false, true);
			}

			if (jackIsAnchor) {
				anchorConnector = m_jackConnector;
				m_errorMessage = "";

				return getAnchorInstanceFromMatedPairs();
			}
		}

		if (m_errorMessage.isEmpty()) {
			m_errorMessage =
					ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.noObjectToJoin");
		}
		return null;
	}

	private boolean isMatePinListLarger(Collection<IPinList> selectedSchems, boolean isPlug)
	{
		for (Pair<IPinList, IPinList> aMatedPair : m_matedPairs) {
			IPinList mainSchem = isPlug ? aMatedPair.getFirst() : aMatedPair.getSecond();
			IPinList mateSchem = isPlug ? aMatedPair.getSecond() : aMatedPair.getFirst();
			if (selectedSchems.contains(mainSchem)) {
				if (isMatePinListLarger(mainSchem, mateSchem)) {
					m_errorMessage = getOverLapMessage(mainSchem, mateSchem, isPlug);
					return true;
				}
			}
		}
		return false;
	}

	private String getOverLapMessage(IPinList mainSchem, IPinList mateSchem, boolean isPlug)
	{
		return ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.LongerOrWider",
				JoinPinlistsHelper.getType(mainSchem), JoinPinlistsHelper.getType(mateSchem));
	}

	private boolean reconstructMatedPairs(Collection<IPinList> selectedSchems, @Nullable IPinList sameMateSchem,
			boolean isPlug, boolean includeMate)
	{
		List<Pair<IPinList, IPinList>> reconstructedMatePairs = new ArrayList<>();
		boolean pairsMarkedForDeletion = false;

		for (Pair<IPinList, IPinList> aMatedPair : m_matedPairs) {
			IPinList mainSchem = isPlug ? aMatedPair.getFirst() : aMatedPair.getSecond();
			IPinList mateSchem = isPlug ? aMatedPair.getSecond() : aMatedPair.getFirst();

			if (mainSchem != null && selectedSchems.contains(mainSchem)) {
				Pair<IPinList, IPinList> newPair;

				if (includeMate) {
					newPair = makePlugJackPair(mainSchem, mateSchem, isPlug);
				}
				else {
					newPair = makePlugJackPair(mainSchem, null, isPlug);
				}
				reconstructedMatePairs.add(newPair);
			}
			else {
				// does it have common mate schem
				if (mateSchem != sameMateSchem) {
					pairsMarkedForDeletion = true;
					break;
				}
			}
		}

		if (!pairsMarkedForDeletion && reconstructedMatePairs.size() > 1) {
			m_matedPairs = reconstructedMatePairs;
			return true;
		}
		m_errorMessage = ResourceMgr.getString(JoinPinlistsAction.class,
				"JoinPinlistAction.notenabled.NonUniqueInstances");
		return false;
	}

	@NotNull private Pair<IPinList, IPinList> makePlugJackPair(IPinList mainSchem, @Nullable IPinList mateSchem,
			boolean isPlug)
	{
		if (isPlug) {
			return new Pair<IPinList, IPinList>(mainSchem, mateSchem);
		}
		return new Pair<IPinList, IPinList>(mateSchem, mainSchem);
	}

	@Nullable private IPinList hasSameMatedSchem(Collection<IPinList> selectedSchems, boolean isPlug)
	{
		boolean firstSchem = true;
		IPinList uniqueMateSchem = null;
		for (Pair<IPinList, IPinList> aMatedPair : m_matedPairs) {
			IPinList mainSchem = isPlug ? aMatedPair.getFirst() : aMatedPair.getSecond();
			IPinList mateSchem = isPlug ? aMatedPair.getSecond() : aMatedPair.getFirst();

			if (selectedSchems.contains(mainSchem)) {
				if (firstSchem) {
					uniqueMateSchem = mateSchem;
					firstSchem = false;
				}
				else {
					if (uniqueMateSchem != mateSchem) {
						uniqueMateSchem = null;
						break;
					}
				}
			}
		}
		return uniqueMateSchem;
	}

	private boolean checkErrorCondition(byte plugCanBeAnchor, byte jackCanBeAnchor)
	{
		if (plugCanBeAnchor == NO_ERROR || jackCanBeAnchor == NO_ERROR) {
			return false;
		}
		if ((plugCanBeAnchor | jackCanBeAnchor) >= DIFF_CONNECTIVITY) {
			m_errorMessage = ResourceMgr
					.getString(JoinPinlistsAction.class, "JoinPinlistAction.notenabled.NonUniqueInstances");
			return true;
		}
		if ((plugCanBeAnchor | jackCanBeAnchor) == SINGLE_OR_NO_SCHEM) {
			m_errorMessage = ResourceMgr
					.getString(JoinPinlistsAction.class, "JoinPinlistAction.notenabled.TwoPinlistsRequired");
			return true;
		}
		m_errorMessage = ResourceMgr.getString(JoinPinlistsAction.class, "JoinPinlistAction.noObjectToJoin");
		return true;
	}

	private byte satisfiesAnchorCriteria(@NotNull Collection<IPinList> selectedSchems, @Nullable IConnector aConnectivity)
	{
		if (selectedSchems.size() <= 1) {
			return SINGLE_OR_NO_SCHEM;
		}

		if (!hasSameConnectivity(selectedSchems, aConnectivity)) {
			return DIFF_CONNECTIVITY;
		}
		return NO_ERROR;
	}

	private boolean hasSameConnectivity(@NotNull Collection<IPinList> selectedSchems, @Nullable IConnector aConnectivity)
	{
		for (IPinList aSchem : selectedSchems) {
			if (aSchem.getConnectivity() != aConnectivity) {
				return false;
			}
		}
		return true;
	}

	private boolean isMatePinListLarger(IPinList pinList, IPinList matePinList)
	{
		if (pinList == null || matePinList == null) {
			return false;
		}
		boolean isHorizontalPlacementofPins = isHorizontalPlacementOfPins(pinList);
		boolean isVerticalPlacementOfPins = isVerticalPlacementOfPins(pinList);
		IExtent sourceExtent = ExtentHelper.getAbsExtent(pinList);
		IExtent mateExtent = ExtentHelper.getAbsExtent(matePinList);
		if (isVerticalPlacementOfPins) {
			return mateExtent.getY() < sourceExtent.getY() || mateExtent.getTop() > sourceExtent.getTop();
		}
		if (isHorizontalPlacementofPins) {
			return mateExtent.getX() < sourceExtent.getX() || mateExtent.getRight() > sourceExtent.getRight();
		}
		return false;
	}

	private IPinList getAnchorInstanceFromMatedPairs()
	{
		if (anchorConnector == m_plugConnector) {
			determinedAnchorConnector = m_matedPairs.iterator().next().getFirst();
		}
		else {
			determinedAnchorConnector = m_matedPairs.iterator().next().getSecond();
		}
		return determinedAnchorConnector;
	}

	IPinList getInstanceOfAnchorObject(Pair<IPinList, IPinList> pairOfPinlist)
	{
		if (anchorConnector == m_plugConnector) {
			return pairOfPinlist.getFirst();
		}
		else {
			return pairOfPinlist.getSecond();
		}
	}

	@Override protected IPinList getSameInstanceInJoin()
	{
		IPinList requiredSchem = samePlugSchem;
		if (requiredSchem == null) {
			requiredSchem = sameJackSchem;
		}

		return requiredSchem;
	}
}
