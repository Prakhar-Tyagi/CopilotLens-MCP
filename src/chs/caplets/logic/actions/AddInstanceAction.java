/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2026 Siemens
 */
package chs.caplets.logic.actions;

import chs.analysis.IDesignPort;
import chs.analysis.IPort;
import chs.analysis.IVHDLFailureDataMapping;
import chs.analysis.IVHDLModelMapping;
import chs.analysis.sv.model.mapping.DesignPort;
import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.AddInstanceBaseAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.ghc.ConnectivityGHCHelper;
import chs.caplets.logic.function.FunctionController;
import chs.cof.draw.FlipAxisEnum;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGriddable;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramText;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IGroundDevice;
import chs.cof.logical.cable.IHarnessPlugConnector;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignSharedPinListUsage;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IBlockIterator;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.SymbolTypeEnum;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IAttributePropertyProvider;
import chs.common.ISymbolledPin;
import chs.common.IUID;
import chs.common.IUIDProvider;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinListPlaceOptionParams;
import chs.services.gfx.GfxView;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.ConductorSplitter;
import chs.utility.DiagramHelper;
import chs.utility.SymbolUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.logic.LogicUtils;
import chs.utility.preferences.PreferenceSetHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds a new schematic + connectivity instance with a user specified symbol to a logic diagram.
 */
public class AddInstanceAction extends AddInstanceBaseAction
{
	protected static final int DEGREES0 = 0;
	protected static final int DEGREES180 = 180;
	protected List<IBlock> blkList = new ArrayList<IBlock>();
	protected boolean bCreatePinList = false;
	private boolean bAddPinListDlgShown = false;
	private List<IPinList> m_PinListsSelected = new ArrayList<IPinList>();
	private List<IPinList> m_PinListsToPlace = new ArrayList<IPinList>();

	// Map to track which block each pin list being placed came from,
	// so that we can calculate the correct offset for each pin list when placing as a group
	private Map<IPinList, IBlock> m_PinListToBlockMap = new HashMap<IPinList, IBlock>();

	// Variable to store the KeyEvents used in the action
	private List<Integer> m_userKeyEvents = new ArrayList<Integer>();

	// If placing blocks from a composite symbol as a group, this flag indicates whether to place as a group or individually
	protected boolean m_AsGroup;

	private Reference<ICapletModel> m_model;
	@Nullable protected IPinList m_pinlist;
	protected boolean reference;


	public AddInstanceAction(ICapletController controller)
	{
		super(controller);
		m_model = new WeakReference<ICapletModel>(controller.getCapletModel());
	}

	protected Model getModel()
	{
		return (Model) m_model.get();
	}

	protected IDiagramObject getPlacementObject()
	{
		//
		// Makes a copy of the symbol, and places that down.
		//
		if (bCreatePinList && m_pinlist != null) {
			return m_pinlist;
		}
		if (m_symdef instanceof ISymbolDef) {
			cleanPinlist();
			ISymbolDef symDef = (ISymbolDef) m_symdef;
			double scale = getScale(symDef);
			m_pinlist = SymbolUtils
					.createSchematicInstance(getModel().getDesign().getProject(), symDef, block, scale, true);
			if (!SymbolUtils.isUnitScale(scale)) {
				SymbolUtils.adjustOffGridPinsToAGridPoint(m_pinlist, getGrid());
			}
			if (bCreatePinList) {
				m_PinListsToPlace.add(m_pinlist);
			}
			//
			// Sort out the reference to the symbol.
			//
			m_symdef.getServerTimeModified();
		}
		return m_pinlist;
	}

	protected IGriddable getGriddable()
	{
		return getModel().getDiagram();
	}

	/**
	 * Overridden here to allow the selection of a block instance from a composite symbol
	 */
	protected IStamp acquireSymbol()
	{
		// get the "stamp" currently selected in the symbol browser tree
		IStamp stamp = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr().getActiveSymbol();

		// if the stamp is a composite symbol, a block may also be specified on the action (e.g. via a dialog)
		if (!bAddPinListDlgShown && !setupBlock(stamp)) {
			stamp = null; // e.g. user cancelled block select dialog
		}
		return stamp;
	}

