/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2010-2024 Siemens
 */
package chs.caplets.logic.merge;

import chs.caplets.logic.actions.ui.IFacetConflictResolutionModel;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IRegenerateableObject;
import chs.cof.library.IFootprintable;
import chs.cof.library.ILibrariedObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAnalysableSymbolAssociatable;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IBaseShareableDiagramObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.IShareableDiagramObject;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryObject;
import chs.cof.symbol.ISymboledObject;
import chs.common.IUID;
import chs.utilities.StringUtils;
import chs.utility.attr.AttributeUtils;
import chs.utility.helpers.PropertyHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 12-Mar-2010 Time: 15:58:04
 */
public abstract class Merger
{

	protected ILogicObject m_sourceLogicObject;
	protected ILogicObject m_targetLogicObject;

	private Map<ILogicObject, ILogicObject> m_mappedConnectivity = new HashMap<ILogicObject, ILogicObject>();
	private Set<IUID> m_processedSchemUIDs = new HashSet<IUID>();
	protected IFacetConflictResolutionModel m_conflictResolution = null;
	@NotNull protected IMergeActionChangeReporter m_reporter;

	protected Merger(ILogicObject sourceLogicObject, ILogicObject targetLogicObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		m_sourceLogicObject = sourceLogicObject;
		m_targetLogicObject = targetLogicObject;
		m_reporter = reporter;
	}

	public static Mergeable areMergeable(ILogicObject sourceObject, ILogicObject targetObject)
	{
		return Mergeable.areMergeable(sourceObject, targetObject);
	}

	@NotNull public static Merger getMerger(ILogicObject sourceObject, ILogicObject targetObject,
			@NotNull IMergeActionChangeReporter reporter)
	{

		assert areMergeable(sourceObject, targetObject) == Mergeable.Possible : "Merging is not possible";

		if (sourceObject instanceof IDevice) {
			if (!((ISymboledObject) sourceObject).getSymbolReferences().isEmpty() ||
					!((ISymboledObject) targetObject).getSymbolReferences().isEmpty()) {
				return new SymbolDevicePinlistMerger(sourceObject, targetObject, reporter);
			}
			return new DevicePinlistMerger(sourceObject, targetObject, reporter);
		}
		if (sourceObject instanceof IGenericInlineConnector && targetObject instanceof IGenericInlineConnector) {
			return new InlinePinlistMerger((IGenericInlineConnector) sourceObject,
					(IGenericInlineConnector) targetObject, reporter);
		}
		if (sourceObject instanceof IConnector) {
			return new ConnectorPinlistMerger(sourceObject, targetObject, reporter);
		}
		if (sourceObject instanceof IBlockDevice) {
			return new BlockDevicePinlistMerger(sourceObject, targetObject, reporter);
		}
		if (sourceObject instanceof IFunction) {
			return new FunctionMerger(sourceObject, targetObject, reporter);
		}
		if (sourceObject instanceof IPinList) {
			return new PinlistMerger(sourceObject, targetObject, reporter);
		}
		if (sourceObject instanceof IGeneralHighway) {
			return new HighwayMerger(sourceObject, targetObject, reporter);
		}
		if(sourceObject instanceof ISingleLine) {
			return new SingleLineMerger(sourceObject, targetObject, reporter);
		}
		if (sourceObject instanceof IWireConductor) {
			return new WireConductorMerger(sourceObject, targetObject, reporter);
		}
		if (sourceObject instanceof IConductor) {
			return new ConductorMerger(sourceObject, targetObject, reporter);
		}

		assert false : "Unknown object type for merging";
		return null;
	}

	@NotNull public static Merger getMerger(ILogicObject sourceObject, ILogicObject targetObject)
	{
		return getMerger(sourceObject, targetObject, IMergeActionChangeReporter.NULL_REPORTER);
	}

