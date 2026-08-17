/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2025 Siemens
 */
package chs.caplets.logic.commands;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.caplet.cmd.IProjectTraverserTransactionHandler;
import chs.caf.caplet.cmd.ProjectTraverserCmd;
import chs.caf.caplet.cmd.ProjectTraverserCommandTransactionHandler;
import chs.caf.caplet.cmd.ProjectTraverserDesignTransactionHandler;
import chs.caf.caplet.helpers.ChangedObjectsHolder;
import chs.caf.caplet.helpers.IChangedObjectsInfo;
import chs.caf.caplet.helpers.LogicUpdateStyledGraphicsHandler;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IBaseSegment;
import chs.cof.drawplus.ICompositeTextDecorationText;
import chs.cof.drawplus.IConnected;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IJoint;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.library.ILibrariedObject;
import chs.cof.logical.IConvertNetsToWiresCmd;
import chs.cof.logical.IConvertNetsToWiresParams;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ConductorQueryClient;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IHighwayConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.INetConductorIterator;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IPhysicalConductor;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IShieldConductorIterator;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.cable.IWireConductorIterator;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ILogicSegmentContainer;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.IPort;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShareableDiagramObjectWithMultipleRepresentation;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.IProjectSharedUsageView;
import chs.cof.logical.shared.ISharedAbstractable;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorIterator;
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedMulticoreIterator;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.cof.logical.shared.ISharedOverbraidIterator;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinIterator;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.logical.shared.ISharedUsage;
import chs.cof.logical.shared.ISharedUsageInfo;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedGenerationEnum;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryObject;
import chs.cof.project.IOptionExpression;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.ILogicBuildList;
import chs.cof.project.naming.INameMgr;
import chs.cof.rules.IRuledObject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.cofUtils.DeAnonymizeDesignDetails;
import chs.cofUtils.cmd.CommandHelper;
import chs.cofUtils.cmd.ResourceCommandEvent;
import chs.cofUtils.parameterized.AddSpliceHelper;
import chs.common.IAssembledObject;
import chs.common.IAttributePropertyProvider;
import chs.common.ICommandHelper;
import chs.common.IDesignAbstraction;
import chs.common.IDesignContainer;
import chs.common.INamedUIDObject;
import chs.common.IObjectFilter;
import chs.common.IPendingModification;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDObjectCollection;
import chs.common.preferencesets.IPreferenceSet;
import chs.common.styles.IStyleableObject;
import chs.common.validation.ValidationHelper;
import chs.system.FactoryMgr;
import chs.system.ICHSSystem;
import chs.system.UIDMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.BuildInfo;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.Environment;
import chs.utilities.ListMap;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.SortedList;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import chs.utility.FirstAvailableUniqueNameGenerator;
import chs.utility.Placement;
import chs.utility.PlacementForNetsToWires;
import chs.utility.PortHelper;
import chs.utility.Replicator;
import chs.utility.SharedObjectDomainAccessibliltyChecker;
import chs.utility.attr.AttributeUtils;
import chs.utility.audit.AuditableEventType;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.DeletionHelper;
import chs.utility.helpers.HighwayHelper;
import chs.utility.helpers.ISharedObjectNameGenerator;
import chs.utility.helpers.IUIDObjectDeletionListener;
import chs.utility.helpers.LibraryAssignmentHelper;
import chs.utility.helpers.LogTabType;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.helpers.PropertyCopier;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.helpers.SharedObjectNameGenerator;
import chs.utility.helpers.UtilsHelper;
import chs.utility.helpers.revisioning.CreateCloneOfSharedMulticore;
import chs.utility.logic.SchemGraphUtils;
import chs.utility.modular.ModuleCodesHelper;
import chs.utility.preferences.DecorationUtils;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.rules.IRuleMatcher;
import chs.utility.rules.IRuleResult;
import chs.utility.rules.RuleErrorListener;
import chs.utility.rules.RuleMatcher;
import chs.utility.rules.RuleStatus;
import chs.utility.rules.RuleUtils;
import chs.utility.ui.progress.IProgress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Class - ConvertNetsToWiresCmd - Converts Net to Wires on Logic Diagrams potentially across multiple Designs
 * <p>
 * Responsibilities - Provides implementations of ProjectTraverserCmd template methods to implement convert nets to wires
 * - checks preconditions are satisfied prior to processing and makes the actual changes to the datamodel.
 * <p>
 * Collaborators - ConvertNetsToWiresAction
 */

