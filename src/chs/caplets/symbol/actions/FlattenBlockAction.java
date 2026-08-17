/*
 * Copyright 2005-2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.symbol.Model;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.ITransform;
import chs.cof.draw.ITransformCompound;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IPropText;
import chs.cof.drawplus.IXRefPlaceholder;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utility.FlatBlockSourceObjectProcessor;
import chs.utility.Replicator;
import chs.utility.TransformUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

public class FlattenBlockAction extends ControllerActionRT implements ICtxMenuProvider
{

	private Model m_model;
	private String m_ctxCommand = null;

	public FlattenBlockAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	public boolean onTerminate(boolean successful)
	{
		if (successful) {
			// Get our factories
			ICompoundObject sheet = m_model.getSheet();

			Replicator replicator = new Replicator(Replicator.INSTANTIATE, true);
			replicator.setInternalToSymbolApp(true);

			// Flatten the selected blocks graphics.
			SelectSet preSelections = getController().getSelectMgr().getPreSelections();
			SelectedUIDObjectIterator iter = preSelections.getSelectedUIDObjects();
			while (iter.hasNext()) {
				IUIDObject obj = iter.getNext();
				if (obj instanceof IBlock) {
					IBlock block = (IBlock) obj;
					block.setIsFlat(true);

					ITransform trans = ((ITransformCompound) block).getTransform();
					AffineTransform blkTrans = trans.getAffineTransform();

					replicator.setNewObject(block.getPinList().getUID(),
							((ISymbolDef) m_model.getSymbolDef()).getPinList());
					replicator.setNewObject(block.getPinList().getConnectivity().getUID(),
							((ISymbolDef) m_model.getSymbolDef()).getPinList().getConnectivity());

					// TODO - This is a hack to work around a bug with compound objects containing BoundedText
					// When iterating through the objects in a compound object and removing them, any BoundedText
					// Objects will not be removed. So we remove all objects andd add back the ones we want to keep.
					// This should be removed when the bug is fixed.
					// TODO - Hack mentioned above, Save this to add back.
					List<IGfxObject> keepObjects = new ArrayList<>();

					ICompoundObject cObj = block.getGfx();
					FlatBlockSourceObjectProcessor processor = new FlatBlockSourceObjectProcessor()
					{
						@Override public void processAttributeText(@NotNull IAttributeText attributeText)
						{
							keepObjects.add(attributeText);
						}

						@Override public void processPropText(@NotNull IPropText propText)
						{
							keepObjects.add(propText);
						}

						@Override public void processXRefPlaceHolderText(@NotNull IXRefPlaceholder refPlaceholder)
						{
							keepObjects.add(refPlaceholder);
						}

						@Override public void processPin(@NotNull IGenericSchemPin genericSchemPin)
						{
							IGenericSchemPin oldPin = genericSchemPin;
							IGenericPin oldAbsPin = oldPin.getConnectivity();

							ILocation loc = oldPin.getLocation();

							// Hack alert - call setNewObject with the connectivity so
							//   the replicator thinks the connectivity has already been
							//   replicated. This will cause the schem pin to be replicated
							//   and just use the old connectivity.
							replicator.setNewObject(oldAbsPin.getUID(), oldAbsPin);

							IPinList plist = block.getPinList().getConnectivity();
							IGenericSchemPin newPin = replicator.replicate(oldPin, plist);

							ILocation newLoc = newPin.getLocation();
							newLoc.setX(loc.getX());
							newLoc.setY(loc.getY());

							cObj.removeObject(oldPin);
							//cObj.addObject(newPin);
							keepObjects.add(newPin);
							oldPin.delete();
						}

						@Override public void processInternalLink(@NotNull ISchemInternalLink schemInternalLink)
						{
							ISchemInternalLink oldLink = schemInternalLink;
							IInternalLink oldAbsLink = oldLink.getConnectivity();

							ILocation loc = oldLink.getLocation();

							// Hack alert - call setNewObject with the connectivity so
							//   the replicator thinks the connectivity has already been
							//   replicated. This will cause the schem link to be replicated
							//   and just use the old connectivity.
							replicator.setNewObject(oldAbsLink.getUID(), oldAbsLink);

							ISchemInternalLink newLink = replicator.replicate(oldLink, false);
							assert newLink != null; // we should probably make replicate an operation that cannot fail
							ILocation newLoc = newLink.getLocation();
							newLoc.setX(loc.getX());
							newLoc.setY(loc.getY());

							cObj.removeObject(oldLink);
							keepObjects.add(newLink);
							oldLink.delete();
						}

						@Override protected void processBlock(@NotNull IBlock block)
						{
							//ignore. this case should not arise.
						}

						@Override public void processOthers(@NotNull IGfxObject gfxObj)
						{
							IGfxObject newObj = replicator.replicateGfxToPropertied(gfxObj, 1.0, false);

							// Remove from block
							// TODO - Hack mentioned above this is where the remove should be.
							// Uncomment when main bug is fixed.
							//cObj.removeObject(gfxObj);

							TransformUtils.transform(block, blkTrans, newObj);

							// Add to symbol sheet
							sheet.addObject(newObj);
						}
					};
					processor.execute(cObj);

					// TODO - This is a hack to work around a bug with compound objects containing BoundedText
					// When iterating through the objects in a compound object and removing them, any BoundedText
					// Objects will not be removed. So we remove all objects andd add back the ones we want to keep.
					// This should be removed when the bug is fixed.
					cObj.removeAllObjects();
					for (IGfxObject gfxObj : keepObjects) {
						cObj.addObject(gfxObj);
					}
				}
			}
		}

		((ISymbolDef) m_model.getSymbolDef()).connectInternalLinks(false);
		List<IUID> flattendBlocks = new ArrayList<IUID>();
		SelectSet preSelections = getController().getSelectMgr().getPreSelections();
		SelectedUIDObjectIterator iter = preSelections.getSelectedUIDObjects();
		while (iter.hasNext()) {
			IUIDObject obj = iter.getNext();
			if (obj instanceof IBlock) {
				flattendBlocks.add(obj.getUID());
			}
		}
		((ISymbolDef) m_model.getSymbolDef()).updateNodeLoc(flattendBlocks);

		getController().getSelectMgr().getPreSelections().clear();
		//
		// Refresh
		//
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		return true;
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return FlattenBlockActionUI.class.getName();
	}

	public boolean isEnabled()
	{
		boolean enable = false;
		//dts0100304219 - Since we have now added this to the Edit menu it makes sense to only enable
		// it when the selection is a block
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		if (selections.getSelectCount() >= 1) {
			SelectionIterator iter = selections.getSelected();
			while (iter.hasNext()) {
				Selection sel = iter.getNext();
				if (IBlock.class.isAssignableFrom(sel.getSelectionClass())) {
					if (!((IBlock) sel.getObject()).isFlat()) {
						enable = true;
						break;
					}
				}
			}
		}
		return enable && super.isEnabled();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		boolean populate = isEnabled();

		Action actionUI = getActionUI();
		if (populate && actionUI != null) {
			String shortDesc = (String) actionUI.getValue(Action.SHORT_DESCRIPTION);
			if (m_ctxCommand == null || !m_ctxCommand.equalsIgnoreCase(shortDesc)) {
				// Make a private copy for command name
				m_ctxCommand = shortDesc;
			}
			container.add(new ActionEntry(actionUI, m_ctxCommand));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}