	/**
	 * If the stamp is a composite symbol, optionally select one of it's blocks, the composite or nothing to cancel the
	 * action. If a block is selected, it is stored in the field for this action.
	 *
	 * @param stamp The stamp which may or may not be a composite symbol with blocks
	 *
	 * @return true to select either the whole composite or a block, false to cancel the whole action
	 */
	protected boolean setupBlock(IStamp stamp)
	{
		if (stamp instanceof ISymbolDef symDef) {
			if (checkForDuplicatePinNames(
					symDef)) {//&& symDef.getNumBlocks() > 0 -- No symbol or Composite having duplicate pins cannot be instantiated
				ResourceBasedMessageContent messageContent;
				if (SymbolUtils.isFunctionSymbol(symDef)) {
					messageContent = new ResourceBasedMessageContent(AddInstanceAction.class,
							"AddInstanceAction.DuplicatePortNames");
				}
				else {
					messageContent = new ResourceBasedMessageContent(AddInstanceAction.class,
							"AddInstanceAction.DuplicatePinNames");
				}
				messageContent.setImplicationsParameters(symDef.getName());
				messageContent.setGuidanceParameters("Capital Symbol");
				Message.show(PromptSeverity.ERROR, messageContent);
				return false;
			}
			if (symDef.getNumBlocks() > 0) {
				// reuse the AddPinList dialog with a null pinlist to show only the symbol selection panel
				IPlacementOptionParams params = createPlacementOptionParams(symDef);
				AddPinListDialog dialog =
						new AddPinListDialog(CAFUtils.getInstance().getDialogFrame(), symDef, params);
				dialog.setTitle((String) getActionUI().getValue(Action.SHORT_DESCRIPTION)); // ok for all actions?
				// For composite symbols, allow the user to select whether to place as a group if selecting blocks
				dialog.enableSymbolPlaceOptions(true);
				// If the symbol is a composite, do not allow mixed selection of blocks and the composite, but only if placing as a group
				dialog.setAllowTreeMixedSelection(!dialog.getPlaceAsGroup());
				if (dialog.selectPinList()) {
					// if the user selected blocks, get the blocks to be added and whether to place as a group or individually
					m_AsGroup = dialog.getPlaceAsGroup();
					IBlock[] blocks = dialog.getBlocks();
					if (blocks != null && blocks.length != 0) {
						getBlocksToBeAdded(symDef, blocks);
					}
					reference = dialog.getReference();
				}
				else {
					return false; // user cancelled
				}
			}
		}
		return true;
	}

	@NotNull protected IPlacementOptionParams createPlacementOptionParams(@NotNull ISymbolDef symDef)
	{
		return new PinListPlaceOptionParams(symDef);
	}

	@SuppressWarnings("UnusedParameters")
	protected void addingBlockInstance(IPinList schemPL, ISymbolDef symDef, IBlock blockDef)
	{

	}

	/**
	 * Adds blocks from a composite symbol to the schematic as a group.
	 *
	 * @param symDef The symbol definition (`ISymbolDef`) representing the composite symbol.
	 * @param blocks An array of blocks (`IBlock[]`) to be added to the schematic.
	 */
	private void getBlocksToBeAddedAsGroup(@NotNull ISymbolDef symDef, @NotNull IBlock[] blocks)
	{
		double scale = getScale(symDef);

		// Look for the composite in the list of blocks to add
		for (IBlock iBlock : blocks) {
			if (iBlock == null) {
				m_AsGroup = false;
				// add the composite symbol as the first block to be added and create a schematic instance for it
				getBlocksToBeAddedIndividually(symDef, blocks);
				// if the composite symbol is in the list of blocks to add, return after placing the composite symbol
				// it's not allowed to place composite symbol and a subset as a group simultaneously as that would be ambiguous for the user
				return;
			}
		}

		// Place all blocks as a group, so add them all to the symbol definition and create schematic instances for each of them to be placed together
		int numLabelsKept = 0;
		for (IBlock value : blocks) {
			if (value != null) {
				IPinList schemePL = SymbolUtils.createSchematicInstance(getModel().getDesign().getProject(), symDef,
						value, scale, false);

				if (!SymbolUtils.isUnitScale(scale)) {
					SymbolUtils.adjustOffGridPinsToAGridPoint(schemePL, getGrid());
				}

				// hide labels from the blocks pin list keeping only one visible label for the whole group of blocks,
				// so that when placing as a group the user is not overwhelmed with multiple labels and can still see a label for the group
				int numLabels = HideLabelsFromSchematicPinList(schemePL, numLabelsKept == 0 ? 1 : 0);
				numLabelsKept += numLabels;

				m_PinListsToPlace.add(schemePL);
				m_PinListToBlockMap.put(schemePL, value);
			}
		}

		// Create composite symbol for the preview during placement of the group

		blkList.add(blocks[0]);

		// Remove blocks from symbol definition as they are not selected to be added to the schematic
		Set<IBlock> blockSet = new HashSet<>(Arrays.asList(blocks));
		for (IBlock iBlock : symDef.getBlocks()) {
			if (!blockSet.contains(iBlock)) {
				symDef.removeBlock(iBlock);
			}
		}

		// Use whilstUndoDisabledDo to prevent the preview from being tracked in undo
		// since it's never added to a diagram (just for visual preview)
		IPinList[] previewPinList = new IPinList[1];
		CreationDeletionHelper.whilstUndoDisabledDo(() -> {
			IPinList schemaPL1 = SymbolUtils.createSchematicInstance(getModel().getDesign().getProject(), symDef,
					null, scale, false);
			// Hide labels from the resulting schematic instance pin list
			HideLabelsFromSchematicPinList(schemaPL1, 0);
			previewPinList[0] = schemaPL1;
		});

		m_PinListsSelected.add(previewPinList[0]);
		m_pinlist = m_PinListsSelected.remove(0);
		block = blkList.remove(0);
	}

