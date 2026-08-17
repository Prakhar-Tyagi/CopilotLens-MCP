/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.icd.ICDPlacementHelper;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IGfxView;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.IICDSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IProject;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IDesignContainer;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinListPlaceOptionsDialog;
import chs.ctf.caf.ui.PinPlaceOptionStateHandler;
import chs.ctf.caf.utils.IPinProxy;
import chs.services.dynamicgfx.ISmartPoint;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.property.ChoiceTypeValue;
import chs.utility.ConductorSplitter;
import chs.utility.DeviceConductorSplitter;
import chs.utility.DiagramHelper;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.logic.LogicUtils;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.ui.PinSelectionAbstractPanel;
import chs.utility.ui.PlacePinLibrarySelection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Adds a parameterized device from a library part (without a symbol).
 * <p>
 * This action places the device initially with no pins and prompts the user to add pins that are on the part.
 */
public class AddParameterizedDeviceFromLibraryPartAction extends CreateNoPinDeviceAction
{

	//Column Indices of Available pin table

	private ILibraryPartSelection librarySelection;
	private AddPinActionHelper addPinHelper;
	protected IPinList createdDevice;
	private List<IPinProxy> pinsToAdd = new ArrayList<IPinProxy>();
	private boolean autogenerate = false;
	private boolean reference = false;
	private boolean withConductor = false;
	private boolean placeAsStack = false;
	private boolean placeAsGroup = false;

	/**
	 * Construct the action.
	 *
	 * @param controller controller
	 * @param part The library part.  Must not have a symbol.
	 */
	protected AddParameterizedDeviceFromLibraryPartAction(ICapletController controller, ILibraryPartSelection part)
	{
		super(controller);
		librarySelection = part;
		assert librarySelection != null;
	}

	public ILibraryPartSelection getLibrarySelection()
	{
		return librarySelection;
	}

	/**
	 * Overridden here to prompt the user for pin positions once the device is placed with no pins.
	 */
	public IActionEnum onActivate(ActionEvent e)
	{
		assert pinsToAdd.isEmpty();
		assert createdDevice == null; // this is only setup if we've added the device
		assert addPinHelper == null; // this is only setup if we're adding pins and have placed the device
		IActionEnum result = super.onActivate(e);
		if (result == IActionEnum.eActivated) {
			if (!selectPins()) {
				result = IActionEnum.eCanceled;
			}
		}
		return result;
	}

	@Override
	protected boolean postActionChanges()
	{
		LogicUtils.deferRegenerationOfSchemDeviceConnectors();
		assert createdDevice != null;
		ISchemDiagram diagram = (ISchemDiagram) getController().getCapletModel().getModelRoot();
		assert createdDevice.getDiagram() == diagram;
		assert diagram != null;
		CompositePinConnectivityFinder connectivityFinder = new CompositePinConnectivityFinder(diagram);
		if (addPinHelper != null) {
			addPinHelper.setIsReference(reference);
			addPinHelper.addPins(createdDevice, diagram, connectivityFinder);
		}
		else if (autogenerate) {
			List<String> pinNames = new ArrayList<String>();
			for (IPinProxy pinObject : pinsToAdd) {
				pinNames.add(pinObject.getName());
			}
			PinListAddPinHelper.autogeneratePins(diagram, createdDevice, pinNames, reference, connectivityFinder);
		}

		//first we need to set the name of device otheriwse the footprint related object creation will fail
		//because mapped icd will not be found.
		ICDPlacementHelper.updateByICDName(createdDevice, librarySelection);

		Runnable partUpdater = () ->
		{
			if (addPinHelper != null) {
				addPinHelper.regenerateGraphics(createdDevice);
			}
			SelectedPartUpdateHelper.updateLibraryPart(createdDevice, librarySelection, diagram, true);
		};

		if (!reference) {
			ConductorSplitter splitter = new DeviceConductorSplitter();
			splitter.splitConductors(Collections.singletonList(createdDevice),
					(IGfxView) CAFUtils.getInstance().getActiveCapletView(), connectivityFinder, partUpdater);
		}
		else {
			partUpdater.run();
		}

		ICDPlacementHelper.updateICDNameAndRouting(createdDevice, librarySelection, diagram, withConductor);

		// DR 306068: Need to regenerate the device on completion so any pin graphics defined by the library part
		// are seen. It is not sufficient to add the library part before adding the pins - in particular,
		// PinListAddPinHelper applied to a pin associated with a device connector footprint regenerates
		// the device connector and erases pin graphics of all non-device connector pins on the device.
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
		gp.setNewObject(true);
		Generator.getGenerator().generate(createdDevice, gp, Generator.NOREGENERATE_PROPERTIES,
				false);

		//test with Pinlist with pin graphics, pin styling
		// if addPinHelper not null, then style on pins already applied during addPins
		applyStyle(diagram, addPinHelper != null);
		return true;
	}

