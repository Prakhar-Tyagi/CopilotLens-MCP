/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ModelChangeEvent;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.IChainSegmentContainer;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDMgr;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.system.ILogicUtilsFactory;
import chs.utility.DiagramHelper;
import chs.utility.GfxObjectUtils;
import chs.utility.helpers.CoordinateHelper;
import chs.utility.helpers.OverlapUtils;
import chs.utility.logic.IMulticoreIndicatorRefresherHelper;
import chs.utility.logic.IndicatorRefresherUtils;
import chs.utility.logic.MulticoreIndicatorRefresher;
import chs.utility.logic.MulticoreUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * * This listener will listen explicitly for model changes BEFORE the edit is closed. Any that * are to
 * multicore-related objects (nets/wires/multicores/indicators) will fire off some * additional fixup work.
 */
public class IndicatorRefresher
{

	private ISchemDiagram m_diagram = null;
	private Generator m_generator = null;
	private GeneratorParameters m_params = null;
	private static final int BOUNDS_OFFSET = 1000;
	private static Map<ISchemDiagram, IndicatorRefresher> instances = new HashMap<ISchemDiagram, IndicatorRefresher>();
	private ElaboratedExtentRange m_extVStripsToRefreshIndicators = new ElaboratedExtentRange();
	private ElaboratedExtentRange m_extHStripsToRefreshIndicators = new ElaboratedExtentRange();
	private List<IExtent> m_extExtentsToRefreshIndicators = new ArrayList<IExtent>();

	public static IndicatorRefresher getIndicatorRefresher(ISchemDiagram diagram)
	{
		IndicatorRefresher refresher = instances.get(diagram);
		if (refresher == null) {
			refresher = new IndicatorRefresher(diagram);
			instances.put(diagram, refresher);
		}
		return refresher;
	}

	public static IndicatorRefresher removeInstance(ISchemDiagram diagram)
	{
		return instances.remove(diagram);
	}

	public static boolean hasInstance(@NotNull ISchemDiagram diagram)
	{
		return instances.containsKey(diagram);
	}

	private IndicatorRefresher(ISchemDiagram diag)
	{
		m_diagram = diag;
		m_generator = Generator.getGenerator();
		m_params = new GeneratorParameters(m_diagram.getGrid().getGridSpacing());
		m_params.setPreferences(m_diagram.getDesign().getProject().getPreferences());
	}

	public void refreshIndicators(ModelChangeEvent e)
	{
		refreshIndicators(e, true);
	}

	public void refreshIndicators(ModelChangeEvent e, boolean cleanupIndicators)
	{
		refreshIndicators(e.getChangedObjectsUIDs(), cleanupIndicators);
	}

	public void refreshIndicators(Collection<IUID> changedObjectsUIDs, boolean cleanupIndicators)
	{
		if (m_diagram == null || m_diagram.getDesign() == null) {
			// I don't see how this can happen, the refesher is deleted/removed whenever a design is destroyed, however,
			// in some unreproducible cases the apparently the design can be closed without the indicator refresher being
			// removed from the models listerners.  This is some extreme defensive code for hot fix and the cause still
			// needs to be investigated.
			return;
		}
		// DR 392913: this call may cause data model changes and so must be done in the first phase of notification,
		// before CreationDeletionHelper.processobjects() is called.
		//LOGIC-4185 C161BashSEEDSI5: Exception occurs on move of pin list after disconnect selected action fails
		//suspend indicator refresh on read-only diagrams. we can't do it under local edit. because we delegate
		//the deletion to CDH which is processed at the end of the action.
		if (m_diagram.isEditable()) {
			doRefreshIndicators(changedObjectsUIDs, cleanupIndicators);
		}
	}

