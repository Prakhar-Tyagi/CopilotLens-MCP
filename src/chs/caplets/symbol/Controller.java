/*
 * Copyright 2002-2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol;

import chs.analysis.CapitalAnalysisFactory;
import chs.caf.AppAction;
import chs.caf.CAFUtils;
import chs.caf.SymbolLibraryBrowser;
import chs.caf.action.utility.ActionableAddOn;
import chs.caf.action.utility.DummyView;
import chs.caf.cafmain.actions.BrowseSelectedObjectAction;
import chs.caf.cafmain.actions.SaveAction;
import chs.caf.cafmain.actions.analysis.AttachModelAction;
import chs.caf.cafmain.actions.analysis.AttachSVModelAction;
import chs.caf.cafmain.actions.analysis.BuildModelAction;
import chs.caf.cafmain.actions.analysis.EditModelAction;
import chs.caf.caplet.IBrowserClient;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.IModelActivationListener;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.CommonControllerActions;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.caf.caplet.helpers.PropertiesAction;
import chs.caf.caplet.helpers.browser.BrowserTabbedPane;
import chs.caf.caplet.helpers.browser.BrowserTreeHelper;
import chs.caf.caplet.helpers.browser.teamplay.ITeamPlayLinksBrowserController;
import chs.caf.caplet.helpers.graphics.AddCommentSymbolAction;
import chs.caf.caplet.helpers.graphics.CreateArcAction;
import chs.caf.caplet.helpers.graphics.CreateCircleAction;
import chs.caf.caplet.helpers.graphics.CreateCurveAction;
import chs.caf.caplet.helpers.graphics.CreateImageAction;
import chs.caf.caplet.helpers.graphics.CreatePolygonAction;
import chs.caf.caplet.helpers.graphics.CreatePolylineAction;
import chs.caf.caplet.helpers.graphics.CreateRectangleAction;
import chs.caf.caplet.helpers.graphics.CreateTextAction;
import chs.caf.caplet.helpers.graphics.DeleteGfxPointAction;
import chs.caf.caplet.helpers.graphics.FlipAction;
import chs.caf.caplet.helpers.graphics.GroupGfxAction;
import chs.caf.caplet.helpers.graphics.InsertGfxPointAction;
import chs.caf.caplet.helpers.graphics.PivotTextAction;
import chs.caf.caplet.helpers.graphics.PolylineModifier;
import chs.caf.caplet.helpers.graphics.RotateAction;
import chs.caf.caplet.helpers.graphics.SetGraphicDimensionAction;
import chs.caf.caplet.helpers.graphics.SymbolPlaceAsGraphicsAction;
import chs.caf.caplet.helpers.graphics.UngroupGfxAction;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.helpers.ui.common.ProjectWindowUtilities;
import chs.caplets.logic.actions.AddInstanceAction;
import chs.caplets.symbol.actions.AddAttributeDatumAction;
import chs.caplets.symbol.actions.AddBlockInstanceAction;
import chs.caplets.symbol.actions.AddDatumAction;
import chs.caplets.symbol.actions.AddDrillPointDatumAction;
import chs.caplets.symbol.actions.AddEngineeringDatumAction;
import chs.caplets.symbol.actions.AddFixturePlacementDatumAction;
import chs.caplets.symbol.actions.AddGenericDatumAction;
import chs.caplets.symbol.actions.AddInternalPinAction;
import chs.caplets.symbol.actions.AddPinAction;
import chs.caplets.symbol.actions.AddPortAction;
import chs.caplets.symbol.actions.AlignAction;
import chs.caplets.symbol.actions.ConvertPinTypeAction;
import chs.caplets.symbol.actions.ConvertToDiodeLinkAction;
import chs.caplets.symbol.actions.ConvertToFuseLinkAction;
import chs.caplets.symbol.actions.ConvertToResistanceLinkAction;
import chs.caplets.symbol.actions.CreateGridDatumAction;
import chs.caplets.symbol.actions.CreateInternalLinkDiodeAction;
import chs.caplets.symbol.actions.CreateInternalLinkFuseAction;
import chs.caplets.symbol.actions.CreateInternalLinkResistanceAction;
import chs.caplets.symbol.actions.CreateNameTextAction;
import chs.caplets.symbol.actions.CreateXRefTextAction;
import chs.caplets.symbol.actions.DeleteAction;
import chs.caplets.symbol.actions.DistributeAction;
import chs.caplets.symbol.actions.FlattenBlockAction;
import chs.caplets.symbol.actions.ReorderDatumAction;
import chs.caplets.symbol.actions.ReverseDiodeDirectionAction;
import chs.caplets.symbol.actions.SelectAction;
import chs.caplets.symbol.actions.SmartEditAction;
import chs.caplets.symbol.actions.SymbolPropertiesAction;
import chs.caplets.symbol.actions.UpdateInstanceAction;
import chs.caplets.symbol.actions.ViewRelatedSymbolAction;
import chs.caplets.symbol.analysis.SymbolAnalysisAttachmentTargetProvider;
import chs.caplets.symbol.properties.PropertiesClient;
import chs.cof.EngineeringDatumType;
import chs.cof.draw.ICompoundObject;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IGfxView;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IInternalLinkPolyline;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.logical.schem.ISegment;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IBlockIterator;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.common.IUIDObject;
import chs.ctf.editui.LogicEditSelectionHelper;
import chs.ctf.ui.form.SymbolSelectionEventListener;
import chs.system.UIDMgr;
import chs.utilities.ArrayStack;
import chs.utilities.BuildInfo;
import chs.utilities.ResourceMgr;
import chs.utility.SymbolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;

public class Controller extends CommonControllerActions
		implements ISelectListener, IModelChangeListener, IModelActivationListener
{

	private SymbolLibraryBrowser m_libBrowser = null;
	protected Model m_model = null;
	protected JPanel m_objectBrowser = null;
	private boolean m_bSelectingAssoc = false;
	private BrowserTabbedPane m_browser = null;

	public Controller(ICaplet caplet, IAbstractLibrary library, IStamp symdef, boolean isEditable)
	{
		super(caplet);

		// Create our model
		m_model = createModel(library, symdef, isEditable);

		//
		// Create the browser tree
		//
		m_objectBrowser = new JPanel();
		m_objectBrowser.setLayout(new BorderLayout());
		JComponent treeView = new BrowserTreeHelper(createBrowserClient(), "SymbolBrowser")
		{
			@Override protected void createViewActions(@NotNull ActionableAddOn actionableAddOn)
			{
				actionableAddOn.addAction(new ViewRelatedSymbolAction(new DummyView(getController())));
				super.createViewActions(actionableAddOn);
			}
		};
		m_objectBrowser.add(new JScrollPane(treeView), BorderLayout.CENTER);

		// Create the browser for this controller
		m_browser = new BrowserTabbedPane(CAFUtils.getInstance().getDialogFrame(), m_model);
		m_browser.setName("SymbolBrowserTabbedPane");
		m_browser.addTab(ResourceMgr.getString(Controller.class, "Controller.Browser.TreeTab.Title.text"),
				null, m_objectBrowser,
				ResourceMgr.getString(Controller.class, "Controller.Browser.TreeTab.ToolTip.text"));

		//
		// Create the symbol library browser
		//

		m_libBrowser = new SymbolLibraryBrowser(true)
		{
			protected void restart(IAction previousAction)
			{

				Class actionClass = previousAction.getClass();
				if (actionClass == AddBlockInstanceAction.class
						|| actionClass == AddCommentSymbolAction.class) {
					int modifiers = 0;
					doubleClickFired(modifiers);
				}
			}

			protected void doubleClickFired(int mouseModifiers)
			{
				IStamp stmp = m_model.getSymbolDef();
				if (stmp instanceof ISymbolDef) {
					ISymbolDef sd = (ISymbolDef) stmp;
					IStamp sub = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr().getActiveSymbol();
					if (sub == null || !(sub instanceof ISymbolDef)) {
						return;
					}
					ISymbolDef subsd = (ISymbolDef) sub;
					//
					// Check ancestry.
					//
					IStamp me = m_model.getSymbolDef();
					ArrayStack totest = new ArrayStack();
					totest.push(subsd);
					while (!totest.isEmpty()) {
						Object o = totest.pop();
						if (o == me) {
							return; // Self reference
						}
						//
						// Check ancestors.
						//
						if (o instanceof IBlock) {
							for (IBlockIterator bitr = ((IBlock) o).getBlocks(); bitr.hasNext(); ) {
								totest.push(bitr.getNext());
							}
						}
					}
					//
					// If we get here, it's legal as far as blocks go...
					//
					IAction action = null;
					final ICapletController capletController = CAFUtils.getInstance().getActiveCapletController();
					if (SymbolUtils.isCommentSymbol(subsd)) {
						// allow add Comment only for Comment symbol types selected.
						action = capletController.getAction(AddCommentSymbolAction.class);
					}
					else {
						// allow add block for selected symbol types other than comment.  More checking is done in the isEnabled() method
						action = capletController.getAction(AddBlockInstanceAction.class);
					}
					if (action != null && action.isEnabled()) {
						ActionEvent ae =
								new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "addinstance", mouseModifiers);
						CAFUtils.getInstance().getActiveActionMgr().actionPerformed(action, ae);
					}
				}
			}
		};
		// Add the SymbolLibraryBrowser as a symbol change listener of the SymbolLibraryMgr
		// This is necessary so the SymbolLibraryMgr can let the SymbolLibraryBrowser
		// know if it has been refreshed
		CAFUtils.getInstance().getSymbolLibraryMgr().addSymbolChangeListener(m_libBrowser);
		m_browser.addTab(ResourceMgr.getString(Controller.class, "Controller.Browser.SymbolTab.Title.text"),
				null, m_libBrowser,
				ResourceMgr.getString(Controller.class, "Controller.Browser.SymbolTab.ToolTip.text"));

		// Add ourselves as a select listener on our SelectMgr
		// so we can select associated objects.
		getSelectMgr().addSelectListener(this);

		// DevExtensions Actions
		if (BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled()) {
			addAction(new BrowseSelectedObjectAction(this));
		}

		// Create all of the controller actions
		createControllerActions();

		m_browser.postInit();

		// Listen to model changes
		getCapletModel().addModelChangeListener(this);
	}

	@NotNull protected IBrowserClient createBrowserClient()
	{
		return new BrowserClient(this);
	}

	public void modelPreChanged(ModelChangeEvent e)
	{
	}

	public void modelChanged(ModelChangeEvent e)
	{
		// Tickle UI of Save Action
		// This allows for instance to prevent saving an empty symbol
		AppAction save = getCaplet().getFIB().getAppActionMgr().getAction(SaveAction.class.getName());
		if (save != null) {
			save.updateUI();
		}
	}

	protected Model createModel(IAbstractLibrary library, IStamp symdef, boolean isModelEditable)
	{
		return new Model(this, library, symdef, isModelEditable);
	}

	@NotNull public ICapletModel getCapletModel()
	{
		return m_model;
	}

	protected void createControllerActions()
	{
		// DevExtensions Actions
		if (BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled()) {
			addAction(new BrowseSelectedObjectAction(this));
		}

		IPropertiesClient propertiesClient = new PropertiesClient(m_model);
		addCommonApplicableActions(propertiesClient);

		// Create the controller actions
		IAction selectAction = new SelectAction(this);
		IAction deleteAction = new DeleteAction(this);
		IAction flipAction = new FlipAction(this);
		IAction rotateAction = new RotateAction(this);
		IAction pivotTextAction = new PivotTextAction(this);
		IAction circleCreateAction = new CreateCircleAction(this);
		IAction lineCreateAction = new CreatePolylineAction(this);
		IAction polygonCreateAction = new CreatePolygonAction(this);
		IAction rectangleCreateAction = new CreateRectangleAction(this);
		IAction imageCreateAction = new CreateImageAction(this);
		IAction commentCreateAction = new AddCommentSymbolAction(this);
		IAction arcCreateAction = new CreateArcAction(this);
		IAction curveCreateAction = new CreateCurveAction(this);
		IAction textCreateAction = new CreateTextAction(this);
		IAction nameTextCreateAction = new CreateNameTextAction(this);
		IAction xrefTextCreateAction = new CreateXRefTextAction(this);
		IAction addPinAction = new AddPinAction(this);
		IAction addPortAction = new AddPortAction(this);
		IAction addInternalPinAction = new AddInternalPinAction(this);
		AddBlockInstanceAction addInstanceAction = new AddBlockInstanceAction(this);
		IAction insertGfxPtAction = new InsertGfxPointAction(this, new PolylineModifier(this));
		IAction deleteGfxPtAction = new DeleteGfxPointAction(this, new PolylineModifier(this));
		IAction groupGfxAction = new GroupGfxAction(this);
		IAction ungroupGfxAction = new UngroupGfxAction(this);
		m_libBrowser.addSymbolSelectionEventListener(addInstanceAction);
		IAction flattenBlockAction = new FlattenBlockAction(this);
//        IAction addLibraryAction = new AddLibraryAction(this, null);
		IAction addDatumAction = new AddDatumAction(this);
		IAction addDrillPointDatumAction = new AddDrillPointDatumAction(this);
		IAction addFixPlacementDatumAction = new AddFixturePlacementDatumAction(this);
		IAction addGenericDatumAction = new AddGenericDatumAction(this);
		IAction internalLinkResistanceCreateAction = new CreateInternalLinkResistanceAction(this);
		IAction internalLinkFuseCreateAction = new CreateInternalLinkFuseAction(this);
		IAction internalLinkDiodeCreateAction = new CreateInternalLinkDiodeAction(this);
		IAction smartEditAction = new SmartEditAction(this);
		addAction(new ConvertPinTypeAction(this));
		addAction(new ReverseDiodeDirectionAction(this));
		addAction(new ConvertToResistanceLinkAction(this));
		addAction(new ConvertToFuseLinkAction(this));
		addAction(new ConvertToDiodeLinkAction(this));
		addAction(new SetGraphicDimensionAction(this));

		//
		// AnalysisActions
		//

		IAction attachModelAction = null;
		IAction buildModelAction = null;
		IAction editModelAction = null;
		IAction svAttachModelAction = null;
		boolean analysisLoaded = false;
		if (CapitalAnalysisFactory.getAnalysisInterface() != null) {
			analysisLoaded = true;

			SymbolAnalysisAttachmentTargetProvider provider =
					new SymbolAnalysisAttachmentTargetProvider(m_model);
			editModelAction = new EditModelAction(this, provider);
			attachModelAction = new AttachModelAction(this, provider, true, false, false);
			buildModelAction = new BuildModelAction(this, provider, true, false, false);
			svAttachModelAction = new AttachSVModelAction(this, provider, true, false);
		}

		// Add the actions to the controller
		addAction(selectAction);
		addAction(deleteAction);
		addAction(addPinAction);
		addAction(addInternalPinAction);
		addAction(addPortAction);
		addAction(circleCreateAction);
		addAction(lineCreateAction);
		addAction(polygonCreateAction);
		addAction(rectangleCreateAction);
		addAction(imageCreateAction);
		addAction(commentCreateAction);
		addAction(internalLinkResistanceCreateAction);
		addAction(internalLinkFuseCreateAction);
		addAction(internalLinkDiodeCreateAction);
		addAction(arcCreateAction);
		addAction(curveCreateAction);
		addAction(textCreateAction);
		addAction(nameTextCreateAction);
		addAction(xrefTextCreateAction);
		addAction(addInstanceAction);
		addAction(insertGfxPtAction);
		addAction(deleteGfxPtAction);
		addAction(groupGfxAction);
		addAction(ungroupGfxAction);
		addAction(flattenBlockAction);

		addAction(flipAction);
		addAction(rotateAction);
		addAction(pivotTextAction);

		createDraftingActions();
		createPrintRegionActions();

		addAction(smartEditAction);

		addAction(new AddInstanceAction(this));

		addAction(addDatumAction);
		addAction(addDrillPointDatumAction);
		addAction(addFixPlacementDatumAction);
		addAction(addGenericDatumAction);
		addAction(new AlignAction(this, AlignAction.LEFT));
		addAction(new AlignAction(this, AlignAction.RIGHT));
		addAction(new AlignAction(this, AlignAction.TOP));
		addAction(new AlignAction(this, AlignAction.BOTTOM));
		addAction(new AlignAction(this, AlignAction.VERTICAL_CENTER));
		addAction(new AlignAction(this, AlignAction.HORIZONTAL_CENTER));
		addAction(new DistributeAction(this, DistributeAction.HORIZONTAL));
		addAction(new DistributeAction(this, DistributeAction.VERTICAL));

//        addAction(addLibraryAction);
		// PW - 04/21/03 - Use the correct TextAttributeEditor
		createZOrderActions();

		addAction(getPropertiesAction()); //FEAT15946 - Absol. Line thickness

		// Add App Actions defined by Resource - extend selection action and show functional source action.
		processResourceAppActions();

		// analysis actions
		if (analysisLoaded) {
			addAnalysisAction(attachModelAction, true);
			addAnalysisAction(editModelAction, true);
			addAnalysisAction(buildModelAction, true);
			addAnalysisAction(svAttachModelAction, false);
		}

		// Register Strokes
//        addStroke("741236987", deleteAction);
//        addStroke("7412687", deleteAction);	// Aliased
		// add the plugin action if necessary

		addAction(new UpdateInstanceAction(this));

		//add datum actions
		addAction(new CreateGridDatumAction(this));

		addAction(new AddDatumAction(this));

		for (EngineeringDatumType engineeringDatumType : EngineeringDatumType.values()) {
			addAction(new AddEngineeringDatumAction(this, engineeringDatumType.getEngineeringDatumType()));
		}

		addAction(new AddAttributeDatumAction(this));

		addAction(new ReorderDatumAction(this));

		// Fix for defect dts0100904706
		addAction(new SymbolPlaceAsGraphicsAction(this));
		IAction action = getAction(SymbolPlaceAsGraphicsAction.class);
		if (action instanceof ActionRT) {
			m_libBrowser.contextMenuAddAction(((ActionRT) action).getActionUI());
		}

		// Set the Select Action as the base action in the
		// action manager.
		getActionMgr().setBaseAction(selectAction);
	}

	protected PropertiesAction getPropertiesAction()
	{
		return new SymbolPropertiesAction(this, createPropertiesClient());
	}

	@Override @NotNull
	public IPropertiesClient createPropertiesClient()
	{
		return new PropertiesClient(m_model);
	}

	public JComponent getBrowser()
	{
		return m_browser;
	}

	public void selectionChanged(SelectEvent e)
	{
		// Ignore notification resulting from us selecting
		// associated objects
		if (m_bSelectingAssoc) {
			return;
		}

		// Select associated objects.
		// For now this means segments of a conductor, and
		// a conductor for segments.
		// We only care about forward selection
		if (e.isSelect()) {
			// Set the state so we ignore selections
			m_bSelectingAssoc = true;
			try {
				Collection<Selection> selections = e.getSelections();
				SelectSet selectSet = new SelectSet();
				for (Selection sel : selections) {
					IUIDObject obj = UIDMgr.getObject(sel.getUID());
					if (obj instanceof IConductor) {
						ICompoundObject cond = (ICompoundObject) obj;
						for (ISegment seg : cond.getObjects(ISegment.class)) {
							selectSet.add(new Selection(seg));
						}
					}
					else if (obj instanceof ISegment) {
						// Select the Conductor for this segment
						ISegment seg = (ISegment) obj;
						selectSet.add(new Selection(seg.getConductor()));
					}
					if (obj instanceof ISchemInternalLink) {
						ICompoundObject cond = (ICompoundObject) obj;

						for (IInternalLinkPolyline seg : cond.getObjects(IInternalLinkPolyline.class)) {
							selectSet.add(new Selection(seg));
						}
					}
					else if (obj instanceof IInternalLinkPolyline) {
						// Get the SchemInternalLink for this PolyLine
						IInternalLinkPolyline seg = (IInternalLinkPolyline) obj;
						ISchemInternalLink link = seg.getInternalLink();
						selectSet.add(new Selection(link));
					}
				}
				// Update the selection and notify other listeners
				e.getSelectSource().add(selectSet);
			}
			finally {
				// Stop ignoring selections
				m_bSelectingAssoc = false;
			}
		}
	}

	public void destroy()
	{
		//
		// Remove the listener [Have to do this before the destroy, as we need the action]
		//
		IAction aii = getAction(AddBlockInstanceAction.class);
		if (aii != null) {
			m_libBrowser.removeSymbolSelectionEventListener((SymbolSelectionEventListener) aii);
		}
		CAFUtils.getInstance().getSymbolLibraryMgr().removeSymbolChangeListener(m_libBrowser);
		m_libBrowser.destroy();
		getCapletModel().removeModelChangeListener(this);
		getDataTransfer().clearPasteBuffer();

		//
		super.destroy();
	}

	public String getDoubleClickAction()
	{
		LogicEditSelectionHelper hesHelper =
				new LogicEditSelectionHelper(getSelectMgr().getPreSelections());
		return hesHelper.getSymbolDoubleClickAction();
	}

	@Override public void modelActivated()
	{
		CAFUtils.getInstance().getScanningLock().obtainScanningLock();
		try {
			//
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			if (view instanceof IGfxView) {
				IBaseDiagram viewDiagram = ((IGfxView) view).getDiagram();
				if (viewDiagram != null) {
					ProjectWindowUtilities.selectPathInProjectWindowForDiagram(viewDiagram);
				}
			}
		}
		finally {
			CAFUtils.getInstance().getScanningLock().releaseScanningLock();
		}
	}

	@Override public void modelDeactivated(boolean isClosing)
	{

	}

	@Nullable @Override
	protected ITeamPlayLinksBrowserController createLinksTabController(ICapletController capletController)
	{
		return null;
	}
}
