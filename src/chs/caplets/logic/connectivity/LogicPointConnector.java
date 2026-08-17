/*
 * Copyright 2003-2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.connectivity;

import chs.caf.CAFUtils;
import chs.caf.caplet.IUndoableContainer;
import chs.caf.caplet.action.IDeferredAction;
import chs.caf.caplet.helpers.CHSUndoableEdit;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.ISegmentContainer;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import chs.common.IUIDProvider;
import chs.common.Side;
import chs.services.dynamicgfx.connectivity.PointConnectorHelper;
import chs.system.UIDMgr;
import chs.utility.DiagramHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.SegmentHelper;
import chs.utility.logic.IndicatorRefresherUtils;
import chs.utility.logic.MulticoreUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * This deals with some of the subtleties with connecting logic objects outside the context of the data model.
 * Originally design for topology, this "deferred connection" mechanism was introduced into logic to address Segment
 * connection issues.
 */
public class LogicPointConnector extends PointConnectorHelper implements IDeferredAction
{

	@NotNull private final Supplier<ISchemDiagram> mSchemDiagramSupplier;
	private Set<IUID> mSegmentContainerUIDs = new LinkedHashSet<IUID>();

	public LogicPointConnector(@NotNull Supplier<ISchemDiagram> schemDiagramSupplier)
	{
		mSchemDiagramSupplier = schemDiagramSupplier;
	}

	/**
	 * Take a schem conductor and set it aside for potential 'fixup' later. By 'fixup', this means taking conductors
	 * that have disconnected segments in them and making them continuous. May also delete empty ones.
	 *
	 * @param objToClean Schem conductor to be fixed up or removed.
	 */
	public void addFixupObject(Object objToClean)
	{
		if (objToClean instanceof ISegmentContainer) {
			mSegmentContainerUIDs.add(((IUIDProvider) objToClean).getUID());
		}
	}

	/**
	 *
	 */
	public void processAction()
	{
		ILogicDesign design = getDiagram().getDesign();
		assert design != null;
		Set<ISchemDiagram> diagramsBeingScrubbed = new HashSet<>();
		for (ISchemDiagram diagram : design.getDiagrams()) {
			if (diagram.isFullyLoaded() && !diagram.isEditable()) {
				diagram.beginLocalEdit();
				diagramsBeingScrubbed.add(diagram);
			}
		}
		try {
			doProcessAction();
		}
		finally {
			for (ISchemDiagram diagram : diagramsBeingScrubbed) {
				diagram.endLocalEdit();
			}
		}
	}