	private void doRefreshIndicators(Collection<IUID> changedObjectsUIDs, boolean cleanupIndicators)
	{
		IUIDMgr uidmgr = chs.system.UIDMgr.getUIDMgr();
		//
		Set<IMulticore> multicores = new LinkedHashSet<IMulticore>();

		Set<IShieldBody> indicators = new LinkedHashSet<IShieldBody>();

		Collection<IShieldBody> diagramShieldBodies = m_diagram.getShieldBodies();
		for (IUID uid : changedObjectsUIDs) {
			//
			// Work out hat indicators need fixing up, and do it.
			//
			chs.cof.logical.cable.IConductor cableConductor;
			IUIDObject uobj = uidmgr.getObject(uid);
			if (uobj == null) {
				continue;
			}
			if (uobj instanceof IConductor) {

				IConductor cond = (IConductor) uobj;
				cableConductor = cond.getConnectivity();
				IMulticore mc = cableConductor.getMulticore();
				//
				// Parents will be affected in ripple pattern...
				//
				if (mc != null) {
					multicores.add(mc);
					while ((mc = mc.getParent()) != null) {
						multicores.add(mc);
					}
				}
			}
			else if (uobj instanceof chs.cof.logical.cable.IConductor) {
				IMulticore mc = ((chs.cof.logical.cable.IConductor) uobj).getMulticore();
				//
				// Parents will be affected in ripple pattern...
				//
				if (mc != null) {
					multicores.add(mc);
					while ((mc = mc.getParent()) != null) {
						multicores.add(mc);
					}
				}
			}
			else if (uobj instanceof IMulticore) {
				IMulticore mc = (IMulticore) uobj;
				//
				// Parents will be affected in ripple pattern...
				//
				multicores.add(mc);
				while ((mc = mc.getParent()) != null) {
					multicores.add(mc);
				}
			}
			else if (uobj instanceof IShieldBody) {
				IShieldBody sb = (IShieldBody) uobj;
				//before adding the indicator make sure that this indicator exist on m_diagram.
				if (diagramShieldBodies.contains(sb)) {
					indicators.add(sb);
				}
			}
			else if (uobj instanceof IChainSegmentContainer) {
				IChainSegmentContainer segmentContainer = (IChainSegmentContainer) uobj;
				Set<IShieldBodyHookup> hookups = segmentContainer.getAttachedHookups();
				for (IShieldBodyHookup hookup : hookups) {
					IMulticore mc = hookup.getShieldBody().getConnectivity().getMulticore();
					multicores.add(mc);
				}
			}
			else if (uobj instanceof ISegment) {
				if (DiagramHelper.getDiagram((ISegment) uobj) == m_diagram) {
					ILogicUtilsFactory logicUtilsFactory = FactoryMgr.getLogicalFactory().getLogicUtilsFactory();
					if (logicUtilsFactory != null) {
						logicUtilsFactory.registerAffectedStripForIndicatorRefresh((ISegment) uobj);
					}
				}
			}
		}
		OverlapUtils.beginCaching(m_diagram, ISchemDiagram.LAYER_TYPE.INDICATOR, IShieldBody.class);
		for (ExtentRange range : m_extVStripsToRefreshIndicators.getDistinctRanges()) {
			IExtent lookupExt = FactoryMgr.getCommonFactory().constructExtent(m_diagram.getExtent());
			lookupExt.setX(range.start());
			lookupExt.setWidth(range.end() - range.start());
			indicators.addAll(OverlapUtils.getIndicatorsAffectedWithExtent(m_diagram, lookupExt));
		}
		m_extVStripsToRefreshIndicators.clear();

		for (ExtentRange range : m_extHStripsToRefreshIndicators.getDistinctRanges()) {
			IExtent lookupExt = FactoryMgr.getCommonFactory().constructExtent(m_diagram.getExtent());
			lookupExt.setY(range.start());
			lookupExt.setHeight(range.end() - range.start());
			indicators.addAll(OverlapUtils.getIndicatorsAffectedWithExtent(m_diagram, lookupExt));
		}
		m_extHStripsToRefreshIndicators.clear();

		for (IExtent extent : m_extExtentsToRefreshIndicators) {
			indicators.addAll(OverlapUtils.getIndicatorsAffectedWithExtent(m_diagram, extent));
		}
		m_extExtentsToRefreshIndicators.clear();
		OverlapUtils.endCaching();
		IMulticoreIndicatorRefresherHelper helper = new IMulticoreIndicatorRefresherHelper()
		{
			@Override public void notifyMissingSchemShieldBodiesCreated(@NotNull IShieldBody schemShieldBody,
					@NotNull Collection<IShieldBody> newShieldBodies)
			{
				ICapletController controller = CAFUtils.getInstance().getControllerForObject(schemShieldBody);
				if (controller != null &&
						controller.getSelectMgr().getCurrentSelections().contains(schemShieldBody.getUID())) {
					controller.getSelectMgr().getCurrentSelections().add(newShieldBodies, false);
				}
			}
		};
		MulticoreIndicatorRefresher multicoreIndicatorRefresher = new MulticoreIndicatorRefresher(helper, m_diagram);
		multicoreIndicatorRefresher.populateShieldIndicators(multicores, indicators);
		OverlapUtils.beginCaching(m_diagram, ISchemDiagram.LAYER_TYPE.CONDUCTOR, ISegment.class);

		Map<IShieldBody, ILocation> sbAbsLocations = new HashMap<>();
		Map<IShieldBody, String> sbNameKeys = new HashMap<>();
		Comparator<IShieldBody> sbSorter = (o1, o2) -> {
			ILocation l1 = sbAbsLocations.computeIfAbsent(o1, s -> CoordinateHelper.getAbsGfxLocation(s, 0, 0));
			ILocation l2 = sbAbsLocations.computeIfAbsent(o2, s -> CoordinateHelper.getAbsGfxLocation(s, 0, 0));
			int result = GfxObjectUtils.compareLocation(l1, l2);
			if (result == 0) {
				//both or at the same location. try name with hierarchy
				String key1 = sbNameKeys.computeIfAbsent(o1, MulticoreUtils::generateNameKeyForShieldBody);
				String key2 = sbNameKeys.computeIfAbsent(o2, MulticoreUtils::generateNameKeyForShieldBody);
				result = key1.compareTo(key2);
			}
			return result;
		};
		//LOGIC-12077: AUTO-FAIL Regression : Multicore indicators generation is inconsistent on "Update from ICD" action,
		List<IShieldBody> sortedCandidates = new ArrayList<>(indicators);
		Collections.sort(sortedCandidates, sbSorter);

		multicoreIndicatorRefresher.refreshIndicators(sortedCandidates, cleanupIndicators);

		//synchronize the hookups and indicator graphics if needed.
		Set<IShieldBody> redrawIndicators = new LinkedHashSet<IShieldBody>();
		multicoreIndicatorRefresher.populateShieldIndicators(multicores, redrawIndicators);
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(m_diagram);
		for (IShieldBody redrawIndicator : redrawIndicators) {
			MulticoreUtils.redrawSchematicIndicator(redrawIndicator, m_diagram, gp, false);
		}
		OverlapUtils.endCaching();
	}