	/**
	 * Adds blocks from a composite symbol to the schematic individually.
	 *
	 * @param symDef The symbol definition (`ISymbolDef`) representing the composite symbol.
	 * @param blocks An array of blocks (`IBlock[]`) to be added to the schematic.
	 */
	private void getBlocksToBeAddedIndividually(@NotNull ISymbolDef symDef, @NotNull IBlock[] blocks)
	{
		// Place blocks individually.
		double scale = getScale(symDef);

		for (IBlock iBlock : blocks) {
			blkList.add(iBlock);
			IPinList schemaPL = SymbolUtils.createSchematicInstance(getModel().getDesign().getProject(), symDef,
					iBlock, scale, false);

			if (!SymbolUtils.isUnitScale(scale)) {
				SymbolUtils.adjustOffGridPinsToAGridPoint(schemaPL, getGrid());
			}
			addingBlockInstance(schemaPL, symDef, iBlock);
			m_PinListsSelected.add(schemaPL);
			m_PinListsToPlace.add(schemaPL);
		}
		m_pinlist = m_PinListsSelected.remove(0);
		block = blkList.remove(0);
	}

	/**
	 * Determines how blocks from a composite symbol should be added to the schematic.
	 * <p>
	 * If the composite symbol is being placed as a group, the blocks are added together.
	 * Otherwise, the blocks are added individually.
	 *
	 * @param symDef The symbol definition (`ISymbolDef`) representing the composite symbol.
	 * @param blocks An array of blocks (`IBlock[]`) to be added to the schematic.
	 */
	protected void getBlocksToBeAdded(@NotNull ISymbolDef symDef, @NotNull IBlock[] blocks)
	{
		if (m_AsGroup) {
			// Check if the composite symbol is being placed as a group.
			getBlocksToBeAddedAsGroup(symDef, blocks);
		}
		else {
			// Place blocks individually.
			getBlocksToBeAddedIndividually(symDef, blocks);
		}
	}

	/**
	 * Hide labels from a schematic pin list by hiding all but a specified number of visible diagram text elements.
	 *
	 * @param schemPL        The schematic pin list (`IPinList`) containing the diagram text elements.
	 * @param keepLabelCount The number of visible labels to keep; all others will be hidden.
	 *
	 * @return The total number of labels visible.
	 */
	private int HideLabelsFromSchematicPinList(@NotNull IPinList schemPL, int keepLabelCount)
	{
		int labelCount = 0;
		// Collect all visible diagram text elements and hide all but the first keepLabelCount.
		List<IGfxObject> gfxObjectsList = schemPL.getObjects().stream()
				.filter(gfxObj -> gfxObj instanceof IDiagramText && gfxObj.isVisible() &&
						((IDiagramText) gfxObj).getTextValue().contains("DEV-"))
				.toList();
		for (IGfxObject gfxObj : gfxObjectsList) {
			gfxObj.setMarkedVisible(!(labelCount >= keepLabelCount));
			labelCount++;
		}

		return labelCount < keepLabelCount ? labelCount : keepLabelCount;
	}