	public void merge()
	{
		preMerge();

		mergeConnectivity(getSourceLogicObject(), getTargetLogicObject());

		postMergeConnectivity(getSourceLogicObject(), getTargetLogicObject());

		processSchematicsFor(getSourceLogicObject(), new ISchematicProcessor()
		{

			public void process(IConnectivityRef schemObject)
			{
				mergeSchematic(schemObject, getTargetLogicObject());
				postSchematicMerge(schemObject);
			}
		});

		//LOGIC-7748: do this apply before post merge because that method
		//might delete some source objects. ie. source mate for inline.
		if (m_conflictResolution != null) {
			m_conflictResolution.apply();
		}
		postMergingComplete();
		getSourceLogicObject().delete();

		processPendingRegenerate();
		reportChanges();
	}

	protected void reportChanges()
	{

	}

	protected List<ILogicObject> getAllTargetObjects()
	{
		return Arrays.asList(getTargetLogicObject());
	}

	private void processPendingRegenerate()
	{
		//LOGIC-8146 Architectural cost text is incorrectly updated in share-into scenario.
		for (ILogicObject logicObject : getAllTargetObjects()) {
			processSchematicsFor(logicObject, (schemObject) ->
			{
				if (schemObject instanceof IRegenerateableObject) {
					// Regenerate the schem source object
					// Introduced in 2010.2.SP1107 - dts0100766928 - the Harness is not graphically reflected in the composite wire name and it is required to performing ?Apply Style"
					((IRegenerateableObject) schemObject).regenerateDiagramObject();
					//regenerate the attached schematic IPinList objects.
					//E.g., schem deviceconnector whenever schem device is being regenerated.
					if (schemObject instanceof chs.cof.logical.schem.IPinList) {
						Collection<chs.cof.logical.schem.IPinList> attachedPinListObjects =
								((chs.cof.logical.schem.IPinList) schemObject).getAttachedPinListObjects();
						attachedPinListObjects.forEach(e -> e.regenerateDiagramObject());
					}
				}
				else {
					assert false : schemObject.toString() + " must be a regeneratable object";
				}
			});
		}
	}

	protected void postMergeConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{

	}

	protected void preMerge()
	{
		assert getSourceLogicObject() != null && getTargetLogicObject() != null &&
				getSourceLogicObject().getDesignContainer() != null;
	}

	protected Set<ISchemDiagram> getDiagramsForLogicObjects(ILogicObject... logicObjects)
	{
		Set<ISchemDiagram> diagrams = new HashSet<ISchemDiagram>();
		for (ILogicObject logicObject : logicObjects) {
			ILogicDesign design = (ILogicDesign) logicObject.getDesignContainer();
			IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
			for (IDesignSharedUsage usage : dwum.getUsages(logicObject)) {
				if (!isSchematicProcessed(usage.getDiagramObjectUID())) {
					ISchemDiagram schemDiagram = usage.getDiagram();
					diagrams.add(schemDiagram);
				}
			}
		}
		return diagrams;
	}

	public ILogicObject getTargetLogicObject()
	{
		return m_targetLogicObject;
	}

	public ILogicObject getSourceLogicObject()
	{
		return m_sourceLogicObject;
	}