	private void applyStyle(@NotNull ISchemDiagram diagram, boolean applyOnDeviceOnly)
	{
		if (applyOnDeviceOnly) {
			PreferenceSetHelper.applyStyleSet(createdDevice, diagram, true);
		}
		else {
			PreferenceSetHelper.applyStyleSet(createdDevice.getObjectsForStyling(), diagram, true);
		}
	}

	@Override
	protected void clearAction(boolean actionSuccess)
	{
		super.clearAction(actionSuccess);
		if (!actionSuccess && createdDevice != null) {
			chs.cof.logical.cable.IPinList connectivityPinList = createdDevice.getConnectivity();
			createdDevice.delete();
			connectivityPinList.delete();
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			if (view != null) {
				view.invalidate(IViewInvalidationEnum.eFull);
			}
		}
		librarySelection = null;
		addPinHelper = null;
		createdDevice = null;
		pinsToAdd.clear();
	}

	/**
	 * Disable the "Add pins on Ctrl-click" for this action.
	 * <p>
	 * We never want to do this here, pins are either added manually or autogenerated elsewhere in this action.
	 */
	protected boolean shouldAddPins()
	{
		return false;
	}

	/**
	 * Overriden for this action to Disable addition of pins on split.
	 *
	 * @return boolean
	 */
	protected boolean allowAddPinsOnSplit()
	{
		return false;
	}

	/**
	 * Overriden for this action to prevent pin creation at placeholders. i.e when conductors end points are coincident
	 * with placeholders
	 *
	 * @return boolean
	 */
	protected boolean allowPinCreationAtPlaceholders()
	{
		return false;
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(AddParameterizedDeviceFromLibraryPartAction.class,
				"AddParameterizedDeviceFromLibraryPartAction.StatusBar.NoPins.text");
	}

	/**
	 * Prompts the user to select the pins they would like to place.
	 *
	 * @return true if the user would like to add 0 or more pins.  False if the dialog is cancelled.
	 */
	private boolean selectPins()
	{
		ILibraryObject selectedObject = librarySelection.getSelectedObject();
		if (selectedObject == null) {
			return true;
		}

		// prompt user to select pins
		String title = getTitle(librarySelection);

		Frame owner = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
		IDesignContainer designContainer = CAFUtils.getInstance().getActiveDesignContainer();
		IProject project = designContainer != null ? designContainer.getProject() : null;
		PinSelectionDialog pinSelector = createPinSelectionDialog(title, owner, project);
		ILibraryObject.GroupType type = selectedObject.getGroupName();
		PinListTypeEnum plType =
				StringUtils.equals(type.toString(), "Device") ? PinListTypeEnum.TypeDevice : PinListTypeEnum.TypePlug;
		IPlacementOptionParams params = createPlacementOptionParams(plType, false);
		pinSelector.buildTypeSpecificOptions(plType, params);
		if (!pinSelector.selectPins()) {
			return false;
		}
		pinsToAdd = pinSelector.getSelectedNames();
		autogenerate = pinSelector.autogenerate();
		reference = pinSelector.reference();
		withConductor = pinSelector.withConductor();
		placeAsStack = pinSelector.placeAsStack();
		placeAsGroup = pinSelector.placeAsGroup();
		return true;
	}

	@NotNull protected PinSelectionDialog createPinSelectionDialog(String title, Frame owner, IProject project)
	{
		return new PinSelectionDialog(owner, title, librarySelection, project);
	}

