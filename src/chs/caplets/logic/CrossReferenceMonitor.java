/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2023 Siemens
 */
package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ICapletWindow;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.ISheet;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IHighwaySegment;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPort;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.CrossReferenceMonitorHelper;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignSharedUsageMgr;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.project.IProject;
import chs.cof.symbol.IZoneIdentifier;
import chs.common.IDesignContainer;
import chs.common.INamedPropertiedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDObjectIterator;
import chs.services.gfx.GfxView;
import chs.system.UIDMgr;
import chs.utilities.Environment;
import chs.utility.helpers.ZoneHelper;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class will update/create the cross reference test objects that are a part of the shared objects
 */
public class CrossReferenceMonitor extends CrossReferenceMonitorHelper
{

	public CrossReferenceMonitor(IProject project)
	{
		super(project);
	}

	/**
	 * Given a collection of changed objects, Find the subset that represents usages of a shared object and 1) Add
	 * XRefTexts to any newly create port graphics
	 *  @param changedUIDs - The uids of the objects that were changed
	 * @param design - Current design
	 */
	public void update(Collection<IUID> changedUIDs, IDesignContainer design)
	{
		Set<IUIDObject> diagrams = new HashSet<IUIDObject>();
		List<ISharedObject> sharedObjectsToProcess = new ArrayList<>();
		for (IUIDObject uidObj : doFilter(changedUIDs)) {
			if (uidObj instanceof ISharedObject) {
				sharedObjectsToProcess.add((ISharedObject) uidObj);
			}
			else if (uidObj instanceof ILogicObject && ((ILogicObject) uidObj).isCrossReferenceable()) {
				diagrams.addAll(generateDesignCrossReferences((ILogicObject) uidObj));
			}
		}
		//Read from <ShdObj-<design>> cache
		Set<ILogicDesign> logicDesigns = getLogicDesignsFromCache(sharedObjectsToProcess, design);

		diagrams.addAll(generateProjectCrossReferences(sharedObjectsToProcess, logicDesigns));
		nudgeViews(diagrams);
	}

	/**
	 * Given a collection of shared objects, check if there already exists an entry for these shared objects in <sharedObject-<design>> cache
	 * If so, collect all the designs, so that they can be passed onto generateProjectCrossReferences & hence avoid DB calls to retrieve used designs information
	 *
	 * @param sharedObjectsToProcess - Collection of shared objects that we want to find the used design information
	 * @param design - current design (to check if the modified shared object is unplaced, in which case we don't worry about XREF generation for that)
	 * @return - The corresponding designs information from cache, if there exists an entry for all these shared objects in the cache
	 * 			 Null, if atleast one shared object didnt have an entry. Hence, goes back to previous flow of making DB call
	 */
	@Nullable protected Set<ILogicDesign> getLogicDesignsFromCache(List<ISharedObject> sharedObjectsToProcess,
			IDesignContainer currentDesign)
	{
		Set<ILogicDesign> logicDesigns = new HashSet<>();
		for(ISharedObject sharedObject: sharedObjectsToProcess){
			//If there exists no usage for this shared object, we don't need to bother about its usages/XREF
			Collection<IUID> designs = cache.get(sharedObject.getUID());
			if(designs == null && isUsed(sharedObject, currentDesign)){
				// Placed in current design now
				logicDesigns=null;
				break;
			}
			if(designs != null) {
				logicDesigns.addAll(
						designs.stream().map(des -> UIDMgr.getObjectOfType(des, ILogicDesign.class))
								.filter(des -> des != null)
								.collect(Collectors.toSet()));
			}
		}
		return logicDesigns;
	}

	protected boolean isUsed(ISharedObject sharedObject, IDesignContainer currentDesign)
	{
		if(currentDesign instanceof ILogicDesign){
			IDesignSharedUsageMgr sharedUsageMgr = ((ILogicDesign) currentDesign).getLoadedSharedUsageMgr();
			return sharedUsageMgr !=null && sharedUsageMgr.hasUsage(sharedObject);
		}
		return false;
	}