	protected void mergeConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		mergeProperties(sourceLogicObject, targetLogicObject);
		mergeAttributes(sourceLogicObject, targetLogicObject);
		mergeLibraryPart(sourceLogicObject, targetLogicObject);
		mergeChildrenConnectivity(sourceLogicObject, targetLogicObject);
	}

	protected void mergeAttributes(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		AttributeUtils.copyAttributes(sourceLogicObject, targetLogicObject, false);
		mergeAnalysisModel(sourceLogicObject, targetLogicObject);
		mergeAnalysableSymbol(sourceLogicObject, targetLogicObject);
	}

	private void mergeAnalysableSymbol(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		if (sourceLogicObject instanceof IAnalysableSymbolAssociatable &&
				targetLogicObject instanceof IAnalysableSymbolAssociatable) {
			IAnalysableSymbolAssociatable srcAnalysable = (IAnalysableSymbolAssociatable) sourceLogicObject;
			IAnalysableSymbolAssociatable tgtAnalysable = (IAnalysableSymbolAssociatable) targetLogicObject;

			if (tgtAnalysable.getAnalysableSymbolUID() == null) {
				tgtAnalysable.setAnalysableSymbolUID(srcAnalysable.getAnalysableSymbolUID());
			}
		}
	}

	private void mergeAnalysisModel(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		if (StringUtils.isBlank(targetLogicObject.getAnalysisModel())) {
			targetLogicObject.setAnalysisModel(sourceLogicObject.getAnalysisModel());
		}
	}

	protected void mergeLibraryPart(ILibrariedObject sourceLogicObject, ILibrariedObject targetLogicObject)
	{
		if (sourceLogicObject.getLibraryRef() != null && targetLogicObject.getLibraryRef() == null) {
			ILibraryBaseObject baseObject = sourceLogicObject.getLibraryObject();
			targetLogicObject.assignLibraryPart((ILibraryObject) baseObject);
			if (targetLogicObject instanceof IFootprintable && sourceLogicObject instanceof IFootprintable) {
				((IFootprintable) targetLogicObject)
						.setFootprintId(((IFootprintable) sourceLogicObject).getFootprintId());
			}
		}
	}

	protected void mergeProperties(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		PropertyHelper.moveProperties(sourceLogicObject, targetLogicObject, false, false);
	}

	private void processDiagram(ISchemDiagram schemDiagram, ILogicObject logicObject, ISchematicProcessor processor)
	{
		IDiagramObjectIterator diagramObjectIterator = schemDiagram.getRepresentations(logicObject.getUID());

		while (diagramObjectIterator.hasNext()) {
			IDiagramObject diagramObject = diagramObjectIterator.getNext();
			processor.process((IConnectivityRef) diagramObject);
		}
		schemDiagram.refreshRepresentations();
	}

	protected void addMapping(ILogicObject key, ILogicObject value)
	{
		m_mappedConnectivity.put(key, value);
	}

	@Nullable
	protected ILogicObject getMappedValue(ILogicObject key)
	{
		return m_mappedConnectivity.get(key);
	}

	protected void postSchematicMerge(IConnectivityRef schemSourceObject)
	{
		doPostSchematicMerge(schemSourceObject);
		if (schemSourceObject instanceof IShareableDiagramObject) {
			((IBaseShareableDiagramObject) schemSourceObject).setHome(false);
		}
		addProcessedSchematic(schemSourceObject);
	}

	protected void doPostSchematicMerge(IConnectivityRef schemObject)
	{

	}

	protected void addProcessedSchematic(IConnectivityRef schemSourceObject)
	{
		m_processedSchemUIDs.add(schemSourceObject.getUID());
	}

	protected boolean isSchematicProcessed(IUID uid)
	{
		return m_processedSchemUIDs.contains(uid);
	}

	protected void postMergingComplete()
	{

	}

	protected void processSchematicsFor(ILogicObject logicObject, ISchematicProcessor processor)
	{
		Set<ISchemDiagram> diagrams = getDiagramsForLogicObjects(logicObject);
		for (ISchemDiagram schemDiagram : diagrams) {
			processDiagram(schemDiagram, logicObject, processor);
		}
		diagrams.clear();
	}

	protected void processSchematicsForDiagram(@NotNull ILogicObject logicObject,
			@NotNull ISchematicProcessor processor, @NotNull ISchemDiagram schemDiagram)
	{
		processDiagram(schemDiagram, logicObject, processor);
	}

	abstract void mergeChildrenConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject);

	@SuppressWarnings({"NoopMethodInAbstractClass"})
	protected void mergeSchematic(IConnectivityRef sourceSchemObject, ILogicObject targetlogicObject)
	{

	}

	public void setupConflictResolution(@Nullable IFacetConflictResolutionModel conflictResolution)
	{
		m_conflictResolution = conflictResolution;
	}
}