	public void keyPressed(int keyCode)
	{
		if (m_AsGroup) {
			switch (keyCode) {
				case (KeyEvent.VK_R): // Rotate +90
					m_userKeyEvents.add(KeyEvent.VK_R);
					RotatePinLists(m_PinListsToPlace, DEGREES90, m_currValidPoint.x, m_currValidPoint.y, 0, 0);
					break;
				case (KeyEvent.VK_T): // Rotate -90
					m_userKeyEvents.add(KeyEvent.VK_T);
					RotatePinLists(m_PinListsToPlace, DEGREES270, m_currValidPoint.x, m_currValidPoint.y, 0, 0);
					break;
				case (KeyEvent.VK_F):
					m_userKeyEvents.add(KeyEvent.VK_F);
					for (IPinList pl : m_PinListsToPlace) {
						switch (m_flipState) {
							case (0):
								pl.flip(FlipAxisEnum.XAxis, m_currValidPoint.x, m_currValidPoint.y, 0, 0);
								break;
							case (1):
								pl.flip(FlipAxisEnum.XAxis, m_currValidPoint.x, m_currValidPoint.y, 0, 0);
								pl.flip(FlipAxisEnum.YAxis, m_currValidPoint.x, m_currValidPoint.y, 0, 0);
								break;
							case (2):
								pl.flip(FlipAxisEnum.YAxis, m_currValidPoint.x, m_currValidPoint.y, 0, 0);
								break;
							default:
								break;
						}
					}
					break;
				default:
					break;
			}
		}

		// Call the base class to handle the composite shown in the preview during placement which also needs
		// to be rotated/flipped as the user makes these adjustments
		super.keyPressed(keyCode);
	}

	// Rotate a list of pin lists by a specified angle around a reference point with an offset
	private void RotatePinLists(List<IPinList> pinLists, int degrees, int ref_x, int ref_y, int offset_x, int offset_y)
	{
		for (IPinList pl : pinLists) {
			pl.rotate(degrees, ref_x, ref_y, offset_x, offset_y);
		}
	}

	/**
	 * Calculates the effective angle of rotation in degrees.
	 *
	 * @return The effective angle of rotation in degrees (0, 90, 180, or 270).
	 */
	private int getEffectiveAngle(int rotations) {
		int remainder = rotations % 4;
		// In Java, the modulo of negative numbers can be negative
		// Normalize to the range [0, 3]
		if (remainder < 0) {
			remainder += 4;
		}
		return remainder * DEGREES90;
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		// reset first...
		//
		cleanPinlist();
		return super.onActivate(e);
	}

	@Override public void mouseReleased(MouseEvent e)
	{
		if (!blkList.isEmpty()) {
			CreationDeletionHelper.getTheCreationHelper().addCreationObject(m_pinlist);
			bAddPinListDlgShown = true;
			block = blkList.remove(0);
			m_pinlist = m_PinListsSelected.remove(0);
			m_placementObject = m_pinlist;
			m_dynamics.addTransientGfx(m_placementObject);
		}
		else {
			bAddPinListDlgShown = false;
			getController().getActionMgr().terminateActiveAction(true);
		}
	}

	public boolean isEnabled()
	{
		ICapletController capletController = CAFUtils.getInstance().getActiveCapletController();

		return !(m_symdef != null && capletController != null && m_symdef instanceof ISymbolDef &&
				(SymbolUtils.isFunctionSymbol(
						(ISymbolDef) m_symdef) ^ (capletController instanceof FunctionController))) &&
				getModel().isEditable() && super.isEnabled();
	}

	public void mouseClicked(MouseEvent e)
	{
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		bCreatePinList = true;
		super.mouseMoved(e);
		bCreatePinList = false;
	}

	protected boolean removeDynamicGfx()
	{
		return false;
	}