	public void removeIndicators()
	{
		List<IShieldBody> shieldBodiesToDelete =
				new ArrayList<IShieldBody>();
		for (IShieldBody shieldBody : m_diagram.getShieldBodies()) {
			if (m_generator.getShieldBodyCrossings(shieldBody.getLocation(),
							Generator.getShieldBodyOrientation(shieldBody), m_diagram,
							shieldBody.getConnectivity().getMulticore(), shieldBody)
					.isEmpty()) {
				//If no valid crossing found, delete it.
				shieldBodiesToDelete.add(shieldBody);
			}
		}

		IndicatorRefresherUtils.deleteShieldBodies(m_diagram, shieldBodiesToDelete, false);
		shieldBodiesToDelete.clear();
	}

	public ISchemDiagram getSchemDiagram()
	{
		return m_diagram;
	}

	@Override public boolean equals(Object obj)
	{
		if (obj instanceof IndicatorRefresher) {
			IndicatorRefresher temp = (IndicatorRefresher) obj;
			if (temp.getSchemDiagram().equals(getSchemDiagram())) {
				return true;
			}
		}
		return false;
	}

	private static class ExtentRange
	{

		private int m_start;
		private int m_end;

		private ExtentRange(int a, int b)
		{
			if (a < b) {
				m_start = a;
				m_end = b;
			}
			else {
				m_start = b;
				m_end = a;
			}
		}

		private int start()
		{
			return m_start;
		}

		private int end()
		{
			return m_end;
		}

		private void merge(ExtentRange range)
		{
			m_start = Math.min(m_start, range.start());
			m_end = Math.max(m_end, range.end());
		}
	}

	private static class ElaboratedExtentRange
	{

		private List<ExtentRange> m_extRanges = new ArrayList<ExtentRange>();

		private void insert(int a, int b)
		{
			ExtentRange insertionRange = new ExtentRange(a, b);
			int insertId = 0;
			int mergeEndId = 0;
			for (ExtentRange currRange : m_extRanges) {
				if (currRange.start() > insertionRange.end()) {
					break;
				}
				if (insertionRange.start() > currRange.end()) {
					insertId++;
				}
				mergeEndId++;
			}
			ExtentRange mergedRange = null;
			for (int i = insertId; i < mergeEndId; i++) {
				ExtentRange removed = m_extRanges.remove(insertId);
				if (removed != null) {
					if (mergedRange != null) {
						mergedRange.merge(removed);
					}
					else {
						mergedRange = new ExtentRange(removed.start(), removed.end());
					}
				}
			}
			if (mergedRange != null) {
				mergedRange.merge(insertionRange);
			}
			else {
				mergedRange = new ExtentRange(insertionRange.start(), insertionRange.end());
			}
			m_extRanges.add(insertId, mergedRange);
		}

		private List<ExtentRange> getDistinctRanges()
		{
			return Collections.unmodifiableList(m_extRanges);
		}

		private void clear()
		{
			m_extRanges.clear();
		}
	}

	public void registerXRange(int a, int b)
	{
		m_extVStripsToRefreshIndicators.insert(a, b);
	}

	public void registerYRange(int a, int b)
	{
		m_extHStripsToRefreshIndicators.insert(a, b);
	}

	public void registerSegmentExtent(IExtent extent)
	{
		m_extExtentsToRefreshIndicators.add(extent);
	}
}
