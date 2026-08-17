/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2023 Siemens
 */
package chs.caplets.symbol;

import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.IWindowMgr;
import chs.caf.cafmain.actions.bridges.change.report.PersistantCMReport;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IUndoableObject;
import chs.caf.caplet.helpers.GfxCapletModelHelper;
import chs.caplets.shared.IGfxDisplayableModel;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.ISheet;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.project.naming.IIndexedNamedObject;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IPSMStamp;
import chs.cof.symbol.ISheetAdapter;
import chs.cof.symbol.IStamp;
import chs.cofUtils.scrubber.OnTheFlyScrubber;
import chs.cofUtils.scrubber.SymbolOnTheFlyScrubber;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utility.DiagramHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.TextHelper;
import chs.utility.logic.ISymbolModel;
import org.jetbrains.annotations.Nullable;

public class Model extends GfxCapletModelHelper implements IGfxDisplayableModel, ISymbolModel
{

	private IAbstractLibrary m_symlib;

	private IUID m_symdef;

	private ISheetAdapter m_sheet;

	private boolean m_bLinksVisible = true;
	private static final SymbolOnTheFlyScrubber scrubber = new SymbolOnTheFlyScrubber();
	private boolean hasPermissionToEdit = true;

	public Model(ICapletController cont, IAbstractLibrary symlib, IStamp symdef, boolean isModelEditable)
	{

		super(cont, symdef);
		m_symlib = symlib;
		m_symdef = symdef.getUID();
		m_sheet = FactoryMgr.getSymbolFactory().createSheetAdapter(symdef);

		hasPermissionToEdit = isModelEditable;

		// PW - 04/28/03 - Set the PinSpacing
		TextHelper.setPinSpacing(m_sheet.getGrid().getGridSpacing());
	}

	protected boolean allowSelectionOfInvisibleObjects()
	{
		return true;
	}

	public boolean isLinksVisible()
	{
		return m_bLinksVisible;
	}

	public void setLinksVisibility(boolean value)
	{
		m_bLinksVisible = value;
		FactoryMgr.getDrawFactory().setLinksVisibilityToggledOn(value);
		// Update the status bar of any views presenting this model.
		IWindowMgr mgr = CAFUtils.getInstance().getWindowMgr();
		for (ICAFWindow window : mgr.getWindows()) {
			ICapletWindow capWin = (ICapletWindow) window;
			ICapletView view = capWin.getCurrentView();
			if (view.getCapletModel() == this) {
				View symbolView = (View) view;
				symbolView.setLinksVisibilityMode(value);
				view.invalidate(IViewInvalidationEnum.eFull);
			}
		}
	}

	/**
	 * Gets the design attribute of the Model object
	 *
	 * @return The design value
	 */
	public IAbstractLibrary getLibrary()
	{

		return m_symlib;
	}

	/**
	 * Gets the diagram attribute of the Model object
	 *
	 * @return The diagram value
	 */
	public IPSMStamp getSymbolDef()
	{

		return (IPSMStamp) UIDMgr.getObject(m_symdef);
	}

	/**
	 * Overridden here to enable on the fly scrubbing for CSymbol - dts0100627899
	 *
	 * @return The SymbolOnTheFlyScrubber
	 */
	@Override @Nullable public OnTheFlyScrubber getScrubber()
	{
		return scrubber;
	}

	public void setModified(boolean modified)
	{
		super.setModified(modified);
		if (m_symdef != null) {
			getSymbolDef().setEdited(modified);
		}
	}

	public IBaseDiagram getDiagram()
	{
		return m_sheet;
	}

	public ISheet getSheet()
	{
		return getDiagram();
	}

	/**
	 * Description of the Method
	 *
	 * @param oldObj Description of the Parameter
	 * @param newObj Description of the Parameter
	 */
	public void replaceObject(IUndoableObject oldObj, IUndoableObject newObj)
	{
		IStamp stamp = getSymbolDef();
		if (oldObj instanceof IIndexedNamedObject) {
			IIndexedNamedObject namedObj = (IIndexedNamedObject) oldObj;
			namedObj.getNameMgr().removeObject(namedObj);
		}
		if (newObj instanceof IIndexedNamedObject) {
			IIndexedNamedObject namedObj = (IIndexedNamedObject) newObj;
			// add the new object to the name manager
			namedObj.getNameMgr().addObject(namedObj);
		}
	}

	public boolean canBePersisted()
	{
		if (!hasPermissionToEdit) {
			return false;
		}
		// Don't allow saving if empty
		IBaseDiagram diagram = getDiagram();
		if (diagram != null) {
			IGfxObjectIterator iter = diagram.getObjects();
			if (iter.getSize() > 0) {
				return true;
			}
		}
		return false;
	}

	public boolean isEditable()
	{
		IPSMStamp symbol = getSymbolDef();
		return symbol != null && symbol.isLocked();
	}

	/**
	 * ICapletModel#getModelRoot()
	 */
	public IUIDObject getModelRoot()
	{
		return getSymbolDef();
	}

	/**
	 * Does this model contain this object, or a representation of this object?
	 *
	 * @param uidObject Object
	 * @return true if object contained in this model
	 */
	public boolean containsObject(IUIDObject uidObject)
	{
		if (uidObject instanceof IDiagramObject) {
			IBaseDiagram diagram = DiagramHelper.getBaseDiagram((IDiagramObject) uidObject);
			return diagram == getDiagram();
		}
		else if (uidObject instanceof ILogicObject) {
			if (getDiagram().getRepresentations(uidObject.getUID()).getSize() > 0) {
				return true;
			}
		}
		return false;
	}

	@Nullable public PersistantCMReport getCMReport(boolean createNewFlag)
	{
		return null;
	}

	public void destroy()
	{
		super.destroy();

		m_symlib = null;
		m_symdef = null;
		m_sheet = null;
	}

	@Override public boolean allowUndoInReadOnlyMode()
	{
		return false;
	}

	public void setDrawingObjectSnap(boolean value)
	{
		super.setDrawingObjectSnap(value);

		// Update the status bar of any views presenting this model.
		for (ICAFWindow window : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			ICapletWindow capWin = (ICapletWindow) window;
			ICapletView view = capWin.getCurrentView();
			View symView = (View) view;
			symView.setSnapToObjectMode(value);
		}
	}

	public void setDrawingGridSnap(boolean value)
	{
		super.setDrawingGridSnap(value);

		// Update the status bar of any views presenting this model.
		for (ICAFWindow window : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			ICapletWindow capWin = (ICapletWindow) window;
			ICapletView view = capWin.getCurrentView();
			if (view.getCapletModel() == this) {
				View harnessView = (View) view;
				harnessView.setSnapToGridMode(value);
			}
		}
	}

	@Override public boolean isHighlightAllowed()
	{
		return true;
	}

	@Override public boolean isHandleModificationAllowed()
	{
		return true;
	}
}