	public boolean onTerminate(boolean successful)
	{
		// Clear the old dynamics.
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
		boolean iFirstBlock = true;
		chs.cof.logical.cable.IPinList cablePL = null;

		for (IPinList pl : m_PinListsToPlace) {
			m_pinlist = pl;
			if (successful && m_pinlist != null) {
				IBlock blockForPinList = m_PinListToBlockMap.get(pl);
				if (m_AsGroup && m_currValidPoint != null && blockForPinList != null) {
					// If placing as a group, calculate the correct offset for each pin list based on the block it came from and the current rotation state
					BlockOffset offsets = calculateOffsetAfterRotationFlipActions(m_userKeyEvents,
							new BlockOffset(blockForPinList.getLocation().getX(), blockForPinList.getLocation().getY()));
					// Update the pin list location with the calculated offsets
					m_pinlist.getLocation().setX(m_currValidPoint.x + offsets.getOffsetX());
					m_pinlist.getLocation().setY(m_currValidPoint.y + offsets.getOffsetY());

					block = blockForPinList;
				}

				if (iFirstBlock) {
					iFirstBlock = false;
					addInstance(); // ignore return - always clean up
					cablePL = m_pinlist.getConnectivity();
				}
				else {
					AddSymbolledPinListAction addPL =
							new AddSymbolledPinListAction(getController(), cablePL, null, block, reference);
					addPL.setPinList(m_pinlist);
					addPL.addInstance();
				}
			}
			else {
				// DR's 359502 and 547650: cleanup following aborted add, if there's anything to cleanup.
				cleanPinlist();
			}
		}
		m_userKeyEvents.clear();

		// get the "stamp" currently selected in the symbol browser tree
		IStamp stamp = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr().getActiveSymbol();
		if (stamp instanceof ISymbolDef && ((ISymbolDef) stamp).getSymbolType() != SymbolTypeEnum.COMMENT) {
			ISymbolDef symbol = (ISymbolDef) stamp;
			IVHDLModelMapping mapping = symbol.getConnectivity().getModelMapping();
			if (mapping != null && cablePL != null) {
				transferModelMappingInformation(cablePL, mapping);
			}
		}
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
		}
		m_drawing = false;
		m_pinlist = null;
		m_PinListsToPlace.clear();
		m_PinListsSelected.clear();
		blkList.clear();
		m_PinListToBlockMap.clear();
		m_symdef = null;
		block = null;
		bAddPinListDlgShown = false;
		return successful;
	}

	/**
	 * Calculates the offset adjustments for a block after applying a sequence of rotation and flip actions.
	 *
	 * @param userKeyEvents A list of key events (`List<Integer>`) representing the sequence of user actions.
	 *                      - `KeyEvent.VK_R`: Rotate +90 degrees.
	 *                      - `KeyEvent.VK_T`: Rotate -90 degrees.
	 *                      - `KeyEvent.VK_F`: Flip the block.
	 * @param blockOffset   A `BlockOffset` object containing the original X and Y offsets of the block.
	 *                      - `getOffsetX()`: Returns the original X offset.
	 *                      - `getOffsetY()`: Returns the original Y offset.
	 * @return A `BlockOffset` object containing the adjusted X and Y offsets after applying the actions.
	 * - `getOffsetX()`: The adjusted X offset.
	 * - `getOffsetY()`: The adjusted Y offset.
	 */
	@NotNull private BlockOffset calculateOffsetAfterRotationFlipActions(List<Integer> userKeyEvents,
			BlockOffset blockOffset)
	{
		int offsetX = blockOffset.getOffsetX();
		int offsetY = blockOffset.getOffsetY();
		int rotate = 0;
		int flipState = 0;
		int previousEvent = 0;

		// Iterate through the list of user key events to calculate the cumulative effect of rotations and flips
		for (int i = 0; i < userKeyEvents.size(); i++) {
			boolean isLast = (i == userKeyEvents.size() - 1);
			int event = userKeyEvents.get(i);

			switch (event) {
				case (KeyEvent.VK_R): // Rotate +90
				case (KeyEvent.VK_T): // Rotate -90
					if (previousEvent == 0 || previousEvent == KeyEvent.VK_R || previousEvent == KeyEvent.VK_T) {
						// Accumulate rotation based on the event type
						rotate += (event == KeyEvent.VK_R) ? 1 : -1;
					}
					else {
						rotate++;
						// Apply the current flip state before starting a new rotation sequence
						BlockOffset offsets = calculateFlipBlockOffset(flipState, offsetX, offsetY);
						offsetX = offsets.getOffsetX();
						offsetY = offsets.getOffsetY();
						flipState = 0; // reset flip state when rotating as a new rotation sequence starts
					}
					break;
				case (KeyEvent.VK_F): // Flip
					flipState++;
					// Apply the current rotation before starting a new flip sequence
					if (previousEvent != 0 && previousEvent != KeyEvent.VK_F) {
						BlockOffset offsets = calculateRotationBlockOffset(getEffectiveAngle(rotate), offsetX, offsetY);
						offsetX = offsets.getOffsetX();
						offsetY = offsets.getOffsetY();
						rotate = 0; // reset rotation when flipping as a new flip sequence starts
					}
					break;
				default:
					break;
			}

			previousEvent = event;

			if (isLast) {
				// Apply any remaining rotations or flips at the end of the sequence
				if (rotate != 0) {
					BlockOffset offsets = calculateRotationBlockOffset(getEffectiveAngle(rotate), offsetX, offsetY);
					offsetX = offsets.getOffsetX();
					offsetY = offsets.getOffsetY();
				}
				if (flipState != 0) {
					BlockOffset offsets = calculateFlipBlockOffset(flipState, offsetX, offsetY);
					offsetX = offsets.getOffsetX();
					offsetY = offsets.getOffsetY();
				}
			}
		}
		return new BlockOffset(offsetX, offsetY);
	}

	/**
	 * Calculates the correct offset for the pin list based on the block's original location and the
	 * current rotation state, so that when placing as a group the relative positions of the blocks
	 * are maintained regardless of any rotations/flips the user has made during placement.
	 *
	 * @param angle        The effective angle of rotation (in degrees).
	 *                     - DEGREES0: No rotation.
	 *                     - DEGREES90: 90 degrees clockwise rotation.
	 *                     - DEGREES180: 180 degrees rotation.
	 *                     - DEGREES270: 270 degrees clockwise rotation.
	 * @param blockOffsetX The block's original X offset.
	 * @param blockOffsetY The block's original Y offset.
	 *
	 * @return A `BlockOffset` object containing the adjusted X and Y offsets after applying the rotation.
	 * - `offsetX`: The calculated X offset after rotation.
	 * - `offsetY`: The calculated Y offset after rotation.
	 */
	@NotNull private BlockOffset calculateRotationBlockOffset(int angle, int blockOffsetX, int blockOffsetY)
	{
		int offsetX = blockOffsetX;
		int offsetY = blockOffsetY;
		switch (angle) {
			case DEGREES0:
				// No rotation, offsets remain unchanged.
				break;
			case DEGREES90:
				// Rotate 90 degrees clockwise: X becomes Y, Y becomes -X.
				offsetX = blockOffsetY;
				offsetY = -blockOffsetX;
				break;
			case DEGREES180:
				// Rotate 180 degrees: X becomes -X, Y becomes -Y.
				offsetX = -blockOffsetX;
				offsetY = -blockOffsetY;
				break;
			case DEGREES270:
				// Rotate 270 degrees clockwise: X becomes -Y, Y becomes X.
				offsetX = -blockOffsetY;
				offsetY = blockOffsetX;
				break;
			default:
				// Invalid angle, no changes applied.
				break;
		}
		return new BlockOffset(offsetX, offsetY);
	}

	/**
	 * Calculates the flipped block offset based on the flip state and the original block offsets.
	 *
	 * @param flipState    The current flip state (0, 1, or 2). If the state exceeds 2, it is normalized to a value within this range.
	 *                     - 0: No flip applied.
	 *                     - 1: Flip along the Y-axis (invert Y offset).
	 *                     - 2: Flip along the X-axis (invert X offset).
	 * @param blockOffsetX The original X offset of the block.
	 * @param blockOffsetY The original Y offset of the block.
	 *
	 * @return A `BlockOffset` object containing the adjusted X and Y offsets after applying the flip.
	 * - `offsetX`: Adjusted X offset.
	 * - `offsetY`: Adjusted Y offset.
	 */
	@NotNull private BlockOffset calculateFlipBlockOffset(int flipState, int blockOffsetX, int blockOffsetY)
	{
		// Normalize the flip state to a value within the range [0, 2]
		int state = flipState;
		if (state > 2) {
			state %= 3;
		}

		// Initialize the adjusted offsets to the original offsets
		int offsetX = blockOffsetX;
		int offsetY = blockOffsetY;

		// Adjust the offsets based on the normalized flip state
		switch (state) {
			case (0): // No flip applied
				break;
			case (1): // Flip along the Y-axis (invert Y offset)
				offsetY = -blockOffsetY;
				break;
			case (2): // Flip along the X-axis (invert X offset)
				offsetX = -blockOffsetX;
				break;
			default: // No action for other states
				break;
		}

		// Return the adjusted offsets encapsulated in a BlockOffset object
		return new BlockOffset(offsetX, offsetY);
	}

	private void transferModelMappingInformation(chs.cof.logical.cable.IPinList cablePL,
			IVHDLModelMapping mapping)
	{
		if (cablePL != null && mapping != null && cablePL.getModelMapping() == null) {
			IVHDLModelMapping targetModelMapping = mapping.getClone();
			Map<IDesignPort, IDesignPort> sourceDestinationPortMap = new HashMap<>();
			sourceDestinationPortMap.put(DesignPort.UNDEFINED, DesignPort.UNDEFINED);
			for (Map.Entry<IPort, IDesignPort> designPortMapEntry : mapping.getPortMapping().entrySet()) {
				IDesignPort targetPort = getTargetSymbolPinInstance(cablePL, designPortMapEntry.getValue());
				sourceDestinationPortMap.put(designPortMapEntry.getValue(), targetPort);
				targetModelMapping.addPortMapping(designPortMapEntry.getKey(), targetPort);
			}

			Set<IVHDLFailureDataMapping> failureDataMappings = new LinkedHashSet<>();
			for (IVHDLFailureDataMapping failure : targetModelMapping.getFailures()) {
				failure.setDesignPort(sourceDestinationPortMap.get(failure.getDesignPort()));
				failureDataMappings.add(failure);
			}
			targetModelMapping.setFailureMapping(failureDataMappings);

			targetModelMapping.setGroundPin(sourceDestinationPortMap.get(targetModelMapping.getGroundPin()));
			targetModelMapping.setPowerPin(sourceDestinationPortMap.get(targetModelMapping.getPowerPin()));

			cablePL.setModelMapping(targetModelMapping);
		}
	}

	private IDesignPort getTargetSymbolPinInstance(chs.cof.logical.cable.IPinList cablePL, IDesignPort port)
	{
		IDesignPort targetPort = DesignPort.UNDEFINED;

		if (port == DesignPort.UNDEFINED) {
			return targetPort;
		}

		IUIDProvider designObject = port.getDesignObject();
		if (designObject == null) {
			return targetPort;
		}

		IAttributePropertyProvider targetPin =
				cablePL.getPins().find(pin -> designObject.getUID() != null && pin.getSymbolReference() != null &&
						pin.getSymbolReference().equals(designObject.getUID()));
		if (targetPin == null && cablePL instanceof IDevice && designObject instanceof ISymbolledPin) {
			targetPin = ((IDevice) cablePL).getInternalPins()
					.find(ipin -> designObject.getUID() != null && ipin.getSymbolReference() != null &&
							ipin.getSymbolReference().equals(designObject.getUID()));
		}

		if (targetPin != null) {
			targetPort = new DesignPort(targetPin);
		}

		return targetPort;
	}

	/**
	 * Attempt to add the new instance to the diagram, based on the action activation
	 *
	 * @return true iff successfully added
	 */
	protected boolean addInstance()
	{
		//
		// Duplicate, and instantiate the object at the
		// specified point.
		//
		chs.cof.logical.cable.IPinList pl = m_pinlist.getConnectivity();
		IConnectivity conn = getModel().getDesign().getConnectivity();
		assert conn != null;
		//
		if (!LogicObjectLockFinder.tryEdit(getModel().getDesign(), pl)) {
			return false;
		}
		if (pl instanceof IGroundDevice) {        // GroundDevice extends Device so need to check first
			conn.addGroundDevice((IGroundDevice) pl);
		}
		else if (pl instanceof IDevice) {
			conn.addDevice((IDevice) pl);
		}
		else if (pl instanceof IConnector) {
			conn.addConnector((IConnector) pl);
		}
		else if (pl instanceof ISplice) {
			conn.addSplice((ISplice) pl);
		}
		else if (pl instanceof IFunction) {
			conn.addFunction((IFunction) pl);
		}
		else {
			assert false : "Unknown connectivity type - " + pl;
			return true;
		}
		// loop through the connectivity pin list setting the names of the pins. This can't be done in the
		// symbol editor because it would turn off default naming, so we do it here when we instantiate
		// the symbol
		Set<? extends IGenericPin> allPins = pl.getGenericPins();
		for (IGenericPin pin : allPins) {
			// set the name so that it isn't ever under default naming
			pin.setName(pin.getName());
			LogicUtils.setMatchingShortDescriptionFromOTI(pin, pin.getProject());
		}
		setPinReference(m_pinlist);

		if (pl instanceof IDevice) {
			Collection<? extends IInternalLink> allLinks = ((IDevice) pl).getInternalLinkCollection();
			for (IInternalLink link : allLinks) {
				// set the name so that it isn't ever under default naming
				link.setName(link.getName());
			}
		}
		//
		// Now we add the graphics...
		//
		ISchemDiagram diagram = getModel().getDiagram();
		diagram.addObject(m_pinlist);
		ILogicDesign design = diagram.getDesign();
		assert design != null;

		if (m_pinlist.getBlockUID() == null && m_symdef != null) {
			List<IDesignSharedUsage> usages = design.getDesignWideUsageMgr().getUsages(pl);
			boolean atLeastOneCSFound = false;
			if (usages != null) {
				for (IDesignSharedUsage usage : usages) {
					if (usage instanceof IDesignSharedPinListUsage) {
						IUID schemUID = ((IDesignSharedPinListUsage) usage).getSchemPinListUID();
						IUID symbolUID = ((IDesignSharedPinListUsage) usage).getSymbolUID();
						IUID blockUID = ((IDesignSharedPinListUsage) usage).getBlockUID();
						if (schemUID != m_pinlist.getUID() && symbolUID != null && symbolUID == m_symdef.getUID() &&
								blockUID == null) {
							atLeastOneCSFound = true;
							break;
						}
					}
				}
			}
			m_pinlist.setHome(!atLeastOneCSFound);
			SymbolUtils.setupHomeForPins(m_pinlist, m_symdef, design);
		}
		else {
			m_pinlist.setHome(false);
		}

		if (shouldGenerateDeviceConnectors()) {
			Generator generator = Generator.getGenerator();
			GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
			generator.regenerateSchemDeviceConnectors(m_pinlist, gp);
		}
		Runnable ghcRunner = () -> ConnectivityGHCHelper.generateHarnessConnectors(diagram, false, false, m_pinlist);

		if (shouldSplitConductorsWhilePlacingInstance()) {
			splitConductors(m_pinlist, ghcRunner);
		}
		else {
			ghcRunner.run();
		}

		//Create connection schematics for Harness Connectors.
		Collection<IPinList> attachedPinListObjects =
				m_pinlist.getAttachedPinListObjects();
		for (IPinList attachedPinList : attachedPinListObjects) {
			chs.cof.logical.cable.IPinList connectivity = attachedPinList.getConnectivity();
			if (connectivity instanceof IHarnessPlugConnector) {
				completePostAction(attachedPinList, diagram);
			}
		}

		completePostAction(m_pinlist, diagram);
		IECAttributeResolver.inheritIECAttributesIfNotPresent(diagram, m_pinlist);

		//
		// Sort out the snapshot...
		//
		//CreationDeletionHelper.getTheCreationHelper().addCreationObject(m_pinlist);
		m_pinlist.extentChanged(m_pinlist);
		return false;
	}

	protected boolean shouldSplitConductorsWhilePlacingInstance()
	{
		return true;
	}

	protected void splitConductors(IPinList pinlist, Runnable ghcRunner)
	{
		GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		ConductorSplitter spliceSplitter = ConductorSplitter.createConductorSplitter(pinlist, true);
		spliceSplitter.splitConductors(pinlist, gview, false, true, true, ghcRunner);
	}

	protected boolean shouldGenerateDeviceConnectors()
	{
		return true;
	}

	public static void completePostAction(IPinList schemPinList, ISchemDiagram diagram)
	{
		ObjectConnectionsGetter.createConnectionSchematics(schemPinList, diagram);

		// Apply styling
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
		PreferenceSetHelper.applyStyleSet(schemPinList.getObjectsForStyling(), gp.getStyleSet(), true);
	}

	private void setPinReference(IPinList pinlist)
	{
		if (pinlist.getConnectivity().canHaveReferencePin()) {
			//FEAT00013786: At this point we do not expect pinlist to have stack pins
			assert pinlist.getStackPins().isEmpty();
			for (IPin pin : pinlist.getPins()) {
				pin.setReference(reference);
			}
		}
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return AddInstanceActionUI.class.getName();
	}

	public void destroy()
	{
		m_dynamics = null;
		super.destroy();
	}

	protected void cleanPinlist()
	{
		deleteObject(m_pinlist);
		m_pinlist = null;
	}

	public void setCreatePinList(boolean val)
	{
		bCreatePinList = val;
	}