public class ConvertNetsToWiresCmd extends ProjectTraverserCmd implements IConvertNetsToWiresCmd,
		IUIDObjectDeletionListener
{

	public static final String DESIGN_SAVED_FAILED_HEADER =
			ResourceMgr.getString(ConvertNetsToWiresCmd.class, "ConvertNetsToWiresCmd.error.DesignSavedFailedHeader");
	public static final String DESIGN_SAVED_FAILED_MESSAGE =
			ResourceMgr.getString(ConvertNetsToWiresCmd.class, "ConvertNetsToWiresCmd.error.DesignSavedFailedMessage");

	private IConvertNetsToWiresParams mParams = null;
	private Replicator mReplicator = new ConvertNetsToWiresCmdReplicator(Replicator.COPY, false);
	private Set<IDesignContainer> mDesignsToProcess = new HashSet<IDesignContainer>();
	private Set<IDesignContainer> mSkippedDesigns = new HashSet<IDesignContainer>();

	/**
	 * Flag used to indicate whether or not we commit or rollback transaction via the IBoundaryTransactionMarshaller
	 */
	private boolean mCommittedTransaction = false;

	/**
	 * Lock results for IDesigns
	 */
	private Map<IDesignContainer, LOCK_RESULT> mLockResults = new HashMap<IDesignContainer, LOCK_RESULT>();

	/**
	 * Lock results for the Shared Object Manager
	 */
	private LOCK_RESULT mSharedPinListMgrLockResult = LOCK_RESULT.LOCK_FAILED;
	private LOCK_RESULT mSharedConductorMgrLockResult = LOCK_RESULT.LOCK_FAILED;

	private SharedNetsConnectivity mPortedNetConnectivity = new SharedNetsConnectivity();
	protected List<IUID> mUnprocessedPortedNets = new ArrayList<IUID>();
	private Map<IUID, String> mUnprocessedNetsDesignNames = new HashMap<IUID, String>();

	private List<ISharedMulticore> mSharedMCWithHighwayRepresentations = new ArrayList<ISharedMulticore>();
	/**
	 * A map of ISharedMulticore to SharedNetsConnectivity - this is built up during the first pass over the designs and
	 * is used to know which ISharedMulticore are valid for conversion in the second pass.
	 */
	private Map<ISharedMulticore, SharedNetsConnectivity> mSharedMulticores =
			new TreeMap<ISharedMulticore, SharedNetsConnectivity>(new Comparator<ISharedMulticore>()
			{
				public int compare(ISharedMulticore o1, ISharedMulticore o2)
				{
					return o1.getName().compareTo(o2.getName());
				}
			});
	/**
	 * A map of IDesign->Set<ISharedMulticore> - built up during the first pass over the designs.
	 */
	private SetMap<IDesignContainer, ISharedMulticore> mDesignsWithSharedMulticores =
			new SetMap<IDesignContainer, ISharedMulticore>();

	/**
	 * This is a map of UIDS of either INetConductor or ISharedConductor to ISharedPinList - this maps from a Ported Net
	 * or Shared Net to a Shared Splice - the SharedSplice it used to maintain connectivity for the Non-shared Wires
	 * which replace the Nets
	 */
	private Map<IUID, ISharedPinList> mSharedSplices = new HashMap<IUID, ISharedPinList>();

	/**
	 * This is a temporary map of UIDS of either INetConductor or ISharedConductor to ISplice - this maps from a Ported
	 * Net or Shared Net to a Connectivity Splice such that we use the same ISplice for each Diagram in a Design Similar
	 * to the mSharedSplices map except it's used to share connectivity for the Shared Splices within a Design
	 */
	private Map<IUID, IUID> mSharedConnectivitySplices = new HashMap<IUID, IUID>();

	/**
	 * This is a temporary Set used by multiple methods - must be cleared at the start of processing each design
	 */
	private Set<IUID> mInvalidMulticores = new HashSet<IUID>();

	/**
	 * This is a temporary Set used by multiple methods - must be cleared at the start of processing each design It is
	 * UIDS of new objects which we will pass to LogicSetAttributesAndPropertiesByRuleCmd
	 */
	private Set<IUID> mObjectUIDsForConstraints = new HashSet<IUID>();

	/**
	 * This is a temporary Map used by multiple methods - must be cleared at the start of processing each diagram. It is
	 * a map of INetConductor to List of schem.IConductor - we cache this ourselves because adding new schem objects to
	 * diagrams invalidates the internal map, and we don't want to have to keep regenerating it
	 */
	private ListMap<INetConductor, IConductor> mConnectivitySchemMapForUseInMC =
			new ListMap<INetConductor, IConductor>();
	private ListMap<INetConductor, IHighwaySchematic> m_netCondToHighwayMap =
			new ListMap<INetConductor, IHighwaySchematic>();

	/**
	 * These are the names of all connectivity objects gathered on the first pass over designs in the scope
	 */
	private Set<String> mWireNames = new HashSet<String>();
	private Set<String> mShieldNames = new HashSet<String>();
	private Set<String> mMulticoreNames = new HashSet<String>();
	private Set<String> mOverbraidNames = new HashSet<String>();

	/**
	 * Indent params used to format the results going to the Output Window
	 */
	private int mIndent = 0;
	private static final int INDENT = 5;

	/**
	 * mPortedConductorWireMap: maps a net connectivity object to a wire connectivity object
	 */
	private Map<IUID, IUID> mPortedConductorWireMap = new HashMap<IUID, IUID>();
	/**
	 * mNetConductortoWireConductorsMap: maps a net connectivity object to resultant wire connectivity objects
	 */
	private Map<IUID, ListMap<IUID, IUID>> mNetConductortoWireConductorsMap =
			new HashMap<IUID, ListMap<IUID, IUID>>();
	private Set<IUID> mMultiCoreWithMultitermNets = new HashSet<IUID>();
	/**
	 * dts0100507370 - mSharedNetToWireMap: maps a shared net object to a shared wire object
	 */
	private Map<ISharedConductor, ISharedConductor> mSharedNetToWireMap =
			new HashMap<ISharedConductor, ISharedConductor>();

	/**
	 * Has the second pass occured?
	 */
	private boolean multicoresProcessed = false;

	/*
	 this will used to choose any of the wires created to make it project wide shared wire of the
	 net conductor is shared across designs.
	 */
	private boolean createSharedSpliceForSharedNet = false;
	private IUID currentSharedSpliceForSharedNet = null;
	private Set<IUID> mSharedSplicesAlreadyCreated = new HashSet<IUID>();

	private NTWStylingHandler m_styledGraphicsHandler = new NTWStylingHandler();

	private Set<ISharedLockableUpdateableObject> mPreProcessingConductorMgrObjects =
			new HashSet<ISharedLockableUpdateableObject>();

	private Set<IUID> mAllDeletedUids = new HashSet<IUID>();

	@Nullable protected INetsToWiresResultCollector getOutputResultCollector()
	{
		return null;
	}

	public ConvertNetsToWiresCmd(CommandHelper commandHelper, IProject proj)
	{
		super(commandHelper, proj, Arrays.<Class<? extends IDesignContainer>>asList(ILogicDesign.class), null);
		DeletionHelper.getDeletionHelper().addUIDObjectDeletionListener(this);
	}

	public ConvertNetsToWiresCmd(IProject proj)
	{
		this(new CAFCommandHelper(), proj);
	}

	@NotNull private Set<ISharedMulticore> getSortedInnerMulticores(@NotNull ISharedMulticore sharedMC)
	{
		Set<ISharedMulticore> result = new TreeSet<ISharedMulticore>(new Comparator<ISharedMulticore>()
		{
			public int compare(ISharedMulticore o1, ISharedMulticore o2)
			{
				return LogicObjectCompare(o1, o2);
			}
		});
		result.addAll(CollectionUtils.createSet(sharedMC.getMulticores()));
		return result;
	}

	@NotNull private Set<IMulticore> getSortedInnerMulticores(@NotNull IMulticore nonSharedMC)
	{
		Set<IMulticore> result = new TreeSet<IMulticore>(new Comparator<IMulticore>()
		{
			public int compare(IMulticore o1, IMulticore o2)
			{
				return LogicObjectCompare(o1, o2);
			}
		});
		result.addAll(CollectionUtils.createSet(nonSharedMC.getMulticores()));
		return result;
	}

	@NotNull private Set<chs.cof.logical.cable.IConductor> getSortedConductors(@NotNull IMulticore nonSharedMC)
	{
		Set<chs.cof.logical.cable.IConductor> result =
				new TreeSet<chs.cof.logical.cable.IConductor>(new Comparator<chs.cof.logical.cable.IConductor>()
				{
					public int compare(chs.cof.logical.cable.IConductor o1,
							chs.cof.logical.cable.IConductor o2)
					{
						return LogicObjectCompare(o1, o2);
					}
				});
		result.addAll(CollectionUtils.createSet(nonSharedMC.getConductors()));
		return result;
	}

	private int LogicObjectCompare(@NotNull INamedUIDObject o1, @NotNull INamedUIDObject o2)
	{
		int i = o1.getName().compareTo(o2.getName());
		return i != 0 ? i : o1.getUID().compareTo(o2.getUID());
	}

	@NotNull private Set<chs.cof.logical.cable.IConductor> getSortedAllConductorsInHierarchy(@NotNull IMulticore nonSharedMC)
	{
		Set<chs.cof.logical.cable.IConductor> result =
				new TreeSet<chs.cof.logical.cable.IConductor>(new Comparator<chs.cof.logical.cable.IConductor>()
				{
					public int compare(chs.cof.logical.cable.IConductor o1,
							chs.cof.logical.cable.IConductor o2)
					{
						return LogicObjectCompare(o1, o2);
					}
				});
		result.addAll(CollectionUtils.createSet(nonSharedMC.getAllConductorsInHierarchy()));
		return result;
	}

	@NotNull private Set<ISharedConductor> getSortedConductors(@NotNull ISharedMulticore sharedMC)
	{
		Set<ISharedConductor> result = new TreeSet<ISharedConductor>(new Comparator<ISharedConductor>()
		{
			public int compare(ISharedConductor o1, ISharedConductor o2)
			{
				return LogicObjectCompare(o1, o2);
			}
		});
		result.addAll(CollectionUtils.createSet(sharedMC.getConductors()));
		return result;
	}

	@NotNull private SortedMap<IMulticore, Set<IConductor>> getMulticoreSchemNetsSorted(
			SetMap<IMulticore, IConductor> multicoreSchemNets)
	{
		SortedMap<IMulticore, Set<IConductor>> result =
				new TreeMap<IMulticore, Set<IConductor>>(new Comparator<IMulticore>()
				{
					public int compare(IMulticore o1, IMulticore o2)
					{
						return LogicObjectCompare(o1, o2);
					}
				});
		result.putAll(multicoreSchemNets);
		return result;
	}

	@NotNull private Set<ISchemDiagram> getSortedDiagrams(@NotNull IDesignContainer design)
	{
		Set<ISchemDiagram> result = new TreeSet<ISchemDiagram>(new Comparator<ISchemDiagram>()
		{
			public int compare(ISchemDiagram o1, ISchemDiagram o2)
			{
				return o1.getName().compareTo(o2.getName());
			}
		});
		result.addAll(CollectionUtils.createSet(design.getDiagramsOfType(ISchemDiagram.class)));
		return result;
	}

	@NotNull private Map<IDesignContainer, Set<ISharedMulticore>> getSortedDesignsWithSharedMCs(
			@NotNull SetMap<IDesignContainer, ISharedMulticore> designsWithSharedMulticores)
	{
		Set<IDesignContainer> designs = new HashSet<IDesignContainer>();
		for (Map.Entry<IDesignContainer, Set<ISharedMulticore>> design : designsWithSharedMulticores.entrySet()) {
			designs.add(design.getKey());
		}
		Map<IDesignContainer, Set<ISharedMulticore>> result =
				new LinkedHashMap<IDesignContainer, Set<ISharedMulticore>>();
		for (IDesignContainer design : sortDesigns(designs)) {
			if (designsWithSharedMulticores.contains(design)) {
				result.put(design, designsWithSharedMulticores.get(design));
			}
		}
		return result;
	}

	@NotNull private Set<IConductor> getSortedSchemNets(@NotNull Collection<IConductor> schemNets)
	{
		List<SchemGraphTree> sortedSchemGraphTrees =
				new SortedList<SchemGraphTree>(SchemGraphTree.getGraphTreePositionComparator());
		for (IConductor schemConductor : schemNets) {
			SchemGraphUtils.SchemGraph theSchemGraph = new SchemGraphUtils.SchemGraph(schemConductor, true);
			ISchemDiagram parentDiagram = DiagramHelper.getDiagram(schemConductor);
			assert parentDiagram != null;
			SchemGraphTree tree = new SchemGraphTree(theSchemGraph, parentDiagram);
			sortedSchemGraphTrees.add(tree);
		}
		Set<IConductor> result = new LinkedHashSet<IConductor>();
		for (SchemGraphTree tree : sortedSchemGraphTrees) {
			result.add(tree.getGraph().getSchemConductor());
		}
		sortedSchemGraphTrees.clear();
		return result;
	}

	@NotNull private Set<IConductor> getSortedSchemConductors(@NotNull ISchemDiagram schemDiagram)
	{
		Set<IConductor> result = new TreeSet<IConductor>(new Comparator<IConductor>()
		{
			public int compare(IConductor o1, IConductor o2)
			{
				chs.cof.logical.cable.IConductor n1 = o1.getConnectivity();
				chs.cof.logical.cable.IConductor n2 = o2.getConnectivity();
				int result = LogicObjectCompare(n1, n2);
				if (result == 0) {
					//we are not going to sort the design wide schem conductors. because its unnecessary, since we are
					//creating wires for a cable net once at a time and also thats consistent (via sorting at that place)
					//so the ordering at this place is not necessary and will addup the burden.
					result = 1;
				}
				return result;
			}
		});
		result.addAll(schemDiagram.getConductors());
		return result;
	}

	@NotNull private Set<IHighwaySchematic> getSortedSchemhighways(@NotNull ISchemDiagram schemDiagram)
	{
		Set<IHighwaySchematic> result = new TreeSet<IHighwaySchematic>(new Comparator<IHighwaySchematic>()
		{
			@Override public int compare(IHighwaySchematic o1, IHighwaySchematic o2)
			{
				IHighway n1 = o1.getConnectivity();
				IHighway n2 = o2.getConnectivity();
				int result = n1.getName().compareTo(n2.getName());
				if (result == 0) {
					result = 1;
				}
				return result;
			}
		});
		result.addAll(schemDiagram.getHighways());
		return result;
	}

	private void registerUnprocessedPortedNet(@NotNull INetConductor net)
	{
		if (!mUnprocessedPortedNets.contains(net.getUID())) {
			mUnprocessedPortedNets.add(net.getUID());
			mUnprocessedNetsDesignNames.put(net.getUID(), net.getDesignContainer().getFullName());
		}
	}

	@SuppressWarnings("ConstantConditions")
	protected final <T> T extractUIDObjectOfType(@NotNull final IUID uid, @NotNull final Class<T> type)
	{
		T obj = UIDMgr.getObjectOfType(uid, type);
		if (obj == null) {
			assert mAllDeletedUids.contains(uid) : "Unexpected state of uid '" + uid.getString() + "'";
		}
		return obj;
	}

	private void sortUnprocessedPortedNets()
	{
		ensureDeletedObjectsRemoved();
		Comparator<IUID> netComparator = new Comparator<IUID>()
		{
			public int compare(IUID o1, IUID o2)
			{
				INetConductor n1 = extractUIDObjectOfType(o1, INetConductor.class);
				INetConductor n2 = extractUIDObjectOfType(o2, INetConductor.class);
				int result = LogicObjectCompare(n1, n2);
				if (result == 0) {
					String designName1 = mUnprocessedNetsDesignNames.get(o1);
					String designName2 = mUnprocessedNetsDesignNames.get(o2);
					result = designName1.compareTo(designName2);
				}
				return result;
			}
		};
		Collections.sort(mUnprocessedPortedNets, netComparator);
	}

	private void ensureDeletedObjectsRemoved()
	{
		//we may have some nets (part of multicores) which could be deleted during multicore processing.
		mUnprocessedPortedNets.removeAll(mAllDeletedUids);
		Set<IUID> idsToRemove = new HashSet<IUID>();
		for (IUID netID : mUnprocessedNetsDesignNames.keySet()) {
			if (!mUnprocessedPortedNets.contains(netID)) {
				idsToRemove.add(netID);
			}
		}
		for (IUID idToRemove : idsToRemove) {
			mUnprocessedNetsDesignNames.remove(idToRemove);
		}
	}

	/**
	 * Sets the parameters for this command
	 * <p>
	 *
	 * @param params ConvertNetsToWiresParams
	 */
	public void setParams(IConvertNetsToWiresParams params)
	{
		mParams = params;

		ILogicBuildList buildListScope = mParams.getBuildListScope();
		if (buildListScope != null) {
			setBuildListToProcess(buildListScope);
			Set<IDesignContainer> designsInBL = new HashSet<IDesignContainer>(getBuildListDesigns());
			mDesignsToProcess.addAll(getDesignsToProcess(designsInBL));
			designsInBL.removeAll(mDesignsToProcess);
			mSkippedDesigns.addAll(designsInBL);
		}
		else if (mParams.getDesignScope() != null) {
			mDesignsToProcess.add(mParams.getDesignScope());
		}
	}

	private Set<IDesignContainer> getDesignsToProcess(Set<IDesignContainer> designs)
	{
		IDesignContainer activeDesign = getActiveDesign();
		// Performs Nets to Wires conversion on all designs whose abstraction is matching with abstraction of active design
		if (activeDesign != null) {
			return getDesignsWithAbstraction(designs, activeDesign.getDesignAbstraction());
		}
		return designs;
	}

	@Nullable protected IDesignContainer getActiveDesign()
	{
		return CAFUtils.getInstance().getActiveDesignContainer();
	}

	@NotNull private Set<IDesignContainer> getDesignsWithAbstraction(@NotNull Set<IDesignContainer> designs,
			IDesignAbstraction designAbstraction)
	{
		Set<IDesignContainer> targetDesigns = new HashSet<IDesignContainer>(designs.size());
		for (IDesignContainer design : designs) {
			if (design.getDesignAbstraction() == designAbstraction) {
				targetDesigns.add(design);
			}
		}
		return targetDesigns;
	}

	public Set<IDesignContainer> getDesignsToProcess()
	{
		return mDesignsToProcess;
	}

	public void clearSelections(IDesignContainer design)
	{
		getCommandHelper().clearSelections(design);
	}

	@Override @NotNull public IProgress getProgress()
	{
		return getCommandListener().getProgress();
	}

	private boolean shouldLogPerformanceInfo()
	{
		return Environment.getPerformanceInfo() != Environment.PerformanceInfo.Off;
	}

	private void logTransactionEntryMessage(IDesignContainer design)
	{
		if (shouldLogPerformanceInfo()) {
			System.out.println("//Entering transaction boundary for design '" + design.getFullName() + "'");
		}
	}

	private void logTransactionExitMessage(IDesignContainer design)
	{
		if (shouldLogPerformanceInfo()) {
			System.out.println("//Exiting transaction boundary for design '" + design.getFullName() + "'");
		}
	}

	private void logTransactionEntryMessageForSharedMulticores()
	{
		if (shouldLogPerformanceInfo()) {
			System.out.println("//Entering transaction boundary for shared multicores");
		}
	}

	private void logTransactionExitMessageForSharedMulticores()
	{
		if (shouldLogPerformanceInfo()) {
			System.out.println("//Exiting transaction boundary for shared multicores");
		}
	}

	/**
	 * @see ProjectTraverserCmd#doExecute()
	 */
	@Override protected boolean doExecute()
	{
		try {
			CreationDeletionHelper.getTheCreationHelper().disableUndo();
			if (mDesignsToProcess.isEmpty()) {
				return true;
			}
			mDesignsToProcess = sortDesigns(mDesignsToProcess);

			if (!checkPreconditions()) {
				return false;
			}

			convertNetsToWires();
		}
		catch (UserSessionException ignore) { // not ignoring, message is displayed
			getCommandHelper().showErrorMessage(DESIGN_SAVED_FAILED_HEADER, DESIGN_SAVED_FAILED_MESSAGE);
		}
		finally {
			getCommandHelper().processEdtRequests();
			unlockAndUnloadObjects();
			//now we re-enable undo.
			CreationDeletionHelper.getTheCreationHelper().enableUndo();
			CreationDeletionHelper.getTheCreationHelper().processObjects();
		}
		return true;
	}

	/**
	 * @see ProjectTraverserCmd#doExecuteAllowed()
	 */
	@Override public boolean doExecuteAllowed()
	{
		return mParams != null && (mBuildList == null || checkAndReportBuildListForInaccessibleDesign(mBuildList));
	}

	/**
	 * @see ProjectTraverserCmd#isDesignValidForProcessing(IDesignContainer)
	 */
	@Override protected boolean isDesignValidForProcessing(IDesignContainer design)
	{
		return (design instanceof ILogicDesign) && super.isDesignValidForProcessing(design);
	}

	@Override protected boolean doProcessObjects(@Nullable final IBaseDiagram diagram,
			@NotNull final Set<IUID> objectUIDsToProcess, int numObjsPerProgressInc)
	{
		return false;
	}

	/**
	 * @see ProjectTraverserCmd#doFilterOrAddObjectsFromRep(IRepresentedObject, Set)
	 */
	@Override protected void doFilterOrAddObjectsFromRep(IRepresentedObject aRepObject, Set<IUID> aObjectUIDsToProcess)
	{
	}

	protected void doFilterOrAddObjectsFromMultipleRepObject(
			IShareableDiagramObjectWithMultipleRepresentation aRepObject, Set<IUID> aObjectUIDsToProcess)
	{

	}

	/**
	 * @see ProjectTraverserCmd#beginProcessing(IDesignContainer, LOCK_RESULT)
	 */
	@Override protected void beginProcessing(IDesignContainer design, LOCK_RESULT lockResult)
			throws UserSessionException
	{
		// Have to clear temporary Collections that hold objects by reference rather than UID as they are only
		// valid within the scope of a Design
		mInvalidMulticores.clear();
		// dts0100594255 - Fix: Don't clear the shared connectivity splices map!
		mObjectUIDsForConstraints.clear();

		collectConductorMgrObjects(getProject().getSharedConductorMgr(), mPreProcessingConductorMgrObjects);

		logTransactionEntryMessage(design);

		outputStatusMessage("Message.ConvertingDesign", design.getFullName());
	}

	private void collectConductorMgrObjects(@NotNull ISharedConductorMgr shdCondMgr,
			Set<ISharedLockableUpdateableObject> conductorMgrObjects)
	{
		//collect the existing set of children of shared conductor manager.
		CollectionUtils.add(shdCondMgr.getSharedConductors(), conductorMgrObjects);
		CollectionUtils.add(shdCondMgr.getSharedGeneralHighways(), conductorMgrObjects);
		CollectionUtils.add(shdCondMgr.getSharedMulticores(), conductorMgrObjects);
		CollectionUtils.add(shdCondMgr.getSharedOverbraids(), conductorMgrObjects);
	}

	private void flushNewDeletedAndModifiedConductorMgrObjects(ISharedConductorMgr shadCondMgr)
	{
		Set<ISharedLockableUpdateableObject> preProcessSet = mPreProcessingConductorMgrObjects;

		Set<ISharedLockableUpdateableObject> postProcessSet = new HashSet<ISharedLockableUpdateableObject>();
		collectConductorMgrObjects(shadCondMgr, postProcessSet);
		Set<ISharedLockableUpdateableObject> condMgrObjsToReset =
				new HashSet<ISharedLockableUpdateableObject>(postProcessSet);

		Set<ISharedLockableUpdateableObject> backupPreProcessSet =
				new HashSet<ISharedLockableUpdateableObject>(preProcessSet);

		preProcessSet.retainAll(postProcessSet);
		Set<ISharedLockableUpdateableObject> commonObjectSet =
				new HashSet<ISharedLockableUpdateableObject>(preProcessSet);

		backupPreProcessSet.removeAll(commonObjectSet);
		Set<ISharedLockableUpdateableObject> deletedObjectSet =
				new HashSet<ISharedLockableUpdateableObject>(backupPreProcessSet);

		postProcessSet.removeAll(commonObjectSet);
		Set<ISharedLockableUpdateableObject> newObjectSet =
				new HashSet<ISharedLockableUpdateableObject>(postProcessSet);

		if (deletedObjectSet.isEmpty()) {
			for (ISharedLockableUpdateableObject newShdObject : newObjectSet) {
				newShdObject.flushNew(shadCondMgr.getObjType(), shadCondMgr);
			}
			//the flush of shared multicore marks it non-modified which is inherited
			//by children also and thus child conductor/multicore might get skipped
			//if parent comes first in the collection. So we need to get a collection of
			//modified items before flushing them.
			List<ISharedLockableUpdateableObject> modifiedItems =
					CollectionUtils.getFilteredCollection(commonObjectSet, ISharedObject::isModified);
			for (ISharedLockableUpdateableObject sharedObject : modifiedItems) {
				sharedObject.flush();
			}
		}
		else {
			//this case should not arise in our processing. Should we try to use manager service
			//to delete the deleted objects and do delta save as above.
			shadCondMgr.flush();
		}
		for (ISharedLockableUpdateableObject condMgrObjToReset : condMgrObjsToReset) {
			if (condMgrObjToReset instanceof IPendingModification) {
				((IPendingModification) condMgrObjToReset).setModified(false);
			}
		}
		mPreProcessingConductorMgrObjects.clear();
	}

	private void populateObjectNames(IDesignContainer design)
	{
		// Add names of objects so that when we create shared multicores/overbraids the names are unique in the scope
		IConnectivity connectivity = getConnectivity(design);
		assert connectivity != null;
		for (IWireConductor wire : connectivity.getWireConductors()) {
			mWireNames.add(wire.getName());
		}
		for (IMulticore multicoreOrOverbraid : connectivity.getMulticores(false)) {
			if (multicoreOrOverbraid instanceof IOverbraid) {
				mOverbraidNames.add(multicoreOrOverbraid.getName());
			}
			else {
				mMulticoreNames.add(multicoreOrOverbraid.getName());
			}
		}
		for (IShieldConductor shieldConductor : connectivity.getShieldConductors()) {
			mShieldNames.add(shieldConductor.getName());
		}
	}

	/**
	 * @see ProjectTraverserCmd#completeProcessing(IDesignContainer, LOCK_RESULT, boolean,
	 * IProjectTraverserTransactionHandler)
	 */
	@Override protected void completeProcessing(@NotNull IDesignContainer aDesign, @NotNull LOCK_RESULT aLockResult,
			boolean aModifiedObjects, @NotNull IProjectTraverserTransactionHandler transactionHandler)
			throws UserSessionException
	{
		// Before we completeProcessing we will apply the Attribute and Property constraints to the new created objects
		ILogicDesign logicDesign = CommonUtils.cast(aDesign, ILogicDesign.class);
		assert logicDesign != null;
		LogicSetAttributesAndPropertiesByRuleCmd constraintsCmd =
				new LogicSetAttributesAndPropertiesByRuleCmd(getCommandHelper(), logicDesign);
		constraintsCmd.setObjectsToProcess(mObjectUIDsForConstraints);
		if (constraintsCmd.prepare()) {
			constraintsCmd.doExecuteOnlyIfAllowed();
		}

		m_styledGraphicsHandler.updateChangedObjectInfo(CreationDeletionHelper.getTheCreationHelper());

		populateObjectNames(aDesign);

		// Regenerates usages and saves design
		// will also validate the design if validationlevel=LOW+
		super.completeProcessing(aDesign, aLockResult, aModifiedObjects, transactionHandler);

		// validate the UIDMgr if validationlevel=HIGH
		ValidationHelper.validateObjectChanges(UIDMgr.getUIDMgr(), aDesign);

		IProject project = getProject();
		// also validate the shared mgrs if validationlevel=LOW+
		ValidationHelper.validateBeforeSave(project.getSharedConductorMgr(), project.getSharedPinListMgr());

		flushNewDeletedAndModifiedConductorMgrObjects(project.getSharedConductorMgr());
		project.getSharedPinListMgr().flush();
		getCommandHelper().endAndCommitTransaction();
		mCommittedTransaction = true;
		//this hack doesn't look good. but can't help. in-between pass of processing the objects are being processed
		//and design connectivity is expected to be loaded. (after completeprocessing but bfore beginprocessing)
		//thus causing connectivity of all processed designs left loaded even after nets2wires action finishes.
		//forcefully loading here because previously populateObjectNames was being called after unload of design
		//and was eventually loading the connectivity.
		getConnectivity(aDesign);
	}

	protected void doPreSaveChanges(IDesignContainer aDesign, IChangedObjectsInfo changedObjectsInfo)
	{
		m_styledGraphicsHandler.updateStyle(aDesign);
		// net conductors are now removed from connectivity, so applyStyleSet correctly
		// evaluates wire-connected context and applies circle instead of square.
		m_styledGraphicsHandler.applyDeferredSpliceStyles();
	}

	/**
	 * @see ProjectTraverserCmd#doesDesignNeedSave(IDesignContainer, LOCK_RESULT)
	 */
	@Override protected boolean doesDesignNeedSave(@NotNull final IDesignContainer design, LOCK_RESULT lockResult)
	{
		return true;
	}

	/**
	 * Checks permissions, attains locks, checks scope. Returns true if we are able to do the conversion.
	 *
	 * @return true Iff the command parameters are valud - primarily all required locks were acquired
	 */
	private boolean checkPreconditions()
	{
		boolean committedTransaction = false;
		boolean result = false;
		// We decrement numObjectsRemainingToBeLocked such that exceptions result in us unlocking and unloading objects
		// yet we can still report all lock failures rather than just the first one.
		IProject project = getProject();
		try (IProjectTraverserTransactionHandler transactionHandler = new ProjectTraverserCommandTransactionHandler(
				getCommandHelper(), this)) {
			// Check user has permission to edit Logic Designs
			ICHSSystem chsSystem = UtilsHelper.getCHSSystem();
			if (!chsSystem.getFunctionalPermissionMgr().hasPermission(FunctionalPermissionEnum.EditLogicDesigns)) {
				outputStatusMessage("Message.NoEditLogicDesignsPermission");
				return false;
			}

			boolean openedAllDesignsForEdit = true;
			// Lock designs second, only if we got locks on the Shared Object Managers
			Set<IDesignContainer> designsAlreadyLocked = new HashSet<IDesignContainer>();
			for (IDesignContainer design : mDesignsToProcess) {
				// Report problems with every design - may save user some time
				LOCK_RESULT designLockResult = lockDesignAndReportErrors(design);
				mLockResults.put(design, designLockResult);
				if (isLockResultOk(designLockResult)) {
					if (designLockResult == LOCK_RESULT.ALREADY_LOCKED) {
						designsAlreadyLocked.add(design);
					}
				}
				else {
					openedAllDesignsForEdit = false;
				}
			}

			openedAllDesignsForEdit = openedAllDesignsForEdit && checkDesignAccessBasedOnSharedObjectDomain();

			if (!openedAllDesignsForEdit) {
				outputStatusMessage("Message.CouldNotOpenDesignsForEdit");
			}

			// Clear UndoableContainers and refresh Shared Multicores
			boolean shouldLockSharedManagers = true;
			if (openedAllDesignsForEdit) {
				// Because we have locked/refreshed the Shared Object Manager we must get rid of
				// all undo/redo state incase any of them refer to non-existent Shared Objects
				for (IDesignContainer designAlreadyLocked : designsAlreadyLocked) {
					getCommandHelper().clearDesignUndoableContainer(designAlreadyLocked);
				}
				result = true;
				// If the context is a single Design make sure it has no Shared Nets
				ILogicDesign singleDesignScope = mParams.getDesignScope();
				if (singleDesignScope != null) {
					Set<ISharedConductor> sharedNets = getSharedNets(singleDesignScope);
					result =handleSharedNetsInSingleDesignScope(sharedNets);
					if (result) {
						shouldLockSharedManagers = false;
					}
				}
			}

			//All succeeded till now, now lock shared object managers
			if (shouldLockSharedManagers) {
				// Lock and refresh the Shared Object Managers first
				mSharedPinListMgrLockResult = lockObject(project.getSharedPinListMgr());
				if (!isLockResultOk(mSharedPinListMgrLockResult)) {
					result = false;
					outputStatusMessage("Message.CouldNotLockSharedPinListMgr", project.getName());
				}
				// Note: refreshing Shared Conductor Manager will ensure all Shared Multicores are refreshed
				mSharedConductorMgrLockResult = lockObject(project.getSharedConductorMgr());
				if (!isLockResultOk(mSharedConductorMgrLockResult)) {
					result = false;
					outputStatusMessage("Message.CouldNotLockSharedConductorMgr", project.getName());
				}
			}

			if (result) {
				getCommandHelper().endAndCommitTransaction();
				transactionHandler.setCommitTransaction(true);
				committedTransaction = true;
			}
		}
		finally {
			if (!committedTransaction) {
				unlockAndUnloadObjects();
			}
		}

		return result && committedTransaction;
	}

	private boolean checkDesignAccessBasedOnSharedObjectDomain()
	{
		List<ILogicDesign> designs = mDesignsToProcess.stream().filter(design -> design instanceof ILogicDesign)
				.map(design -> (ILogicDesign) design).collect(
						Collectors.toList());

		Set<IUID> accessibleDesignUIDs = SharedObjectDomainAccessibliltyChecker
				.filterDesignContainersBasedOnAccessibility(designs);

		if (designs.size() != accessibleDesignUIDs.size()) {
			reportDesignAccessFailure(designs, accessibleDesignUIDs);
			return false;
		}
		return true;
	}

	private void reportDesignAccessFailure(List<ILogicDesign> designs, Set<IUID> accessibleDesignUIDs)
	{
		// for failure output message to be consistent
		Collections.sort(designs, new Comparator<ILogicDesign>()
		{
			@Override public int compare(ILogicDesign o1, ILogicDesign o2)
			{

				return new AlphaNumComparator<String>().compare(getDesignName(o1), getDesignName(o2));
			}
		});

		for (ILogicDesign design : designs) {
			if (!accessibleDesignUIDs.contains(design.getUID())) {
				reportDesignHasInaccessibleSharedDataFailure(design);
				break;
			}
		}
	}

	@NotNull private static String getDesignName(ILogicDesign design){

		DeAnonymizeDesignDetails extractDesignDetailsHelper = new DeAnonymizeDesignDetails(
				design, design.getProject());
		return extractDesignDetailsHelper.getName();
	}


	/**
	 * Converts all convertable Nets to Wires in the scope specified in mParams.
	 * <p>
	 *
	 * @throws UserSessionException Potentially thrown by beginProcessing
	 */
	private void convertNetsToWires() throws UserSessionException
	{
		IProject project = getProject();
		IUIDObject conversionScope =
				mParams.getBuildListScope() != null ? mParams.getBuildListScope() : mParams.getDesignScope();
		getCommandHelper()
				.postAuditTrailEvent(AuditableEventType.CONVERT_NETS_TO_WIRES_RUN, project.getUID(), conversionScope,
						getDesignNames(mDesignsToProcess));

		getCommandListener().startProcessing(mDesignsToProcess.size());

		outputStatusMessage("Message.StartingConvertion");
		for (IDesignContainer design : mSkippedDesigns) {
			String skippedDesignName = design.getFullName();
			String skippedDesignAbstraction =
					design.getDesignAbstraction() != null ? design.getDesignAbstraction().getName() : "";
			IDesignContainer activeDesign = CAFUtils.getInstance().getActiveDesignContainer();
			if (activeDesign != null) {
				String activeDesignName = activeDesign.getName();
				String activeDesignAbstraction =
						activeDesign.getDesignAbstraction() != null ? activeDesign.getDesignAbstraction().getName() :
								"";
				outputStatusMessage("Message.SkipDesign", skippedDesignName, skippedDesignAbstraction,
								activeDesignName, activeDesignAbstraction);
			}
		}

		outputStatusMessage("Message.StartingConversionFirstPass");
		mUnprocessedPortedNets.clear();
		mUnprocessedNetsDesignNames.clear();
		mSharedSplicesAlreadyCreated.clear();
		for (IDesignContainer design : mDesignsToProcess) {
			if (isCancelled()) {
				break;
			}
			// Make sure the design is locked
			if (!design.lock()) {
				LogicActionMessageHelper.warnLocked(design);
				return;
			}
			convertNetsToWires(design);

			getCommandListener().incrementProcessing();
		}

		if (!isCancelled()) {
			convertSharedMulticores();

			//Process shared nets with no more than two pins
			if (multicoresProcessed) {
				outputStatusMessage("Message.StartingConversionThirdPass");
			}
			else {
				outputStatusMessage("Message.StartingConversionSecondPass");
			}

			//we have to do it here because the shared nets are being sorted using design full name.
			//at the start we are having a list because while collecting them
			sortUnprocessedPortedNets();
		}
		for (IDesignContainer design : mDesignsToProcess) {
			if (isCancelled()) {
				break;
			}
			// Make sure the design is locked
			if (!design.lock()) {
				LogicActionMessageHelper.warnLocked(design);
				return;
			}
			convertPortedNetsToWires(design);
			getCommandListener().incrementProcessing();
		}
		mSharedConnectivitySplices.clear();
		mPortedConductorWireMap.clear();
		mSharedNetToWireMap.clear();
		mSharedSplicesAlreadyCreated.clear();

		outputStatusMessage("Message.ConversionComplete");
		getCommandListener().endProcessing(mDesignsToProcess.size());
	}
	private void applyStyleToAllSplicesConnectedToWireAndShield(@NotNull IDesignContainer design)
	{
		IConnectivity connectivity = getConnectivity(design);
		if (connectivity == null) {
			return;
		}
		Collection<ISplice> splices = connectivity.getSpliceList().stream()
				.filter(splice -> ConnectionHelper.isSpliceConnectedToWireAndMCShieldAndNoNet(splice))
				.collect(Collectors.toList());

		for (ISchemDiagram diagram : getSortedDiagrams(design)) {

			Set<IStyleableObject> styleableObjects = splices.stream()
					.flatMap(splice -> CollectionUtils.getObjectList(diagram.getRepresentations(splice.getUID()),
							IPinList.class).stream())
					.flatMap(schemSplice -> schemSplice.getObjectsForStyling().stream())
					.collect(Collectors.toSet());

			PreferenceSetHelper.applyStyleSet(styleableObjects, diagram, true);
		}
	}

	/**
	 * Potentially convert each ported net, to a single ported wire, if it has no more than two ends (isValid) Otherwise
	 * convert it to multiple wires and a single shared splice
	 *
	 * @param design Design scope
	 *
	 * @throws UserSessionException Exception
	 */
	private void convertPortedNetsToWires(IDesignContainer design) throws UserSessionException
	{
		mCommittedTransaction = false;
		mObjectUIDsForConstraints.clear();
		Set<INetConductor> convertedNets =
				new HashSet<INetConductor>();//new TreeSet<INetConductor>(new AlphaNumComparator<INetConductor>());

		try (IProjectTraverserTransactionHandler transactionHandler = beginTransactionForDesign(design,
				getCommandHelper())) {

			beginProcessing(design, mLockResults.get(design));
			Set<IConductor> schemConductorsToDelete = new HashSet<IConductor>();
			FirstAvailableUniqueNameGenerator firstAvail = new FirstAvailableUniqueNameGenerator();
			firstAvail.setStartIndex(1);
			for (ISchemDiagram diagram : getSortedDiagrams(design)) {
				outputStatusMessage("Message.ConvertingDiagram", indent(), link(design, diagram, diagram.getName()));
				mIndent++;
				convertPortedNetsToWires(convertedNets, diagram, design, schemConductorsToDelete);
				mIndent--;

				diagram.refreshRepresentations();
			}

			for (IConductor schemNetConductor : schemConductorsToDelete) {
				schemNetConductor.delete();
			}

			deleteRedundantConnectivity(design, convertedNets, null);
			completeProcessing(design, mLockResults.get(design), true, transactionHandler);
			applyStyleToAllSplicesConnectedToWireAndShield(design);
		}
	}

	/**
	 * Converts net object on the diagram
	 *
	 * @param convertedNets List of connectivity, will contain all replaced nets
	 * @param diagram Diagram in scope
	 * @param design Design containing the connectivity info
	 * @param schemConductorsToDelete List will contain schem nets to be deleted by the end of the pass
	 */
	private void convertPortedNetsToWires(Set<INetConductor> convertedNets, ISchemDiagram diagram,
			IDesignContainer design, Set<IConductor> schemConductorsToDelete)
	{
		ensureDeletedObjectsRemoved();
		for (IUID netUID : mUnprocessedPortedNets) {
			INetConductor netConductor = extractUIDObjectOfType(netUID, INetConductor.class);
			boolean success = convertedNets.add(netConductor);
			if (!mPortedNetConnectivity.isValid(netConductor)) {
				if (success) {
					Set<IConductor> representedConductors = getDWSchemConductors(design, netConductor);
					//we must delete schem conductors here only because the above loop contains list of nets across
					//designs and for shared conductor even if the net in processing belongs to another design.
					//the design wide shared usage manager will return empty schem conductors (which have been
					//converted to wires in the previous iteration but not deleted yet).
					Set<IConductor> schemConductorsToDeleteInCurrentDesign = new HashSet<IConductor>();

					Set<IHighwaySchematic> representedHighways = getDWSchemHighways(design, netConductor);
					if (representedHighways.isEmpty()) {
						List<SchemGraphUtils.SchemGraph> graphs =
								convertSingleCableNetToWires(design, representedConductors, true);
						createSchematicConductor(design, schemConductorsToDeleteInCurrentDesign, graphs);
						for (IConductor schemNetConductor : schemConductorsToDeleteInCurrentDesign) {
							schemNetConductor.delete();
						}
					}
					else {
						outputStatusMessage("Message.InvalidHighway", link(design, netConductor, netConductor.getName()));
					}
				}
			}
			else {
				convertSinglePortedCableNetToWires(design, netConductor, diagram, schemConductorsToDelete);
			}
		}
	}

	private void convertSinglePortedCableNetToWires(IDesignContainer design, INetConductor portedNetConductor,
			ISchemDiagram diagram, Set<IConductor> schemConductorsToDelete)
	{
		Set<IConductor> schemConductors =
				CollectionUtils.getObjects(diagram.getRepresentations(portedNetConductor.getUID()), IConductor.class);
		for (IConductor schemConductor : getSortedSchemNets(schemConductors)) {
			IWireConductor wireConductor = convertPortedSchemNetToSingleWire(design, schemConductor, true);
			IGrid grid = diagram.getGrid();
			IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
			createSchemWireConductor(design, wireConductor, diagram, grid, styleSet, schemConductor,
					schemConductorsToDelete);
		}

		// Convert nets in the highway to wires.
		Set<IHighwaySchematic> schemHighways =
				CollectionUtils
						.getObjects(diagram.getRepresentations(portedNetConductor.getUID()), IHighwaySchematic.class);
		for (IHighwaySchematic schemConductor : schemHighways) {
			convertPortedSchemNetOnHighwayToSingleWire(design, portedNetConductor, schemConductor, true);
		}
	}

	private IWireConductor convertPortedSchemNetToSingleWire(IDesignContainer design, IConductor schemConductor,
			boolean bShare)
	{
		IWireConductor wireConductor = createWireConnectivity(design,
				(INetConductor) schemConductor.getConnectivity(), bShare);
		mObjectUIDsForConstraints.add(wireConductor.getUID());
		updateNets2WiresMap((INetConductor) schemConductor.getConnectivity(), wireConductor);
		return wireConductor;
	}

	/**
	 * Convert nets in highways (nets represented by highways) to wires. If net already converted to wire, just updates
	 * highway to refer to new wire
	 *
	 * @param design Design
	 * @param netCond Net to be converted to wire
	 * @param schemHighway Highway within which net to be converted
	 */
	private void convertPortedSchemNetOnHighwayToSingleWire(IDesignContainer design, INetConductor netCond,
			IHighwaySchematic schemHighway, boolean bShare)
	{

		IWireConductor wireConductor = createWireConnectivity(design, netCond, bShare);
		mObjectUIDsForConstraints.add(wireConductor.getUID());

		IGeneralHighway highway = HighwayHelper.toGeneralHighway(schemHighway);
		if (highway != null) {
			highway.removeStackPinConductor(netCond);
			highway.addStackPinConductor(wireConductor);
		}
		Set<IAbstractPin> terms = netCond.getPinSet();
		m_styledGraphicsHandler.recordTerminationsForStyling(terms);
		INetsToWiresResultCollector outputResults = getOutputResultCollector();
		if (outputResults != null) {
			outputResults.addInfoUsingConnectivity(netCond.getName(), design.getName(), terms,
					Collections.singleton(wireConductor), Collections.<ISplice>emptySet());
		}
		outputNetToSingleWireMessage(design, netCond.getName(), wireConductor.getName(), schemHighway);
	}

	/**
	 * Creates a schem wire object for a given connectivity wire object
	 *
	 * @param design Design that contains the conenctivity
	 * @param wireConductor Wire connectivity
	 * @param diagram Diagram the should contain the schematic
	 * @param grid Grid
	 * @param styleSet StyleSet
	 * @param schemConductor The schematic object of the net conductor, this method will clone its properties
	 * @param schemConductorsToDelete The list will contain by the end of the "third pass", all the converted instances
	 * of the schematic nets
	 */
	private void createSchemWireConductor(IDesignContainer design,
			IWireConductor wireConductor, ISchemDiagram diagram, IGrid grid, IPreferenceSet styleSet,
			IConductor schemConductor, Set<IConductor> schemConductorsToDelete)
	{

		Set<IGfxObject> condObjects = CollectionUtils.getObjects(schemConductor.getObjects(), IGfxObject.class);

		Set<IAbstractPin> terminations = getNetTerminations(schemConductor);
		if (wireConductor != null && !schemConductor.getSegments().isEmpty()) {
			// Reparent all objects from existing Schem Conductor to new Schem Conductor for wire

			schemConductorsToDelete.add(schemConductor);
			for (IGfxObject gfxObject : condObjects) {
				schemConductor.removeObject(gfxObject);
			}
			IConductor schemWireConductor = FactoryMgr.getSchemFactory()
					.constructConductor(FactoryMgr.getCommonFactory().createUID(), wireConductor);
			for (IGfxObject gfxObject : condObjects) {
				gfxObject.setContainer(schemWireConductor);
				schemWireConductor.addObject(gfxObject);
			}

			IAssembly assembly = schemConductor.getConnectivity().getAssembly();
			if (assembly != null) {
				assembly.addElement(wireConductor);
			}
			schemWireConductor.setHome(schemConductor.isHome());

			diagram.removeObject(schemConductor);
			updateSchemConductor(schemWireConductor, wireConductor);
			diagram.addObject(schemWireConductor);
			if (styleSet != null) {
				schemWireConductor.applyStyle();
			}
			m_styledGraphicsHandler.recordTerminationsForStyling(terminations);

			INetsToWiresResultCollector outputResults = getOutputResultCollector();
			if (outputResults != null) {
				outputResults.addInfoUsingSchem(schemConductor.getConnectivity().getName(), design.getName(),
						terminations, Collections.<IConductor>singleton(schemWireConductor),
						Collections.<IPinList>emptySet());
			}
			outputNetToSingleWireMessage(design, schemConductor.getConnectivity().getName(), schemWireConductor);
		}
	}

	/**
	 * Creates a shared wire conenctivity object to replace the given net coductor. If there already exists a shared
	 * connectivty object it just returns it
	 *
	 * @param design Design to create the wire conenctivty in
	 * @param netConductor Connectivity object of the net conductor (to create a shared wire of the same
	 * specifications)
	 *
	 * @return Wire connectivity object
	 */
	protected IWireConductor createWireConnectivity(IDesignContainer design, INetConductor netConductor, boolean bShare)
	{
		IProject project = design.getProject();
		if (project != null) {
			project.getNameMgr().getObjectPrefix(INameMgr.WIRECONDUCTOR).getString();
		}
		IUID wireUID = mPortedConductorWireMap.get(netConductor.getUID());
		IWireConductor wireConductor = wireUID != null ? extractUIDObjectOfType(wireUID, IWireConductor.class) : null;

		IConnectivity connectivity = getConnectivity(design);
		assert connectivity != null;

		if (wireConductor == null) {
			//dts0100513657, Fix: Use the propertied cable factory to create wires, to get the properties defined in CProject auto-assigned
			wireConductor =
					FactoryMgr.getCablePropertiedFactory()
							.createWireConductor(FactoryMgr.getCommonFactory().createUID());
			assignLibrayPartAndCopyAttributesAndProperties(netConductor, wireConductor);
			copyConductorPinConnections(netConductor, wireConductor);
			copyAssembly(netConductor, wireConductor);

			IOptionExpression optionExpre = netConductor.getOptionExpression();
			if (optionExpre != null) {
				wireConductor.setOptionExpression(
						FactoryMgr.getProjectFactory().constructOptionExpression(optionExpre.getExpression()));
			}
			IMulticore multicore = netConductor.getMulticore();
			if (multicore != null) {
				multicore.addConductor(wireConductor);
			}
			Set<IGeneralHighway> highways = netConductor.getHighways();
			for (IGeneralHighway highway : highways) {
				highway.addConductor(wireConductor);
			}
			connectivity.addWireConductor(wireConductor);
			mWireNames.add(wireConductor.getName());
			mPortedConductorWireMap.put(netConductor.getUID(), wireConductor.getUID());
		}
		else {
			return wireConductor;
		}

		if (bShare) {
			if (netConductor.getSharedConductor() != null) {
				ISharedConductor sharedWire =
						createSharedWire(netConductor.getSharedConductor(), wireConductor, design);
				wireConductor.setSharedConductor(sharedWire);
				//PropertyTemplateHelper.AssociateAutoAssignProperties(wireConductor, getProject(), false);
			}
			else {
				//wireConductor.setPort(true);

				/*String wireBaseName = firstAvailableName.generateName(prefix, mWireNames);
			 mWireNames.add(wireBaseName);
			 wireConductor.setName(wireBaseName);*/
			}
		}
		return wireConductor;
	}

	private void assignLibrayPartAndCopyAttributesAndProperties(INetConductor netConductor,
			IWireConductor wireConductor)
	{
		assignLibraryPart(netConductor, wireConductor);
		//the attributes must be setup after part assignment otherwise wire color/csa etc are being reset to null if
		//library part is not assigned. this method should not override the values because of library part.
		copyAttributesAndProperties(netConductor, wireConductor);
		ConvertNetsToWiresCmdReplicator.setupTopoSignal(netConductor, wireConductor);
	}

	private void assignLibraryPart(ILibrariedObject netConductor, ILibrariedObject wireConductor)
	{
		ILibraryBaseObject libraryObject = netConductor.getLibraryObject();
		if (libraryObject != null) {
			wireConductor.assignLibraryPart((ILibraryObject) libraryObject);
		}
		//dts0100686182 - Nets to wires action adds another set of usable inner cores to the multicore in the
		// browser tree. In case of Multicore Net, Conductor.getLibraryObject() returns null as it would be a
		// ILibrarySingleWirecore object type. Hence make use of the below function to assign library info to
		// converted wires.
		else {
			LibraryAssignmentHelper.assignLibraryObject(wireConductor, netConductor);
		}
	}

	/**
	 * Returns a string of comma separated designs' names
	 *
	 * @param designs Set of designs
	 *
	 * @return String of comma separated designs' names
	 */
	@NotNull
	private String getDesignNames(@Nullable Set<IDesignContainer> designs)
	{
		if (designs == null || designs.isEmpty()) {
			return "";
		}
		List<String> designNames = new ArrayList<String>();
		for (IDesignContainer design : designs) {
			if (design != null) {
				designNames.add(design.getName());
			}
		}
		Collections.sort(designNames);
		return StringUtils.convertListToString(designNames, ", ");
	}

	/**
	 * Converts Nets to Wires on a single Design - excluding Shared Multicores
	 *
	 * @param design IDesign Design to process
	 *
	 * @throws UserSessionException Thrown if save fails
	 */
	private void convertNetsToWires(@NotNull IDesignContainer design) throws UserSessionException
	{
		mCommittedTransaction = false;

		try (IProjectTraverserTransactionHandler transactionHandler = beginTransactionForDesign(design,
				getCommandHelper())) {

			beginProcessing(design, mLockResults.get(design));
			mIndent = 1;
			Set<INetConductor> convertedNets = new HashSet<INetConductor>();
			Set<IConductor> schemConductorsToDelete = new HashSet<IConductor>();
			Set<IMulticore> processedMulticores = new HashSet<IMulticore>(); //to handle across diagrams MCs
			mMultiCoreWithMultitermNets.clear();
			updateSourceSignalNameOnNonSharedShieldConductors(design);
			prepareMulticoreInnerCoresForNetsToWires(design);
			for (ISchemDiagram diagram : getSortedDiagrams(design)) {
				outputStatusMessage("Message.ConvertingDiagram", indent(), link(design, diagram, diagram.getName()));
				mIndent++;
				convertNetsToWires(design, diagram, convertedNets, schemConductorsToDelete, processedMulticores);
				mIndent--;
				diagram.refreshRepresentations();
			}

			List<IMulticore> toDel = new ArrayList<IMulticore>();
			for (IUID mcUID : mMultiCoreWithMultitermNets) {
				IMulticore mc = extractUIDObjectOfType(mcUID, IMulticore.class);
				toDel.addAll(mc.getAllMulticoresInHierarchy());
			}
			for (ISchemDiagram diagram : getSortedDiagrams(design)) {
				DeleteHelper.getInstance().delete(diagram, toDel, true);
			}

			for (IConductor schemNetConductor : schemConductorsToDelete) {
				schemNetConductor.delete();
			}
			deleteRedundantConnectivity(design, convertedNets, null);

			completeProcessing(design, mLockResults.get(design), true, transactionHandler);
		}
	}

	private void updateSourceSignalNameOnNonSharedShieldConductors(IDesignContainer design)
	{
		IConnectivity connectivity = getConnectivity(design);
		assert connectivity != null;
		IObjectFilter<IPhysicalConductor> filter = chs.cof.logical.cable.IConductor.Statics.getNotInMulticoreFilter();
		for (IShieldConductor shieldConductor : connectivity.getShieldConductors()) {
			if (!filter.accept(shieldConductor)) {
				ConvertNetsToWiresCmdReplicator.setupTopoSignal(shieldConductor, shieldConductor);
			}
		}
	}

	/**
	 * Converts all Nets to Wires in mSharedMulticores by cloning each Multicore and changing each Net to a Wire -
	 * basically changes the type and renames it
	 * <p>
	 *
	 * @throws UserSessionException Thrown if save fails
	 */
	private void convertSharedMulticores() throws UserSessionException
	{
		IProject project = getProject();
		ISharedObjectNameGenerator sharedObjectNameGenerator = new SharedObjectNameGenerator(project);
		ISharedConductorMgr sharedConductorMgr = getProject().getSharedConductorMgr();
		ISharedConductorIterator sharedConductors = sharedConductorMgr.getSharedConductors();
		for (ISharedConductor sharedConductor : sharedConductors) {
			if (sharedConductor.isWire()) {
				mWireNames.add(sharedConductor.getName());
			}
			else if (sharedConductor.isShield()) {
				mShieldNames.add(sharedConductor.getName());
			}
		}
		FirstAvailableUniqueNameGenerator wireNameGenerator = new FirstAvailableUniqueNameGenerator();
		wireNameGenerator.setStartIndex(1);

		FirstAvailableUniqueNameGenerator multicoreNameGenerator = new FirstAvailableUniqueNameGenerator();
		multicoreNameGenerator.setStartIndex(1);
		ISharedMulticoreIterator sharedMulticores = sharedConductorMgr.getSharedMulticores();
		for (ISharedMulticore sharedMulticore : sharedMulticores) {
			mMulticoreNames.add(sharedMulticore.getName());
		}

		FirstAvailableUniqueNameGenerator overbraidNameGenerator = new FirstAvailableUniqueNameGenerator();
		overbraidNameGenerator.setStartIndex(1);
		ISharedOverbraidIterator sharedOverbraids = sharedConductorMgr.getSharedOverbraids();
		for (ISharedOverbraid sharedOverbraid : sharedOverbraids) {
			mOverbraidNames.add(sharedOverbraid.getName());
		}

		FirstAvailableUniqueNameGenerator shieldNameGenerator = new FirstAvailableUniqueNameGenerator();
		shieldNameGenerator.setStartIndex(1);

		String wireNamePrefix = project.getNameMgr().getObjectPrefix(INameMgr.WIRECONDUCTOR).getString();
		String shieldNamePrefix = project.getNameMgr().getObjectPrefix(INameMgr.SHIELDCONDUCTOR).getString();
		String multicoreNamePrefix = project.getNameMgr().getObjectPrefix(INameMgr.MULTICORE).getString();
		String overbraidNamePrefix = project.getNameMgr().getObjectPrefix(INameMgr.OVERBRAID).getString();

		Map<ISharedObject, ISharedObject> oldNewSharedObjectMap = new HashMap<ISharedObject, ISharedObject>();

		mIndent = 1;
		if (!mSharedMulticores.isEmpty()) {
			multicoresProcessed = true;
			outputStatusMessage("Message.StartingConversionSecondPass");
		}

		if(shouldExitSharedMulticoreConversion()) {
			return;
		}

		try (IProjectTraverserTransactionHandler transactionHandler = new ProjectTraverserCommandTransactionHandler(
				getCommandHelper(), this)) {
			logTransactionEntryMessageForSharedMulticores();
			boolean createdSharedObjs = false;
			for (Map.Entry<ISharedMulticore, SharedNetsConnectivity> sharedMulticoreConnectivity : mSharedMulticores
					.entrySet()) {
				ISharedMulticore sharedMulticore = sharedMulticoreConnectivity.getKey();
				SharedNetsConnectivity sharedNets = sharedMulticoreConnectivity.getValue();
				if (sharedNets.isValid()) {
					Map<IUID, IUID> oldToNewUIDMap = new HashMap<>();
					ISharedMulticore newSharedMulticore =
							CreateCloneOfSharedMulticore
									.clone(sharedMulticore, oldNewSharedObjectMap, oldToNewUIDMap,
											ISharedAbstractable::getDesignAbstraction);
					convertSharedMulticore(sharedObjectNameGenerator,
							sharedConductorMgr, newSharedMulticore,
							wireNamePrefix, wireNameGenerator,
							shieldNamePrefix, shieldNameGenerator,
							multicoreNamePrefix, multicoreNameGenerator,
							overbraidNamePrefix, overbraidNameGenerator);
					copyDomainInfo(sharedMulticore, newSharedMulticore);
					getCommandHelper()
							.postAuditTrailEvent(AuditableEventType.SHARED_OBJECT_ADDED, getProject().getUID(),
									newSharedMulticore, "");
					createdSharedObjs = true;
				}
			}
			transactionHandler.setCommitTransaction(createdSharedObjs);
		}
		finally {
			logTransactionExitMessageForSharedMulticores();
		}

		Map<IDesignContainer, Set<ISharedMulticore>> sharedMulticoreDesigns =
				getSortedDesignsWithSharedMCs(mDesignsWithSharedMulticores);
		for (Map.Entry<IDesignContainer, Set<ISharedMulticore>> designSharedMulticores : sharedMulticoreDesigns
				.entrySet()) {
			convertSharedMulticoresOnDesign(designSharedMulticores.getKey(), designSharedMulticores.getValue(),
					oldNewSharedObjectMap);
		}
		convertSharedMulticoresWithInvalidNets();
	}

	private void convertSharedMulticoresWithInvalidNets()
	{
		Map<IDesignContainer, Set<ISharedMulticore>> sharedMulticoreDesigns =
				getSortedDesignsWithSharedMCs(mDesignsWithSharedMulticores);
		Set<IMulticore> mSharedMultiCoreWithMultitermNets = new HashSet<IMulticore>();
		Map<ISharedMulticore, IDesign> sharedMCsToSkip = new HashMap<ISharedMulticore, IDesign>();
		for (Map.Entry<IDesignContainer, Set<ISharedMulticore>> sharedMulticores : sharedMulticoreDesigns
				.entrySet()) {
			mMultiCoreWithMultitermNets.clear();
			IDesignContainer design = sharedMulticores.getKey();
			Set<INetConductor> convertedNets = new HashSet<INetConductor>();
			Set<IConductor> schemConductorsToDelete = new HashSet<IConductor>();
			for (ISharedMulticore sharedMC : sharedMulticores.getValue()) {
				SharedNetsConnectivity sharedNetsConnectivity = mSharedMulticores.get(sharedMC);
				if (sharedNetsConnectivity == null) { //shared MCs with WIRES
					continue;
				}
				if (!sharedNetsConnectivity.isValid()) {
					if (mSharedMCWithHighwayRepresentations.contains(sharedMC)) {
						sharedMCsToSkip.put(sharedMC, (IDesign) design);
					}
					else {
						ILogicDesign logicDesign = (ILogicDesign) design;
						IMulticore rootMulticore = getCableMCOfGivenSharedMCInADesign(logicDesign, sharedMC);
						if (rootMulticore != null) {
							mSharedMultiCoreWithMultitermNets.add(rootMulticore);
							convertMulticoreWithMultiTermNets(convertedNets, schemConductorsToDelete, logicDesign,
									getExistingShields(rootMulticore), rootMulticore);
						}
					}
				}
			}
			List<IMulticore> toDel = new ArrayList<IMulticore>();
			for (IMulticore mc : mSharedMultiCoreWithMultitermNets) {
				toDel.addAll(mc.getAllMulticoresInHierarchy());
			}
			for (ISchemDiagram diagram : getSortedDiagrams(design)) {
				DeleteHelper.getInstance().delete(diagram, toDel, true);
			}
//			for (ISchemDiagram diagram : getSortedDiagrams(design)) {
//				DeleteHelper.getInstance().delete(diagram, mSharedMultiCoreWithMultitermNets, true);
//			}

			for (IConductor schemNetConductor : schemConductorsToDelete) {
				schemNetConductor.delete();
			}
			deleteRedundantConnectivity(design, convertedNets, null);
			mMultiCoreWithMultitermNets.clear();
		}
		for (ISharedMulticore sharedMCtoSkip : sharedMCsToSkip.keySet()) {
			outputStatusMessage("Message.MulticoreIsPartOfHighway", getMulticoreType(sharedMCtoSkip),
					link(sharedMCsToSkip.get(sharedMCtoSkip), sharedMCtoSkip, sharedMCtoSkip.getName()));
		}
	}

	private String getMulticoreType(Object multcore)
	{
		return CommonUtils.getClassDisplayNameFromResources(multcore.getClass());
	}

	private IMulticore getCableMCOfGivenSharedMCInADesign(ILogicDesign logicDesign, ISharedMulticore sharedMulticore)
	{
		IMulticore rootMulticore = null;
		for (IMulticore mc : logicDesign.getConnectivity().getMulticores()) {
			if (mc.getRootMulticore() == mc && mc.getSharedMulticore() != null &&
					mc.getSharedMulticore() == sharedMulticore) {
				rootMulticore = mc;
			}
		}
		return rootMulticore;
	}

	/**
	 * Converts Shared Multicores on a design, this involves creating one Connectivity Multicore per ISharedMulticore
	 * which points to the new Shared Multicore, then setting all the Schematic Objects to point to the new
	 * Connectivity
	 * <p>
	 *
	 * @param design IDesign to process
	 * @param sharedMulticoresInDesign ISharedMulticores which are instanced on design
	 * @param oldNewSharedObjectMap Map of original Shared Objects to new Shared Objects from convertSharedMulticores
	 *
	 * @throws UserSessionException Thrown if save fails
	 */
	private void convertSharedMulticoresOnDesign(@NotNull final IDesignContainer design,
			@NotNull final Set<ISharedMulticore> sharedMulticoresInDesign,
			@NotNull final Map<ISharedObject, ISharedObject> oldNewSharedObjectMap) throws UserSessionException
	{
		mCommittedTransaction = false;

		try (IProjectTraverserTransactionHandler transactionHandler = beginTransactionForDesign(design,
				getCommandHelper())) {

			beginProcessing(design, mLockResults.get(design));
			// Make sure the design is locked
			if (!design.lock()) {
				LogicActionMessageHelper.warnLocked(design);
				return;
			}

			Set<ISharedMulticore> sharedMulticoresToConvert = new HashSet<ISharedMulticore>();
			for (ISharedMulticore sharedMulticore : sharedMulticoresInDesign) {
				if (oldNewSharedObjectMap.containsKey(sharedMulticore)) {
					sharedMulticoresToConvert.add(sharedMulticore);
				}
			}

			Map<ILogicObject, ILogicObject> oldNewConnectivityObjectMap = new HashMap<ILogicObject, ILogicObject>();
			for (ISchemDiagram diagram : getSortedDiagrams(design)) {
				convertSharedMulticoresOnDiagram(diagram, sharedMulticoresToConvert, oldNewConnectivityObjectMap,
						oldNewSharedObjectMap);
				diagram.refreshRepresentations();
			}

			Set<IMulticore> convertedMulticores = CollectionUtils.getObjects(oldNewConnectivityObjectMap.keySet(),
					IMulticore.class);
			deleteRedundantConnectivity(design, null, convertedMulticores);

			completeProcessing(design, mLockResults.get(design), true, transactionHandler);
		}
	}

	/**
	 * Recursively renames and flushes sharedMulticore contents using the naming mechanisms provided by the parameters.
	 * <p>
	 *
	 * @param sharedObjectNameGenerator Generates names unique within a Design Abstraction
	 * @param sharedConductorMgr ISharedConductorMgr
	 * @param sharedMulticore ISharedMulticore to convert
	 * @param wireNamePrefix Wire name prefix from prefs
	 * @param wireNameGenerator Ensures Wire names are unique
	 * @param shieldNamePrefix Shield name prefix from prefs
	 * @param shieldNameGenerator Ensures Shield names are unique
	 * @param multicoreNamePrefix Multicore name prefix from prefs
	 * @param multicoreNameGenerator Ensures Multicore names are unique
	 * @param overbraidNamePrefix Overbraid name prefix from prefs
	 * @param overbraidNameGenerator Ensures Overbraid names are unique
	 */

	// TODO: does this work with composite names?
	private void convertSharedMulticore(@NotNull final ISharedObjectNameGenerator sharedObjectNameGenerator,
			@NotNull final ISharedConductorMgr sharedConductorMgr,
			@NotNull final ISharedMulticore sharedMulticore,
			@NotNull final String wireNamePrefix,
			@NotNull final FirstAvailableUniqueNameGenerator wireNameGenerator,
			@NotNull final String shieldNamePrefix,
			@NotNull final FirstAvailableUniqueNameGenerator shieldNameGenerator,
			@NotNull final String multicoreNamePrefix,
			@NotNull final FirstAvailableUniqueNameGenerator multicoreNameGenerator,
			@NotNull final String overbraidNamePrefix,
			@NotNull final FirstAvailableUniqueNameGenerator overbraidNameGenerator)
	{
		boolean isOverbraid = sharedMulticore instanceof ISharedOverbraid;
		String namePrefix = isOverbraid ? overbraidNamePrefix : multicoreNamePrefix;
		Set<String> conductorGroupNames = isOverbraid ? mOverbraidNames : mMulticoreNames;
		FirstAvailableUniqueNameGenerator conductorGroupNameGenerator =
				isOverbraid ? overbraidNameGenerator : multicoreNameGenerator;

		String multicoreBaseName = conductorGroupNameGenerator.generateName(namePrefix, conductorGroupNames);
		String multicoreName = sharedObjectNameGenerator
				.getUniqueMulticoreName(multicoreBaseName, sharedMulticore.getDesignAbstraction());
		conductorGroupNames.add(multicoreName);

		sharedMulticore.setGenerationType(SharedGenerationEnum.TypeGenerated);
		sharedMulticore.setName(multicoreName);
		// CreateCloneOfSharedMulticore revisions the object, not a plain clone
		sharedMulticore.setBaseId(sharedMulticore.getUID());
		if (sharedMulticore instanceof ISharedOverbraid) {
			sharedConductorMgr.addSharedOverbraid((ISharedOverbraid) sharedMulticore);
		}
		else {
			sharedConductorMgr.addSharedMulticore(sharedMulticore);
		}

		for (ISharedConductor sharedConductor : getSortedConductors(sharedMulticore)) {
			sharedConductor.setGenerationType(SharedGenerationEnum.TypeGenerated);
			// For both Wires and Nets we rename and add to sharedConductorMgr
			if (!sharedConductor.isShield()) {
				sharedConductor.setType(ISharedConductor.WIRE_TYPE);
				String wireBaseName = wireNameGenerator.generateName(wireNamePrefix, mWireNames);
				String name = sharedObjectNameGenerator
						.getUniqueConductorName(wireBaseName, sharedConductor.getDesignAbstraction());
				mWireNames.add(name);
				sharedConductor.setName(name);
				// CreateCloneOfSharedConductor revisions the object, not a plain clone
				sharedConductor.setBaseId(sharedConductor.getUID());
				sharedConductorMgr.addSharedConductor(sharedConductor);
				sharedConductor.flushNew(sharedConductorMgr.getObjType(), sharedConductorMgr);
			}
		}
		ISharedConductor sharedShield = sharedMulticore.getShield();
		if (sharedShield != null) {
			sharedShield.setGenerationType(SharedGenerationEnum.TypeGenerated);
			String shieldBaseName = shieldNameGenerator.generateName(shieldNamePrefix, mShieldNames);
			String name = sharedObjectNameGenerator
					.getUniqueConductorName(shieldBaseName, sharedShield.getDesignAbstraction());
			mShieldNames.add(name);
			sharedShield.setName(name);
			// CreateCloneOfSharedConductor revisions the object, not a plain clone
			sharedShield.setBaseId(sharedShield.getUID());
			sharedConductorMgr.addSharedConductor(sharedShield);
			sharedShield.flushNew(sharedConductorMgr.getObjType(), sharedConductorMgr);
		}
		for (ISharedMulticore innerSharedMulticore : getSortedInnerMulticores(sharedMulticore)) {
			// Recursive call for nested multicores
			convertSharedMulticore(sharedObjectNameGenerator, sharedConductorMgr, innerSharedMulticore, wireNamePrefix,
					wireNameGenerator, shieldNamePrefix, shieldNameGenerator,
					multicoreNamePrefix, multicoreNameGenerator,
					overbraidNamePrefix, overbraidNameGenerator);
		}
		sharedMulticore.flushNew(sharedConductorMgr.getObjType(), sharedConductorMgr);
	}

	private ISharedConductor createSharedWire(ISharedConductor sharedConductor,
			@NotNull chs.cof.logical.cable.IConductor wireConductor, IDesignContainer design)
	{
		//dts0100507370 - Check if we have already created a shared wire replacement for that shared net object
		ISharedConductor sharedWire = mSharedNetToWireMap.get(sharedConductor);
		if (sharedWire != null) {
			prepareToShare(wireConductor, sharedWire);
			return sharedWire;
		}

//		sharedWire =
//				FactoryMgr.getSharedFactory().createSharedConductor(FactoryMgr.getCommonFactory().createUID());
//        IDesign logicalDesign = wireConductor.getDesign();

		//Use the helper class to share the conductor, instead of creating a new shared object manually
		//Because that will preserve the properties (maybe other things) auto-assigned to the original conductor
		sharedWire = SharedConductorHelper
				.shareConductor(wireConductor, null, (IDesign) design/*, sharedObjectNameGenerator*/);
		mSharedNetToWireMap.put(sharedConductor, sharedWire);
		//dts0100507370 - Copy attributes/properties first before setting any wire-specific values
		copyAttributesAndProperties(sharedConductor, sharedWire);

		sharedWire.setType(ISharedConductor.WIRE_TYPE);
		sharedWire.setGenerationType(SharedGenerationEnum.TypeGenerated);
		//String wireBaseName = wireNameGenerator.generateName(wireNamePrefix, mWireNames);

		//String name = sharedObjectNameGenerator
		//		.getUniqueConductorName(wireBaseName, sharedWire.getDesignAbstraction());
		mWireNames.add(sharedWire.getName());
		//sharedWire.setName(name);
		//sharedConductorMgr.addSharedConductor(sharedWire);

		//sharedWire.flushNew(sharedConductorMgr.getObjType(), sharedConductorMgr);
		getCommandHelper().postAuditTrailEvent(AuditableEventType.SHARED_OBJECT_ADDED, getProject().getUID(),
				sharedWire, "");

		return sharedWire;
	}

	/**
	 * Deletes Connectivity Objects which are redundant after converting Nets to Wires
	 * <p>
	 *
	 * @param design IDesign to process
	 * @param convertedNets INetConductors that were converted to IWireConductor
	 * @param convertedMulticores IMulticores longer referenced by any Schematic Objects on any diagram in design
	 */
	private void deleteRedundantConnectivity(@NotNull final IDesignContainer design,
			@Nullable final Set<INetConductor> convertedNets,
			@Nullable final Set<IMulticore> convertedMulticores)
	{
		// We have to defer object deletion until the end because otherwise the diagram representation cache gets
		// invalidated and this is a large performance bottleneck
		IConnectivity connectivity = getConnectivity(design);
		assert connectivity != null;

		// Nets
		INetConductorIterator netsIter = connectivity.getNetConductors();
		List<INetConductor> netsToDelete = new ArrayList<INetConductor>(netsIter.getSize());

		List<ISchemDiagram> diagrams = design.getDiagramsOfType(ISchemDiagram.class);
		for (INetConductor netConductor : netsIter) {
			if (isConvertedNet(netConductor, convertedNets, convertedMulticores)) {
				int repCount = 0;
				for (ISchemDiagram diagram : diagrams) {
					repCount += diagram.getRepresentations(netConductor.getUID()).getSize();
				}
				if (repCount == 0) {
					netsToDelete.add(netConductor);
				}
			}
		}
		List<IWireConductor> wiresToDelete = null;
		List<IShieldConductor> shieldsToDelete = null;
		List<IMulticore> multicoresToDelete = null;
		if (convertedMulticores != null) {
			// Wires - will only be made redundent when converting Shared Multicores
			IWireConductorIterator wiresIter = connectivity.getWireConductors();
			wiresToDelete = new ArrayList<IWireConductor>(wiresIter.getSize());
			for (IWireConductor wireConductor : wiresIter) {
				if (convertedMulticores.contains(wireConductor.getMulticore())) {
					int repCount = 0;
					for (ISchemDiagram diagram : diagrams) {
						repCount += diagram.getRepresentations(wireConductor.getUID()).getSize();
					}
					if (repCount == 0) {
						wiresToDelete.add(wireConductor);
					}
				}
			}

			IShieldConductorIterator shieldsIter = connectivity.getShieldConductors();
			shieldsToDelete = new ArrayList<IShieldConductor>(shieldsIter.getSize());
			for (IShieldConductor shieldConductor : shieldsIter) {
				if (convertedMulticores.contains(shieldConductor.getMulticore())) {
					int repCount = 0;
					for (ISchemDiagram diagram : diagrams) {
						repCount += diagram.getRepresentations(shieldConductor.getUID()).getSize();
					}
					if (repCount == 0) {
						shieldsToDelete.add(shieldConductor);
					}
				}
			}
			IMulticoreIterator multicoresIter = connectivity.getMulticores(false);
			multicoresToDelete = new ArrayList<IMulticore>(multicoresIter.getSize());
			for (IMulticore multicore : multicoresIter) {
				if (convertedMulticores.contains(multicore.getParent())) {
					chs.cof.logical.cable.IShieldBody shieldBody = multicore.getShieldBody();
					if (shieldBody != null) {
						int repCount = 0;
						for (ISchemDiagram diagram : diagrams) {
							repCount += diagram.getRepresentations(shieldBody.getUID()).getSize();
						}
						if (repCount == 0) {
							multicoresToDelete.add(multicore);
						}
					}
				}
			}
			IMulticoreIterator topMulticoresIter = connectivity.getMulticores(true);
			for (IMulticore multicore : topMulticoresIter) {
				if (convertedMulticores.contains(multicore)) {
					chs.cof.logical.cable.IShieldBody shieldBody = multicore.getShieldBody();
					if (shieldBody != null) {
						int repCount = 0;
						for (ISchemDiagram diagram : diagrams) {
							repCount += diagram.getRepresentations(shieldBody.getUID()).getSize();
						}
						if (repCount == 0) {
							multicoresToDelete.add(multicore);
						}
					}
				}
			}
		}
		for (INetConductor netToDelete : netsToDelete) {
			CreationDeletionHelper.getTheCreationHelper().addDeletionObject(netToDelete);
		}
		// Designs PortMgr contains UID references to both connectivity and schematic conductors and is not
		// updated automatically - hence have to call removeFromPortMgr method to remove them
		if (wiresToDelete != null) {
			for (IWireConductor wireToDelete : wiresToDelete) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(wireToDelete);
			}
		}
		if (shieldsToDelete != null) {
			for (IShieldConductor shieldToDelete : shieldsToDelete) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(shieldToDelete);
			}
		}
		if (multicoresToDelete != null) {
			for (IMulticore multicoreToDelete : multicoresToDelete) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(multicoreToDelete);
			}
		}
	}

	/**
	 * Checks a net to see if it's in convertedNets or convertedMulticores
	 * <p>
	 *
	 * @param netConductor INetCondcutor
	 * @param convertedNets Set of INetConductor
	 * @param convertedMulticores Set of IMulticore
	 *
	 * @return boolean
	 */
	private boolean isConvertedNet(@NotNull final INetConductor netConductor,
			@Nullable final Set<INetConductor> convertedNets,
			@Nullable final Set<IMulticore> convertedMulticores)
	{
		if (convertedMulticores != null) {
			IMulticore rootMC = netConductor.getMulticore();
			if (rootMC != null) {
				for (IMulticore mc : convertedMulticores) {
					if (mc.getUID().equals(rootMC.getUID())) {
						return true;
					}
				}
			}
			return false;
		}
		assert convertedNets != null;
		for (INetConductor net : convertedNets) {
			if (net.getUID().equals(netConductor.getUID())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Converts nets in shared multicores into wires
	 *
	 * @param diagram IDiagram to convert
	 * @param sharedMulticoresToConvert Shared Multicores to convert
	 * @param oldNewConnectivityObjectMap Map of Connectivity Objects to new Connectivity Objects - multicore contents
	 * @param oldNewSharedObjectMap Map of Shared Objects to new Shared Objects - multicore contents
	 */
	private void convertSharedMulticoresOnDiagram(@NotNull final ISchemDiagram diagram,
			@NotNull final Set<ISharedMulticore> sharedMulticoresToConvert,
			@NotNull final Map<ILogicObject, ILogicObject> oldNewConnectivityObjectMap,
			@NotNull final Map<ISharedObject, ISharedObject> oldNewSharedObjectMap)
	{
		Set<IConductor> schemConductorsToConvert = new HashSet<IConductor>();
		for (IConductor schemConductor : getSortedSchemConductors(diagram)) {
			chs.cof.logical.cable.IConductor conductor =
					CommonUtils.cast(schemConductor.getConnectivity(), chs.cof.logical.cable.IConductor.class);
			// Create new connectivity multicore if not already created
			if (createNewConnectivityMulticore(diagram, sharedMulticoresToConvert, oldNewConnectivityObjectMap,
					oldNewSharedObjectMap, conductor)) {
				schemConductorsToConvert.add(schemConductor);
			}
		}

		IGrid grid = diagram.getGrid();
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);

		// Converts schem nets to wires
		Set<chs.cof.logical.cable.IConductor> processedHighwayConds =
				processSchemMulticoreNets(diagram, oldNewConnectivityObjectMap, schemConductorsToConvert);

		// Converts nets of multicores in highways
		processMulticoreNetsInHighway(diagram, sharedMulticoresToConvert, oldNewConnectivityObjectMap,
				oldNewSharedObjectMap, processedHighwayConds);

		IUIDObjectCollection<IShieldBody> schemShieldBodies = diagram.getShieldBodies();
		for (IShieldBody schemShieldBody : schemShieldBodies) {
			chs.cof.logical.cable.IShieldBody connectivityShieldBody = schemShieldBody.getConnectivity();
			if (sharedMulticoresToConvert
					.contains(connectivityShieldBody.getMulticore().getRootMulticore().getSharedMulticore())) {
				chs.cof.logical.cable.IShieldBody newConnectivity = CommonUtils.cast(
						oldNewConnectivityObjectMap.get(connectivityShieldBody),
						chs.cof.logical.cable.IShieldBody.class);
				if (newConnectivity != null) {
					updateSchemShieldBody(schemShieldBody, newConnectivity, styleSet);
				}
			}
		}
	}

	/**
	 * @param diagram Diagram
	 * @param sharedMulticoresToConvert Shared Multicores to convert
	 * @param oldNewConnectivityObjectMap Map of Connectivity Objects to new Connectivity Objects - multicore contents
	 * @param oldNewSharedObjectMap Map of Shared Objects to new Shared Objects - multicore contents
	 * @param condsToSkip Conductors to be skipped due these are already processed
	 */
	private void processMulticoreNetsInHighway(ISchemDiagram diagram, Set<ISharedMulticore> sharedMulticoresToConvert,
			Map<ILogicObject, ILogicObject> oldNewConnectivityObjectMap,
			Map<ISharedObject, ISharedObject> oldNewSharedObjectMap, Set<chs.cof.logical.cable.IConductor> condsToSkip)
	{
		for (IHighwaySchematic schemHighway : getSortedSchemhighways(diagram)) {
			IGeneralHighway cableHighway = HighwayHelper.toGeneralHighway(schemHighway);
			if (cableHighway != null) {
				for (chs.cof.logical.cable.IConductor cableConductor : schemHighway.getStackedPinConductors()) {
					if (createNewConnectivityMulticore(diagram, sharedMulticoresToConvert, oldNewConnectivityObjectMap,
							oldNewSharedObjectMap, cableConductor)) {
						chs.cof.logical.cable.IConductor newConnectivity = CommonUtils.cast(
								oldNewConnectivityObjectMap.get(cableConductor),
								chs.cof.logical.cable.IConductor.class);
						if (newConnectivity != null && !condsToSkip.contains(cableConductor)) {
							cableHighway.removeStackPinConductor((IHighwayConductor) cableConductor);
							cableHighway.addStackPinConductor((IHighwayConductor) newConnectivity);
						}
					}
				}
			}
		}
	}

	/**
	 * @param diagram Diagram
	 * @param oldNewConnectivityObjectMap Map of Connectivity Objects to new Connectivity Objects - multicore contents
	 * @param schemConductorsToConvert Conductors to be converted
	 *
	 * @return Net conductors converted to wires
	 */
	private Set<chs.cof.logical.cable.IConductor> processSchemMulticoreNets(ISchemDiagram diagram,
			Map<ILogicObject, ILogicObject> oldNewConnectivityObjectMap, Set<IConductor> schemConductorsToConvert)
	{
		IGrid grid = diagram.getGrid();
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
		Set<chs.cof.logical.cable.IConductor> processedHighwayConds = new HashSet<chs.cof.logical.cable.IConductor>();
		for (IConductor schemConductor : schemConductorsToConvert) {
			chs.cof.logical.cable.IConductor cableConductor = schemConductor.getConnectivity();
			chs.cof.logical.cable.IConductor newConnectivity = CommonUtils.cast(
					oldNewConnectivityObjectMap.get(cableConductor), chs.cof.logical.cable.IConductor.class);
			if (newConnectivity != null) {
				processedHighwayConds.add(cableConductor);
				if (cableConductor instanceof IHighwayConductor) {
					Set<IGeneralHighway> highways = ((IHighwayConductor) cableConductor).getHighways();
					for (IGeneralHighway highway : highways) {
						highway.addConductor((IHighwayConductor) newConnectivity);
					}

					IDiagramObjectIterator diagIterator = diagram.getRepresentations(cableConductor.getUID());
					while (diagIterator.hasNext()) {
						IDiagramObject diagramObject = diagIterator.getNext();
						IGeneralHighway highway = HighwayHelper.toGeneralHighway(diagramObject);
						if (highway != null) {
							highway.removeStackPinConductor((IHighwayConductor) cableConductor);
							highway.addStackPinConductor((IHighwayConductor) newConnectivity);
						}
					}
				}
				// diagram.removeObject and diagram.addObject ensure that the schemConductor is on the correct layer
				// for it's type for the schematic conductors that were changed from a net to a wire
				diagram.removeObject(schemConductor);
				updateSchemConductor(schemConductor, newConnectivity);
				diagram.addObject(schemConductor);
				if (styleSet != null) {
					schemConductor.applyStyle();
				}
			}
		}
		return processedHighwayConds;
	}

	/**
	 * Creates new shared multicore if needed
	 *
	 * @param diagram Diagram
	 * @param sharedMulticoresToConvert Shared Multicores to convert
	 * @param oldNewConnectivityObjectMap Map of Connectivity Objects to new Connectivity Objects - multicore contents
	 * @param oldNewSharedObjectMap Map of Shared Objects to new Shared Objects - multicore contents
	 * @param conductor conductor whose multicore to to be processed
	 *
	 * @return true if shared conductor can be converted to wire
	 */
	private boolean createNewConnectivityMulticore(ISchemDiagram diagram,
			Set<ISharedMulticore> sharedMulticoresToConvert,
			Map<ILogicObject, ILogicObject> oldNewConnectivityObjectMap,
			Map<ISharedObject, ISharedObject> oldNewSharedObjectMap, chs.cof.logical.cable.IConductor conductor)
	{
		IDesignContainer design = diagram.getDesignContainer();
		if (conductor != null) {
			IMulticore rootMulticore = conductor.getRootMulticore();
			if (rootMulticore != null && sharedMulticoresToConvert.contains(rootMulticore.getSharedMulticore())) {
				if (oldNewConnectivityObjectMap.get(rootMulticore) == null &&
						isMulticoreCorrectlyShared(rootMulticore, oldNewSharedObjectMap)) {
					IMulticore newRootMulticore = createConnectivityMulticore(design, null, rootMulticore,
							oldNewConnectivityObjectMap, oldNewSharedObjectMap);
					mObjectUIDsForConstraints.add(newRootMulticore.getUID());
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Clones an existing IMulticore - populates oldNewConnectivityObjectMap
	 * <p>
	 *
	 * @param design IDesign
	 * @param parentMulticore IMulticore parent of multicore param - may be null
	 * @param multicore IMulticore Original (source) IMulticore to clone
	 * @param oldNewConnectivityObjectMap Map of Connectivity Objects to new Connectivity Objects - multicore contents
	 * @param oldNewSharedObjectMap Map of Shared Objects to new Shared Objects - multicore contents
	 *
	 * @return IMulticore The new root multicore
	 */
	@NotNull private IMulticore createConnectivityMulticore(@NotNull final IDesignContainer design,
			@Nullable final IMulticore parentMulticore,
			@NotNull final IMulticore multicore,
			@NotNull final Map<ILogicObject, ILogicObject> oldNewConnectivityObjectMap,
			@NotNull final Map<ISharedObject, ISharedObject> oldNewSharedObjectMap)
	{
		IConnectivity connectivity = getConnectivity(design);
		assert connectivity != null;
		IMulticore newMulticore = mReplicator
				.replicateMulticoreOrOverbraid(multicore, false, true, CommonUtils.cast(design, IDesign.class));
		updateAssemblyDetails(multicore, newMulticore, mReplicator);
		addGeneratedMulticoreToResults(multicore, newMulticore, mReplicator);
		final ISharedMulticore newSharedMulticore =
				CommonUtils.cast(oldNewSharedObjectMap.get(multicore.getSharedObject()), ISharedMulticore.class);
		prepareToShare(newMulticore, newSharedMulticore);
		newMulticore.setSharedMulticore(newSharedMulticore);
		if (parentMulticore != null) {
			parentMulticore.addMulticore(newMulticore);
		}
		oldNewConnectivityObjectMap.put(multicore, newMulticore);
		outputStatusMessage(getResourceName("Message.ConvertedShared", multicore), indent(), multicore.getName(),
				link(design, newMulticore, newMulticore.getName()));

		mIndent++;
		for (chs.cof.logical.cable.IConductor conductor : getSortedConductors(multicore)) {
			if (conductor instanceof INetConductor || conductor instanceof IWireConductor) {
				IWireConductor wireConductor =
						FactoryMgr.getCablePropertiedFactory()
								.createWireConductor(FactoryMgr.getCommonFactory().createUID());
				final ISharedConductor newSharedConductor = CommonUtils.cast(
						oldNewSharedObjectMap.get(conductor.getSharedObject()), ISharedConductor.class);
				//we need to copy properties so that auto-assign wire props will come to the shared wire.
				copyAttributesAndProperties(wireConductor, newSharedConductor);
				prepareToShare(wireConductor, newSharedConductor);
				wireConductor.setSharedConductor(newSharedConductor);
				connectivity.addWireConductor(wireConductor);
				newMulticore.addConductor(wireConductor);
				copyConductorPinConnections(conductor, wireConductor);
				ConvertNetsToWiresCmdReplicator.setupTopoSignal(conductor, wireConductor);
				oldNewConnectivityObjectMap.put(conductor, wireConductor);
				Set<IAbstractPin> terms = conductor.getPinSet();
				m_styledGraphicsHandler.recordTerminationsForStyling(terms);
				INetsToWiresResultCollector outputResults = getOutputResultCollector();
				if (outputResults != null) {
					outputResults.addInfoUsingConnectivity(conductor.getName(), design.getName(), terms,
							Collections.singleton(wireConductor), Collections.<ISplice>emptySet());
				}
				outputStatusMessage("Message.ConvertedSharedConductor", indent(),
								conductor.getName(), link(design, wireConductor, wireConductor.getName()));
			}
		}
		IShieldConductor shieldConductor = multicore.getShield();
		if (shieldConductor != null) {
			IShieldConductor newShieldConductor = newMulticore.getShield();
			final ISharedConductor newSharedConductor = CommonUtils.cast(
					oldNewSharedObjectMap.get(shieldConductor.getSharedObject()), ISharedConductor.class);
			prepareToShare(newShieldConductor, newSharedConductor);
			newShieldConductor.setSharedConductor(newSharedConductor);
			copyConductorPinConnections(shieldConductor, newShieldConductor);
			ConvertNetsToWiresCmdReplicator.setupTopoSignal(shieldConductor, newShieldConductor);
			oldNewConnectivityObjectMap.put(shieldConductor, newShieldConductor);
			outputStatusMessage("Message.ConvertedSharedShield", indent(), shieldConductor.getName(),
					link(design, newShieldConductor, newShieldConductor.getName()));
		}
		mIndent--;

		chs.cof.logical.cable.IShieldBody shieldBody = multicore.getShieldBody();
		if (shieldBody != null) {
			chs.cof.logical.cable.IShieldBody newShieldBody = newMulticore.getShieldBody();
			oldNewConnectivityObjectMap.put(shieldBody, newShieldBody);
		}
		for (IMulticore innerMulticore : getSortedInnerMulticores(multicore)) {
			mIndent++;
			createConnectivityMulticore(design, newMulticore, innerMulticore, oldNewConnectivityObjectMap,
					oldNewSharedObjectMap);
			mIndent--;
		}
		return newMulticore;
	}

	private void prepareToShare(ILogicObject newLogicObject, ISharedObject newSharedObject)
	{
		// make sure we have the same properties, as we need to remove the properties from the connectivity obj
		if (newSharedObject != null) {
			newLogicObject.removeAllProperties();        // shared object defines the properties
		}
	}

	/**
	 * Connects targetConductor to the same IAbstractPin as sourceConductor </p>
	 *
	 * @param sourceConductor Pin connections source
	 * @param targetConductor Pin connections target
	 */
	private void copyConductorPinConnections(@NotNull final chs.cof.logical.cable.IConductor sourceConductor,
			@NotNull final chs.cof.logical.cable.IConductor targetConductor)
	{
		Set<IAbstractPin> sourcePins = sourceConductor.getPinSet();
		for (IAbstractPin sourcePin : sourcePins) {
			targetConductor.addPin(sourcePin);
		}
	}

	private void prepareMulticoreInnerCoresForNetsToWires(@NotNull final IDesignContainer design)
	{
		mConnectivitySchemMapForUseInMC.clear();
		IConnectivity connectivity = getConnectivity(design);
		assert connectivity != null;
		INetConductorIterator netConductorIterator = connectivity.getNetConductors();

		netConductorIterator.forEach(aNet -> {

			mConnectivitySchemMapForUseInMC.put(aNet, new ArrayList<>(getDWSchemConductors(design, aNet)));
		});
	}

	/**
	 * The 'first pass' conversion where Shared and Non-Shared Nets are converted and also Non-Shared Multicores.
	 * Connectivity of Shared Multicores is incrementally built up for use in the 'second pass'. Nets within the
	 * highways (nets represented by the highways) are also convereted to wires. Some nets within the highway which
	 * cannot be converted to wies without creating shared splic will not be converted to wires for now
	 * <p>
	 *
	 * @param design IDesign to process
	 * @param diagram IDiagram to process
	 * @param convertedNets Set<INetConductor> to be populated with each net that is converted
	 * @param schemConductorsToDelete Set<INetConductor> to be populated with each schem net that is converted
	 * @param processedMulticores
	 */
	private void convertNetsToWires(@NotNull final IDesignContainer design, @NotNull final ISchemDiagram diagram,
			@NotNull final Set<INetConductor> convertedNets, @NotNull final Set<IConductor> schemConductorsToDelete,
			Set<IMulticore> processedMulticores)
	{
		//
		// Make our own cache of INetConductor to schem.IConductors - because we generate the new schematics
		// incrementally this invalidates the internal diagram map which reduces performance
		//
		IConnectivity connectivity = getConnectivity(design);
		assert connectivity != null;
		INetConductorIterator netConductorIterator = connectivity.getNetConductors();
		while (netConductorIterator.hasNext()) {
			INetConductor netConductor = netConductorIterator.next();

			List<IHighwaySchematic> schemHighways =
					CollectionUtils
							.getObjectList(diagram.getRepresentations(netConductor.getUID()), IHighwaySchematic.class);
			m_netCondToHighwayMap.put(netConductor, schemHighways);
		}

		//
		// First - convert Nets which are NOT in Multicores
		//
		SetMap<IMulticore, IConductor> multicoreSchemNets = new SetMap<IMulticore, IConductor>();
		Set<IHighwayConductor> stackedpinCondsToSkip = new HashSet<IHighwayConductor>();

		processConductors(diagram, convertedNets, schemConductorsToDelete, multicoreSchemNets, stackedpinCondsToSkip);

		//
		// Second - convert Non-Shared Multicores
		//
		processNonSharedMulticores(diagram, multicoreSchemNets, convertedNets, schemConductorsToDelete,
				processedMulticores);

		processHighwayConductors(diagram, convertedNets, stackedpinCondsToSkip, processedMulticores);

		reportSkippedStackedConductors(design, stackedpinCondsToSkip);
	}

	private void reportSkippedStackedConductors(IDesignContainer design, Set<IHighwayConductor> stackedpinCondsToSkip)
	{
		for (IHighwayConductor cond : stackedpinCondsToSkip) {
			outputStatusMessage("Message.InvalidHighway", link(design, cond, cond.getName()));
		}
	}

	/**
	 * Converts nets within non-shared multicores into wires.
	 *
	 * @param diagram Diagram
	 * @param multicoreSchemNets Nets in multicores to be converted to wires
	 * @param convertedNets Set<INetConductor> to be populated with each net that is converted
	 * @param schemConductorsToDelete Set<INetConductor> to be populated with each schem net that is converted
	 *
	 * @return Multicores whose nots are not converted to wires
	 */

	private Set<IMulticore> processNonSharedMulticores(ISchemDiagram diagram,
			SetMap<IMulticore, IConductor> multicoreSchemNets, Set<INetConductor> convertedNets,
			Set<IConductor> schemConductorsToDelete,
			Set<IMulticore> processedMulticores)
	{
		IDesignContainer design = diagram.getDesign();
		SortedMap<IMulticore, Set<IConductor>> multicoreSchemNetsSorted =
				getMulticoreSchemNetsSorted(multicoreSchemNets);

		// Process each multicore
		List<IShieldConductor> existingShields = new ArrayList<IShieldConductor>();
		for (Map.Entry<IMulticore, Set<IConductor>> multicore : multicoreSchemNetsSorted.entrySet()) {
			existingShields.addAll(getExistingShields(multicore.getKey()));
		}

		for (Map.Entry<IMulticore, Set<IConductor>> multicore : multicoreSchemNetsSorted.entrySet()) {
			IMulticore rootMulticore = multicore.getKey();
			if (processedMulticores.contains(rootMulticore)) {
				continue;
			}
			processedMulticores.add(rootMulticore);
			if (!isValidToConvertMulticore(design, rootMulticore)) {
				continue;
			}
			if (doesMCHasMultiTermOrMultiEdgeNet(rootMulticore)) {
				//Added this piece of code to convert muti-edge MC Nets to wires.
				if (doesMCNetHasHighwayRepresentation(design, rootMulticore)) {
					outputStatusMessage("Message.MulticoreIsPartOfHighway", getMulticoreType(rootMulticore),
							link(design, rootMulticore, rootMulticore.getName()));
				}
				else {
					convertMulticoreWithMultiTermNets(convertedNets, schemConductorsToDelete, design,
							existingShields,
							rootMulticore);
				}
			}
			else {
				Map<IConductor, SchemGraphUtils.SchemGraph> schemGraphs =
						new HashMap<IConductor, SchemGraphUtils.SchemGraph>();

				Set<IConductor> allSchemConductors = new HashSet<>(multicore.getValue());
				for (IConductor schemNetConductor : multicore.getValue()) {
					for (IConductor aConductor : getDWSchemConductors(design, schemNetConductor.getConnectivity())) {
						if (!allSchemConductors.contains(aConductor)) {
							allSchemConductors.add(aConductor);
						}
					}
				}

				for (IConductor schemNetConductor : allSchemConductors) {

					SchemGraphUtils.SchemGraph schemGraph = new SchemGraphUtils.SchemGraph(schemNetConductor);
					schemGraphs.put(schemNetConductor, schemGraph);
				}

				convertNonSharedMulticore(design, diagram, rootMulticore, schemGraphs, convertedNets,
						schemConductorsToDelete);
			}
		}

		return processedMulticores;
	}

	private boolean doesMCNetHasHighwayRepresentation(IDesignContainer design, IMulticore rootMulticore)
	{
		boolean bHighwayReps = false;
		for (chs.cof.logical.cable.IConductor connectivityConductor : getSortedAllConductorsInHierarchy(
				rootMulticore)) {
			INetConductor netConductor = CommonUtils.cast(connectivityConductor, INetConductor.class);
			if (netConductor != null) {
				Set<IHighwaySchematic> representedHighways = getDWSchemHighways(design, netConductor);
				if (!representedHighways.isEmpty()) {
					bHighwayReps = true;
				}
			}
		}
		return bHighwayReps;
	}

	private void convertMulticoreWithMultiTermNets(Set<INetConductor> convertedNets,
			Set<IConductor> schemConductorsToDelete, IDesignContainer design, List<IShieldConductor> existingShields,
			IMulticore rootMulticore)
	{
		Map<IConductor, IWireConductor> newWireVsOldSchem = new HashMap<IConductor, IWireConductor>();
		List<SchemGraphUtils.SchemGraph> graphs = new ArrayList<SchemGraphUtils.SchemGraph>();

		//create wires out of nets
		processConductorsOfMulticore(convertedNets, design, rootMulticore, newWireVsOldSchem, graphs);

		//get grouping information
		List<List<ISegment>> groupInfo = PlacementForNetsToWires.getSegmentGrouping(rootMulticore);
		Map<ISegment, ShieldTerminationUtils.ShieldPinInfo> segVsshieldConns =
				ShieldTerminationUtils.getShieldConnections(rootMulticore);

		//create wire schematics
		createSchematicConductors(schemConductorsToDelete, design, rootMulticore, newWireVsOldSchem, graphs);

		//create multicores
		List<IMulticore> newMCs = createMulticores(design, rootMulticore, groupInfo);

		//distribute shield connections
		Map<IShieldConductor, Collection<IPin>> map =
				ShieldTerminationUtils.addShieldConnections(segVsshieldConns, design, this, getOutputResultCollector());

		//populate indicators
		for (ISchemDiagram diag : getSortedDiagrams(design)) {
			for (IMulticore newMC : newMCs) {
				Placement.populateIndicators(diag, newMC);
				ShieldTerminationUtils.drawSchemShields(newMC, diag, map, existingShields);
			}
		}
	}

	private List<IMulticore> createMulticores(IDesignContainer design, IMulticore rootMulticore,
			List<List<ISegment>> groupInfo)
	{
		ListMap<IUID, IUID> netConductortoWireConductorsMap =
				mNetConductortoWireConductorsMap.get(rootMulticore.getUID());
		List<IMulticore> newMCs = cloneMulticore(rootMulticore, netConductortoWireConductorsMap, groupInfo);

		OutputConvertedMulticores(design, rootMulticore, newMCs);
		return newMCs;
	}

	/**
	 * This is to create schematic conductors from old net schem conductor. Point these new schem cond to point to newly
	 * created wire conductors
	 *
	 * @param schemConductorsToDelete - populate the net schematic Conductors to delete
	 * @param design - the current design
	 * @param rootMulticore - root multicore that is being processed
	 * @param newWireVsOldSchem - new cable wire vs old schem  - to create new schem for new cable
	 * @param graphs - the graphs for which schem cond has to be created
	 */
	private void createSchematicConductors(Set<IConductor> schemConductorsToDelete, IDesignContainer design,
			IMulticore rootMulticore, Map<IConductor, IWireConductor> newWireVsOldSchem,
			List<SchemGraphUtils.SchemGraph> graphs)
	{
		outputStatusMessage(getResourceName("Message.Converting", rootMulticore), indent(),
				link(design, rootMulticore, rootMulticore.getName()));
		mIndent++;

		//create wire schematics
		createSchematicConductor(design, schemConductorsToDelete, graphs);
		for (Map.Entry<IConductor, IWireConductor> entry : newWireVsOldSchem.entrySet()) {
			IConductor schemConductor = entry.getKey();
			if (!schemConductor.getSegments().isEmpty()) {
				IWireConductor wireConductor = entry.getValue();
				ISchemDiagram diagram = DiagramHelper.getDiagram(schemConductor);
				IGrid grid = diagram.getGrid();
				IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);
				createSchemWireConductor(design, wireConductor, diagram, grid, styleSet, schemConductor,
						schemConductorsToDelete);
			}
		}
	}

	/**
	 * This function gets all conductors & process them to convert into wires
	 *
	 * @param convertedNets -   populate this list as and when a net is converted
	 * @param design - current design
	 * @param rootMulticore - the multicore that is being processed
	 * @param newWireVsOldSchem - process this map of new cable conductor vs old schem (will be used further down the
	 * line to create schem for new wire)
	 * @param graphs - populate this graphs list as a multi-edge net is converted. will be used later to create schem
	 * wire
	 */
	private void processConductorsOfMulticore(Set<INetConductor> convertedNets,
			IDesignContainer design, IMulticore rootMulticore, Map<IConductor, IWireConductor> newWireVsOldSchem,
			List<SchemGraphUtils.SchemGraph> graphs)
	{
		for (chs.cof.logical.cable.IConductor connectivityConductor : getSortedAllConductorsInHierarchy(
				rootMulticore)) {
			INetConductor netConductor = CommonUtils.cast(connectivityConductor, INetConductor.class);
			if (netConductor != null) {
				Set<IConductor> representedConductors = getDWSchemConductors(design, netConductor);
				if (netConductor.getSharedConductor() == null && isValidNetConductorOfMC(representedConductors)) {
					for (IConductor schemNetConductor : representedConductors) {
						IWireConductor wireConductor = convertPortedSchemNetToSingleWire(design, schemNetConductor,
								false);
						if (wireConductor.getMulticore() != null) {
							IMulticore mc = wireConductor.getMulticore();
							mc.removeConductor(wireConductor);
						}
						newWireVsOldSchem.put(schemNetConductor, wireConductor);
					}
				}
				else {
					graphs.addAll(convertSingleCableNetToWires(design, representedConductors, false));
					mMultiCoreWithMultitermNets.add(rootMulticore.getUID());
				}
				convertedNets.add(netConductor);
			}
		}
	}

	private void OutputConvertedMulticores(IDesignContainer design, IMulticore rootMulticore, List<IMulticore> newMCs)
	{
		StringBuilder sb = new StringBuilder();
		List<IMulticore> sortedMCs = new ArrayList<IMulticore>(newMCs);
		Collections.sort(sortedMCs, new NamedObjectComparator<IMulticore>()
		{
			protected String getString(IMulticore object)
			{
				return object.getName();
			}
		});
		boolean first = true;
		for (IMulticore newMC : sortedMCs) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			sb.append(link(design, newMC, newMC.getName()));
		}
		outputStatusMessage("Message.ConvertedToWires", indent(), rootMulticore.getName(), sb.toString());
	}

	private List<IShieldConductor> getExistingShields(IMulticore mc)
	{
		List<IShieldConductor> existingShields = new ArrayList<IShieldConductor>();
		existingShields.add(mc.getShield());
		for (IMulticore childMC : mc.getMulticoresAsList()) {
			existingShields.addAll(getExistingShields(childMC));
		}
		return existingShields;
	}

	private boolean isValidNetConductorOfMC(Set<IConductor> representedConductors)
	{
		for (IConductor schemNetConductor : representedConductors) {
			if (!updatePortedNetConnectivity(schemNetConductor, (INetConductor) schemNetConductor.getConnectivity())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Processes nets in the given diagram to convert to wires. Not all nets will be converted to wires here. Nets which
	 * are not converted to wires are registered to process in second pass. Nets in multicores will not be converted
	 * here, but populared in the map multicoreSchemNets. Some nets which cannot be converted to wies without creating
	 * shared splic and are connected to pin stack through highway will not be converted to wires for now. These nets
	 * will be populated in list stackedpinCondsToSkip
	 *
	 * @param diagram Diagram on which nets to be processed
	 * @param convertedNets Set<INetConductor> to be populated with each net that is converted
	 * @param schemConductorsToDelete Set<INetConductor> to be populated with each schem net that is converted
	 * @param multicoreSchemNets Set<INetConductor> to be populated with nets of multicore to be converted to wires
	 * @param stackedpinCondsToSkip Set<IHighwayConductor> stackedpinCondsToSkip to be populated with nets to skip the
	 * nets to wires operation
	 */

	protected void processConductors(ISchemDiagram diagram, Set<INetConductor> convertedNets,
			Set<IConductor> schemConductorsToDelete, SetMap<IMulticore, IConductor> multicoreSchemNets,
			Set<IHighwayConductor> stackedpinCondsToSkip)
	{
		IDesignContainer design = diagram.getDesignContainer();
		for (IConductor schemConductor : getSortedSchemConductors(diagram)) {
			chs.cof.logical.cable.IConductor conductor =
					CommonUtils.cast(schemConductor.getConnectivity(), chs.cof.logical.cable.IConductor.class);
			if (conductor != null) {

				if (!hasValidDomainAccess(conductor.getSharedConductor())) {
					outputStatusMessage("Message.NODomainAccess", indent(), conductor.getSharedConductor().getName());
					continue;
				}

				INetConductor netConductor = CommonUtils.cast(conductor, INetConductor.class);
				IMulticore rootMulticore = conductor.getRootMulticore();
				if (rootMulticore == null && netConductor != null) {
					if (!isObjectExcluded(netConductor)) {
						if (!updatePortedNetConnectivity(schemConductor, netConductor)) {
							// Convert nets to wires using shared splice
							boolean success = convertedNets.add(netConductor);
							if (success) {
								Set<IConductor> representedConductors = getDWSchemConductors(design, netConductor);

								// Don't convert if conductor is part of highway and it need shared splice to convert it into wires
								Set<IHighwaySchematic> representedHighways = getDWSchemHighways(design, netConductor);
								if (representedHighways.isEmpty()) {
									List<SchemGraphUtils.SchemGraph> graphs =
											convertSingleCableNetToWires(design, representedConductors, true);
									createSchematicConductor(design, schemConductorsToDelete, graphs);
								}
								else {
									stackedpinCondsToSkip.add(netConductor);
								}
							}
						}
					}
					else {
						outputStatusMessage("Message.ConstrainedNet", indent(), netConductor.getName());
					}
				}
				else {
					if (netConductor != null) {
						//dts0100544799 we must check on the pin count rather than the number of representations
						int pinCount = netConductor.getPinSet().size();
						if (pinCount >
								2) {//This is the place where a multi-term net conductor part of MC is being ignored
							mInvalidMulticores.add(rootMulticore.getUID());
						}
					}
					if (!processMulticores(design, conductor, rootMulticore)) {
						continue;
					}

					// We need to call updateSharedMulticoreConnectivity whether the conductor is a net, wire or shield
					// as this also populates mDesignsWithSharedMulticores - only INetConductor connectivity is checked
					if (rootMulticore != null && rootMulticore.getSharedMulticore() != null) {
						updateSharedMulticoreConnectivity(design, rootMulticore, schemConductor, conductor);
					}
					else if (netConductor != null && !schemConductorsToDelete.contains(schemConductor)) {
						// We only need to process INetConductors in non-shared Multicores, existing IWireConductors
						// are completely untouched by this command
						multicoreSchemNets.add(rootMulticore, schemConductor);
					}
				}
			}
		}
	}

	private IRuleMatcher getRuleMatcher()
	{
		return new RuleMatcher(new RuleErrorListener(LogTabType.TAB_CONFLICTS, true));
	}

	private String getLinksForConstrainedObjects(final IDesignContainer design, Set<ILogicObject> constrainedObjs)
	{
		StringBuilder links = new StringBuilder();
		List<ILogicObject> sortedconstrainedObjs = new ArrayList<ILogicObject>(constrainedObjs);
		Collections.sort(sortedconstrainedObjs, new NamedObjectComparator<ILogicObject>()
		{
			protected String getString(ILogicObject object)
			{
				return object.getName();
			}
		});
		Iterator<ILogicObject> itr = sortedconstrainedObjs.iterator();
		if (itr.hasNext()) {
			ILogicObject logObj = itr.next();
			links.append(link(design, logObj, logObj.getName()));
		}
		while (itr.hasNext()) {
			ILogicObject logObj = itr.next();
			links.append(", ");
			links.append(link(design, logObj, logObj.getName()));
		}
		return links.toString();
	}

	@NotNull private Set<ILogicObject> getConstrainedObjectsHierarchically(IMulticore rootMulticore)
	{
		Set<ILogicObject> logicObjectsToCheckConstraints = new HashSet<ILogicObject>();
		Set<ILogicObject> logicObjectsExcluded =
				new TreeSet<ILogicObject>(new NamedObjectComparator<ILogicObject>(true, true, true));
		logicObjectsToCheckConstraints.addAll(rootMulticore.getAllMulticoresInHierarchy());
		final ConductorQueryClient netsQuery = new ConductorQueryClient().includeNets();
		logicObjectsToCheckConstraints.addAll(rootMulticore.getAllConductorsInHierarchy(netsQuery));
		for (ILogicObject logicObjectToCheckConstraints : logicObjectsToCheckConstraints) {
			if (isObjectExcluded(logicObjectToCheckConstraints)) {
				logicObjectsExcluded.add(logicObjectToCheckConstraints);
			}
		}
		return logicObjectsExcluded;
	}

	private boolean isValidToConvertMulticore(IDesignContainer design, IMulticore rootMulticore)
	{

		// first check the nets to wires exclude constraint
		Set<ILogicObject> logicObjectsExcluded = getConstrainedObjectsHierarchically(rootMulticore);
		if (!logicObjectsExcluded.isEmpty()) {
			String constrainedObjLinks = getLinksForConstrainedObjects(design, logicObjectsExcluded);
			outputStatusMessage(getResourceName("Message.Constrained", rootMulticore), indent(),
					link(design, rootMulticore, rootMulticore.getName()), constrainedObjLinks);
			return false;
		}

//		boolean multicoreIsValid = doesNonSharedMulticoreHasMultiTermNet(rootMulticore);
//		if (!multicoreIsValid) {
//			if (isMulticoreConnectedToStack(rootMulticore, design)) {
//				getCommandListener().handleEvent(
//						ResourceCommandEvent.create("Message.MulticoreIsPartOfHighway",
//								link(design, rootMulticore, rootMulticore.getName())));
//			}
//			else {
//				getCommandListener().handleEvent(ResourceCommandEvent.create(
//						getResourceName("Message.Invalid", rootMulticore), indent(),
//						link(design, rootMulticore, rootMulticore.getName())));
//			}
//			return false;
//		}
		return true;
	}

	private boolean processMulticores(IDesignContainer design, chs.cof.logical.cable.IConductor conductor,
			IMulticore rootMulticore)
	{
		// We need to call updateSharedMulticoreConnectivity whether the conductor is a net, wire or shield
		// as this also populates mDesignsWithSharedMulticores - only INetConductor connectivity is checked
		if (rootMulticore != null && rootMulticore.getSharedMulticore() != null) {
			// first check the nets to wires exclude constraint
			Set<ILogicObject> logicObjectsExcluded = getConstrainedObjectsHierarchically(rootMulticore);
			if (!logicObjectsExcluded.isEmpty()) {
				String constrainedObjLinks = getLinksForConstrainedObjects(design, logicObjectsExcluded);
				IMulticore mc = conductor.getMulticore();
				while (mc != null) {
					outputStatusMessage(getResourceName("Message.Constrained", mc), indent(),
							link(design, mc, mc.getName()), constrainedObjLinks);
					mInvalidMulticores.add(mc.getUID());
					mc = mc.getParent();
				}
				return false;
			}
		}
		return true;
	}

	private RuleStatus evaluateRuleConstraint(IRuleMatcher ruleMatcher, ILogicObject logicObject)
	{
		RuleStatus ruleStatus = RuleStatus.matchINVALID;
		IRuledObject ruledObject = (IRuledObject) logicObject.getLogicDesign();
		if (ruledObject != null) {
			IRuleResult ruleResult = ruleMatcher
					.evaluateConstraint(logicObject, ruledObject, RuleUtils.instance().NET_TO_WIRE_ATTR_TEMPLATE());
			ruleStatus = ruleResult.getStatus();
		}
		return ruleStatus;
	}

	/**
	 * Converts nets within the highways (nets represented by the highways) into wires
	 *
	 * @param diagram Diagram
	 * @param convertedNets Set<INetConductor> to be populated with each net that is converted
	 * @param condsToSkip Conductors which are already processed
	 * @param skipMulticores Multicores which are already processed
	 */
	private void processHighwayConductors(ISchemDiagram diagram, @NotNull final Set<INetConductor> convertedNets,
			Set<IHighwayConductor> condsToSkip, Set<IMulticore> skipMulticores)
	{
		ILogicDesign design = diagram.getDesign();
		Map<IMulticore, SetMap<IHighwaySchematic, chs.cof.logical.cable.IConductor>> netConsToHighways =
				new HashMap<IMulticore, SetMap<IHighwaySchematic, chs.cof.logical.cable.IConductor>>();
		for (IHighwaySchematic schemHighway : getSortedSchemhighways(diagram)) {
			for (chs.cof.logical.cable.IConductor cond : schemHighway.getStackedPinConductors()) {
				if (!hasValidDomainAccess(cond.getSharedObject())) {
					outputStatusMessage("Message.NODomainAccess", indent(), cond.getSharedConductor().getName());
					continue;
				}
				if (!condsToSkip.contains(cond)) {
					INetConductor netConductor = null;
					if (cond instanceof INetConductor) {
						netConductor = (INetConductor) cond;
					}
					if (netConductor != null && netConductor.getMulticore() == null) {
						if (!isObjectExcluded(netConductor)) {
							Set<IAbstractPin> pins = schemHighway.getConnectedPins(netConductor);
							if (pins.size() <= 2) {
								registerUnprocessedPortedNet(netConductor);
							}
							else {
								condsToSkip.add(netConductor);
							}
						}
						else {
							outputStatusMessage("Message.ConstrainedNet", indent(), cond.getName());
						}
					}
					else {
						IMulticore rootMulticore = cond.getRootMulticore();
						if (rootMulticore != null && rootMulticore.getSharedMulticore() != null) {
							mSharedMCWithHighwayRepresentations.add(rootMulticore.getSharedMulticore());
						}
						if (skipMulticores.contains(rootMulticore)) {
							continue;
						}
						if (netConductor != null && netConductor.getPinSet().size() > 2) { //REDUNDANT ?
							condsToSkip.add(netConductor);
							continue;
						}
						if (rootMulticore != null && rootMulticore.getSharedMulticore() == null &&
								doesMCHasMultiTermOrMultiEdgeNet(rootMulticore)) {

							continue;
						}

						if (!processMulticores(design, cond, rootMulticore)) {
							continue;
						}

						// We need to call updateSharedMulticoreConnectivity whether the conductor is a net, wire or shield
						// as this also populates mDesignsWithSharedMulticores - only INetConductor connectivity is checked
						if (rootMulticore != null && rootMulticore.getSharedMulticore() != null) {
							updateSharedMulticoreConnectivity(design, rootMulticore, null, cond);
							//Highway pins are not being considered
							//SharedNetsConnectivity sharedNetsConnectivity = getSharedMulticoreConnectivity(rootMulticore.getSharedMulticore());
							//if(sharedNetsConnectivity.isValid(netConductor)){
							//sharedNetsConnectivity.deltaPin(netConductor, netConductor.getPinSet());
							//}
						}
						else if (netConductor != null) {
							// We only need to process INetConductors in non-shared Multicores, existing IWireConductors
							// are completely untouched by this command
							SetMap<IHighwaySchematic, chs.cof.logical.cable.IConductor> map =
									netConsToHighways.get(rootMulticore);
							if (map == null) {
								map = new SetMap<IHighwaySchematic, chs.cof.logical.cable.IConductor>();
								netConsToHighways.put(rootMulticore, map);
							}
							map.add(schemHighway, netConductor);
						}
					}
				}
			}
		}

		for (IMulticore rootMulticore : netConsToHighways.keySet()) {
			SetMap<IHighwaySchematic, chs.cof.logical.cable.IConductor> highwayToconvert =
					netConsToHighways.get(rootMulticore);
			if (highwayToconvert != null) {

				for (IHighwaySchematic highway : highwayToconvert.keySet()) {
					for (chs.cof.logical.cable.IConductor netConductor : highwayToconvert.get(highway)) {
						convertPortedSchemNetOnHighwayToSingleWire(design, (INetConductor) netConductor, highway, true);
						convertedNets.add((INetConductor) netConductor);
					}
				}
			}
		}
	}

	/**
	 * Converts a non shared multicore Net contents to wires. The schem nets to convert are passed in schemGraphs but
	 * for reporting purposes we want to show the entire multicore hierarchy so we recurse the multicore and look up the
	 * reps via a map
	 *
	 * @param design IDesign
	 * @param diagram ISchemDiagram
	 * @param multicore IMulticore to convert - recusively
	 * @param schemGraphs SchemGraphs of the schem nets to convert
	 * @param convertedNets Set to add converted net to
	 * @param schemConductorsToDelete collect the schem conductor which will be deleted after processing
	 */
	private void convertNonSharedMulticore(@NotNull final IDesignContainer design, @NotNull final ISchemDiagram diagram,
			@NotNull final IMulticore multicore, @NotNull final Map<IConductor, SchemGraphUtils.SchemGraph> schemGraphs,
			@NotNull final Set<INetConductor> convertedNets, Set<IConductor> schemConductorsToDelete)
	{
		outputStatusMessage(getResourceName("Message.Converting", multicore), indent(),
				link(design, multicore, multicore.getName()));
		mIndent++;
		for (chs.cof.logical.cable.IConductor connectivityConductor : getSortedConductors(multicore)) {
			INetConductor netConductor = CommonUtils.cast(connectivityConductor, INetConductor.class);
			if (netConductor != null) {
				Collection<IConductor> schemNets = mConnectivitySchemMapForUseInMC.pull(netConductor);
				if (schemNets != null) {
					for (IUIDObject uidObject : getSortedSchemNets(schemNets)) {
						IConductor schemNetConductor = CommonUtils.cast(uidObject, IConductor.class);
						if (schemNetConductor != null) {
							final SchemGraphUtils.SchemGraph schemGraph = schemGraphs.get(schemNetConductor);
							if (schemGraph != null) {
								ISchemDiagram parentDiagram = DiagramHelper.getDiagram(schemNetConductor);
								if (parentDiagram != null) {
									//dts0100594284 - Part of the fix: design-wide nets belonging to a multicore should be converted using this method,
									// to make sure different schematics have the same connectivity. Also at this point we are sure we are processing valid multicores
									IWireConductor wireConductor =
											convertPortedSchemNetToSingleWire(design, schemNetConductor, true);
									IGrid grid = parentDiagram.getGrid();
									IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(parentDiagram);
									createSchemWireConductor(design, wireConductor, parentDiagram, grid, styleSet,
											schemNetConductor,
											schemConductorsToDelete);
								}
								convertedNets.add(netConductor);
							}
						}
					}
				}

				Collection<IHighwaySchematic> highwaySchems = m_netCondToHighwayMap.pull(netConductor);
				if (highwaySchems != null) {
					for (IHighwaySchematic highway : highwaySchems) {
						convertPortedSchemNetOnHighwayToSingleWire(design, netConductor, highway, true);
						convertedNets.add(netConductor);
					}
				}
			}
		}
		mIndent--;
		for (IMulticore innerMulticore : getSortedInnerMulticores(multicore)) {
			mIndent++;
			convertNonSharedMulticore(design, diagram, innerMulticore, schemGraphs, convertedNets,
					schemConductorsToDelete);
			mIndent--;
		}
	}

	protected boolean isObjectExcluded(@NotNull ILogicObject logicObject)
	{
		RuleStatus ruleStatus = evaluateRuleConstraint(getRuleMatcher(), logicObject);
		return ruleStatus == RuleStatus.matchEXCLUDE;
	}

	/**
	 * Does a through check on the state of a multicore - to ensure we only convert when they are completely valid.
	 * <p>
	 *
	 * @param multicore IMulticore
	 * @param oldNewSharedObjectMap Map of Shared Objects to new Shared Objects - multicore contents
	 *
	 * @return boolean Iff multicore is ok to convert - from a sharing perspective
	 */
	private boolean isMulticoreCorrectlyShared(@NotNull final IMulticore multicore,
			@NotNull final Map<ISharedObject, ISharedObject> oldNewSharedObjectMap)
	{
		ISharedMulticore sharedMulticore = multicore.getSharedMulticore();
		assert sharedMulticore != null;
		if (!oldNewSharedObjectMap.containsKey(sharedMulticore)) {
			return false;
		}
		IShieldConductor shieldConductor = multicore.getShield();
		if (shieldConductor != null && !oldNewSharedObjectMap.containsKey(shieldConductor.getSharedObject())) {
			return false;
		}
		IConductorIterator conductors = multicore.getConductors();
		for (chs.cof.logical.cable.IConductor conductor : conductors) {
			if (!oldNewSharedObjectMap.containsKey(conductor.getSharedObject())) {
				return false;
			}
		}
		IMulticoreIterator innerMulticores = multicore.getMulticores();
		for (IMulticore innerMulticore : innerMulticores) {
			if (!isMulticoreCorrectlyShared(innerMulticore, oldNewSharedObjectMap)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Updates a SharedNetsConnectivity object such that we can determine if a ISharedMulticore is valid for conversion
	 * at the end of one pass through all the IDesigns in scope.
	 * <p>
	 *
	 * @param design IDesign that owns multicore
	 * @param multicore IMulticore must be Shared
	 * @param schemConductor IConductor
	 * @param conductor chs.cof.logical.cable.IConductor
	 */
	private void updateSharedMulticoreConnectivity(@NotNull final IDesignContainer design,
			@NotNull final IMulticore multicore,
			final IConductor schemConductor, @NotNull final chs.cof.logical.cable.IConductor conductor)
	{
		ISharedMulticore sharedMulticore = getSharedMulticore(conductor);
		assert schemConductor == null || schemConductor.getConnectivity() == conductor;
		assert sharedMulticore != null;
		assert conductor.getSharedConductor() != null;
		if(!hasValidDomainAccess(sharedMulticore)){
			return;
		}
		ISharedConductor sharedConductor = conductor.getSharedConductor();
		if (sharedConductor != null) {
			// We must populate mDesignsWithSharedMulticores when we encounter any part of a Shared Multicore, even
			// a Wire or a Shield, however we are only interested in Net connectivity for determining validity
			mDesignsWithSharedMulticores.add(design, sharedMulticore);
			if (conductor instanceof INetConductor) {
				SharedNetsConnectivity sharedNetsConnectivity = getSharedMulticoreConnectivity(sharedMulticore);
				if (schemConductor != null) {
					SchemGraphUtils.SchemGraph schemGraph = new SchemGraphUtils.SchemGraph(schemConductor, false);
					if (schemGraph.getNumEdges() > 2 || mInvalidMulticores.contains(multicore.getUID())) {
						sharedNetsConnectivity.invalidate();
					}
					else {
						//sharedNetsConnectivity.deltaPinCount(conductor, schemGraph.getNumSchemPins());
						Set<IUID> pinset = new HashSet<IUID>();
						for (SchemGraphUtils.SchemEdge edge : schemGraph.getEdges()) {
							for (IPin pin : edge.getPins()) {
								IAbstractPin cablePin = pin.getConnectivity();
								IUID SharedPin = cablePin.getSharedObjectUID();
								pinset.add(SharedPin != null ? SharedPin : cablePin.getUID());
							}
						}
						sharedNetsConnectivity.deltaPin(conductor, pinset);
					}
				}
			}
		}
	}

	/**
	 * Determines whether the ported conductor is candidate to be converted to multiple unshared wires or to be stored
	 * for later processing
	 *
	 * @param schemConductor The schem object of the net conductor
	 * @param netConductor The connectivity object of the net condcutor
	 *
	 * @return True if the net is to stored for later processing, otherwise returns False
	 */
	private boolean updatePortedNetConnectivity(@NotNull final IConductor schemConductor,
			@NotNull final INetConductor netConductor)
	{
		// Already been invalidated on some other design or diagram
		if (!mPortedNetConnectivity.isValid(netConductor)) {
			return false;
		}
		if (netConductor.getNumPins() <= 2) {
			// Use SchemGraph that removes support for shared splices, checkSharedSpliceRequired param is false
			SchemGraphUtils.SchemGraph schemGraph = new SchemGraphUtils.SchemGraph(schemConductor, false);
			if (schemGraph.getNumEdges() <= 1) {

				registerUnprocessedPortedNet(netConductor);
				// Potentially can be converted to a single Shared/Ported Wire
				mPortedNetConnectivity.deltaPinCount(netConductor, schemGraph.getNumSchemPins());
				return true;
			}
			else if (schemGraph.getNumEdges() > 2) {
				mPortedNetConnectivity.invalidate(netConductor);
			}
		}
		else {
			mPortedNetConnectivity.invalidate(netConductor);
		}
		return false;
	}

	private boolean doesMCHasMultiTermOrMultiEdgeNet(IMulticore rootMulticore)
	{
		boolean bMultiTermNets = false;
		Set<chs.cof.logical.cable.IConductor> multicoreConductors =
				rootMulticore.getAllConductorsInHierarchy(false);
		//check for multiEdge nets
		for (chs.cof.logical.cable.IConductor conductor : multicoreConductors) {
			INetConductor netConductor = CommonUtils.cast(conductor, INetConductor.class);
			if (netConductor != null) {
				if (netConductor.getNumPins() > 2) {
					bMultiTermNets = true;
					break;
				}
			}
		}
		//check for multiEdge nets
		if (!bMultiTermNets) {
			for (chs.cof.logical.cable.IConductor conductor : multicoreConductors) {
				INetConductor netConductor = CommonUtils.cast(conductor, INetConductor.class);
				if (netConductor != null) {
					Set<IConductor> representedConductors =
							getDWSchemConductors(rootMulticore.getLogicDesign(), netConductor);
					for (IConductor schemNetConductor : representedConductors) {
						SchemGraphUtils.SchemGraph schemGraph =
								new SchemGraphUtils.SchemGraph(schemNetConductor, false);
						if (schemGraph.getNumEdges() > 2) {
							bMultiTermNets = true;
							break;
						}
					}
				}
			}
		}
		return bMultiTermNets;
	}

	private void updateEndLineStyles(@NotNull final IConductor schemWireConductor, @NotNull final ISchemDiagram diagram)
	{
		//we must remove existing association of ports from net conductor. otherwise addportgfx doesn't add port gfx.
		//we shouldn't delete them because they will be deleted along with net conductor. we just need to disassociate
		//them from the joint.
		for (IConnected seg : schemWireConductor.getSegments()) {
			IJoint sj = seg.getStartJoint();
			IJoint ej = seg.getEndJoint();
			if (sj != null) {
				for (IPort portGfx : sj.getAssociations(IPort.class)) {
					sj.removeAssociation(portGfx);
				}
			}
			if (ej != null) {
				for (IPort portGfx : ej.getAssociations(IPort.class)) {
					ej.removeAssociation(portGfx);
				}
			}
		}
		int gridSpacing = diagram.getGrid().getGridSpacing();
		PortHelper.addPortGfx(schemWireConductor, gridSpacing);
	}

	private IWireConductor createWireConductor(@NotNull final SchemGraphUtils.SchemEdge edge,
			@NotNull final IConnectivity connectivity,
			@NotNull final INetConductor netConductor)
	{
		IWireConductor wireConductor =
				FactoryMgr.getCablePropertiedFactory().createWireConductor(FactoryMgr.getCommonFactory().createUID());
		assignLibrayPartAndCopyAttributesAndProperties(netConductor, wireConductor);
		copyAssembly(netConductor, wireConductor);
		updateNets2WiresMap(netConductor, wireConductor);
		mObjectUIDsForConstraints.add(wireConductor.getUID());
		Set<IGeneralHighway> highways = netConductor.getHighways();
		for (IGeneralHighway highway : highways) {
			highway.addConductor(wireConductor);
		}
		edge.setWireConductor(wireConductor);
		connectivity.addWireConductor(wireConductor);
		mWireNames.add(wireConductor.getName());
		Set<IPin> pins = edge.getPins();
		for (IPin pin : pins) {
			IAbstractPin connectivityPin = pin.getConnectivity();
			wireConductor.addPin(connectivityPin);
		}
		return wireConductor;
	}

	private void updateNets2WiresMap(INetConductor netConductor, IWireConductor wireConductor)
	{
		if (netConductor.getMulticore() != null) {
			IMulticore rootMC = netConductor.getMulticore().getRootMulticore();
			ListMap<IUID, IUID> nets2WiresMap = mNetConductortoWireConductorsMap.get(rootMC.getUID());
			if (nets2WiresMap == null) {
				nets2WiresMap = new ListMap<IUID, IUID>();
			}
			nets2WiresMap.addUnique(netConductor.getUID(), wireConductor.getUID());
			mNetConductortoWireConductorsMap.put(rootMC.getUID(), nets2WiresMap);
		}
	}

	//Added param "bAssignMC" to not do mutlticore assignment in case of multi-term/multi-edge nets part of MC
	protected IWireConductor createWireConductor(@NotNull final SchemGraphUtils.SchemEdge edge,
			@NotNull final IConnectivity connectivity,
			@NotNull final INetConductor netConductor, @NotNull final Set<SchemGraphUtils.SchemEdge> edges,
			boolean bAssignMC)
	{
		IWireConductor wireConductor = createWireConductor(edge, connectivity, netConductor);
		IMulticore netMulticore = netConductor.getMulticore();
		if (netMulticore != null && bAssignMC) {
			// For non-shared MC the Wire is a conductor of the same MC as the Net
			assert edges.size() == 1;    //TODO SCRUM - wrong assert..remove ?
			netMulticore.addConductor(wireConductor);
		}
		return wireConductor;
	}

	private void createSharedWireConductor(IDesignContainer design, INetConductor netConductor,
			Set<SchemGraphUtils.SchemEdge> edgesToProcess)
	{
		IConnectivity connectivity = getConnectivity(design);
		IWireConductor wireConductor = null;
		for (SchemGraphUtils.SchemEdge edge : edgesToProcess) {
			if (wireConductor == null && connectivity != null) {
				wireConductor = createWireConductor(edge, connectivity, netConductor);
			}
			//now go to unset the need for shared splice.
			if (wireConductor != null) {
				edge.setWireConductor(wireConductor);
				edge.setSharedEdge(true);
				SchemGraphUtils.SchemVertex startVertex = edge.getStart();
				//for this type of edges we can't create splice on its ends. so prevent it.
				if (startVertex != null && startVertex.isDanglingEnd()) {
					startVertex.setSharedVertex(true);
				}
				SchemGraphUtils.SchemVertex endVertex = edge.getEnd();
				if (endVertex != null && endVertex.isDanglingEnd()) {
					endVertex.setSharedVertex(true);
				}
				//if wire conductor is already created by previous iteration
				//we need to update the connectivity pins also.
				Set<IPin> pins = edge.getPins();
				for (IPin pin : pins) {
					IAbstractPin connectivityPin = pin.getConnectivity();
					wireConductor.addPin(connectivityPin);
				}
			}
		}
	}

	//Added param "bAssignMC" to not do mutlticore assignment in case of multi-term/multi-edge nets part of MC
	private List<SchemGraphUtils.SchemGraph> convertSingleCableNetToWires(@NotNull final IDesignContainer design,
			@NotNull final Set<IConductor> schemConductors,/* all should belong to same cable conductor */
			boolean bAssignMC)
	{
		if (schemConductors.isEmpty()) {
			return Collections.emptyList();
		}
		List<SchemGraphTree> treesNoDanglingEdge =
				new SortedList<SchemGraphTree>(SchemGraphTree.getGraphTreePositionComparator());
		List<SchemGraphTree> treesMultiDanglingEdge =
				new SortedList<SchemGraphTree>(SchemGraphTree.getGraphTreePositionComparator());
		List<SchemGraphTree> treesSingleDanglingEdge =
				new SortedList<SchemGraphTree>(SchemGraphTree.getGraphTreePositionComparator());
		List<SchemGraphTree> treesBothEndDanglingEdge =
				new SortedList<SchemGraphTree>(SchemGraphTree.getGraphTreePositionComparator());

		INetConductor netConductor = null;
		for (IConductor schemConductor : schemConductors) {
			netConductor = CommonUtils.cast(schemConductor.getConnectivity(), INetConductor.class);
			SchemGraphUtils.SchemGraph theSchemGraph = new SchemGraphUtils.SchemGraph(schemConductor, true);
			ISchemDiagram parentDiagram = DiagramHelper.getDiagram(schemConductor);
			assert parentDiagram != null;
			SchemGraphTree tree = new SchemGraphTree(theSchemGraph, parentDiagram);
			if (tree.getType() == SchemGraphTree.TreeType.TREE_SINGLE_DANGLING_EDGE) {
				treesSingleDanglingEdge.add(tree);
			}
			else if (tree.getType() == SchemGraphTree.TreeType.TREE_MULTIPLE_DANGLING_EDGE) {
				treesMultiDanglingEdge.add(tree);
			}
			else if (tree.getType() == SchemGraphTree.TreeType.TREE_BOTH_END_DANGLING_EDGE) {
				treesBothEndDanglingEdge.add(tree);
			}
			else {
				treesNoDanglingEdge.add(tree);
			}
		}

		//now we already have all the graphs in some sorted order. we will just create a ordered list of all the graphs
		//so that we will get a consistent way for wire creation.
		List<SchemGraphUtils.SchemGraph> graphs = new ArrayList<SchemGraphUtils.SchemGraph>();
		for (SchemGraphTree tree : treesNoDanglingEdge) {
			graphs.add(tree.getGraph());
		}
		for (SchemGraphTree tree : treesMultiDanglingEdge) {
			graphs.add(tree.getGraph());
		}
		for (SchemGraphTree tree : treesSingleDanglingEdge) {
			graphs.add(tree.getGraph());
		}
		for (SchemGraphTree tree : treesBothEndDanglingEdge) {
			graphs.add(tree.getGraph());
		}

		assert netConductor != null;
		//check if the shared conductor is used in other design.
		boolean isNetUsedInAnotherDesign = false;
		ISharedConductor netSharedConductor = netConductor.getSharedConductor();
		if (netConductor.isShared() && netSharedConductor != null) {
			if (mSharedSplicesAlreadyCreated.contains(netSharedConductor.getUID())) {
				isNetUsedInAnotherDesign = true;
			}
			else {
				IProjectSharedUsageView sharedUsageView = design.getProject().getSharedUsageView();
				ISharedUsageInfo info = sharedUsageView.getSharedUsageInfo(netSharedConductor);
				for (ISharedUsage usage : info.getUsages()) {
					if (!usage.getDesignUID().equals(design.getUID())) {
						for (IDesignContainer mDes : mDesignsToProcess) {
							if (usage.getDesignUID().equals(mDes.getUID())) {
								isNetUsedInAnotherDesign = true;
								mSharedSplicesAlreadyCreated.add(netSharedConductor.getUID());
								break;
							}
						}
						if (isNetUsedInAnotherDesign) {
							break;
						}
					}
				}
			}
		}
		createSharedSpliceForSharedNet = isNetUsedInAnotherDesign;
		currentSharedSpliceForSharedNet = null;

		List<SchemGraphTree> listOfTreesToSearchForFinalJoint = new ArrayList<SchemGraphTree>();
		//we must have the above as array list because we need to process them in order of
		//multidangling trees first then the single dangling trees.
		//this edgesToJoin must maintain order so that pairing of edges will within same multidangling
		//tree will be consistent.
		List<SchemGraphUtils.SchemEdge> edgesToJoin = new ArrayList<SchemGraphUtils.SchemEdge>();
		Set<SchemGraphUtils.SchemEdge> edgesAlreadyJoined = new HashSet<SchemGraphUtils.SchemEdge>();
		SchemGraphUtils.SchemEdge prevJoinEdge = null;
		for (SchemGraphTree tree : treesMultiDanglingEdge) {
			listOfTreesToSearchForFinalJoint.add(tree);
			if (prevJoinEdge != null) {
				Set<SchemGraphUtils.SchemEdge> edgesToProcess = new HashSet<SchemGraphUtils.SchemEdge>();
				edgesToProcess.add(prevJoinEdge);
				edgesToProcess.add(tree.getBackDanglingEdge());
				createSharedWireConductor(design, netConductor, edgesToProcess);
				edgesAlreadyJoined.addAll(edgesToProcess);
			}
			prevJoinEdge = tree.getFrontDanglingEdge();
			edgesToJoin.addAll(tree.getOpenJoiningEdges());
		}
		edgesToJoin.removeAll(edgesAlreadyJoined);
		edgesAlreadyJoined.clear();

		List<Pair<SchemGraphTree, SchemGraphTree>> listOfPairTreesToSearchForSuitableJoint =
				new ArrayList<Pair<SchemGraphTree, SchemGraphTree>>();
		if (edgesToJoin.size() > treesSingleDanglingEdge.size()) {
			Iterator<SchemGraphUtils.SchemEdge> edgeToJoinItr = edgesToJoin.iterator();
			for (SchemGraphTree tree : treesSingleDanglingEdge) {
				listOfTreesToSearchForFinalJoint.add(tree);
				SchemGraphUtils.SchemEdge treeEdgeToJoin = tree.getFrontDanglingEdge();
				SchemGraphUtils.SchemEdge edgeToJoin = edgeToJoinItr.next();
				Set<SchemGraphUtils.SchemEdge> edgesToProcess = new HashSet<SchemGraphUtils.SchemEdge>();
				edgesToProcess.add(treeEdgeToJoin);
				edgesToProcess.add(edgeToJoin);
				createSharedWireConductor(design, netConductor, edgesToProcess);
			}
			while (true) {
				SchemGraphUtils.SchemEdge edge1 = null;
				if (edgeToJoinItr.hasNext()) {
					edge1 = edgeToJoinItr.next();
				}
				SchemGraphUtils.SchemEdge edge2 = null;
				if (edgeToJoinItr.hasNext()) {
					edge2 = edgeToJoinItr.next();
				}
				if (edge1 == null || edge2 == null) {
					break;
				}
				Set<SchemGraphUtils.SchemEdge> edgesToProcess = new HashSet<SchemGraphUtils.SchemEdge>();
				edgesToProcess.add(edge1);
				edgesToProcess.add(edge2);
				createSharedWireConductor(design, netConductor, edgesToProcess);
			}
		}
		else {
			Iterator<SchemGraphTree> treeToJoinItr = treesSingleDanglingEdge.iterator();
			for (SchemGraphUtils.SchemEdge edgeToJoin : edgesToJoin) {
				SchemGraphTree tree = treeToJoinItr.next();
				listOfTreesToSearchForFinalJoint.add(tree);
				SchemGraphUtils.SchemEdge treeEdgeToJoin = tree.getFrontDanglingEdge();
				Set<SchemGraphUtils.SchemEdge> edgesToProcess = new HashSet<SchemGraphUtils.SchemEdge>();
				edgesToProcess.add(treeEdgeToJoin);
				edgesToProcess.add(edgeToJoin);
				createSharedWireConductor(design, netConductor, edgesToProcess);
			}
			while (true) {
				SchemGraphUtils.SchemEdge edge1 = null;
				SchemGraphTree tree1 = null;
				if (treeToJoinItr.hasNext()) {
					tree1 = treeToJoinItr.next();
					edge1 = tree1.getFrontDanglingEdge();
				}
				SchemGraphUtils.SchemEdge edge2 = null;
				SchemGraphTree tree2 = null;
				if (treeToJoinItr.hasNext()) {
					tree2 = treeToJoinItr.next();
					edge2 = tree2.getFrontDanglingEdge();
				}

				if (tree1 != null || tree2 != null) {
					listOfPairTreesToSearchForSuitableJoint.add(new Pair<SchemGraphTree, SchemGraphTree>(tree1, tree2));
				}
				if (edge1 == null || edge2 == null) {
					break;
				}

				//we must create wires before searching for suitable vertex to join
				//so that we can skip shared vertices.
				if (edge1 != null && edge2 != null) {
					Set<SchemGraphUtils.SchemEdge> edgesToProcess = new HashSet<SchemGraphUtils.SchemEdge>();
					edgesToProcess.add(edge1);
					edgesToProcess.add(edge2);
					createSharedWireConductor(design, netConductor, edgesToProcess);
				}
			}
		}
		edgesToJoin.clear();

		//first we need to determine whether we need a shared splice to join different
		//complete but separate graph trees.
		int numberOfCompleteButSeparateTrees = 0;
		numberOfCompleteButSeparateTrees += (listOfTreesToSearchForFinalJoint.isEmpty() ? 0 : 1);
		numberOfCompleteButSeparateTrees += treesNoDanglingEdge.size();
		numberOfCompleteButSeparateTrees += listOfPairTreesToSearchForSuitableJoint.size();

		Set<SchemGraphUtils.SchemVertex> verticesToJoin = new HashSet<SchemGraphUtils.SchemVertex>();
		if (numberOfCompleteButSeparateTrees > 1) {
			for (SchemGraphTree tree : treesNoDanglingEdge) {
				SchemGraphUtils.SchemVertex vertex = tree.getJoiningSpliceVertex();
				if (vertex == null) {
					tree.getGraph().split(tree.getDiagram().getGrid(), tree.getGraph().getEdges().iterator().next());
					tree.init();
					vertex = tree.getJoiningSpliceVertex();
				}
				verticesToJoin.add(vertex);
			}

			for (Pair<SchemGraphTree, SchemGraphTree> pair : listOfPairTreesToSearchForSuitableJoint) {
				//this will join the singledangling trees to rest of the forest.
				List<SchemGraphTree> listOfTreesToSearchForSuitableJoint = new ArrayList<SchemGraphTree>();
				if (pair.getFirst() != null) {
					listOfTreesToSearchForSuitableJoint.add(pair.getFirst());
				}
				if (pair.getSecond() != null) {
					listOfTreesToSearchForSuitableJoint.add(pair.getSecond());
				}
				SchemGraphUtils.SchemVertex suitableJointVertex = null;
				for (SchemGraphTree tree : listOfTreesToSearchForSuitableJoint) {
					suitableJointVertex = tree.getJoiningSpliceVertex();
					if (suitableJointVertex != null) {
						break;
					}
				}
				if (suitableJointVertex == null) {
					for (SchemGraphTree tree : listOfTreesToSearchForSuitableJoint) {
						tree.getGraph()
								.split(tree.getDiagram().getGrid(), tree.getGraph().getEdges().iterator().next());
						tree.init();
						suitableJointVertex = tree.getJoiningSpliceVertex();
						if (suitableJointVertex != null) {
							break;
						}
					}
				}
				if (suitableJointVertex != null) {
					verticesToJoin.add(suitableJointVertex);
				}
				listOfTreesToSearchForSuitableJoint.clear();
			}

			//this will join the multidangling trees to rest of the forest.
			SchemGraphUtils.SchemVertex finalJointVertex = null;
			for (SchemGraphTree tree : listOfTreesToSearchForFinalJoint) {
				finalJointVertex = tree.getJoiningSpliceVertex();
				if (finalJointVertex != null) {
					break;
				}
			}
			if (finalJointVertex == null) {
				for (SchemGraphTree tree : listOfTreesToSearchForFinalJoint) {
					tree.getGraph()
							.split(tree.getDiagram().getGrid(), tree.getGraph().getEdges().iterator().next());
					tree.init();
					finalJointVertex = tree.getJoiningSpliceVertex();
					if (finalJointVertex != null) {
						break;
					}
				}
			}
			if (finalJointVertex != null) {
				verticesToJoin.add(finalJointVertex);
			}
			listOfTreesToSearchForFinalJoint.clear();
		}
		if (verticesToJoin.size() < 2) {
			//there is no nedd to create splices of only one tree is present.
			verticesToJoin.clear();
		}

		if (createSharedSpliceForSharedNet && verticesToJoin.isEmpty()) {
			//if we have no joining shared vertices and we need to create a shared splice.
			//if we must confirm that there are at least one vertex so that at least one
			//splice will be created.
			//this will join the bothenddangling trees to rest of the forest.
			List<SchemGraphTree> listOfTreesToSearchForSharedSplice = new ArrayList<SchemGraphTree>();
			listOfTreesToSearchForSharedSplice.addAll(treesNoDanglingEdge);
			listOfTreesToSearchForSharedSplice.addAll(treesMultiDanglingEdge);
			listOfTreesToSearchForSharedSplice.addAll(treesSingleDanglingEdge);

			SchemGraphUtils.SchemVertex sharedSpliceJointVertex = null;
			for (SchemGraphTree tree : listOfTreesToSearchForSharedSplice) {
				sharedSpliceJointVertex = tree.getJoiningSpliceVertex();
				if (sharedSpliceJointVertex != null) {
					break;
				}
			}
			if (sharedSpliceJointVertex == null) {
				for (SchemGraphTree tree : listOfTreesToSearchForSharedSplice) {
					tree.getGraph()
							.split(tree.getDiagram().getGrid(), tree.getGraph().getEdges().iterator().next());
					tree.init();
					sharedSpliceJointVertex = tree.getJoiningSpliceVertex();
					if (sharedSpliceJointVertex != null) {
						break;
					}
				}
			}
			if (sharedSpliceJointVertex == null) {
				//probably there are only both end dangling edges. split the first edge of these trees.
				Iterator<SchemGraphTree> itr = treesBothEndDanglingEdge.iterator();
				if (itr.hasNext()) {
					SchemGraphTree tree = itr.next();
					sharedSpliceJointVertex = tree.getJoiningSpliceVertex();
				}
			}
			if (sharedSpliceJointVertex != null) {
				//this is must because if the vertex is on shared edge the splice will not be created.
				verticesToJoin.add(sharedSpliceJointVertex);
			}
		}

		Set<SchemGraphUtils.SchemEdge> edgesToProcess = new HashSet<SchemGraphUtils.SchemEdge>();
		for (SchemGraphTree tree : treesBothEndDanglingEdge) {
			edgesToProcess.addAll(tree.getJoiningEdges());
		}
		if (!edgesToProcess.isEmpty()) {
			//this will join the bothenddangling trees to rest of the forest.
			List<SchemGraphTree> listOfTreesToSearchForFinalEdge = new ArrayList<SchemGraphTree>();
			listOfTreesToSearchForFinalEdge.addAll(treesNoDanglingEdge);
			listOfTreesToSearchForFinalEdge.addAll(treesMultiDanglingEdge);
			listOfTreesToSearchForFinalEdge.addAll(treesSingleDanglingEdge);
			SchemGraphUtils.SchemEdge finalJointEdge = null;
			for (SchemGraphTree tree : listOfTreesToSearchForFinalEdge) {
				finalJointEdge = tree.getJoiningEdge(false);
				if (finalJointEdge != null) {
					break;
				}
			}
			if (finalJointEdge == null) {
				for (SchemGraphTree tree : listOfTreesToSearchForFinalEdge) {
					finalJointEdge = tree.getJoiningEdge(true);
					if (finalJointEdge != null) {
						break;
					}
				}
			}
			if (finalJointEdge != null) {
				edgesToProcess.add(finalJointEdge);
			}
		}

		treesNoDanglingEdge.clear();
		treesMultiDanglingEdge.clear();
		treesSingleDanglingEdge.clear();
		treesBothEndDanglingEdge.clear();

		createSharedWireConductor(design, netConductor, edgesToProcess);
		edgesToProcess.clear();

		//we may have cases where there is a vertex which is marked for potential splice but
		//now redundant for a splice. here we may create multiple wires at a joint without a splice.
		//to avoid this merge the edges at these vertices but retain the already created sharing wire
		//conductor if any.
		for (SchemGraphUtils.SchemGraph graph : graphs) {
			Set<SchemGraphUtils.SchemVertex> redundantVertices = new HashSet<SchemGraphUtils.SchemVertex>();
			for (SchemGraphUtils.SchemVertex vertex : graph.getVertices()) {
				if (vertex.needsSharingSplice() && !verticesToJoin.contains(vertex) &&
						vertex.isRedundantForEssentialJointSplice() && !vertex.needsEssentialJointSplice()) {
					redundantVertices.add(vertex);
				}
			}
			for (SchemGraphUtils.SchemVertex vertex : redundantVertices) {
				IWireConductor chosenWire = null;
				Set<IWireConductor> otherWires = new HashSet<IWireConductor>();
				Set<SchemGraphUtils.SchemEdge> candidates =
						new HashSet<SchemGraphUtils.SchemEdge>(vertex.getSortedEdges());
				for (SchemGraphUtils.SchemEdge edge : candidates) {
					IWireConductor wire = edge.getWireConductor();
					if (wire != null) {
						if (chosenWire == null) {
							chosenWire = wire;
						}
						else {
							otherWires.add(wire);
						}
					}
				}
				graph.mergeEdgesAtVertex(vertex);
				if (chosenWire != null) {
					for (IWireConductor otherWire : otherWires) {
						for (IAbstractPin otherWirePin : otherWire.getPins()) {
							chosenWire.addPin(otherWirePin);
						}
						otherWire.removeAllPins();
					}
					Set<SchemGraphUtils.SchemEdge> currentEdges = graph.getEdges();
					for (SchemGraphUtils.SchemEdge edge : candidates) {
						if (currentEdges.contains(edge)) {
							edge.setWireConductor(chosenWire);
						}
					}
				}
			}
		}

		//we must create wires first then splices. this is restriction because during connect splice the
		//the edges are also connected. so ther must be wires for edges before connecting them to splices.
		Set<SchemGraphUtils.SchemVertex> emptyVertexList = new HashSet<SchemGraphUtils.SchemVertex>();
		for (SchemGraphUtils.SchemGraph graph : graphs) {
			// For each SchemEdge create a IWireConductor, for each SchemVertex that needs a ISplice create one
			createConnectivity(design, netConductor, graph.getSortedEdges(), emptyVertexList, bAssignMC);
		}

		//clearout mark for shared splice. otherwise sometimes we are not getting splices created.
		for (SchemGraphUtils.SchemVertex vertex : verticesToJoin) {
			if (vertex.isSuitableForSharingSplice()) {
				vertex.setNeedsSharingSplice(false);
			}
		}
		createSharedConnectivitySplice(getConnectivity(design), netConductor, verticesToJoin);
		verticesToJoin.clear();
		Set<SchemGraphUtils.SchemEdge> emptyEdgeList = new HashSet<SchemGraphUtils.SchemEdge>();
		for (SchemGraphUtils.SchemGraph graph : graphs) {
			//clearout mark for shared splice. otherwise sometimes we are not getting splices created.
			for (SchemGraphUtils.SchemVertex vertex : graph.getVertices()) {
				if (vertex.isSuitableForSharingSplice()) {
					vertex.setNeedsSharingSplice(false);
				}
			}
			// For each SchemEdge create a IWireConductor, for each SchemVertex that needs a ISplice create one
			createConnectivity(design, netConductor, emptyEdgeList, graph.getSortedVertices(), bAssignMC);
		}

		createSharedSpliceForSharedNet = false;
		currentSharedSpliceForSharedNet = null;

		return graphs;
	}

	private void createSchematicConductor(IDesignContainer design, Set<IConductor> schemConductorsToDelete,
			List<SchemGraphUtils.SchemGraph> graphs)
	{
		for (SchemGraphUtils.SchemGraph graph : graphs) {
			ILogicSegmentContainer segContainer = graph.getSchemConductor();
			if (segContainer instanceof IConductor) {
				IConductor schemConductor = (IConductor) segContainer;
				ISchemDiagram parentDiagram = DiagramHelper.getDiagram(schemConductor);
				assert parentDiagram != null;
				if (parentDiagram != null) {
					// Create schem Splices and Wires as needed
					createSchematics(design, parentDiagram, schemConductor, graph.getSortedEdges(),
							graph.getSortedVertices(),
							schemConductorsToDelete);
				}
			}
		}
	}

	/**
	 * Creates connectivity for the given schematic graph edges and vertices.
	 * <p>
	 *
	 * @param design IDesign
	 * @param netConductor INetConductor to convert
	 * @param edges Set<SchemEdge> Edges - creates one IWireConductor per edge
	 * @param vertices List<SchemVertex> Vertices - creates ISplices for some vertices, may use one as return value
	 * @param bAssignMC - Added param "bAssignMC" to not do mutlticore assignment in case of multi-term/multi-edge nets
	 * part of MC
	 *
	 * @return SchemVertex Optional vertex to be used to accomodate a Shared Splice
	 */
	@Nullable private SchemGraphUtils.SchemVertex createConnectivity(@NotNull final IDesignContainer design,
			@NotNull final INetConductor netConductor,
			@NotNull final Set<SchemGraphUtils.SchemEdge> edges,
			@NotNull final Set<SchemGraphUtils.SchemVertex> vertices, boolean bAssignMC)
	{
		// For each SchemEdge create IWireConductor - MUST be done before creating Splices
		IConnectivity connectivity = getConnectivity(design);
		assert connectivity != null;
		for (SchemGraphUtils.SchemEdge edge : edges) {
			if (!edge.isSharedEdge()) {
				createWireConductor(edge, connectivity, netConductor, edges, bAssignMC);
			}
		}
		// For each SchemVertex that has no pin we create a Connectivity Splice and connect the pin to the correct Wires
		SchemGraphUtils.SchemVertex preferredVertexForSharedSplice = null;
		for (SchemGraphUtils.SchemVertex vertex : vertices) {
			if (vertex.isSharedVertex()) {
				//skip it. we won't create splice for them. they will be already created.
			}
			else if (vertex.needsSharingSplice()) {
				assert preferredVertexForSharedSplice == null;
				preferredVertexForSharedSplice = vertex;
			}
			else if (vertex.needsSplice()) {
				Set<SchemGraphUtils.SchemVertex> verticesToProcess = new HashSet<SchemGraphUtils.SchemVertex>();
				verticesToProcess.add(vertex);
				createSharedConnectivitySplice(connectivity, netConductor, verticesToProcess);
			}
		}
		return preferredVertexForSharedSplice;
	}

	private void createSharedConnectivitySplice(@NotNull final IConnectivity connectivity,
			@NotNull final INetConductor netConductor,
			@NotNull final Set<SchemGraphUtils.SchemVertex> verticesToProcess)
	{
		ISplice splice = null;
		IMulticore multicore = netConductor.getMulticore();
		IAssembly assembly = (multicore != null && !(multicore instanceof IOverbraid)) ? multicore.getAssembly() :
				netConductor.getAssembly();
		for (SchemGraphUtils.SchemVertex vertex : verticesToProcess) {
			if (splice == null) {
				//we will create the shared splice for first splice created during the processing of a net conductor.
				if (createSharedSpliceForSharedNet && currentSharedSpliceForSharedNet == null) {
					ISharedConductor sharedNetConductor = netConductor.getSharedConductor();
					if (sharedNetConductor != null) {
						IDesignContainer design = connectivity.getDesign();
						if (design != null) {
							splice = getSharedConnectivitySplice(design, netConductor);
							currentSharedSpliceForSharedNet = splice.getUID();
						}
					}
				}
				//if share splice is not created create non-shared splice now.
				if (splice == null) {
					splice = FactoryMgr.getCablePropertiedFactory()
							.createSplice(FactoryMgr.getCommonFactory().createUID());
					splice.addPin(FactoryMgr.getCablePropertiedFactory().createSplicePin(
							FactoryMgr.getCommonFactory().createUID()));
					connectivity.addSplice(splice);
					if (assembly != null) {
						assembly.addElement(splice);
					}
				}
			}
			vertex.connectSplice(splice);
			vertex.setSharedVertex(true);
		}
	}

	/**
	 * Copies Attributes and Properties from source to dest
	 * <p>
	 *
	 * @param source - Source object
	 * @param dest - Dest object
	 */
	private void copyAttributesAndProperties(IAttributePropertyProvider source, IAttributePropertyProvider dest)
	{
		AttributeUtils.copyDataModelAttributes(source, dest);
		copyDomainInfo(source, dest);
		// copyAllProperties params are DEST,SOURCE - but it populates it's map source,dest - grrrr
		PropertyCopier.copyAllProperties(dest, source);
		ModuleCodesHelper.copyFunctionalModuleCodes(source, dest);
	}

	private void copyDomainInfo(IAttributePropertyProvider source, IAttributePropertyProvider dest)
	{
		ISharedObject sourceObject = CommonUtils.cast(source, ISharedObject.class);
		ISharedObject targetObject = CommonUtils.cast(dest, ISharedObject.class);
		if (sourceObject != null && targetObject != null) {
			targetObject.addSharedDomains(sourceObject.getSharedDomains());
		}
	}

	/**
	 * Create schematic IConductor (for IWireConductors) and schem.IPinList (for ISplices). Uses the existing ISegments
	 * and INodes.
	 * <p>
	 *
	 * @param design IDesignContainer
	 * @param diagram IDiagram
	 * @param schemConductor IConductor source schematic
	 * @param edges Set<SchemEdge> Edges - creates one schem.IConductor per edge
	 * @param vertices List<SchemVertex> Vertices - creates schem splices for some vertices
	 * @param schemConductorsToDelete collects the schem conductors which will be deleted after full processing
	 */
	private void createSchematics(@NotNull final IDesignContainer design, @NotNull final ISchemDiagram diagram,
			@NotNull final IConductor schemConductor,
			@NotNull final Set<SchemGraphUtils.SchemEdge> edges,
			@NotNull final Set<SchemGraphUtils.SchemVertex> vertices,
			@NotNull final Collection<IConductor> schemConductorsToDelete)
	{
		IGrid grid = diagram.getGrid();
		IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(diagram);

		Set<IAbstractPin> terminations = getNetTerminations(schemConductor);

		// Create schem IPinLists for Splices
		Set<IPinList> schemSplices = new HashSet<IPinList>();
		IPinList schemSharedSplice = null;
		for (SchemGraphUtils.SchemVertex vertex : vertices) {
			ISplice connectivitySplice = vertex.getSplice();
			Set<ISegment> segments = vertex.getSegments();
			if (connectivitySplice != null && !segments.isEmpty() && connectivitySplice.getNumPins() == 1) {
				Point pos = vertex.getPoint();
				IPinList schemSplice =
						AddSpliceHelper.generateSplice(connectivitySplice, grid, null, pos.x, pos.y);
				diagram.addObject(schemSplice);
				IUIDObjectCollection<IPin> splicePins = schemSplice.getPins();
				//splices have only one pin.
				IPin splicePin = splicePins.iterator().next();
				for (ISegment seg : segments) {
					seg.connectPin(splicePin);
				}
				if (connectivitySplice.getSharedObject() == null) {
					schemSplices.add(schemSplice);
				}
				else {
					schemSharedSplice = schemSplice;
				}
				schemSplice.setHome(schemConductor.isHome());
				if (styleSet != null) {
					m_styledGraphicsHandler.recordSchemSpliceForStyling(schemSplice, styleSet);
				}
			}
		}

		// Create schem IConductors for Wires
		Set<IConductor> schemWireConductors = new HashSet<IConductor>();
		for (SchemGraphUtils.SchemEdge edge : edges) {
			IWireConductor wireConductor = edge.getWireConductor();
			Set<IBaseSegment> segments = edge.getActuals();
			if (wireConductor != null && !segments.isEmpty()) {
				// Reparent all segments from existing Schem Conductor to new Schem Conductor for wire
				IConductor schemNetConductor =
						CommonUtils.cast(segments.iterator().next().getParent(), IConductor.class);
				if (schemNetConductor != null) {
					schemConductorsToDelete.add(schemNetConductor);
					for (IBaseSegment segment : segments) {
						schemNetConductor.removeObject(segment);
					}
					IConductor schemWireConductor = FactoryMgr.getSchemFactory()
							.constructConductor(FactoryMgr.getCommonFactory().createUID(), wireConductor);
					for (IBaseSegment segment : segments) {
						segment.setParent(schemWireConductor);
						schemWireConductor.addObject(segment);
					}
					IAssembly assembly = schemConductor.getConnectivity().getAssembly();
					if (assembly != null) {
						assembly.addElement(wireConductor);
					}
					diagram.addObject(schemWireConductor);
					if (edge.isSharedEdge()) {
						updateEndLineStyles(schemWireConductor, diagram);
					}
					updateSchemConductor(schemWireConductor, wireConductor);
					if (styleSet != null) {
						schemWireConductor.applyStyle();
					}
					schemWireConductors.add(schemWireConductor);
				}
			}
		}

		Set<IPinList> schemSplicesIncludingShared = new HashSet<IPinList>();
		schemSplicesIncludingShared.addAll(schemSplices);
		if (schemSharedSplice != null) {
			schemSplicesIncludingShared.add(schemSharedSplice);
		}
		m_styledGraphicsHandler.recordTerminationsForStyling(terminations);
		INetsToWiresResultCollector outputResults = getOutputResultCollector();
		if (outputResults != null) {
			outputResults.addInfoUsingSchem(schemConductor.getConnectivity().getName(), design.getName(),
					terminations, schemWireConductors, schemSplicesIncludingShared);
		}
		outputNetToWiresMessage(design, schemConductor.getConnectivity().getName(), schemWireConductors, schemSplices);
		if (schemSharedSplice != null) {
			outputStatusMessage("Message.ConvertedToSharedSplice", indent(),
					link(design, schemSharedSplice, schemSharedSplice.getConnectivity().getName()));
		}
	}

	/**
	 * Outputs conversion message
	 * <p>
	 *
	 * @param design IDesign
	 * @param netName String Name of net (source)
	 * @param schemWireConductors Set<IConductor> schem conductors that were created from source net.
	 * @param schemSplices Set<IPinList> schem splices that were created from source net.
	 */
	protected void outputNetToWiresMessage(@NotNull final IDesignContainer design, @NotNull final String netName,
			@NotNull final Set<IConductor> schemWireConductors, @NotNull final Set<IPinList> schemSplices)
	{
		String wires = getSchemObjectLinks(design, schemWireConductors);
		if (wires != null) {
			outputStatusMessage("Message.ConvertedToWires", indent(), netName, wires);
		}
		String splices = getSchemObjectLinks(design, schemSplices);
		if (splices != null) {
			mIndent++;
			outputStatusMessage("Message.ConvertedToSplices", indent(), splices);
			mIndent--;
		}
	}

	protected void outputNetToSingleWireMessage(IDesignContainer design, String netName, IConductor schemWireConductor)
	{
		String wire = getSchemObjectLinks(design, Collections.singleton(schemWireConductor));
		if (wire != null) {
			outputStatusMessage("Message.ConvertedToWires", indent(), netName, wire);
		}
	}

	protected void outputNetToSingleWireMessage(IDesignContainer design, String netName, String wireName,
			IHighwaySchematic schemWireConductor)
	{
		String wire = getSchemObjectLinks(design, Collections.singleton(schemWireConductor), wireName);
		if (wire != null) {
			outputStatusMessage("Message.ConvertedToWires", indent(), netName, wire);
		}
	}

	@Nullable private <T extends IConnectivityRef> String getSchemObjectLinks(@NotNull final IDesignContainer design,
			@NotNull final Set<T> schemObjects)
	{
		return getSchemObjectLinks(design, schemObjects, "");
	}

	/**
	 * Gets the String which links to schematics objects of any IConnectivityRef derivative
	 * <p>
	 *
	 * @param design IDesign
	 * @param schemObjects Set<T extends IConnectivityRef> Objects to link to
	 * @param name Name of the wire
	 *
	 * @return String comma separated object links
	 */
	@Nullable private <T extends IConnectivityRef> String getSchemObjectLinks(@NotNull final IDesignContainer design,
			@NotNull final Set<T> schemObjects, final String name)
	{
		StringBuilder sb = new StringBuilder();
		List<T> sortedSchemObjects = new ArrayList<T>(schemObjects);
		Collections.sort(sortedSchemObjects, new NamedObjectComparator<T>()
		{
			protected String getString(T object)
			{
				return getEquivalentName(name, object);
			}
		});
		boolean first = true;
		for (T schemObject : sortedSchemObjects) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			sb.append(link(design, schemObject, getEquivalentName(name, schemObject)));
		}
		String s = sb.toString();
		return s.isEmpty() ? null : s;
	}

	private <T extends IConnectivityRef> String getEquivalentName(String name, T schemObject)
	{
		return name == null || name.isEmpty() ? schemObject.getConnectivity().getName() : name;
	}

	/**
	 * Sets connectivity and applies styling to a schem.IConductor
	 * <p>
	 *
	 * @param schemConductor Schem conductor to modify
	 * @param connectivityConductor Connectivity conductor to set on schem conductor
	 */
	private void updateSchemConductor(@NotNull final IConductor schemConductor,
			@NotNull final chs.cof.logical.cable.IConductor connectivityConductor)
	{
		// These strange looking calls reset the Attribute Text to point to connectivity conductor - cannot make it
		// correct on construction since cannot reparent the segments until we have created the schem conductor
		schemConductor.setConnectivity(null);
		schemConductor.setConnectivity(connectivityConductor);
//Fix for SP1504_dts0101125857[CH] 1425920195369:: Validation Failure Detected:   VALIDATION FAILURE: Non existent owner of the composite text null Source of failure
		for (ISegment segment : schemConductor.getSegmentsOfType(ISegment.class)) {
			for (ICompositeTextDecorationText attText : segment.getObjects(ICompositeTextDecorationText.class)) {
				/* delete leader lines before removing the decorative from the container*/
				DecorationUtils.deleteLeaderLines(attText);
				segment.removeObject(attText);
			}
		}
	}

	/**
	 * Sets connectivity and applies styling to a schem.IShieldBody
	 * <p>
	 *
	 * @param schemShieldBody Schem ShieldBody to modify
	 * @param connectivityShieldBody Connectivity ShieldBody to set on schem ShieldBody
	 * @param grid IGrid
	 * @param styleSet IPreferenceSet - source for styling prefs
	 */
	private void updateSchemShieldBody(@NotNull final IShieldBody schemShieldBody,
			@NotNull final chs.cof.logical.cable.IShieldBody connectivityShieldBody,
			@Nullable final IPreferenceSet styleSet)
	{
		// These strange looking calls reset the Attribute Text to point to connectivity conductor - cannot make it
		// correct on construction since cannot reparent the segments until we have created the schem conductor
		schemShieldBody.setConnectivity(null);
		schemShieldBody.setConnectivity(connectivityShieldBody);

		if (styleSet != null) {
			PreferenceSetHelper.applyStyleSet(schemShieldBody, styleSet, true);
		}
	}

	/**
	 * Gets (or creates) a ISplice - that is shared, creating the shared splice if necessary too.
	 * <p>
	 *
	 * @param design IDesign
	 * @param netConductor INetConductor Shared Net
	 *
	 * @return ISplice Shared splice for the given design and shared net
	 */
	@NotNull private ISplice getSharedConnectivitySplice(@NotNull final IDesignContainer design,
			@NotNull final INetConductor netConductor)
	{
		ISharedConductor sharedConductor = netConductor.getSharedConductor();
		IAssembly assembly = netConductor.getAssembly();
		IConnectivity connectivity = getConnectivity(design);
		assert connectivity != null;
//		assert sharedConductor != null;
		// Deal with Ported Conductors and Shared Conductors transparently
		IUID conductorUID = netConductor.getUID();
		IUID sharedConductorUID = sharedConductor != null ? sharedConductor.getUID() : conductorUID;
		IUID spliceID = mSharedConnectivitySplices.get(conductorUID);
		ISplice connectivitySplice = spliceID != null ? extractUIDObjectOfType(spliceID, ISplice.class) : null;
		if (connectivitySplice == null) {
			connectivitySplice =
					FactoryMgr.getCablePropertiedFactory().createSplice(FactoryMgr.getCommonFactory().createUID());
			connectivitySplice
					.addPin(FactoryMgr.getCablePropertiedFactory().createSplicePin(
							FactoryMgr.getCommonFactory().createUID()));
			connectivity.addSplice(connectivitySplice);

			if (assembly != null) {
				assembly.addElement(connectivitySplice);
			}

			ISharedPinList sharedSplice = mSharedSplices.get(sharedConductorUID);
			if (sharedSplice == null && netConductor.isShared()) {
				sharedSplice = FactoryMgr.getSharedFactory()
						.createSharedPinListForType(FactoryMgr.getCommonFactory().createUID(), connectivitySplice);
				sharedSplice.setType(PinListTypeEnum.TypeSplice);
				sharedSplice.setName(connectivitySplice.getName());

				ISharedPin sharedPin = FactoryMgr.getSharedFactory()
						.createSharedPinForOwner(FactoryMgr.getCommonFactory().createUID(), sharedSplice);
				IAbstractPin splicePin = connectivitySplice.getPin();
				assert splicePin != null;
				sharedPin.setName(splicePin.getName());
				sharedSplice.addPin(sharedPin);
				ISharedPinListMgr sharedPinListMgr = getProject().getSharedPinListMgr();
				sharedPinListMgr.addSharedPinList(sharedSplice);
				sharedSplice.save();
				getCommandHelper().postAuditTrailEvent(AuditableEventType.SHARED_OBJECT_ADDED, getProject().getUID(),
						sharedSplice, "");
				mSharedSplices.put(sharedConductorUID, sharedSplice);
			}
			prepareToShare(connectivitySplice, sharedSplice);
			connectivitySplice.setSharedPinList(sharedSplice);
			IAbstractPin splicePin = connectivitySplice.getPin();
			if (splicePin != null && sharedSplice != null && netConductor.isShared()) {
				ISharedPinIterator sharedPins = sharedSplice.getPins();
				assert sharedPins.getSize() == 1;
				if (sharedPins.getSize() > 0) {
					final ISharedPin sharedPin = sharedPins.next();
					prepareToShare(splicePin, sharedPin);
					splicePin.setSharedPin(sharedPin);
				}
			}
			mSharedConnectivitySplices.put(conductorUID, connectivitySplice.getUID());
		}
//		assert connectivitySplice.getSharedObject() != null;
		return connectivitySplice;
	}

	/**
	 * Returns all ISharedConductors instanced on the given design
	 * <p>
	 *
	 * @param design IDesign
	 *
	 * @return Set<ISharedConductor> All ISharedConductors instanced on design
	 */
	@NotNull private Set<ISharedConductor> getSharedNets(@NotNull final IDesign design)
	{
		IConnectivity connectivity = design.getConnectivity();
		assert connectivity != null;
		INetConductorIterator nets = connectivity.getNetConductors();
		Set<ISharedConductor> sharedNets = new LinkedHashSet<ISharedConductor>(nets.getSize(), 1.0f);
		for (INetConductor net : nets) {
			ISharedConductor sharedNet = net.getSharedConductor();
			if (sharedNet != null) {
				assert sharedNet.isNet();
				if (sharedNet.isNet()) {
					sharedNets.add(sharedNet);
				}
			}
		}
		return sharedNets;
	}
	protected boolean handleSharedNetsInSingleDesignScope(@NotNull Set<ISharedConductor> sharedNets)
	{
		if (sharedNets.isEmpty()) {
			return true;
		}
		outputStatusMessage("Message.SharedNetInSingleDesignScope");
		return false;
	}

	/**
	 * Gets a resource for an IMulticore or IOverbraid
	 * <p>
	 *
	 * @param partialName Partial resource name
	 * @param uidObject IMulticore or IOverbraid or INetConductor
	 *
	 * @return String full resource name
	 */
	@NotNull private String getResourceName(@NotNull final String partialName, @NotNull final IUIDObject uidObject)
	{
		if (uidObject instanceof IOverbraid || uidObject instanceof ISharedOverbraid) {
			return partialName + "Overbraid";
		}
		if (uidObject instanceof IMulticore || uidObject instanceof ISharedMulticore) {
			return partialName + "Multicore";
		}
		if (uidObject instanceof INetConductor || uidObject instanceof ISharedConductor) {
			return partialName + "Net";
		}
		return partialName;
	}

	/**
	 * @see ProjectTraverserCmd#unlockAndUnloadObjects()
	 */
	@Override protected void unlockAndUnloadObjects()
	{
		// Since we unlock designs followed by the Shared Object Managers we do not have to do this in a transaction
		super.unlockAndUnloadObjects(); // Unlocks and unloads Designs

		IProject project = getProject();
		// Should be ok to unlock the Shared Object Managers after unloading designs
		if (mSharedPinListMgrLockResult == LOCK_RESULT.ATTAINED_LOCK) {
			unlockObject(project.getSharedPinListMgr());
			mSharedPinListMgrLockResult = LOCK_RESULT.READ_ONLY_MODE; // So we know it's unlocked
		}
		if (mSharedConductorMgrLockResult == LOCK_RESULT.ATTAINED_LOCK) {
			unlockObject(project.getSharedConductorMgr());
			mSharedConductorMgrLockResult = LOCK_RESULT.READ_ONLY_MODE; // So we know it's unlocked
		}
	}

	/**
	 * Get the root shared multicore for the given cable.IConductor
	 * <p>
	 *
	 * @param conductor IConductor
	 *
	 * @return ISharedMulticore
	 */
	@Nullable private ISharedMulticore getSharedMulticore(@NotNull chs.cof.logical.cable.IConductor conductor)
	{
		IMulticore rootMulticore = conductor.getRootMulticore();
		if (rootMulticore != null) {
			return rootMulticore.getSharedMulticore();
		}
		return null;
	}

	/**
	 * Get the SharedNetsConnectivity for the given ISharedMulticore
	 * <p>
	 *
	 * @param sharedMulticore ISharedMulticore
	 *
	 * @return SharedNetsConnectivity
	 */
	@NotNull private SharedNetsConnectivity getSharedMulticoreConnectivity(@NotNull ISharedMulticore sharedMulticore)
	{
		SharedNetsConnectivity sharedNetsConnectivity = mSharedMulticores.get(sharedMulticore);
		if (sharedNetsConnectivity == null) {
			sharedNetsConnectivity = new SharedNetsConnectivity();
			mSharedMulticores.put(sharedMulticore, sharedNetsConnectivity);
		}
		return sharedNetsConnectivity;
	}

	/**
	 * Returns an HTML string of spaced based on mIndent * INDENT
	 * <p>
	 *
	 * @return String
	 */
	@NotNull private String indent()
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mIndent * INDENT; ++i) {
			sb.append("&nbsp;");
		}
		return sb.toString();
	}

	/**
	 * Gets IDesignWideUsageMgr from IDesignContainer if possible </p>
	 *
	 * @param design The design
	 *
	 * @return The DesignWideUsageMgr (DWUM) or null if for some badly designed reason the design is not a logic design
	 */
	@Nullable private IDesignWideUsageMgr getDWUM(@NotNull IDesignContainer design)
	{
		// TODO : change signatures here to expect ILogicDesign
		ILogicDesign logicDesign = CommonUtils.cast(design, ILogicDesign.class);
		return logicDesign != null ? logicDesign.getDesignWideUsageMgr() : null;
	}

	private Set<IConductor> getDWSchemConductors(IDesignContainer design, ILogicObject netConductor)
	{
		IDesignWideUsageMgr dwum = getDWUM(design);
		assert dwum != null;
		return CollectionUtils.getObjects(dwum.getRepresentations(netConductor), IConductor.class);
	}

	private Set<IHighwaySchematic> getDWSchemHighways(IDesignContainer design, INetConductor netConductor)
	{
		IDesignWideUsageMgr dwum = getDWUM(design);
		assert dwum != null;
		return CollectionUtils.getObjects(dwum.getRepresentations(netConductor), IHighwaySchematic.class);
	}

	protected final Map<IUID, ListMap<IUID, IUID>> getNets2ConvertedWiresIDs()
	{
		return mNetConductortoWireConductorsMap;
	}

	/**
	 * Gets IConnectivity from IDesignContainer if possible </p>
	 *
	 * @param design owner design
	 *
	 * @return IConnectivity
	 */
	@Nullable private IConnectivity getConnectivity(@NotNull final IDesignContainer design)
	{
		IDesign connectivtyDesign = CommonUtils.cast(design, IDesign.class);
		return connectivtyDesign != null ? connectivtyDesign.getConnectivity() : null;
	}

	/**
	 * Top level function to clone a given multicore
	 *
	 * @param rootMC - the multicore to clone
	 * @param nets2ConvertedWires - nets to converted wires information
	 * @param segGroupInfo - segment grouping information
	 *
	 * @return - list of newly created multicores
	 */
	private List<IMulticore> cloneMulticore(IMulticore rootMC,
			ListMap<IUID, IUID> nets2ConvertedWires,
			List<List<ISegment>> segGroupInfo)
	{
		if (segGroupInfo.isEmpty()) {
			return Collections.emptyList();
		}
		ConvertNetsToWiresCmdReplicator replicator = new ConvertNetsToWiresCmdReplicator(Replicator.COPY, false);
		List<IMulticore> newMCs = new ArrayList<IMulticore>();
		int grpNum = 0;

		//1. populate wire vs groupNum map
		Map<IWireConductor, Integer> wireGroupingMap = new HashMap<IWireConductor, Integer>();
		for (List<ISegment> group : segGroupInfo) {
			//don't bother about groups with one conductor..may be they can pair up with some other ungrouped conds
			if (group.size() > 1) {
				grpNum++;
				for (ISegment seg : group) {
					IWireConductor wire = (IWireConductor) seg.getConductor().getConnectivity();
					//different segments may be converted to same wire and if these two segments are in differnet groups, we need to consider only one
					if (!wireGroupingMap.containsKey(wire)) {
						wireGroupingMap.put(wire, grpNum);
					}
				}
			}
		}

		//2. process grouped conductors
		List<Bucket> buckets = new ArrayList<Bucket>();
		for (int i = 1; i <= grpNum; i++) {
			buckets.add(new Bucket());
		}
		Set<IUID> convertedNets = nets2ConvertedWires.keySet();
		UnGroupedNets ungroupedList = new UnGroupedNets(convertedNets);

		populateBuckets(nets2ConvertedWires, wireGroupingMap, buckets, ungroupedList);

		createMCsForGroupedConductors(rootMC, replicator, newMCs, buckets, convertedNets, ungroupedList);

		//3. process ungrouped conductors
		newMCs.addAll(createMCsForUnGroupedConductors(rootMC, replicator, ungroupedList));

		return newMCs;
	}

	/**
	 * Given a root multicore & the grouping information, create multicores
	 *
	 * @param rootMC - the root multicore which has to be replicated
	 * @param replicator - the replicator in which nets to converted wires info will be populated
	 * @param newMCs - this function will return the newly created multicores
	 * @param groupedBuckets -  the grouping information
	 * @param convertedNets - list of converted nets
	 * @param ungroupedList - the list which will be populated with ungrouped wires
	 */
	private void createMCsForGroupedConductors(IMulticore rootMC, ConvertNetsToWiresCmdReplicator replicator,
			List<IMulticore> newMCs,
			List<Bucket> groupedBuckets, Set<IUID> convertedNets,
			UnGroupedNets ungroupedList)
	{
		for (Bucket bucket : groupedBuckets) {
			if (!bucket.isEmpty()) {
				if (bucket.size() == 1) {
					for (Map.Entry<INetConductor, IWireConductor> entry : bucket.entrySet()) {
						INetConductor netcond = entry.getKey();
						IWireConductor wirecond = entry.getValue();
						ungroupedList.add(netcond.getUID(), wirecond.getUID());
					}
				}
				else {
					replicator.reset();
					for (IUID netUID : convertedNets) {
						INetConductor netcond = extractUIDObjectOfType(netUID, INetConductor.class);
						IWireConductor wirecond = bucket.get(netcond);
						if (wirecond == null) {
							wirecond = replicateNetAsUnplacedWire(netcond);
						}
						replicator.setNewObject(netcond.getUID(), wirecond);
					}
					newMCs.add(replicateMulticore(rootMC, replicator));
				}
			}
		}
	}

	/**
	 * Given the nets to converted wires information & grouping information of wires, populate the bucket information
	 * i.e. for each group, populate the net vs wire information. If a net is not part of any group, populate in
	 * ungropued list
	 *
	 * @param nets2ConvertedWires - map of net to converted wires
	 * @param wireGroupingMap - wire grouping information
	 * @param groupedBuckets - this will be populated based on above two information
	 * @param ungroupedList - this will be populated with list of ungrouped wires
	 */
	private void populateBuckets(ListMap<IUID, IUID> nets2ConvertedWires,
			Map<IWireConductor, Integer> wireGroupingMap, List<Bucket> groupedBuckets,
			UnGroupedNets ungroupedList)
	{
		for (Map.Entry<IUID, List<IUID>> entry : nets2ConvertedWires.entrySet()) {
			INetConductor net = extractUIDObjectOfType(entry.getKey(), INetConductor.class);
			List<IUID> wireList = entry.getValue();
			for (IUID wireUID : wireList) {
				IWireConductor wire = extractUIDObjectOfType(wireUID, IWireConductor.class);
				Integer groupNum = wireGroupingMap.get(wire);
				if (groupNum != null) {
					if (isDebugEnabled()) {
						System.out.println(
								"Wire: " + wire.getName() + " Net: " + net.getName() + " found in grouping " +
										groupNum);
					}
					groupedBuckets.get(groupNum - 1).put(net, wire);
				}
				else {
					if (isDebugEnabled()) {
						System.out.println(
								"Wire: " + wire.getName() + " Net: " + net.getName() + " NOT found in grouping");
					}
					ungroupedList.add(net.getUID(), wire.getUID());
				}
			}
		}
	}

	private boolean isDebugEnabled()
	{
		return BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled();
	}

	@Override public void preDeleteObject(@NotNull IUIDObject objectToDelete)
	{
		//collect only those uids which are of type of interest.
		//the types are those which are being used in extractUIDObjectOfType.
		//otherwise this set will be too huge to ignore. can we avoid instanceof check?
		if (objectToDelete instanceof chs.cof.logical.cable.IConductor ||
				objectToDelete instanceof IMulticore || objectToDelete instanceof ISplice) {
			mAllDeletedUids.add(objectToDelete.getUID());
		}
	}

	private static class UnGroupedNets extends ListMap<IUID, IUID>
	{

		UnGroupedNets(Set<IUID> nets)
		{
			for (IUID net : nets) {
				put(net, new ArrayList<IUID>());
			}
		}
	}

	@SuppressWarnings({"EmptyClass"})
	private static class Bucket extends HashMap<INetConductor, IWireConductor>
	{
		//do nothing
	}

	/**
	 * This function created new multicores for ungrouped conductors
	 *
	 * @param rootMC - the root multicore for which new multicores have to be created
	 * @param replicator - the replicator in which the net->wire information is populated
	 * @param ungroupedList - list of ungrouped nets
	 *
	 * @return - newly created list of multicores
	 */
	private List<IMulticore> createMCsForUnGroupedConductors(IMulticore rootMC,
			ConvertNetsToWiresCmdReplicator replicator,
			UnGroupedNets ungroupedList)
	{
		List<IMulticore> newMCs = new ArrayList<IMulticore>();
		int iMoreMCcnt = 0;
		Collection<List<IUID>> wires2 = ungroupedList.values();
		for (List<IUID> wireList : wires2) {
			if (iMoreMCcnt < wireList.size()) {
				iMoreMCcnt = wireList.size();
			}
		}
		if (isDebugEnabled()) {
			System.out.println("More multicores required: " + iMoreMCcnt);
		}
		for (IUID net : ungroupedList.keySet()) {
			List<IUID> wires = ungroupedList.get(net);
			int size = wires.size();
			for (int j = size; j < iMoreMCcnt; j++) {
				INetConductor netConductor = extractUIDObjectOfType(net, INetConductor.class);
				IWireConductor wireConductor = replicateNetAsUnplacedWire(netConductor);
				ungroupedList.add(net, wireConductor.getUID());
			}
		}
		for (int i = 0; i < iMoreMCcnt; i++) {
			replicator.reset();
			for (IUID net : ungroupedList.keySet()) {
				IWireConductor wire = extractUIDObjectOfType(ungroupedList.getList(net).get(i), IWireConductor.class);
				replicator.setNewObject(net, wire);
			}
			newMCs.add(replicateMulticore(rootMC, replicator));
		}
		return newMCs;
	}

	/**
	 * replicate a ungrouped net as unplaced wire
	 *
	 * @param netConductor - the net conductor to replicate
	 *
	 * @return newly created wire conductor
	 */
	private IWireConductor replicateNetAsUnplacedWire(INetConductor netConductor)
	{
		IWireConductor wireConductor =
				FactoryMgr.getCablePropertiedFactory().createWireConductor(FactoryMgr.getCommonFactory().createUID());
		copyAssembly(netConductor, wireConductor);
		assignLibrayPartAndCopyAttributesAndProperties(netConductor, wireConductor);
		mObjectUIDsForConstraints.add(wireConductor.getUID());
		Set<IGeneralHighway> highways = netConductor.getHighways();
		for (IGeneralHighway highway : highways) {
			highway.addConductor(wireConductor);
		}
		return wireConductor;
	}

	/**
	 * To replicate a given multicore
	 *
	 * @param orig - original multicore whose copy is required
	 * @param replicator - replicator to use
	 *
	 * @return new multicore
	 */
	@Nullable private IMulticore replicateMulticore(IMulticore orig, ConvertNetsToWiresCmdReplicator replicator)
	{
		IMulticore newMC;
		if (orig.isShared() && orig.getPartNumber() == null) {
			newMC = replicator.replicateShardMulticoredAsNonShared(orig, orig.getLogicDesign());
		}
		else {
			newMC = replicateNonSharedMulticore(orig, replicator);
		}
		addGeneratedMulticoreToResults(orig, newMC, replicator);
		return newMC;
	}

	/**
	 * replicate a non shared multicore as a non shared multicore
	 *
	 * @param orig - original multicore
	 * @param replicator - the replicator to use
	 *
	 * @return the replicated non shared multicore
	 */
	private IMulticore replicateNonSharedMulticore(IMulticore orig, ConvertNetsToWiresCmdReplicator replicator)
	{
		IMulticore newMC = replicator.replicateMulticoreOrOverbraid(orig, true, true, orig.getLogicDesign());
		addGeneratedMulticoreToResults(orig, newMC, replicator);
		for (IMulticore originalChildMc : orig.getMulticores()) {
			IMulticore childMc = replicateNonSharedMulticore(originalChildMc, replicator);
			newMC.addMulticore(childMc);
		}
		updateAssemblyDetails(orig, newMC, replicator);
		return newMC;
	}

	/**
	 * update assembly details
	 *
	 * @param originalMC - original multicore
	 * @param newMC - new multicore
	 * @param replicator - the replicator to use
	 */
	private void updateAssemblyDetails(IMulticore originalMC, IMulticore newMC, Replicator replicator)
	{
		copyAssembly(originalMC, newMC);
		if (originalMC instanceof IOverbraid) {
			for (chs.cof.logical.cable.IConductor cond : originalMC.getConductorsAsSet()) {
				chs.cof.logical.cable.IConductor newObject =
						replicator.getNewObject(cond.getUID(), chs.cof.logical.cable.IConductor.class);
				copyAssembly(cond, newObject);
			}
			if (originalMC.getShield() != null) {
				IShieldConductor shield = originalMC.getShield();
				IShieldConductor newObject = replicator.getNewObject(shield.getUID(), IShieldConductor.class);
				if (newObject != null) {
					copyAssembly(shield, newObject);
				}
			}
		}
	}

	private void copyAssembly(IAssembledObject sourceObj, IAssembledObject targetObj)
	{
		IAssembly assembly = sourceObj.getAssembly();
		if (assembly != null) {
			IAssembly oldAssembly = targetObj.getAssembly();
			if (oldAssembly != null) {
				oldAssembly.removeElement(targetObj);
			}
			assembly.addElement(targetObj);
		}
	}

	private void addGeneratedMulticoreToResults(IMulticore oldRootMC, IMulticore newRootMC, Replicator replicator)
	{
		assert replicator.getNewObject(oldRootMC.getUID()) == newRootMC;
		for (IMulticore oldMC : oldRootMC.getAllMulticoresInHierarchy()) {
			IMulticore newMC = (IMulticore) replicator.getNewObject(oldMC.getUID());
			if (newMC != null) {
				INetsToWiresResultCollector outputResults = getOutputResultCollector();
				if (outputResults != null) {
					outputResults.addGeneratedMulticore(oldMC, newMC);
				}
			}
		}
	}

	/**
	 * Class to record the connectivity of a ISharedConductor - that is how many pin connections it has
	 */
	private static class SharedNetsConnectivity
	{

		private Map<IUID, Integer> mSharedNetsPinCount = new HashMap<IUID, Integer>();
		private Map<IUID, Set<IUID>> mSharedNetsPins = new HashMap<IUID, Set<IUID>>();
		private boolean mValid = true;

		private SharedNetsConnectivity()
		{
		}

		private void invalidate(@NotNull final chs.cof.logical.cable.IConductor conductor)
		{
			ISharedConductor sharedCond = conductor.getSharedConductor();
			IUID uid = sharedCond != null ? sharedCond.getUID() : conductor.getUID();
			mSharedNetsPinCount.put(uid, -1);
		}

		private void invalidate()
		{
			mValid = false;
		}

		private boolean isValid(@NotNull final chs.cof.logical.cable.IConductor conductor)
		{
			ISharedConductor sharedCond = conductor.getSharedConductor();
			IUID uid = sharedCond != null ? sharedCond.getUID() : conductor.getUID();
			Integer value = mSharedNetsPinCount.get(uid);
			return value == null || (value != -1 && value <= 2);
		}

		private boolean isValid()
		{
			if (!mValid) {
				return false;
			}
//			for (int pinCount : mSharedNetsPinCount.values()) {
//				if (pinCount == -1 || pinCount > 2) {
//					return false;
//				}
//			}
			for (Set<IUID> pinset : mSharedNetsPins.values()) {
				if (pinset.size() > 2) {
					return false;
				}
			}
			return true;
		}

		private void deltaPinCount(@NotNull final chs.cof.logical.cable.IConductor conductor, int delta)
		{
			ISharedConductor sharedCond = conductor.getSharedConductor();
			IUID uid = sharedCond != null ? sharedCond.getUID() : conductor.getUID();
			if (!mSharedNetsPinCount.containsKey(uid)) {
				mSharedNetsPinCount.put(uid, delta);
			}
			else {
				int newValue = mSharedNetsPinCount.get(uid) + delta;
				mSharedNetsPinCount.put(uid, newValue);
			}
		}

		private void deltaPin(@NotNull final chs.cof.logical.cable.IConductor conductor, Set<IUID> pins)
		{
			ISharedConductor sharedCond = conductor.getSharedConductor();
			IUID uid = sharedCond != null ? sharedCond.getUID() : conductor.getUID();
			if (!mSharedNetsPins.containsKey(uid)) {
				mSharedNetsPins.put(uid, pins);
				mSharedNetsPinCount.put(uid, pins.size());
			}
			else {
				Set<IUID> pinset = mSharedNetsPins.get(uid);
				pinset.addAll(pins);
				mSharedNetsPins.put(uid, pinset);
				mSharedNetsPinCount.put(uid, pinset.size());
			}
		}
	}

	private static class SchemGraphTree
	{

		private enum TreeType
		{

			TREE_NO_DANGLING_EDGE, TREE_SINGLE_DANGLING_EDGE, TREE_MULTIPLE_DANGLING_EDGE, TREE_BOTH_END_DANGLING_EDGE
		}

		private TreeType m_type;
		private SchemGraphUtils.SchemGraph m_graph;
		private ISchemDiagram m_diagram;
		private List<SchemGraphUtils.SchemEdge> m_sortedClosedEdges;
		private List<SchemGraphUtils.SchemEdge> m_sortedOpenEdges;
		private List<SchemGraphUtils.SchemVertex> m_sortedVertices;

		private SchemGraphTree(final SchemGraphUtils.SchemGraph graph, final ISchemDiagram diagram)
		{
			m_graph = graph;
			m_diagram = diagram;
			init();
		}

		private void init()
		{
			m_sortedVertices =
					new SortedList<SchemGraphUtils.SchemVertex>(m_graph.getVertices(),
							SchemGraphUtils.SchemVertex.getSplicePositionComparator());
			m_sortedClosedEdges =
					new SortedList<SchemGraphUtils.SchemEdge>(SchemGraphUtils.SchemEdge.getEdgePositionComparator());
			m_sortedOpenEdges =
					new SortedList<SchemGraphUtils.SchemEdge>(SchemGraphUtils.SchemEdge.getEdgePositionComparator());
			for (SchemGraphUtils.SchemEdge edge : m_graph.getEdges()) {
				if (edge.isHavingDanglingEnd()) {
					m_sortedOpenEdges.add(edge);
				}
				else {
					m_sortedClosedEdges.add(edge);
				}
			}
			if (m_sortedOpenEdges.isEmpty()) {
				m_type = TreeType.TREE_NO_DANGLING_EDGE;
			}
			else if (m_sortedOpenEdges.size() == 1) {
				m_type = m_sortedOpenEdges.get(0).isHavingBothEndDangling() ? TreeType.TREE_BOTH_END_DANGLING_EDGE :
						TreeType.TREE_SINGLE_DANGLING_EDGE;
			}
			else {
				m_type = TreeType.TREE_MULTIPLE_DANGLING_EDGE;
			}
		}

		/**
		 * Returns a Comparator for sorting SchemGraphTree by position, for selecting SchemGraphTree for shared wires
		 *
		 * @return Comparator<SchemGraphTree>
		 */
		private static Comparator<SchemGraphTree> getGraphTreePositionComparator()
		{
			return new Comparator<SchemGraphTree>()
			{
				public int compare(SchemGraphTree o1, SchemGraphTree o2)
				{
					String diagramName1 = o1.getDiagram().getName();
					String diagramName2 = o2.getDiagram().getName();
					int nameResult = diagramName1.compareToIgnoreCase(diagramName2);
					if (nameResult < 0) {
						return -1;
					}
					else if (nameResult > 0) {
						return 1;
					}
					Set<SchemGraphUtils.SchemEdge> listEdges1 = o1.getJoiningEdges();
					Set<SchemGraphUtils.SchemEdge> listEdges2 = o2.getJoiningEdges();
					if (listEdges1.size() < listEdges2.size()) {
						return -1;
					}
					if (listEdges1.size() > listEdges2.size()) {
						return 1;
					}
					Comparator<SchemGraphUtils.SchemEdge> edgePositionComparator =
							SchemGraphUtils.SchemEdge.getEdgePositionComparator();
					List<SchemGraphUtils.SchemEdge> list1 =
							new SortedList<SchemGraphUtils.SchemEdge>(listEdges1, edgePositionComparator);
					List<SchemGraphUtils.SchemEdge> list2 =
							new SortedList<SchemGraphUtils.SchemEdge>(listEdges2, edgePositionComparator);
					Iterator<SchemGraphUtils.SchemEdge> itr2 = list2.iterator();
					for (SchemGraphUtils.SchemEdge edge1 : list1) {
						SchemGraphUtils.SchemEdge edge2 = itr2.next();
						int result = SchemGraphUtils.SchemEdge.compareSchemEdge(edge1, edge2);
						if (result < 0) {
							return -1;
						}
						else if (result > 0) {
							return 1;
						}
					}
					return 0;
				}
			};
		}

		@Nullable private SchemGraphUtils.SchemVertex getJoiningSpliceVertex()
		{
			//prefer dangling ends first.
			for (SchemGraphUtils.SchemVertex vertex : m_sortedVertices) {
				if (!vertex.isSharedVertex() && vertex.isDanglingEnd()) {
					return vertex;
				}
			}
			for (SchemGraphUtils.SchemVertex vertex : m_sortedVertices) {
				if (!vertex.isSharedVertex() && vertex.isSuitableForSharingSplice()) {
					return vertex;
				}
			}
			return null;
		}

		@Nullable private SchemGraphUtils.SchemEdge getFrontDanglingEdge()
		{
			return !m_sortedOpenEdges.isEmpty() ? m_sortedOpenEdges.get(0) : null;
		}

		@Nullable private SchemGraphUtils.SchemEdge getBackDanglingEdge()
		{
			return m_sortedOpenEdges.size() > 1 ? m_sortedOpenEdges.get(1) : getFrontDanglingEdge();
		}

		@Nullable private SchemGraphUtils.SchemEdge getJoiningEdge(boolean sharedAlso)
		{
			for (SchemGraphUtils.SchemEdge edge : m_sortedOpenEdges) {
				if (sharedAlso || !edge.isSharedEdge()) {
					return edge;
				}
			}
			for (SchemGraphUtils.SchemEdge edge : m_sortedClosedEdges) {
				if (sharedAlso || !edge.isSharedEdge()) {
					return edge;
				}
			}
			return null;
		}

		private Set<SchemGraphUtils.SchemEdge> getJoiningEdges()
		{
			Set<SchemGraphUtils.SchemEdge> edges = new LinkedHashSet<SchemGraphUtils.SchemEdge>();
			edges.addAll(m_sortedClosedEdges);
			edges.addAll(m_sortedOpenEdges);
			return Collections.unmodifiableSet(edges);
		}

		private Set<SchemGraphUtils.SchemEdge> getOpenJoiningEdges()
		{
			Set<SchemGraphUtils.SchemEdge> edges = new LinkedHashSet<SchemGraphUtils.SchemEdge>();
			edges.addAll(m_sortedOpenEdges);
			return Collections.unmodifiableSet(edges);
		}

		private TreeType getType()
		{
			return m_type;
		}

		private SchemGraphUtils.SchemGraph getGraph()
		{
			return m_graph;
		}

		private ISchemDiagram getDiagram()
		{
			return m_diagram;
		}
	}

	public interface INetsToWiresResultCollector
	{

		/**
		 * Get the source shield for the specified generated shield conductor
		 *
		 * @param generatedShield generated shield conductor
		 *
		 * @return source shield corresponding to the specified shield
		 */
		@Nullable IShieldConductor getSourceShield(@NotNull IShieldConductor generatedShield);

		void addIgnoredShield(@NotNull IAbstractPin pin, @NotNull IShieldConductor sourceShield);

		void addGeneratedMulticore(@NotNull IMulticore source, @NotNull IMulticore generated);

		void addInfoUsingSchem(@NotNull String netName, String designName, @NotNull Set<IAbstractPin> terminations,
				@NotNull Set<IConductor> schemWires, @NotNull final Set<IPinList> schemSplices);

		void addInfoUsingConnectivity(@NotNull String netName, String designName,
				@NotNull Set<IAbstractPin> terminations, @NotNull Set<IWireConductor> wires,
				@NotNull Set<ISplice> splices);
	}

	@NotNull private static Set<IAbstractPin> getNetTerminations(@NotNull IConductor schemNet)
	{
		Set<IAbstractPin> terminations = new HashSet<IAbstractPin>();
		for (IPin schemPin : schemNet.getPins()) {
			terminations.add(schemPin.getConnectivity());
		}
		return terminations;
	}

	private static class NTWStylingHandler extends LogicUpdateStyledGraphicsHandler
	{

		private IChangedObjectsInfo m_changedObjectsInfo = IChangedObjectsInfo.EMPTY;
		private Set<IAbstractPin> m_terminations = new HashSet<IAbstractPin>();
		private Map<chs.cof.logical.schem.IPinList, IPreferenceSet> m_createdSchemSplices = new HashMap<chs.cof.logical.schem.IPinList, IPreferenceSet>();

		public void recordTerminationsForStyling(@NotNull Set<IAbstractPin> terminations)
		{
			m_terminations.addAll(terminations);
		}

		public void recordSchemSpliceForStyling(@NotNull chs.cof.logical.schem.IPinList schemSplice,
				@NotNull IPreferenceSet styleSet)
		{
			m_createdSchemSplices.put(schemSplice, styleSet);
		}

		/**
		 * Applies style to splices created during conversion, deferred until net conductors are
		 * fully removed from connectivity. If applied while net conductors still exist, the style
		 * rule evaluates the splice as net-connected and applies the wrong style.
		 */
		public void applyDeferredSpliceStyles()
		{
			for (Map.Entry<chs.cof.logical.schem.IPinList, IPreferenceSet> entry : m_createdSchemSplices.entrySet()) {
				PreferenceSetHelper.applyStyleSet(entry.getKey().getObjectsForStyling(), entry.getValue(), true);
			}
			m_createdSchemSplices.clear();
		}

		public void updateStyle(@NotNull IDesignContainer logicDesign)
		{
			updateStyledGraphics(logicDesign, m_changedObjectsInfo);
			Set<IStyleableObject> styleableObjects =
					CollectionUtils.getObjects(m_changedObjectsInfo.getUpdatedObjects(), IStyleableObject.class);
			for (IStyleableObject styleableObject : styleableObjects) {
				styleableObject.refreshStyle();
			}
		}

		public void updateChangedObjectInfo(CreationDeletionHelper deletionHelper)
		{
			IChangedObjectsInfo changedObjectsInfo = IChangedObjectsInfo.EMPTY;
			if (!deletionHelper.isTemporary()) {
				Set<IUIDObject> newObjects = new HashSet<IUIDObject>();
				Set<IUIDObject> delObjects = new HashSet<IUIDObject>();
				Set<IUIDObject> updatedObjects = new HashSet<IUIDObject>();

				CollectionUtils.add(deletionHelper.getNewObjectsToProcess(), newObjects);
				newObjects.removeAll(m_changedObjectsInfo.getNewObjects());

				for (IUIDObject newObject : newObjects) {
					if (newObject instanceof IConductor) {
						IConductor schemCond = (IConductor) newObject;
						updatedObjects.addAll(schemCond.getPins());
					}
				}

				deletionHelper.processOnDeletionObjects(delList -> {
					CollectionUtils.add(delList, delObjects);
					return Void.TYPE;
				});
				delObjects.removeAll(m_changedObjectsInfo.getDeletedObjects());

				Set<INetConductor> deletedNetConductors = CollectionUtils.getObjects(delObjects, INetConductor.class);
				for (INetConductor netConductor : deletedNetConductors) {
					IMulticore mc = netConductor.getMulticore();
					if (mc != null && !delObjects.contains(mc)) {
						updatedObjects.add(mc);
					}
				}

				Set<IPhysicalConductor> newPhysicalConds =
						CollectionUtils.getObjects(newObjects, IPhysicalConductor.class);

				// ideally updated highways will be the net conductor highways. But by the time the control reaches here they get updated.
				// hence we will check whether new objects are part of highways
				for (IPhysicalConductor newPhysicalCond : newPhysicalConds) {
					IMulticore mc = newPhysicalCond.getMulticore();
					if (mc != null && !newObjects.contains(mc)) {
						updatedObjects.add(mc);
					}
					if (newPhysicalCond instanceof IHighwayConductor) {
						IHighwayConductor highwayConductor = (IHighwayConductor) newPhysicalCond;
						// as highways will not be created by nets to wires action, there is no need to check in newobjects
						updatedObjects.addAll(highwayConductor.getHighways());
						updatedObjects.addAll(HighwayHelper.getStackedHighways(highwayConductor));
					}
					IAssembly assembly = newPhysicalCond.getAssembly();
					if (assembly != null) {
						updatedObjects.add(assembly);
					}
				}

				updatedObjects.addAll(m_terminations);

				changedObjectsInfo = new ChangedObjectsHolder(newObjects, updatedObjects, delObjects);
			}
			m_changedObjectsInfo = changedObjectsInfo;
			m_terminations.clear();
		}
	}

	private boolean hasValidDomainAccess(@Nullable ISharedObject object)
	{
		if (object != null) {
			return object.isAccesible();
		}
		return true;
	}

	// This is done to avoid db invocation for individual designs, instead batch variant
	// DomainAccessibilityAuthenticator.doesDesignsContainInAccessibleSharedObjects is used on set of locked designs in checkPreconditions method
	@Override protected boolean hasValidDomainAccess(IDesignContainer design)
	{
		return true;
	}

	@NotNull protected IProjectTraverserTransactionHandler beginTransactionForDesign(
			@NotNull IDesignContainer designContainer,
			@NotNull ICommandHelper commandHelper)
	{
		return new ProjectTraverserDesignTransactionHandler(designContainer, commandHelper, this);
	}

	@NotNull protected Map<ISharedMulticore, SharedNetsConnectivity> getSharedMulticores()
	{
		return mSharedMulticores;
	}

	protected boolean shouldExitSharedMulticoreConversion()
	{
		return false;
	}

	protected void outputStatusMessage(@NotNull String message, @NotNull String... resourceParameters)
	{
		getCommandListener().handleEvent(ResourceCommandEvent.create(message, resourceParameters));
	}
}