/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2025 Siemens
 */
package chs.caplets.logic.analysis;

import chs.analysis.AnalysisColoringServices;
import chs.analysis.AnalysisServices;
import chs.analysis.GraphServices;
import chs.analysis.IAnalysisColoringProcessor;
import chs.analysis.IAnalysisNetlistScope;
import chs.analysis.scope.AnalysisNetlistScopeColoringProcessor;
import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ICapletWindow;
import chs.caf.helpers.GfxViewHelper;
import chs.caplets.analysis.AbstractAnalysisColoringProcessor;
import chs.caplets.logic.Model;
import chs.cof.draw.IColor;
import chs.cof.draw.IGfxAttribute;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGrid;
import chs.cof.draw.ISheet;
import chs.cof.draw.LineStyle;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IConnected;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.drawplus.IGfxView;
import chs.cof.drawplus.IJoint;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.common.IDesignContainer;
import chs.common.IUID;
import chs.common.IUIDIterator;
import chs.common.IUIDObject;
import chs.common.IUIDObjectCollection;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxMediator;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.Environment;
import chs.utilities.Pair;
import chs.utilities.StringUtils;
import chs.utility.AnalysisHelper;
import chs.utility.DiagramHelper;
import chs.utility.gfx.IDrawingComponentOwner;
import chs.utility.gfx.IViewInvalidationEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coloring importer object
 *
 * @version 1.0 Created on 05 March 2004
 */
public class LogicAnalysisColoringProcessor extends AbstractAnalysisColoringProcessor
{

	// ///////// //
	// Constants //
	// ///////// //

	private static final double GRAPHIC_ARROW_GRID_PERCENT = 0.5;
	private static final double DEFAULT_ARROW_LENGTH = 1000.0d;

	// /////////////// //
	// Class variables //
	// /////////////// //

	protected static Map<IUID, String> blockStates = null;
	/**
	 * The map of design uids to the appropriate coloring processor
	 */
	private static Map<String, LogicAnalysisColoringProcessor> processorMap = null;
	private static final String ACTIVE = "active";

	//Map that stores only centerstrip wire's active/color value (in specific use case)
	//allocating less size for map as the use case possibility is rare
	private Map<IUID, String> centerstripWireColorMap = new HashMap<>(10);

	// ///////////// //
	// Class methods //
	// ///////////// //

	/**
	 * This method gets the appropriate coloring processor for the given model.
	 *
	 * @param model, the model for which coloring is required.
	 *
	 * @return the processor
	 */
	public static synchronized LogicAnalysisColoringProcessor getProcessor(Model model)
	{
		registerModel(model);
		return processorMap.get(model.getDesign().getUID().getString());
	}

	/**
	 * This method returns the appropriate coloring processor for the given uid.
	 *
	 * @param uid, the uid of the design for which coloring is required.
	 *
	 * @return the processor
	 */
	@Nullable public static synchronized LogicAnalysisColoringProcessor getProcessor(String uid)
	{
		if (processorMap != null) {
			return processorMap.get(uid);
		}
		return null;
	}

	/**
	 * This method registers a model with the processor in preparation for the processor being required to color it.
	 * This allows models to register when they are created to ensure none are 'missed'
	 *
	 * @param model, the model to register
	 */
	public static synchronized void registerModel(Model model)
	{
		if (processorMap == null) {
			processorMap = new HashMap<String, LogicAnalysisColoringProcessor>();
		}

		String uid = model.getDesign().getUID().toString();

		LogicAnalysisColoringProcessor processor = processorMap.get(uid);

		if (processor == null) {
			processor = new LogicAnalysisColoringProcessor();
			processorMap.put(uid, processor);
		}

		processor.addModel(model);
	}

	/**
	 * Unregister a model that has been registered as above. This is done when the Model is destroyed to avoid memory
	 * leaks
	 *
	 * @param model The model to unregister
	 */
	public static synchronized void unregisterModel(Model model)
	{
		if (processorMap != null) {
			String uid = model.getDesign().getUID().toString();
			LogicAnalysisColoringProcessor processor = processorMap.get(uid);

			if (processor != null) {
				processor.removeModel(model);

				if (!processor.hasModel()) {
					processorMap.remove(uid);
				}
			}
		}
	}

	// ////////////////// //
	// Instance variables //
	// ////////////////// //

	private double arrowLength = DEFAULT_ARROW_LENGTH; // default to be recomputed according to grid size
	private final Map<IDiagramObject, String> objectColorMap;

	/**
	 * The list of models that we will need to color. For one design we may have multiple diagrams open that we should
	 * process.
	 * <p>
	 * Hence this list is processed as the coloring is done.
	 */
	private final Set<Model> modelList;

	/**
	 * The dynamic graphics service
	 */
	private IDynamicGfxService dgs;

	/**
	 * The current diagram being processed
	 */
	//protected ISchemDiagram diagram ;
	protected LogicAnalysisColoringProcessor()
	{
		resetBlockStates();
		modelList = new LinkedHashSet<Model>();
		objectColorMap = new HashMap<IDiagramObject, String>();
	}

	public static void resetBlockStates()
	{
		if (blockStates == null) {
			blockStates = new HashMap<IUID, String>();
		}
		else {
			blockStates.clear();
		}
	}

	// ///////////// //
	// Other methods //
	// ///////////// //

	/**
	 * This method adds a model to the model list if it is not already present
	 *
	 * @param model, the model to add to those to be colored
	 */
	protected void addModel(Model model)
	{
		synchronized (modelList) {
			modelList.add(model);
		}
	}

	/**
	 * This method removes a model from the model list, if it is present
	 *
	 * @param model, the model to add to those to be colored
	 */
	protected void removeModel(Model model)
	{
		synchronized (modelList) {
			modelList.remove(model);
		}
	}

	protected boolean hasModel()
	{
		synchronized (modelList) {
			return !modelList.isEmpty();
		}
	}