	@NotNull @Override
	protected IPlacementOptionParams createPlacementOptionParams(@NotNull PinListTypeEnum pinListType, boolean isShared)
	{
		IPlacementOptionParams params = super.createPlacementOptionParams(pinListType, isShared);
		params.enableWithConductorOption(librarySelection instanceof IICDSelection, getCurrentProject());
		return params;
	}

	protected String getTitle(@NotNull ILibraryPartSelection selection)
	{
		String title;
		if (selection instanceof IICDSelection) {
			//dts0101162955 "Place ICD" dialog shows title as "Place Device From Library Part". Title should be proper
			String icdRole = ((IICDSelection) selection).getSelectedDeviceName();
			//todo refactor the code to ICD class, this is not a correct place, this class should not have any
			//reference to ICD
			title = ResourceMgr.getString(AddParameterizedDeviceFromLibraryPartAction.class,
					"PlaceICD.PinSelectionDialog.title.text", icdRole);
		}
		else {
			final ILibraryObject selectedObject = selection.getSelectedObject();
			final String prefix = ResourceMgr.getString(AddParameterizedDeviceFromLibraryPartAction.class,
					"AddParameterizedDeviceFromLibraryPartAction.PinSelectionDialog.title.text");
			title = selectedObject != null ? prefix + ' ' + selectedObject.getPartNumber() : prefix;
		}
		return title;
	}

	/**
	 * Overridden here to allow a single pinlist to be created/stored by this action.
	 */
	protected IGfxObject createDisplayObject(List<ISmartPoint> point_list)
	{
		// TODO jacobt FEAT3099.1 : Generify CreateParameterizedObjectAction.createDisplayObject, weaken arg type to List?
		if (createdDevice == null) {
			createdDevice = (IPinList) super.createDisplayObject(point_list);
			if (createdDevice != null) {
				chs.cof.logical.cable.IPinList pl = createdDevice.getConnectivity();
				pl.assignLibraryDetails(librarySelection);
			}
		}
		return createdDevice;
	}

	// TODO jacobt FEAT3099.1 : Bad things happen when you start adding pins and hit escape.
	// Objects getting into UIDMgr before onTerminate...
	// Could fix by implementing keyPressed here?
	// The same bad things happen for add shared device so maybe fix should be in AddPinActionHelper?

	public void mouseClicked(MouseEvent e)
	{
		if (addPinHelper != null) {
			addPinHelper.mouseClicked(e); // we're adding pins
		}
		else {
			super.mouseClicked(e); // we're placing the device
		}
	}

	public void mouseDragged(MouseEvent e)
	{
		// we're adding pins via AddPinHelper
		if (addPinHelper != null) {
			addPinHelper.mouseDragged(e); // we're adding pins
		}
		else {
			super.mouseDragged(e); // we're placing the device
		}
	}

	public void mouseEntered(MouseEvent e)
	{
		if (addPinHelper != null) {
			addPinHelper.mouseEntered(e); // we're adding pins
		}
		else {
			super.mouseEntered(e); // we're placing device
		}
	}

	public void mouseExited(MouseEvent e)
	{
		if (addPinHelper != null) {
			addPinHelper.mouseExited(e); // we're adding pins
		}
		else {
			super.mouseExited(e); // we're placing device
		}
	}

	public void mouseMoved(MouseEvent e)
	{
		if (addPinHelper != null) {
			addPinHelper.mouseMoved(e); // we're adding pins
		}
		else {
			super.mouseMoved(e); // we're placing the device
		}
	}

	public void mousePressed(MouseEvent e)
	{
		if (addPinHelper != null) {
			addPinHelper.mousePressed(e); // we're adding pins
		}
		else {
			super.mousePressed(e); // we're placing the device
		}
	}

	public void keyPressed(KeyEvent e)
	{
		// state cannot be used here as this class is not managing the states
		if (addPinHelper != null) {
			addPinHelper.keyPressed(e);
		}
		super.keyPressed(e);
	}

