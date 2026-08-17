/*
 * Copyright 2004-2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.AddInstanceBaseAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.symbol.Model;
import chs.cof.draw.IGriddable;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolRef;
import chs.cof.symbol.SymbolTypeEnum;
import chs.ctf.ui.form.SymbolSelectionEvent;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utility.Replicator;
import chs.utility.SymbolUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.ReplicateBlockHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public class AddBlockInstanceAction extends AddInstanceBaseAction
{

	private Reference<ICapletModel> m_model;
	private IBlock m_block;

	public AddBlockInstanceAction(ICapletController controller)
	{
		super(controller);
		m_model = new WeakReference<ICapletModel>(controller.getCapletModel());
	}

	private Model getModel()
	{
		return (Model) m_model.get();
	}

	@Nullable private ISymbolDef getSymbolDef()
	{
		return CommonUtils.cast(m_symdef, ISymbolDef.class);
	}

	protected IDiagramObject getPlacementObject()
	{
		//
		// Makes a copy of the symbol, and places that down.
		//
		//
		ISymbolDef sd = getSymbolDef();
		if (sd != null) {
			cleanBlock();

			Replicator replicator = createReplicator();
			final double scale = getScale(sd);
			if (sd.getNumBlocks() > 0) {
				// per dts0100889783 we now need to place the block and its nested blocks as a single block
				m_block = FactoryMgr.getSymbolFactory().constructBlock(FactoryMgr.createUID(), sd.getName());
				IPinList replicatedSymbolDef = replicator.replicate(sd, scale);

				m_block.setPinList(replicatedSymbolDef);
				m_block.setConnectivity(replicatedSymbolDef.getConnectivity());
			}
			else {
				m_block = replicator.replicateBlock(sd, scale);
			}
			if (!SymbolUtils.isUnitScale(scale)) {
				SymbolUtils.adjustOffGridPinsToAGridPoint(m_block.getPinList(), getGrid());
			}
			ISymbolRef sref = FactoryMgr.getSymbolFactory().constructSymbolRef(m_symdef);
			m_block.setSymbolRef(sref);
		}
		return m_block;
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		cleanBlock();
		return super.onActivate(e);
	}

	private Replicator createReplicator()
	{
		Replicator replicator = new Replicator(Replicator.INSTANTIATE, true);
		replicator.setInternalToSymbolApp(true);
		return replicator;
	}

	protected IGriddable getGriddable()
	{
		return (IGriddable) getModel().getSheet();
	}

	public boolean onTerminate(boolean successful)
	{
		//
		// Clear the old dynamics.
		//
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();

		if (successful) {

			// dts0100591391, if the mouse hasn't been moved since the action started (possible if you use
			// keyboard shortcuts and the cursor is already where you want to place the block) then at this point
			// m_block will be null, so prepare for the placement here...
			if (m_block == null) {
				m_block = (IBlock) updateTransients();
			}
			//
			// We will instantiate the object at the specified point.

			//
			// We don't have to deal with nested blocks since they were fixed in onActivate.
			ISymbolDef symbolDef = (ISymbolDef) getModel().getSymbolDef();
			// Rename the block, add it onto the symbolDef, fix properties
			ReplicateBlockHelper.instantiateBlock(symbolDef, m_block);
			AddBlockActionHelper helper = null;
			if (symbolDef.getSymbolType() == SymbolTypeEnum.FUNCTION) {
				helper = new AddFunctionBlockActionHelper(m_block);
			}
			else {
				helper = new AddDeviceBlockActionHelper(m_block);
			}
			// loop through the connectivity pin list setting the names of the pins. This can't be done in the
			// symbol editor because it would turn off default naming, so we do it here when we instantiate
			// the symbol
			helper.setPinNames();

			IPinList pinList = (IPinList) m_block.getGfx();

			helper.connectInternalLinks(pinList);
			helper.checkforDuplicatePinNames((ISymbolDef) getModel().getSymbolDef());

			ConnectionHelper connectionHelper = new ConnectionHelper();
			connectionHelper.examineGraphics(pinList, CAFUtils.getInstance().getActiveGfxContext(),
					getModel().getSheet());
		}
		else if (m_block != null) {
			cleanBlock();
		}

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		m_block = null;
		m_drawing = false;
		return true;
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return AddBlockInstanceActionUI.class.getName();
	}

	public void destroy()
	{
		m_dynamics = null;
		super.destroy();
	}

	public boolean isEnabled()
	{
		if (!super.isEnabled()) {
			return false;
		}

		Model m = getModel();

		IStamp sym = acquireSymbol();
		if (sym == null) {
			return false;
		}
		IStamp stmp = m.getSymbolDef();
		if (stmp == sym) {
			// don't allow self instantiating
			return false;
		}
		if (stmp instanceof ISymbolDef) {
			// Can't instance blocks on a backshell splice, or comment symbol! 
			if (SymbolUtils.isBackshellSymbol((ISymbolDef) stmp)
					|| SymbolUtils.isSpliceSymbol((ISymbolDef) stmp)
					|| SymbolUtils.isCommentSymbol((ISymbolDef) stmp)) {
				return false;
			}

			// Can't instance block of a different type than the composite symbol.
			if (sym instanceof ISymbolDef) {
				if (((ISymbolDef) sym).getSymbolType() != ((ISymbolDef) stmp).getSymbolType()) {
					return false;
				}
			}
		}
		return true;
	}

	private void cleanBlock()
	{
		deleteObject(m_block);
		m_block = null;
	}

	public void symbolSelectionChanged(SymbolSelectionEvent sse)
	{
		super.symbolSelectionChanged(sse);

		// Also prevent self-instantiation
		IStamp sel = sse.getSymbol();
		if (sel == getModel().getSymbolDef()) {
			m_enabled = false;
			getActionUI().setEnabled(false);
		}
	}

	@Override public void mouseEntered(MouseEvent e)
	{
		// During automation, the mouse doesn't gradually move to a point, but actually jumps from an point outside
		// canvas to a point within canvas. As a result, the transient graphics (preview) is not shown if the mouse is
		// not moved any further within the canvas and the block is placed at this point. Subsequent operations like
		// rotate/flip depends on mouseMove() being called to correctly place the block after such operations.
		// (As part of fix for dts0100937805).
		mouseMoved(e);
	}

	@Override protected boolean shouldScaleElectricalSymbols(@NotNull ISymbolDef symbolDef)
	{
		return true;
	}
}