	// ////////////////////////////////// //
	// IAnalysisColoringProcessor methods //
	// ////////////////////////////////// //

	/**
	 * This method initializes the coloring processor
	 */
	public void preProcess()
	{
		objectColorMap.clear();
		cStripComponents = new ArrayList<Component>();
		isProcessingCenterStrips = false;
		resetBlockStates();
	}

	/**
	 * This method is called to process a wire elements
	 *
	 * @param uid, the uid of the wire object
	 * @param elt, the document element
	 */
	public void processWire(IUID uid, Element elt)
	{
		synchronized (modelList) {
			updateBlockColor(elt);
			for (Model model : modelList) {
				for (ISchemDiagram diagram : model.getDiagrams()) {
					if (diagramHasView(diagram)) {
						IWireConductor cond = UIDMgr.getObjectOfType(uid, IWireConductor.class);
						Component wireComponent = new Component(model, diagram.getUID(), uid, elt);
						if (cond != null && cond.getNumCenterStripSplices() > 0) {
							cStripComponents.add(wireComponent);
						}
						else {
							processComponentInEDT(wireComponent, true, false);
						}
					}
				}
			}
		}
	}

	private static class Component
	{

		private IUID diagramUID;
		private IUID componentUID;
		private Element xmlElement;
		private Model model;

		private Component(Model model, IUID diagramUID, IUID componentUID, Element elt)
		{
			this.model = model;
			this.diagramUID = diagramUID;
			this.componentUID = componentUID;
			xmlElement = elt;
		}

		@Nullable public IUID getDesignUID()
		{
			IDesignContainer design = model.getDesign();
			if (design != null) {
				return design.getUID();
			}
			return null;
		}

		public IUID getDiagramUID()
		{
			return diagramUID;
		}

		public IUID getComponentUID()
		{
			return componentUID;
		}

		public Element getXmlElement()
		{
			return xmlElement;
		}

		public IDynamicGfxService getDynamicGfxService()
		{
			return model.getDynamicGfxService();
		}
	}

	/**
	 * This method is called to process a component element
	 *
	 * @param uid, the uid of the component object
	 * @param elt, the document element describing the componet
	 */
	public void processComponent(final IUID uid, final Element elt)
	{
		synchronized (modelList) {
			updateBlockColor(elt);
			for (Model model : modelList) {
				for (ISchemDiagram diagram : model.getDiagrams()) {
					if (diagramHasView(diagram)) {
						IUID diagramUid = diagram.getUID();
						Component component = new Component(model, diagramUid, uid, elt);
						// we must iterate over all the representations of the given
						// object.
						processComponentInEDT(component, false, false);
					}
				}
			}
		}
	}

	private void processComponentInEDT(final Component component, final boolean isWire, final boolean isBlock)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// check the processor is still in the map: could have been removed
				// by closing project ro design
				if (component.getDesignUID() != null && getProcessor(component.getDesignUID().getString()) == null) {
					return;
				}