	private void doProcessAction()
	{
		Map<chs.cof.logical.cable.IConductor, Set<IConductor>> allNewConnectivityConductors =
				new HashMap<chs.cof.logical.cable.IConductor, Set<IConductor>>();
		List<ISegmentContainer> segmentContainers =
				UIDMgr.getListOfType(mSegmentContainerUIDs, ISegmentContainer.class);

		//dts0100735936- If a shared multicore is split by an inline the system creates 2 multicores yet the inner cores do not match up correctly
		Map<IConductor, Side> condAndSideRestrictionMap = null;
		IUndoableContainer ucontainer = CAFUtils.getInstance().getCurrentUndoableContainer();
		CHSUndoableEdit edit = ucontainer != null ? ucontainer.getCurrentEdit() : null;
		Collection<IUID> newObjs = edit != null ? edit.getNewUIDs() : Collections.<IUID>emptyList();
		Collection<IUID> deletedUIDs = edit != null ? edit.getDeletedUIDs() : Collections.<IUID>emptyList();

		//SP1304_dts0100918332 [CH] java.lang.NullPointerException  at chs.utility.helpers.SegmentHelper.determineMCcondsPlacementRestriction(SegmentHelper.
		Set<ISegmentContainer> filteredSegmentContainers =
				filterDeletedSegmentContainers(deletedUIDs, segmentContainers);
		if (!filteredSegmentContainers.isEmpty() && !newObjs.isEmpty()) {
			condAndSideRestrictionMap =
					SegmentHelper.determineMCcondsPlacementRestriction(filteredSegmentContainers, newObjs);
		}

		Set<IHighwaySchematic> highwaySchems = new HashSet<IHighwaySchematic>();
		Set<IHighwaySchematic> deletedHWSchems = new HashSet<IHighwaySchematic>();

		for (ISegmentContainer segmentContainer : segmentContainers) {
			if (segmentContainer instanceof IHighwaySchematic) {
				IHighwaySchematic cond = (IHighwaySchematic) segmentContainer;
				IBaseDiagram dexp = DiagramHelper.getBaseDiagram(cond);
				// check if conductor is in this diagram, if not would get assertion in CHSUndoableEdit
				if (isSameContainer(dexp)) {
					highwaySchems.add((IHighwaySchematic) segmentContainer);
				}
			}
		}
		HighwayHelper.mergeHighwaySchem(highwaySchems, deletedHWSchems);
		//m_schemConds.removeAll(deletedHWSchems); Is this required ?

		// First go off and make all the modified schem conductors continuous
		for (ISegmentContainer segmentContainer : filteredSegmentContainers) {
			if (segmentContainer instanceof IConductor) {
				IConductor cond = (IConductor) segmentContainer;
				IBaseDiagram dexp = DiagramHelper.getBaseDiagram(cond);
				// check if conductor is in this diagram, if not would get assertion in CHSUndoableEdit
				if (isSameContainer(dexp)) {
					//dts0100735519  Aerobash : ClassCastException when deleting a highway and its interface net
					CreationDeletionHelper cdHelper = CreationDeletionHelper.getTheCreationHelper();
					if (!cdHelper.goingToDelete(segmentContainer)) {
						Set<IConductor> newSchemConductors = cond.makeContinuous(condAndSideRestrictionMap, newObjs);
						if (cond.getConnectivity().getMulticore() != null && !newSchemConductors.isEmpty()) {
							if (!IShieldConductor.class.isAssignableFrom(cond.getRepresentedClass())) {
								allNewConnectivityConductors.put(cond.getConnectivity(), newSchemConductors);
							}
						}
					}
				}
			}
			else if (segmentContainer instanceof IHighwaySchematic) {
				IHighwaySchematic highwaySchematic = (IHighwaySchematic) segmentContainer;
				IBaseDiagram dexp = DiagramHelper.getBaseDiagram(highwaySchematic);
				// check if conductor is in this diagram, if not would get assertion in CHSUndoableEdit
				if (isSameContainer(dexp)) {
					highwaySchematic.makeContinuous();
				}
			}
		}

		// Now handle any multicores which may have been schem split.
		if (!allNewConnectivityConductors.isEmpty()) {
			Map<IMulticore, IMulticore> mcs =
					MulticoreUtils.processSplitMulticores(allNewConnectivityConductors, getDiagram());
			IndicatorRefresherUtils.reparentShieldBodies(mcs, getDiagram());
		}

		// todo creddy: Commenting this for now. Review with team.
		// todo creddy: Shared MCs are not created now. See the code in MulticoreUtils.deepCopyMulticore()
//		if (!segmentContainers.isEmpty()) {
//			diagram.getDesign().getProject().getSharedConductorMgr().fireChangeEvent();
//		}
		mSegmentContainerUIDs.clear();
	}

	protected boolean isSameContainer(@Nullable IBaseDiagram dexp)
	{
		return dexp != null && (dexp.getDesignContainer() == getDiagram().getDesignContainer());
	}

	protected Set<ISegmentContainer> filterDeletedSegmentContainers(Collection<IUID> deletedUIDs,
			List<ISegmentContainer> segmentContainers)
	{
		//we don't want to process already deleted objects. Hence, filter them.
		Set<ISegmentContainer> filteredSegmentContainers = new LinkedHashSet<ISegmentContainer>();
		for (ISegmentContainer schemCond : segmentContainers) {
			if (!deletedUIDs.contains(schemCond.getUID())) {
				filteredSegmentContainers.add(schemCond);
			}
		}
		return filteredSegmentContainers;
	}

	public boolean runInLast()
	{
		return false;
	}

	@NotNull private ISchemDiagram getDiagram()
	{
		return mSchemDiagramSupplier.get();
	}
}
