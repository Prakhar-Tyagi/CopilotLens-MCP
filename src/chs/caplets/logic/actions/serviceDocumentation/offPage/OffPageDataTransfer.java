package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.caf.caplet.helpers.replication.IDataTransferReplicator;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.logic.LogicDataTransfer;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.ISupplementaryObject;
import chs.cof.logical.IPinFilter;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.IChainSegmentContainer;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.common.IExtent;
import chs.common.IObjectFilter;
import chs.common.IUIDObject;
import chs.services.gfx.GfxView;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageReporterWithContext;
import chs.utility.Replicator;
import chs.view.memory.DiagramSpaceTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class OffPageDataTransfer extends LogicDataTransfer
{

	private static final int EXTRAGRIDSPACE = 3;
	private int m_prevLeftOffSet = 0;
	private int m_prevBottomOffSet = 0;
	private int m_extraSpacing = 0;
	@NotNull private final Collection<IUIDObject> copiedContent = new HashSet<>();
	@Nullable private IMessageReporterWithContext m_messageReporter;
	private ISchemDiagram m_sourceDiagram;
	private Replicator m_Replicator;
	private IPinFilter m_pinFilter;

	OffPageDataTransfer()
	{
		this(IPinFilter.getDefaultFilter());
	}

	OffPageDataTransfer(IPinFilter pinFilter)
	{
		m_pinFilter = pinFilter;
		setPreserveObjectNames(true);
		ISchemDiagram targetDiagram = CommonUtils.cast(CAFUtils.getInstance().getActiveDiagram(), ISchemDiagram.class);
		if (targetDiagram != null) {
			IExtent diagramExtent = DiagramSpaceTracker.getDiagramNoBorderExtent(targetDiagram);
			m_prevLeftOffSet += diagramExtent.getRight();
			Collection<IUIDObject> selectedObjects = Collections.emptySet();
			ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
			if (controller != null) {
				selectedObjects = controller.getSelectMgr().getPreSelections().getSelectedObjects(IUIDObject.class);
			}
			if (selectedObjects.isEmpty()) {
				m_prevBottomOffSet += diagramExtent.getCenterY();
			}
			else {
				IExtent selectedObjsExtent = calculateSelectedObjsExtent(selectedObjects);
				m_prevBottomOffSet += selectedObjsExtent.getCenterY();
			}
			setExtraSpacing(targetDiagram.gridSpacing() * EXTRAGRIDSPACE);
		}
	}

	protected IPinList replicate(IPinList origPL, Replicator replicator, boolean createConnectivity,
			IObjectFilter<IGfxObject> gfxFilter)
	{
		return replicator.replicate(origPL, 1.0, createConnectivity, gfxFilter, null,
				m_pinFilter);
	}

	@NotNull protected Replicator constructReplicator(int initialCapacity)
	{
		return new Replicator(Replicator.COPY, true, initialCapacity)
		{
			protected IGenericPin replicate(IGenericPin pin, SymbolPinReplicationType replicationType)
			{
				return super.replicate(pin, replicationType);
			}

			protected IAbstractPin replicate(IAbstractPin pin)
			{
				return super.replicate(pin);
			}
		};
	}

	@Override protected void setupReplicator(@NotNull Replicator rep)
	{
		super.setupReplicator(rep);
		rep.setAllowPartialMulticoreReplication(true);
		m_Replicator = rep;
	}

	@Nullable Replicator getReplicator()
	{
		return m_Replicator;
	}

	@NotNull
	Collection<IUIDObject> getCopiedContent()
	{
		return new HashSet<>(copiedContent);
	}

	@NotNull Map<ILogicObject, ILogicObject> getNewVsOldConnectivity()
	{
		Map<ILogicObject, ILogicObject> newVsOldConnectivity = new HashMap<>(m_newVsOriginalConnectivity);
		for (ILogicObject newConnectivityObj : m_newVsOriginalConnectivity.keySet()) {
			if (newConnectivityObj instanceof IGeneralHighway) {
				IConductorIterator iterator = ((IGeneralHighway) newConnectivityObj).getStackPinConductors();
				while (iterator.hasNext()) {
					chs.cof.logical.cable.IConductor newCond = iterator.next();
					chs.cof.logical.cable.IConductor oldCond =
							m_Replicator.getOldObject(newCond, chs.cof.logical.cable.IConductor.class);
					if (oldCond != null) {
						newVsOldConnectivity.put(newCond, oldCond);
					}
				}
			}
		}
		return newVsOldConnectivity;
	}

	private void setExtraSpacing(int extraSpacing)
	{
		m_extraSpacing = extraSpacing;
	}

	public boolean transfer(@NotNull Set<IDiagramObject> diagramObjects, @NotNull ISchemDiagram sourceDiagram,
			@Nullable IMessageReporterWithContext messageReporter)
	{
		m_sourceDiagram = sourceDiagram;
		setSourceDesign(sourceDiagram.getDesignContainer());
		m_messageReporter = messageReporter;
		SelectSet selectSet = new SelectSet();
		selectSet.add(diagramObjects, false);
		boolean success = copy(selectSet);
		if (!success) {
			return false;
		}
		ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
		success = paste(controller);
		if (!success) {
			return false;
		}
		copiedContent.addAll(m_objectBuffer);
		return true;
	}

	protected boolean paste(ICapletController controller)
	{
		calculatePasteCenterForCopiedContent();
		return doCAFPaste(controller);
	}

	private void calculatePasteCenterForCopiedContent()
	{
		IExtent copiedContentExtent = calculateSelectedObjsExtent(m_objectBuffer);
		int leftOffSet = m_prevLeftOffSet + m_extraSpacing + (copiedContentExtent.getWidth() / 2);
		int bottomOffSet = m_prevBottomOffSet;
		m_PrevPoint = new Point(leftOffSet, bottomOffSet);
		m_prevLeftOffSet += m_extraSpacing + copiedContentExtent.getWidth();
	}

	@Override
	@NotNull protected Point getOffset(GfxView gview)
	{
		Point delta = new Point();
		if (m_sourcePoint != null && m_PrevPoint != null) {
			IGrid grid = gview.getGridConfig().getGrid();
			delta.setLocation(grid.snap(m_PrevPoint.x - m_sourcePoint.x),
					grid.snap(m_PrevPoint.y - m_sourcePoint.y));
		}
		else {
			delta = super.getOffset(gview);
		}
		return delta;
	}

	private boolean copy(SelectSet selectSet)
	{
		//LOGIC-10223 : foreign design content can get added to undo queue while replicating
		CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
		boolean copySuccess;
		try {
			initCAFCopy();
			copySuccess = doCAFCopy(null, selectSet);
		}
		finally {
			CAFUtils.getInstance().clearTempUndoableContainer();
		}
		return copySuccess;
	}

	@Override protected void performPostCopy(IDataTransferReplicator rep)
	{
		super.performPostCopy(rep);
		m_objectBuffer.stream()
				.filter(ISupplementaryObject.class::isInstance)
				.map(ISupplementaryObject.class::cast)
				.forEach(ISupplementaryObject::markAsSupplementary);
	}

	@Override protected void expandSelection(SelectSet selSet)
	{
		SelectSet expandSet = new SelectSet();
		Set<IUIDObject> visited = new HashSet<>();
		for (SelectionIterator it = selSet.getSelected(); it.hasNext(); ) {
			Selection sel = it.getNext();
			Class<?> selClass = sel.getSelectionClass();
			if (IConductor.class.isAssignableFrom(selClass)) {
				IConductor schemConductor = (IConductor) sel.getObject();
				expandSet.add(sel, false);
				chs.cof.logical.cable.IConductor conductor = schemConductor.getConnectivity();
				if (conductor != null) {
					for (IMulticore mc = conductor.getMulticore(); mc != null; mc = mc.getParent()) {
						if (!visited.contains(mc)) {
							visited.add(mc);
							expandSelectionForMulticore(mc, expandSet);
						}
					}
				}
			}
			else if (IHighwaySchematic.class.isAssignableFrom(selClass)) {
				expandSet.add(sel, false);
			}
		}
		visited.clear();
		m_selectExpander.propagate(expandSet, getSourceDiagram(), Collections.singleton(IShieldBody.class));
		selSet.add(expandSet);
		super.expandSelection(selSet);
	}

	@Override protected ISchemDiagram getSourceDiagram()
	{
		return m_sourceDiagram != null ? m_sourceDiagram : super.getSourceDiagram();
	}

	private void expandSelectionForMulticore(@NotNull IMulticore mc, @NotNull SelectSet selSet)
	{
		chs.cof.logical.cable.IShieldBody shieldBody = mc.getShieldBody();
		if (shieldBody != null) {
			for (IDiagramObject diagramObject : m_sourceDiagram
					.getRepresentations(mc.getShieldBody().getUID())) {
				if (diagramObject instanceof IShieldBody) {
					IShieldBody schemShieldBody = (IShieldBody) diagramObject;
					selSet.add(schemShieldBody, false);
					for (IShieldBodyHookup hookup : schemShieldBody.getShieldBodyHookups()) {
						for (IChainSegmentContainer chainSegmentContainer : hookup.getShieldChains()) {
							selSet.add(chainSegmentContainer, false);
						}
					}
				}
			}
		}
	}

	@Override protected void updatePasteActionUI()
	{

	}

	@Override protected void buildModularConnectorAssociations(Replicator replicator, SelectSet selSet)
	{
		//we need to pull modular connectors individually to be able share into shared modular connector
		if (getSourceDesign() != CAFUtils.getInstance().getActiveDesignContainer()) {
			return;
		}
		super.buildModularConnectorAssociations(replicator, selSet);
	}

	@Override protected void updateSelections(ICapletController controller)
	{

	}

	@Override protected void displayRestrictedSharedPinPasteWarning()
	{
		if (m_messageReporter != null) {
			String msg = getDuplicatePinWarningMessage();
			m_messageReporter.report(PromptSeverity.ERROR, msg);
		}
	}

	@Override protected String getDuplicatePinWarningMessage()
	{
		return ResourceMgr.getString(FetchOffPageContentHelper.class,
				"FetchOffPageContentHelper.restricitedpinpaste.dupPinWarningMessage.text");
	}

	@Override protected void clearCurrentEdit()
	{

	}
}