	public void mouseReleased(MouseEvent e)
	{
		if (addPinHelper != null) {
			addPinHelper.mouseReleased(e); // we're adding pins
		}
		else if (!(autogenerate || pinsToAdd.isEmpty())) {
			// see if we've just finished placing the device
			final List<ISmartPoint> pointList = getPointList();
			if (pointList != null) {
				// we've finished placing the device with no pins and need a pinlist for the AddPinActionHelper
				assert createdDevice == null;
				createDisplayObject(pointList); // sets up createdDevice
				assert createdDevice != null;
				setupAddPinHelper();
			}
			else {
				super.mouseReleased(e); // first mouse release whilst placing the device
			}
		}
		else {
			super.mouseReleased(e); // no pins to add - we're just placing a no-pin device
		}
	}

	/**
	 * Setup the AddPinActionHelper with the pin names that were chosen earlier.
	 */
	private void setupAddPinHelper()
	{
		assert !pinsToAdd.isEmpty(); // we checked this earlier
		addPinHelper = new AddPinActionHelper(this, true, true);
		addPinHelper.setIsReference(reference);
		addPinHelper.setPlaceAsStack(placeAsStack);
		addPinHelper.setPlaceAsGroup(placeAsGroup);
		addPinHelper.setUp(createdDevice, pinsToAdd); // now we'll be delegated mouse events to place the pins
	}

	@Override
	protected void connectGfxObjectToModel(IGfxObject newObject)
	{
		// Do nothing. Pins are not created at this stage. So it cannot connected to any conductor or pinlist.
	}

	/**
	 * Dialog to select/add pin names from a list.
	 * <p>
	 * <p>
	 * TODO jacobt FEAT3099.1 : Move PinSelectionDialog somewhere useful
	 */
	public class PinSelectionDialog extends PinListPlaceOptionsDialog

	{

		private boolean success = false;
		//		private JCheckBox autogenerateCheck;
//		private JCheckBox referenceCheck;
//		private JCheckBox placeAsStackCheck;
		private static final int PREFERRED_WIDTH = 180;
		private static final int PREFERRED_HEIGHT = 150;

		private ConnectivityCommonPinSelectionPanel pinSelectionPanel;

		/**
		 * Construct a dialog that provides a choice between a list of pin names.
		 */
		public PinSelectionDialog(Frame frame, String title, ILibraryPartSelection libraryPartSelection,
				@Nullable IProject project)
		{
			super(frame, title, true);

			addComponents(libraryPartSelection, project);
			hookupButtons();
		}

		/**
		 * Display the (modal) dialog to prompt for a choice of pin names.
		 *
		 * @return true if a choice was made, false if the dialog was cancelled
		 */
		public boolean selectPins()
		{
			pack();
			loadPrefs();
			setVisible(true);
			return success;
		}

		/**
		 * Get the pin names that were selected on the last call to showDialog()
		 *
		 * @return Selected pin name objects
		 */
		public List<IPinProxy> getSelectedNames()
		{

			return pinSelectionPanel.getPins();
		}

		/**
		 * Is the Auto-Generate checkbox on?
		 */
		public boolean autogenerate()
		{
			return m_autoGenerate;
		}

		/**
		 * Is the Reference checkbox on?
		 */
		public boolean reference()
		{
			return m_reference;
		}

		public boolean withConductor()
		{
			return m_withConductorOption != null && m_withConductorOption.getValue();
		}

		public boolean placeAsStack()
		{
			return m_placeAsStack;
		}

		public boolean placeAsGroup()
		{
			return m_placeAsGroup;
		}