//	protected void copyBlockSrcGraphics(IPinList pinlist, IBlock symBlock, int scale)
//	{
//		SymbolUtils.synchronizeBlockSourceGfx(symBlock, scale, pinlist,
//				SymbolUtils.sourceSymbolPinDeriver(symBlock.getBlockOwner()), true);
//	}

	private boolean checkForDuplicatePinNames(IStamp sdef)
	{
		Set<String> pinNames = new HashSet<String>();
		boolean duplicateFound = false;
		if (sdef instanceof ISymbolDef) {
			ISymbolDef symDef = (ISymbolDef) sdef;
			for (IBlockIterator it = symDef.getBlocks(); it.hasNext() && !duplicateFound; ) {
				IBlock iBlock = it.getNext();
				if (iBlock.getConnectivity() != null) {
					for (IAbstractPinIterator pinIter = iBlock.getConnectivity().getPins(); pinIter.hasNext(); ) {
						IAbstractPin curPin = pinIter.getNext();
						if (!pinNames.add(curPin.getName())) {
							duplicateFound = true;
							break;
						}
					}
				}
			}
			if (symDef.getConnectivity() != null) {
				for (IAbstractPinIterator pinIter = symDef.getConnectivity().getPins();
						pinIter.hasNext() && !duplicateFound; ) {
					IAbstractPin curPin = pinIter.getNext();
					if (!pinNames.add(curPin.getName())) {
						duplicateFound = true;
					}
				}
			}
		}
		return duplicateFound;
	}

	@Override protected boolean shouldScaleElectricalSymbols(@NotNull ISymbolDef symbolDef)
	{
		return getController().getCaplet().isLayoutCaplet();
	}

	protected static class BlockOffset {
		private int m_offsetX;
		private int m_offsetY;

		public BlockOffset(int offsetX, int offsetY) {
			m_offsetX = offsetX;
			m_offsetY = offsetY;
		}

		public int getOffsetX() {
			return m_offsetX;
		}

		public int getOffsetY() {
			return m_offsetY;
		}
	}
}