	public void borderChanged(ISheet sheet)
	{
		if (!(sheet instanceof ISchemDiagram)) {
			return;
		}
		ISchemDiagram diagram = (ISchemDiagram) sheet;
		ILogicDesign design = diagram.getDesign();
		IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
		Iterator<?>[] dsuIterators = new Iterator<?>[]{dwum.getConductorUsages().iterator(),
				dwum.getHighwayUsages().iterator(),
				dwum.getPinListUsages().iterator(),
				dwum.getPinUsages().iterator()};
		Set<INamedPropertiedObject> sharedSet = new HashSet<INamedPropertiedObject>();
		for (Iterator<?> dsuIt : dsuIterators) {
			while (dsuIt.hasNext()) {
				IDesignSharedUsage usage = (IDesignSharedUsage) dsuIt.next();
				if (usage.getDiagram() == diagram) {
					// Zone must be set on diagram objects since usages are transitory objects
					IZoneIdentifier key = ZoneHelper.getZoneExtentForDiagramObject(usage.getDiagramObject(),
							usage.getDiagram());
					usage.setZoneKey(key);
					ISharedObject shared = usage.getSharedObject();
					if (shared != null) {
						sharedSet.add(shared);
					}
					else {
						sharedSet.add(usage.getLogicObject());
					}
				}
			}
		}

		Set<IUIDObject> diagrams = new HashSet<IUIDObject>();
		List<ISharedObject> sharedObjectsToProcess = new ArrayList<>();
		for (Object aSharedSet : sharedSet) {
			IUIDObject uidObj = (IUIDObject) aSharedSet;
			if (uidObj instanceof ISharedObject) {
				sharedObjectsToProcess.add((ISharedObject) uidObj);
			}
			else if (uidObj instanceof ILogicObject) {
				diagrams.addAll(generateDesignCrossReferences((ILogicObject) uidObj));
			}
		}
		//Read from <ShdObj-<design>> cache
		Set<ILogicDesign> logicDesigns = getLogicDesignsFromCache(sharedObjectsToProcess, design);
		diagrams.addAll(generateProjectCrossReferences(sharedObjectsToProcess, logicDesigns));
		nudgeViews(diagrams);
	}

	public void register(Collection<? extends ISharedObject> sharedObjs)
	{
		//System.out.println("CrossReferenceMonitor.register ISharedObject = " + shared.getName());
		Collection<IUIDObject> diagrams = generateProjectCrossReferences(sharedObjs, null);
		nudgeViews(diagrams);
	}

	public void register(ILogicObject lObj)
	{
		//System.out.println("CrossReferenceMonitor.register ILogicObject = " + lObj.getName());
		Collection<IUIDObject> diagrams = generateDesignCrossReferences(lObj);
		nudgeViews(diagrams);
	}

	// Private methods

	protected void nudgeViews(Collection<IUIDObject> diagrams)
	{
		if (Environment.isHeadless()) {
			return;
		}
		for (ICAFWindow cafWin : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			if (cafWin instanceof ICapletWindow) {
				for (ICapletView view : ((ICapletWindow) cafWin).getViewsList()) {
					GfxView vigfxVieww = (GfxView) view;
					ICapletModel capletModel = vigfxVieww.getCapletModel();
					if (capletModel instanceof ILogicModel) {
						ILogicModel model = (ILogicModel) capletModel;
						ISchemDiagram logicDiagram = (ISchemDiagram) vigfxVieww.getDiagram();
						if (diagrams.contains(logicDiagram) && model.getDiagrams().contains(logicDiagram)) {
							vigfxVieww.resetSelections(); // in case we updated a selected text
							if (cafWin.isDisplayed()) {
								vigfxVieww.invalidate();
								vigfxVieww.repaint();
							}
						}
					}
				}
			}
		}
	}

	private static Set<IUIDObject> doFilter(Collection<IUID> uids)
	{
		Set<IUIDObject> filtered = new HashSet<IUIDObject>(uids.size());
		for (IUID uid : uids) {
			IUIDObject uidObj = UIDMgr.getObject(uid);
			filtered.add(doReduce(uidObj));
			if (uidObj instanceof IPinList) {
				//TODO FEAT00013786: I think Cross reference may not supported for Stack pins
				for (IUIDObjectIterator giter = ((IPinList) uidObj).getPins().getUIDObjects(); giter.hasNext(); ) {
					IUIDObject pin = giter.getNext();
					filtered.add(doReduce(pin));
				}
			}
		}
		filtered.remove(null);
		return filtered;
	}

	private static IUIDObject doReduce(IUIDObject changedObject)
	{
		// TODO jacobt FEAT13040 - replace this stuff with ReferenceHelper.reduceToLogicObject
		// but IPort is missed out there...
		IUIDObject uidObject = changedObject;
		if (uidObject instanceof ISegment) {
			uidObject = ((ISegment) uidObject).getConductor();
		}
		if (uidObject instanceof IHighwaySegment) {
			uidObject = ((IHighwaySegment) uidObject).getHighway();
		}
		else if (uidObject instanceof IPort) {
			uidObject = (IUIDObject) ((IGfxObject) uidObject).getContainer();
		}

		if (uidObject instanceof IRepresentedObject) {
			uidObject = ((IRepresentedObject) uidObject).getRawConnectivity();
		}

		if (uidObject instanceof ILogicObject) {
			ILogicObject logicObject = (ILogicObject) uidObject;
			ISharedObject sharedObject = logicObject.getSharedObject();
			if (sharedObject != null) {
				uidObject = sharedObject;
			}
			// else we stick with the logic object - just need to update xrefs in this design
		}

		return uidObject;
	}
}