				IUIDObject componentObject = UIDMgr.getNonDeletedObject(component.getComponentUID());
				LogicAnalysisServices.getAnalysisServices();
				IAnalysisNetlistScope netlistScope = AnalysisServices.getCurrentAnalysisNetlistScope();
				if (componentObject == null || netlistScope == null ||
						!netlistScope.isInScope(componentObject)) {
					return;
				}
				// Retrieve diagram
				ISchemDiagram diag = UIDMgr.getObjectOfType(component.getDiagramUID(), ISchemDiagram.class);
				if (diag != null) {
					dgs = component.getDynamicGfxService();

					if (dgs == null) {
						return;
					}

					if (isBlock) {
						IDiagramObjectIterator iter = diag.getRepresentations(component.getComponentUID());
						while (iter.hasNext()) {
							try {
								drawBlockDevice(iter.getNext(), getBlockPriority(component.getComponentUID()));
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					else if (isWire) {

						for (IConductor schemConductor : CollectionUtils
								.getObjectList(diag.getRepresentations(component.getComponentUID()),
										IConductor.class)) {
							try {
								_processWire(schemConductor, component.getXmlElement());
								if(isCenterStripWire(schemConductor) && ACTIVE.equals(objectColorMap.get(schemConductor))){
									//note down information about centerstrip wire's activeness
									//this info will be used while processing centerstrip splice
									noteCenterstripWireActiveInfo((IWireConductor) componentObject, component);
								}
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					else {
						for (IDiagramObjectIterator iter = diag.getRepresentations(component.getComponentUID());
								iter.hasNext(); ) {
							try {
								IDiagramObject diagramObject = iter.next();
								_processComponent(diagramObject, component.getXmlElement());
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		});
	}

	/*
		if any centerstrip splice connected with this wire is having no conductors connected to its pins,
		  then store the color/activeness of the wire
	 */
	private void noteCenterstripWireActiveInfo(@NotNull final IWireConductor wireConductor,
			Component component)
	{
		boolean spliceWithOnlyCenterStripWire =
				wireConductor.getCenterStripSplices().stream()
						.anyMatch(splice -> !areAnyConductorsConnectedToPins(splice));

		if(spliceWithOnlyCenterStripWire){
			//if the centerstrip splice is present with out any conductors connected to its pins and with only one centerstrip wire, store the active/color value of the wire
			centerstripWireColorMap.put(component.getComponentUID(), getColorName(component.getXmlElement()));
		}
	}

	private boolean areAnyConductorsConnectedToPins(ISplice centerstripSplice)
	{
		return centerstripSplice.getPins().stream()
				.anyMatch(pin -> pin.getConductors().getSize() != 0);
	}

	private void updateBlockColor(Element elt)
	{
		String desginUID = elt.getAttribute("srcDesignUID");
		//Get the blocksUID where current design is used as blocks,
		Set<IUID> blocks = AnalysisServices.getCurrentAnalysisNetlistScope().getAssociatedBlockIds(desginUID);

		if (blocks == null) {
			return;
		}
		//Get the color of the component and update the color of blocks as per the priority
		String colorAttr = elt.getAttribute(COLOR_ATTR_COLOR);
		int priority = getPriority(colorAttr);
		updateStateForBlocks(blocks, priority, colorAttr);
	}

	private void updateStateForBlocks(Set<IUID> blocks, int priority, String colorAttr)
	{
		if (blocks != null) {
			for (IUID blockUID : blocks) {
				if (blockStates.containsKey(blockUID)) {
					int currentPriority = getPriority(blockStates.get(blockUID));
					if (priority < currentPriority) {
						blockStates.put(blockUID, colorAttr);
					}
				}
				else {
					blockStates.put(blockUID, colorAttr);
				}
			}
		}
	}

	/**
	 * This method is provided to allow special processing for elements that contain reference to more than one uid (
	 * i.e. connectors in logic )
	 *
	 * @param uids, the String reps of the uids
	 * @param elt, the element in question
	 */
	public void processSpecial(String[] uids, Element elt)
	{
		for (String uid : uids) {
			IUID compUID = AnalysisServices.constructUID(uid);
			String boolStr = elt.getAttribute(IAnalysisColoringProcessor.COLOR_ATTR_ISWIRE);
			boolean isWire = boolStr != null && boolStr.equalsIgnoreCase(IAnalysisColoringProcessor.COLOR_VAL_TRUE);
			if (isWire) {
				processWire(compUID, elt);
			}
			else {

				processComponent(FactoryMgr.getCommonFactory().constructUID(uid), elt);
			}
		}
	}

	private boolean isProcessingCenterStrips = false;
	List<Component> cStripComponents;

	private void processCenterStrips()
	{
		isProcessingCenterStrips = true;
		for (Component component : cStripComponents) {
			try {
				processComponentInEDT(component, true, false);

				//make the centerstrip splice active if needed
				makeSpliceActiveIfCenterstripWireIsActive(component);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/*
		If we find that the centerstrip splice is not having any conductors connected throught its pins and if the centerstripped wire is active,
		we need to make the centerstrip splice as active -- dts0101257826
	 */
	private void makeSpliceActiveIfCenterstripWireIsActive(Component component)
	{
		Runnable processSpliceTask = new Runnable()
		{
			@Override public void run()
			{
				IWireConductor centerstripWire =
						(IWireConductor) UIDMgr.getNonDeletedObject(component.getComponentUID());

				if (centerstripWire != null && ACTIVE.equals(centerstripWireColorMap.get(centerstripWire.getUID()))) {
					ISchemDiagram schemDiagram = UIDMgr.getObjectOfType(component.getDiagramUID(), ISchemDiagram.class);
					assert schemDiagram != null : "Schematic diagram should be available";
					centerstripWire.getCenterStripSplices()
							.stream()
							.filter(centerstripSplice -> isSpliceNotActive(schemDiagram, centerstripSplice))
							.forEach(splice -> updateColorForSplice(schemDiagram, centerstripWire, splice));
				}
			}
		};

		//processing in EDT as the processing of respective centerstrip wire is also in EDT
		//so to ensure that centerstrip wire color informaiton should be available before procesing the splice, we are processing in EDT
		SwingUtilities.invokeLater(processSpliceTask);
	}

	private void updateColorForSplice(ISchemDiagram schemDiagram, IUIDObject centerstripWire, ISplice splice)
	{
		objectColorMap.put(schemDiagram.getRepresentations(splice.getUID()).getNext(), ACTIVE);
		IDiagramObject cs = schemDiagram.getRepresentation(
				((IWireConductor) centerstripWire).getCenterStripSplices().getNext());
		createAndAddDynamicGfxWithAttribure(cs, AnalysisColoringServices.getColorAttribute(ACTIVE, cs));
	}

	private boolean isSpliceNotActive(ISchemDiagram schemDiagram, ISplice centerstripSplice)
	{
		IDiagramObject centerstripDiagRepresentation =
				schemDiagram.getRepresentations(centerstripSplice.getUID()).getNext();
		return "inactive".equals(objectColorMap.get(centerstripDiagRepresentation));
	}

	/**
	 * This method handles the post processing of the elements, in our case show the coloring to the user...
	 */
	public void postProcess()
	{
		processCenterStrips();
		showColoring();
	}

	// ///////////// //
	// Other methods //
	// ///////////// //

	/**
	 * This method iterates over the modela and open views and if one is displaying the model then it asks it to
	 * revalidate.
	 */
	public void showColoring()
	{
		synchronized (modelList) {
			for (Model model : modelList) {
				processBlocks(model);
				processViews(model);
			}
		}
	}

	/**
	 * This method iterates over the models and open views and if one is showing then it asks it to remove any transient
	 * graphics.
	 */
	public void clearColoring()
	{
		final Set<Model> modelsToClear;

		//clear centerstrip color map
		centerstripWireColorMap.clear();

		synchronized (modelList) {
			modelsToClear = new LinkedHashSet<Model>(modelList);
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override public void run()
			{
				for (Model model : modelsToClear) {
					IDynamicGfxService dgs = model.getDynamicGfxService();
					if (dgs != null) {
						dgs.removeAllTransientGfx();

						processViews(model);
					}
				}
			}
		});
	}

	/**
	 * This iterates over the open views invalidating them if their model equals the model in question
	 *
	 * @param m, the model to look for
	 */
	protected void processViews(Model m)
	{
		// get all the open windows
		for (ICAFWindow window : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			if (window instanceof ICapletWindow) {
				ICapletWindow capletWindow = (ICapletWindow) window;
				ICapletController controller = capletWindow.getController();
				if (controller != null &&
						controller.getCapletModel() != null &&
						controller.getCapletModel().equals(m)) {
					final ICapletView view = capletWindow.getCurrentView();
					Runnable task = new Runnable()
					{
						public void run()
						{
							if (view != null) {
								view.invalidate(IViewInvalidationEnum.eTransient);
							}
						}
					};
					if (SwingUtilities.isEventDispatchThread()) {
						task.run();
					}
					else {
						SwingUtilities.invokeLater(task);
					}
				}
			}
		}
	}

	private void processBlocks(Model model)
	{

		for (ISchemDiagram diagram : model.getDiagrams()) {
			if (diagramHasView(diagram)) {
				for (IBlockDevice block : model.getDesign().getConnectivity().getBlockDevices()) {
					//If associated design is not in the scope then don't color the block.
					if (!AnalysisServices.getCurrentAnalysisNetlistScope()
							.isInScope(block.getAssociatedDesign(null))) {
						continue;
					}
					Component blockComponent = new Component(model, diagram.getUID(), block.getUID(), null);
					processComponentInEDT(blockComponent, false, true);
				}
			}
		}
	}

	private String getBlockPriority(IUID uid)
	{
		if (blockStates.containsKey(uid)) {
			return blockStates.get(uid);
		}
		return "inactive";
	}

	protected void drawBlockDevice(IDiagramObject obj, String color)
	{
		//All horizontal/vertical diagram should be updated - dts0100994125
		createAndAddDynamicGfxWithAttribure(obj, AnalysisColoringServices.getColorAttribute(color, obj));
	}

	private void computeArrowLength(ISchemDiagram diagram)
	{
		// Compute arrow length based on grid size
		IGrid grid = diagram.getGrid();
		arrowLength = grid.getGridSpacing() * GRAPHIC_ARROW_GRID_PERCENT;
	}

	private void _processComponent(IDiagramObject iCmpt, Element iElt)
	{

		String color = iElt.getAttribute(COLOR_ATTR_COLOR);
		if (skipComponent(iCmpt, color)) {
			//System.err.println("Skipping component : " + iCmpt ) ;
			return;
		}
		// Check if component is a conductor (net)
		if (iCmpt instanceof IConductor) {
			colorAllConductorSegments((IConductor) iCmpt, iElt, null);
		}
		else if (iCmpt instanceof IDynamicGfxMediator) {
			createAndAddDynamicGfxWithAttribure(iCmpt, getColorAttribute(iElt, iCmpt));
		}

		objectColorMap.put(iCmpt, color);
	}

	private void createAndAddDynamicGfxWithAttribure(IDiagramObject iCmpt, IGfxAttribute attribute)
	{
		IGfxView view = getView(iCmpt);
		IDynamicGfx dynGfx =
				createDynamicGfxForDiagramObjectWithAttribute(dgs.getFactory(), (IDynamicGfxMediator) iCmpt, attribute,
						false);
		if (dynGfx != null) {
			addDynamicGraphics(view, dynGfx);
		}
	}

	@Nullable private IGfxView getView(IDiagramObject iCmpt)
	{
		IGfxView view = null;
		IBaseDiagram diagram = DiagramHelper.getBaseDiagram(iCmpt);
		if (diagram != null) {
			view = GfxViewHelper.diagramFindView(diagram, false);
		}
		return view;
	}

	private void addDynamicGraphics(@Nullable IGfxView view, @NotNull IGfxObject dynGfx)
	{
		if (view != null) {
			dgs.addTransientGfx(view, dynGfx);
		}
	}

	protected boolean skipComponent(IDiagramObject object, String color)
	{
		String previousColor = objectColorMap.get(object);
		if (previousColor != null) {
			if (color.equals(previousColor)) {
				return true; // don't draw it twice
			}
			if ("failed".equals(previousColor) && !"shorted".equals(color)) {
				return true;
			}
			if (ACTIVE.equals(previousColor) && !("shorted".equals(color) || "failed".equals(color))) {
				return true;
			}
		}

		return false;
	}

	private void _processWire(IDiagramObject iWire, Element iElt) throws Exception
	{
		// Check input is actually a conductor
		IConductor wire = (IConductor) iWire;
		boolean isCenterStrip = isCenterStripWire(wire);
		if (isCenterStrip) {
			assert isProcessingCenterStrips : "In _processWire for a centerstrip unexpectedly!";
		}

		// Retrieve source and destination components
		IUID srcUID = AnalysisServices.constructUID(iElt.getAttribute(COLOR_ATTR_SRCCMPTID));
		String srcNode = iElt.getAttribute(COLOR_ATTR_SRCPIN);

		IUID destUID = AnalysisServices.constructUID(iElt.getAttribute(COLOR_ATTR_DESTCMPTID));
		String destNode = iElt.getAttribute(COLOR_ATTR_DESTPIN);

		// Loop segments to search Joints that connect components
		int direction = 0;
		IJoint root = null;
		ISegment segment = null;

		for (IGfxObjectIterator iter = wire.getObjects(); root == null && iter.hasNext(); ) {
			IGfxObject grObj = iter.getNext();

			if (grObj instanceof ISegment) {
				segment = (ISegment) grObj;

				for (int i = 0; root == null && i < 2; i++) {
					IJoint joint = i == 0 ? segment.getStartJoint() : segment.getEndJoint();
					if (connectsComponent(joint, srcUID, srcNode)) {
						root = joint;
						direction = 1;
					}
					if (root == null && connectsComponent(joint, destUID, destNode)) {
						root = joint;
						direction = -1;
					}
				}
			}
		}

		//		Pair<List<ISegment>, List<ISegment>> paths =
		//				new Pair<List<ISegment>, List<ISegment>>(new ArrayList<ISegment>( ),new ArrayList<ISegment>( )  );

		HashMap<Object, List<ISegment>> paths = new HashMap<Object, List<ISegment>>();

		if (isCenterStrip) {
			IJoint joint = root;
			boolean setRoot = false;
			if (joint == null && segment != null) {
				root = segment.getEndJoint();
				direction = -1;
				setRoot = true;
			}
			final int pathDirection = direction;
			GraphServices graph = new GraphServices(root, segment);
			graph.setData(paths);
			graph.setBranchingAtPin(true);
			graph.setLoyalToParent(true);
			graph.visit(new GraphServices.IGraphVisitor()
			{
				public boolean processElement(ISegment segment,
						IJoint destNode,
						int branch,
						GraphServices parent
				)
				{
					//Pair<List<ISegment>, List<ISegment>> data = (Pair<List<ISegment>, List<ISegment>>)parent.getData();
					//List<ISegment> pathCollector = pathDirection == 1 ? data.getSecond() :data.getFirst();
					HashMap<Object, List<ISegment>> data = (HashMap<Object, List<ISegment>>) parent.getData();
					List<ISegment> pathCollector = data.get("currentPath");
					if (pathCollector == null) {
						pathCollector = new ArrayList<ISegment>();
						data.put("currentPath", pathCollector);
					}

					pathCollector.add(segment);
					Set<IDiagramObject> objs = new HashSet<IDiagramObject>();
					objs.addAll(segment.getStartJoint().getAssociations(IPin.class));
					objs.addAll(segment.getEndJoint().getAssociations(IPin.class));
					int plistCount = 0;
					for (IDiagramObject obj : objs) {
						if (((IPin) obj).getConnectivity().getOwner() != null) {
							IPinList plist = (IPinList) ((IPin) obj).getConnectivity().getOwner();
							plistCount++;
							if (data.get(plist.getUID()) == null && plistCount > 1) {
								data.put(plist, pathCollector);
								data.put("currentPath", new ArrayList<ISegment>());
								return true;
							}
						}
					}
					return true;
				}
			});
			if (setRoot) {
				root = null;
				direction = 0;
			}
		}

		// Create arrows
		Map<IUID, ISegment> colored = null;
		if (root != null && !isCenterStrip) {
			// Draw arrows
			colored = drawWireSegments(root, segment, direction, iElt);
		}

		boolean isValidColorForPathAlg = ACTIVE.equals(getColorName(iElt)) ||
				"inactive".equals(getColorName(iElt));
		if (isCenterStrip && isValidColorForPathAlg) {
			if (colored == null) {
				colored = new HashMap<IUID, ISegment>();
			}
			List<IDiagramObject> activeCenterStrips = getActiveCenterStrips(wire);
			colorCenterStripPaths(wire, iElt, colored, paths, activeCenterStrips);
		}

		// Loop all segments again and color those which were not previously
		colorAllConductorSegments(wire, iElt, colored);
		objectColorMap.put(wire, getColorName(iElt));
	}

	private boolean isCenterStripWire(IConductor wire)
	{
		Set<ISplice> centerStrips = ((IWireConductor) wire.getConnectivity()).getCenterStripSplicesAsSet();
		return centerStrips != null && !centerStrips.isEmpty();
	}

	private int decideArrowDirection(List<IUIDObject> segMentAndJointListToDrawArrow, IConductor wire, IUID srcUID,
			String srcNode, IUID destUID, String destNode, boolean isCenterstrippedwire)
	{
		int direction = 0;
		IJoint root = null;
		ISegment segment = null;
		for (IGfxObjectIterator iter = wire.getObjects(); root == null && iter.hasNext(); ) {
			IGfxObject grObj = iter.getNext();

			if (grObj instanceof ISegment) {
				segment = (ISegment) grObj;

				boolean isCenterStripPresentAsSourceOrDestination =
						UIDMgr.getNonDeletedObject(srcUID) instanceof ISplice ||
								UIDMgr.getNonDeletedObject(destUID) instanceof ISplice;
				if (isCenterstrippedwire && isCenterStripPresentAsSourceOrDestination) {
					root = connectsComponent(segment, srcUID, srcNode, destUID, destNode);
					if (root != null) {
						direction = 1;
					}
				}
				else {
					for (int i = 0; root == null && i < 2; i++) {
						IJoint joint = i == 0 ? segment.getStartJoint() : segment.getEndJoint();
						if (connectsComponent(joint, srcUID, srcNode)) {
							root = joint;
							direction = 1;
						}
						if (root == null && connectsComponent(joint, destUID, destNode)) {
							root = joint;
							direction = -1;
						}
					}
				}
			}
		}

		segMentAndJointListToDrawArrow.add(segment);
		segMentAndJointListToDrawArrow.add(root);
		return direction;
	}

	@Nullable private IJoint connectsComponent(ISegment segment, IUID srcUID, String srcNode, IUID destUID,
			String destNode)
	{
		IJoint root = null;
		if (segment != null) {
			if (srcUID != null && destUID != null) {
				for (int i = 0; root == null && i < 2; i++) {
					IJoint srcJoint = i == 0 ? segment.getStartJoint() : segment.getEndJoint();
					if (connectsComponent(srcJoint, srcUID, srcNode)) {
						IJoint destJoint = i == 0 ? segment.getEndJoint() : segment.getStartJoint();
						if (connectsComponent(destJoint, destUID, destNode)) {
							root = srcJoint;
						}
					}
				}
			}
		}
		return root;
	}

	private List<IDiagramObject> getActiveCenterStrips(IConductor wire)
	{
		List<IDiagramObject> activeSplices = new ArrayList<IDiagramObject>();
		for (Object obj : wire.getConnectedObjects()) {
			if (obj instanceof IPin) {
				IDiagramObject diagObj = ((IPin) obj).getParent();
				if (diagObj != null && diagObj instanceof IRepresentedObject) {
					if (((IRepresentedObject) diagObj).getRawConnectivity() instanceof ISplice) {
						ISplice splice = (ISplice) ((IRepresentedObject) diagObj).getRawConnectivity();
						if (splice != null && splice.getNumCenterStrippedWires() != 0) {
							for (Object cond : ((IPin) obj).getConductors()) {
								if (!cond.equals(wire)) {
									if (ACTIVE.equals(objectColorMap.get(cond))) {
										if (!activeSplices.contains(diagObj)) {
											activeSplices.add(diagObj);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return activeSplices;
	}

	private void 	colorCenterStripPaths(IConductor cond,
			Element iElt,
			Map<IUID, ISegment> alreadyColored,
			HashMap<Object, List<ISegment>> paths,
			List<IDiagramObject> activeCenterStrips)
	{

		IGfxView view = getView(cond);
		// We look at the components at the start and end of the paths. If either is inactive
		// but the wire is active then we must fail that section of the wire path.

		boolean isActive = ACTIVE.equals(getColorName(iElt));
		boolean isFullyActive = isActive;
		HashMap<IDiagramObject, IPin> activeComps = new HashMap<IDiagramObject, IPin>();
		for (IPin pin : cond.getPins()) {
			IDiagramObject obj = pin.getParent();
			IRepresentedObject rep = (IRepresentedObject) obj;
			if (rep.getRawConnectivity() instanceof ISplice) {
				ISplice splice = (ISplice) rep.getRawConnectivity();
				if (splice.getNumCenterStrippedWires() != 0) {
					continue; // center strips are hendled below...
				}
			}
			if (!ACTIVE.equals(objectColorMap.get(obj))) {
				isFullyActive = false;
			}
			else {
				activeComps.put(obj, pin);
			}
		}

		// look at the individual path segments
		boolean activePath = false;
		List<List<ISegment>> inactivePaths = new ArrayList<List<ISegment>>();
		List<ISegment> segs = new ArrayList<ISegment>();
		for (List<ISegment> path : paths.values()) {

			//if (!isPathActive(path)) {
			boolean b = isPathActive(path, activeCenterStrips, false);
			activePath |= b;
			if (isFullyActive || b) {
				colorPath(alreadyColored, path, view, ACTIVE);
			}
			else {
				inactivePaths.add(new ArrayList<ISegment>(path));
			}
			//}
		}

		if (isActive && !activePath) {
			colorPathBasedOnActiveComponents(alreadyColored, cond, view, activeComps, activeCenterStrips);
		}

		for (List<ISegment> path : inactivePaths) {
			colorPath(alreadyColored, path, view, "inactive");
		}
	}

	private void colorPathBasedOnActiveComponents(Map<IUID, ISegment> alreadyColored,
			IConductor cond, IGfxView view, Map<IDiagramObject, IPin> objects, List<IDiagramObject> centerStrips)
	{

		List<ISegment> segs = new ArrayList<ISegment>();
		boolean active = false;
		// Look for a device to start on
		ISplice targetSplice = null;
		IPinList targetDevice = null;
		for (IDiagramObject obj : objects.keySet()) {
			if (obj instanceof IRepresentedObject &&
					((IRepresentedObject) obj).getRawConnectivity() instanceof IPinList) {

				IPinList plist = (IPinList) ((IRepresentedObject) obj).getRawConnectivity();
				if (plist instanceof ISplice) {
					targetSplice = (ISplice) plist;
				}
				else {
					targetDevice = plist; // may be device or connector
				}
			}
		}

		IPinList startPoint = targetDevice != null ? targetDevice : targetSplice;
		ISegment seg = null;
		IJoint joint = null;
		for (IConnected conn : cond.getSegments()) {
			seg = (ISegment) conn;
			joint = connectsComponent(seg, startPoint);
			if (joint != null) {
				break;
			}
		}
		if (joint != null) {
			List<IDiagramObject> activeObjects = new ArrayList<IDiagramObject>();
			activeObjects.addAll(objects.keySet());
			activeObjects.addAll(centerStrips);

			Pair<List<IDiagramObject>, List<ISegment>> path =
					new Pair<List<IDiagramObject>, List<ISegment>>(activeObjects, new ArrayList<ISegment>());

			try {
				GraphServices graph = new GraphServices(joint, seg);
				graph.setData(path);
				graph.setBranchingAtPin(true);
				graph.setLoyalToParent(true);
				graph.visit(new GraphServices.IGraphVisitor()
				{
					public boolean processElement(ISegment segment,
							IJoint destNode,
							int branch,
							GraphServices parent
					)
					{
						Pair<List<IDiagramObject>, List<ISegment>> path =
								(Pair<List<IDiagramObject>, List<ISegment>>) parent.getData();

						path.getSecond().add(segment);
						List<IDiagramObject> objs = new ArrayList<IDiagramObject>();
						for (ISegment seg : path.getSecond()) {
							IUIDObjectCollection coll = seg.getAssociatedObjects(IPin.class);
							for (Object obj : coll.getUIDObjects()) {
								if (obj instanceof IPin) {
									IPin pin = (IPin) obj;
									objs.add(pin.getParent());
								}
							}
						}
						if (objs.containsAll(path.getFirst())) {
							path.getFirst().clear();
							return false;
						}
						return true;
					}
				});
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			if (path.getFirst().isEmpty()) {
				// We found everything...
				colorPath(alreadyColored, path.getSecond(), view, ACTIVE);
			}
		}
	}

	@Nullable private IJoint connectsComponent(ISegment seg, @Nullable IPinList plist)
	{
		IUID plistUID = plist == null ? null : plist.getUID();
		IJoint joint = seg.getStartJoint();
		if (connectsComponent(joint, plistUID, "")) {
			return joint;
		}
		joint = seg.getEndJoint();
		if (connectsComponent(joint, plistUID, "")) {
			return joint;
		}
		return null;
	}

	private void colorPath(Map<IUID, ISegment> alreadyColored,
			List<ISegment> path, IGfxView view, String color)
	{

		for (ISegment seg : path) {
			colorSegment(color, alreadyColored, view, seg);
			alreadyColored.put(seg.getUID(), seg);
		}
	}

	private boolean isPathActive(List<ISegment> segs, List<IDiagramObject> activeCenterStrips, boolean firstAndLast)
	{

//		   for ( ISegment seg : segs ) {
//			   IJoint joint = seg.getStartJoint();
//			   if ( isAssociatedWithActiveObject( joint ) ) {
//				   return true ;
//			   }
//			   joint = seg.getEndJoint() ;
//			   if ( isAssociatedWithActiveObject( joint ) ) {
//				   return true ;
//			   }
//		   }

		List<IPin> pins = new ArrayList<IPin>();
		List<ISegment> segments = new ArrayList<ISegment>(segs);
		if (firstAndLast && !segs.isEmpty()) {
			segments.clear();
			segments.add(segs.get(0));
			segments.add(segs.get(segs.size() - 1));
		}
		for (ISegment seg : segs) {
			IJoint joint = seg.getStartJoint();
			pins.addAll(joint.getAssociations(IPin.class));
			joint = seg.getEndJoint();
			pins.addAll(joint.getAssociations(IPin.class));
		}

		if (pins.size() < 2) {
			return false;
		}

		for (IPin pin : pins) {
			IDiagramObject obj = pin.getParent();
			IRepresentedObject rep = (IRepresentedObject) obj;
			if (rep.getRawConnectivity() instanceof ISplice) {
				if (!activeCenterStrips.contains(rep) && !ACTIVE.equals(objectColorMap.get(rep))) {
					return false;
				}
			}
			else {
				if (!ACTIVE.equals(objectColorMap.get(rep))) {
					return false;
				}
			}
		}

		return true;

		   /*if ( !segs.isEmpty( ) ) {
		   	ISegment first = segs.get(0) ;
		   	ISegment last = segs.get(segs.size( ) -1 ) ;

			   IJoint startJoint = getTerminatingJoint( first ) ;
			   IJoint endJoint = getTerminatingJoint( last ) ;
			   if ( endJoint == startJoint ) {
				   endJoint = first.getEndJoint( ) ;
			   }
			   if ( startJoint == null || endJoint == null ) {
				   return false;
			   }

			   if ( isAssociatedWithActiveObject( startJoint ) && isAssociatedWithActiveObject( endJoint ) ) {
				   return true ;
			   }
		   }
		   return false ;*/
	}

	@Nullable private IJoint getTerminatingJoint(ISegment seg)
	{
		IJoint joint = seg.getStartJoint();
		if (isJointTerminating(seg, joint)) {
			return joint;
		}
		joint = seg.getEndJoint();
		if (isJointTerminating(seg, joint)) {
			return joint;
		}
		return null;
	}

	private boolean isJointTerminating(ISegment seg, IJoint joint)
	{
		int count = 0;
		for (IDiagramObject obj : joint.getAssociations()) {
			if (obj instanceof IPin || obj instanceof IConductor) {
				IDiagramObject toTest = obj;
				if (obj instanceof IPin) {

					// Skip splices
					if ((((IRepresentedObject) toTest).getRawConnectivity() instanceof ISplice)) {
						continue;
					}
					// If we've a device return
					return true;
				}
				else {
					if (!seg.getConductor().equals(obj)) {
						count++;
					}
				}
			}
		}
		if (count > 0) {
			return true;
		}
		return false;
	}

	private boolean isAssociatedWithActiveObject(IJoint joint)
	{
		for (IDiagramObject obj : joint.getAssociations()) {
			if (obj instanceof IPin || obj instanceof ISegment) {
				IDiagramObject toTest = obj;
				if (obj instanceof IPin) {
					toTest = obj.getParent();
				}
				if (toTest instanceof IRepresentedObject &&
						!(((IRepresentedObject) toTest).getRawConnectivity() instanceof ISplice)) {
					if (ACTIVE.equals(objectColorMap.get(toTest))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void colorAllConductorSegments(IConductor cond,
			Element iElt,
			Map<IUID, ISegment> alreadyColored)
	{
		IGfxView view = getView(cond);
		for (IGfxObjectIterator iter = cond.getObjects(); iter.hasNext(); ) {
			IGfxObject grObj = iter.getNext();

			if (grObj instanceof ISegment) {
				ISegment segment = (ISegment) grObj;
				if(segment.getLength() != 0) {
					colorSegment(getColorName(iElt), alreadyColored, view, segment);
				}
			}
		}
	}

	private void colorSegment(String color, Map<IUID, ISegment> alreadyColored, IGfxView view, ISegment segment)
	{
   		if (segment.isFilteredVisible()) {
			ISegment colored = alreadyColored == null ? null : alreadyColored.get(segment.getUID());
			if (colored == null) {
				drawSegment(segment, segment.getStartJoint(),
						segment.getEndJoint(),
						0,
						arrowLength,
						getColorAttribute(color, segment),
						true,
						true,
						view);
			}
		}
	}

	protected IGfxAttribute getColorAttribute(Element iElt, IDiagramObject diagramObject)
	{

		return getColorAttribute(getColorName(iElt), diagramObject);
	}

	protected IGfxAttribute getColorAttribute(String color, IDiagramObject diagramObject)
	{
		if (Environment.isUnitTest()) {
			return FactoryMgr.getDrawFactory()
					.constructGfxAttribute(FactoryMgr.getDrawFactory().constructColor(IColor.BLUE), 1, LineStyle.SOLID);
		}
		return AnalysisColoringServices.getColorAttribute(color, diagramObject);
	}

	/**
	 * This is a convenience method to get the name of the required color from an Element
	 *
	 * @param iElt, the element from which the color should be retrieved
	 *
	 * @return String, the color stored in the element.
	 */
	private String getColorName(Element iElt)
	{
		return iElt.getAttribute(COLOR_ATTR_COLOR);
	}

	private boolean connectsComponent(@Nullable IJoint iJoint, @Nullable IUID iCmptUid, String pinName)
	{
		boolean oConnects = false;

		if (iJoint != null && iCmptUid != null) {
			//ISchemDiagram diagram = m_model.getDiagram();

			// Search pin associations
			for (IDiagramObjectIterator iter = iJoint.getAssociations(); !oConnects && iter.hasNext(); ) {
				IDiagramObject obj = iter.getNext();
				if (obj instanceof IPin) {
					IPin pinObj = (IPin) obj;
					IAbstractPin pin = pinObj.getConnectivity();
					IPinList pinlist = pin == null ? null : pin.getOwner();
					IUID parentUid = pinlist != null ? pinlist.getUID() : null;
					//System.err.println( "Joint : " + iJoint + " compU " + iCmptUid + " pUID " + parentUid ) ;
					//System.err.println( "pinName : " + pinName + " pin " + pin.getName( ) + " am " + pin.getAnalysisModel( ) ) ;
					if (iCmptUid.equals(parentUid)) {
						//if ( "".equals( pinName ) || pin.getName().equals( pinName ) ) {
						// if we don't know the pin name then we assume the connection to be right via the uid
						// if we do know the pin name but the compt has no analysis model attached then chances are
						// it is a dynamic model so we should use this as the connection point. Finally if we know the pin
						// name and it matches the one we're after then we're good to go...
                        if ("".equals(pinName) || isDirectionFoundFromPin(pinName, pin)) {
                            oConnects = true;
                        }

                    }
				}
			}
		}

		return oConnects;
	}

    private boolean isDirectionFoundFromPin(String pinName, @NotNull IAbstractPin pin)
    {
        String electricalPin = AnalysisHelper.getInstance().isLegacyAnalysisMode() ? pin.getAnalysisModel() : pin.retrieveAnalysisPort();
        return StringUtils.isBlank(electricalPin) || electricalPin.equals(pinName);
    }


    protected Map<IUID, ISegment> drawWireSegments(IJoint root,
			ISegment initSegment,
			final int direction,
			final Element iElt) throws Exception
	{
		//System.out.println(">> dbg:SubsystemBaseAction.drawWireSegments direction=" + direction);
		//Thread.dumpStack( ) ;
		GraphServices graph = new GraphServices(root, initSegment);
		graph.visit(new GraphServices.IGraphVisitor()
		{
			public boolean processElement(ISegment segment,
					IJoint destNode,
					int branch,
					GraphServices parent
			)
			{
				//System.err.println("Processing segment " + segment.getUID().toString() ) ;
				//System.out.println("  >> Visitor.processElement branch=" + branch);
				// Orient arrow
				int orient = 0;
				if (segment.getEndJoint().equals(destNode)) {
					orient = direction;
				}
				else if (segment.getStartJoint().equals(destNode)) {
					orient = -direction;
				}
				//System.err.println("Orient is " + orient ) ;
				IJoint j1 = null;
				IJoint j2 = null;
				if (orient == 1) {
					j1 = segment.getStartJoint();
					j2 = segment.getEndJoint();
				}
				else if (orient == -1) {
					j2 = segment.getStartJoint();
					j1 = segment.getEndJoint();
				}

				if (j1 != null && j2 != null) {
					// Create arrow
					//System.err.println("The segment length is " + segment.getLength( ) ) ;
					//System.err.println("Segment is " + segment.getClass( ).getName( ) ) ;
					drawSegment(segment, j1,
							j2,
							segment.getLength(),
							arrowLength,
							AnalysisColoringServices.getColorAttribute(getColorName(iElt), segment),
							false,
							true, getView(segment));
				}
				//System.out.println("  << Visitor.processElement branch");
				return true;
			}
		});

		//System.out.println("<< dbg:SubsystemBaseAction.drawWireSegments");
		return (Map<IUID, ISegment>) graph.getData();
	}

	/**
	 * Method to wrap the calls to draw a segment
	 *
	 * @param j1, the start joint
	 * @param j2, the end joint
	 * @param length, the segment length
	 * @param arrowLen, the arrow head length
	 * @param colorAttribute, the gfx attribute for coloring
	 * @param drawBody, should we draw the arrow body ?
	 * @param drawArrow, should we draw the arrow head ?
	 * @param view - if it needs to be drawn only in this view. Otherwise pass null
	 */
	private void drawSegment(@NotNull IConnected segment, @Nullable IJoint j1, @Nullable IJoint j2,
			double length, double arrowLen, IGfxAttribute colorAttribute, boolean drawBody, boolean drawArrow,
			@Nullable IGfxView view)
	{
		List<IGfxObject> lines = AnalysisColoringServices.createLinesToDrawSegment(segment, j1, j2, length, arrowLen,
				colorAttribute, drawBody, drawArrow);
		for (IGfxObject line : lines) {
			addDynamicGraphics(view, line);
		}
	}

	/**
	 * This method provides a coloring processor that can cope with the given scope.
	 */
	@Nullable public IAnalysisColoringProcessor getScopedColoringProcessor(IAnalysisNetlistScope scope)
	{
		return staticGetScopedColoringProcessor(scope);
	}

	@Nullable public static synchronized IAnalysisColoringProcessor staticGetScopedColoringProcessor(
			IAnalysisNetlistScope scope)
	{
		ArrayList<IAnalysisColoringProcessor> list = new ArrayList<IAnalysisColoringProcessor>();
		//add processors for the designs in the scope
		addProcessorForDesigns(scope.getDesignUIDs(), list);
		if (!list.isEmpty()) {
			return new AnalysisNetlistScopeColoringProcessor(list);
		}
		return null;
	}

	private static void addProcessorForDesigns(IUIDIterator iterator, ArrayList<IAnalysisColoringProcessor> list)
	{
		while (iterator.hasNext()) {
			IUID designUID = iterator.getNext();
			IAnalysisColoringProcessor processor = processorMap.get(designUID.toString());
			if (processor != null) {
				list.add(processor);
			}
		}
	}

	private boolean diagramHasView(ISheet diagram)
	{
		for (ICAFWindow win : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			if (win instanceof ICapletWindow) {
				ICapletView view = ((ICapletWindow) win).getCurrentView();
				if (view instanceof GfxView) {
					ISheet sheet = ((IDrawingComponentOwner) view).getSheet();
					if (sheet != null) {
						if (sheet == diagram) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