		private void addComponents(ILibraryPartSelection libraryPartSelection, IProject project)
		{
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(new JPanel(), BorderLayout.EAST);
			panel.add(new JPanel(), BorderLayout.WEST);
			panel.add(new JPanel(), BorderLayout.NORTH);

			panel.add(pinListPanel(libraryPartSelection, project), BorderLayout.CENTER);
			m_optionsPanel = new JPanel();
			m_optionsPanel.setLayout(new BorderLayout());
			initOptionsPropertyGroup();
//			initOptionsPanel();
			JPanel pan = new JPanel();
			pan.setLayout(new BorderLayout());
			pan.add(new JPanel(), BorderLayout.WEST);
			pan.add(m_optionsPanel, BorderLayout.CENTER);
			panel.add(pan, BorderLayout.AFTER_LAST_LINE);
			Dimension preferredSize = new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT);
			panel.setPreferredSize(preferredSize);
			panel.setMaximumSize(preferredSize);
			getContentPane().add(panel, BorderLayout.CENTER);
		}

//		private void initOptionsPanel()
//		{
//			buildOptionsPanel();
//			//placeAsStackCheck.setEnabled(false);
////			JPanel pan = new JPanel();
////			pan.setLayout(new BorderLayout());
////			pan.add(placeAsStackCheck, BorderLayout.CENTER);
////			pan.add(new JPanel(), BorderLayout.EAST);
////			m_optionsPanel.add(pan, BorderLayout.EAST);
//		}

		@Override protected void createAutoGenerateOption()
		{
			m_autoGenerateOption = buildOption(AUTOGENERATE_OPTION, AUTOGENERATE_TOOLTIP, true);
			m_autoGenerateOption.setName("autogenerate");
			m_autoGenerateOption.setMnemonic(KeyEvent.VK_G);
			//autogenerateCheck.setSelected(true);
			m_autoGenerateOption.addPropertyChangeListener(evt -> m_autoGenerate = (boolean) evt.getNewValue());
			m_autoGenerateOption.setDefaultValue(true);
		}

		@Override protected void createAsStackOption()
		{
			m_placeAsStackOption = buildOption(AS_STACK_OPTION, PLACEASSTACK_TOOLTIP, true);
			m_placeAsStackOption.setName(AS_STACK_OPTION);
			m_placeAsStackOption.addPropertyChangeListener(new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					m_placeAsStack = (boolean) evt.getNewValue();
				}
			});
			m_placeAsStackOption.setMnemonic(ResourceMgr.getMnemonic(AddParameterizedDeviceFromLibraryPartAction.class,
					"AddParameterizedDeviceFromLibraryPartAction.PinSelectionDialog.PlaceAsStack.mnemonic"));
			m_placeAsStackOption.setDefaultValue(false);
		}

		@Override protected void createAsReferenceCheckBox()
		{
			m_referenceOption = buildOption(REFERENCE_OPTION, REFERENCE_TOOLTIP, false);
			m_referenceOption.setChoiceType(ChoiceTypeValue.CHECK_BOX);
			m_referenceOption.setName("chkreference");
			m_referenceOption.setMnemonic(KeyEvent.VK_R);
			//referenceCheck.setSelected(false);
			m_referenceOption.addPropertyChangeListener(new PinPlaceOptionStateHandler(this));
			m_referenceOption.addPropertyChangeListener(new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					m_reference = (boolean) evt.getNewValue();
				}
			});
			m_referenceOption.setDefaultValue(false);
		}

		private void hookupButtons()
		{
			getOkButton().
					addActionListener(
							new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									success = true;
									savePrefs();
									setVisible(false);
									dispose();
								}
							}
					);

			getCancelButton().
					addActionListener(
							new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									success = false;
									savePrefs();
									setVisible(false);
									dispose();
								}
							}
					);
		}

		/**
		 * Creates the list representing the pinlist (on the right side of the panel).
		 *
		 * @return The panel with the JList containing the list of pins.
		 */
		private JPanel pinListPanel(ILibraryPartSelection libraryPartSelection, IProject project)
		{
			pinSelectionPanel = new ConnectivityCommonPinSelectionPanel(
					new PlacePinLibrarySelection(libraryPartSelection), this, new Consumer<List<?>>()
			{
				@Override public void accept(List<?> objects)
				{
					placeAsStackButtonStatusUpdate(objects);
				}
			}, getEscapeListener(), project, getDesign());
			return pinSelectionPanel;
		}

		private ILogicDesign getDesign()
		{
			return getLocalModel().getDesign();
		}

		@Nullable protected PinSelectionAbstractPanel getPinSelectionPanel()
		{
			return pinSelectionPanel;
		}
	}

	@Override public boolean shouldDisableUndoForNonUndoableChanges()
	{
		return true;
	}
}