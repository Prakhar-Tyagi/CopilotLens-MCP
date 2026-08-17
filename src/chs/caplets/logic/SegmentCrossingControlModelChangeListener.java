/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic;

import chs.caf.caplet.IGfxModel;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.ICHSUndoRedoListener;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IDesignContainer;
import chs.common.IUID;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.subsystem.crossover.CrossoverStylingServices;
import chs.subsystem.crossover.ISegmentCrossingMgr;
import chs.subsystem.crossover.LogicCrossoverGenerator;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author chandras on 29-04-2025.
 */
public class SegmentCrossingControlModelChangeListener implements IModelChangeListener, ICHSUndoRedoListener
{

	private final IUID m_design;

	public SegmentCrossingControlModelChangeListener(@NotNull IUID design)
	{
		m_design = design;
	}

	@Override public void modelPreChanged(ModelChangeEvent e)
	{
	}

	private void purgeCrossingInformation(Collection<IUID> deletedObjectsUIDs)
	{
		if (!deletedObjectsUIDs.isEmpty()) {
			for (ISchemDiagram diagram : getDiagramsToProcess()) {
				ISegmentCrossingMgr segmentCrossingMgr = CrossoverStylingServices.getDiagramCrossingMgr(diagram);
				if (segmentCrossingMgr != null) {
					for (IUID deletedObjectsUID : deletedObjectsUIDs) {
						segmentCrossingMgr.purgeCrossingInformation(deletedObjectsUID);
					}
				}
			}
		}
	}

	@NotNull protected List<ISchemDiagram> getDiagramsToProcess()
	{
		List<ISchemDiagram> diagramsToProcess = new ArrayList<>();
		ILogicDesign design = UIDMgr.getObjectOfType(m_design, ILogicDesign.class);
		if (design != null) {
			//do process the loaded diagrams. however process the already registered diagrams also.
			for (ISchemDiagram diagram : design.getDiagrams()) {
				if (diagram.isLoadedInMemory()) {
					diagramsToProcess.add(diagram);
				}
			}
		}
		return Collections.unmodifiableList(diagramsToProcess);
	}

	@Override public void modelChanged(ModelChangeEvent e)
	{
		purgeCrossingInformation(e.getDeletedObjectsUIDs());
		Set<IUID> candidates = new HashSet<>();
		candidates.addAll(e.getNewObjectsUIDs());
		candidates.addAll(e.getChangedObjectsUIDs());
		regenerateCrossingInformation(candidates);
		//The dynamic graphics of segments with crossing graphics should
		//be refreshed to reflect the new crossing information.
		ensureDynamicGraphicsUpdated(e);
	}

	private void ensureDynamicGraphicsUpdated(ModelChangeEvent e)
	{
		IGfxModel model = CommonUtils.cast(e.getModel(), IGfxModel.class);
		IDynamicGfxService dynamicGfxService = model != null ? model.getDynamicGfxService() : null;
		if (dynamicGfxService != null) {
			dynamicGfxService.refresh();
		}
	}

	private void regenerateCrossingInformation(@NotNull Collection<IUID> candidates)
	{
		//now we are trying to sync crossing if out-of-date using cookies. However, the unsaved local connectivity
		//change also needs to be accounted for. So we will process all usage diagrams.
		//Otherwise the cookie mechanism will not work. If connectivity has local changes and diagram is loaded after.
		//If cookie is saved with time-stamp 10, and connectivity has time-stamp 10. Suppose connectivity is loaded
		//without this diagram and modified. Suppose we don't load that diagram and process here. When the diagram
		//is loaded later it will find that the time-stamp of connectivity is 10 and its segment also has time-stamp 10
		//and will skip the sync. This will be in-correct. This same issue is true for shared objects also but they are
		//immediately saved and their time-stamp is incremented. So we need to process unloaded diagrams also.
		//The cookie is now replaced to use outside-diameter of object which is used by the in-built crossover control
		//algorithm also. The timestamp cookie of design connectivity was fragile and was not able to manage things easily.
		//Just save of connectivity was potentially causing all the diagrams which were not saved along with connectivity
		//as out-of-sync and thus forcing re-evaluation at the load of diagram which might result into different state
		//if the plugin is changed. Thus causing change on a released design too if the release of a design doesn't force
		//all the diagrams to be saved along with connectivity causing performance issue also. Since the updated outside
		//diameter is on the conductor/single-line we can manage the later loaded diagrams also. So process only loaded.
		ILogicDesign design = UIDMgr.getObjectOfType(m_design, ILogicDesign.class);
		if (design != null) {
			LogicCrossoverGenerator.regenerateCrossingInformation(candidates, design, new HashSet<>(), true);
		}
	}

	@Override
	public void objectsDeleted(@Nullable IDesignContainer activeDesignContainer, Collection<IUID> deletedObjects)
	{
		purgeCrossingInformation(deletedObjects);
	}

	@Override
	public void objectsRestored(@Nullable IDesignContainer activeDesignContainer, Collection<IUID> restoredObjects)
	{
		regenerateCrossingInformation(restoredObjects);
	}
}
