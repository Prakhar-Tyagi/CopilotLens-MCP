package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.caf.helpers.ui.std.DesignAbstractionHelper;
import chs.caf.helpers.ui.std.UIManager;
import chs.cof.draw.IGrid;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cofUtils.parameterized.AddPinHelper;
import chs.common.DesignAbstractionType;
import chs.common.IExtent;
import chs.services.dynamicgfx.DynamicRingTerminalRotationIndicator;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.utilities.CommonUtils;
import chs.utility.helpers.LibraryHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.LogicUtils;
import chs.utility.preferences.PreferenceSetHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CreateRingTerminalAction extends CreateConnectorAction
{

	private static Cursor m_ringTermCursor = null;

	private ILibraryPartSelection m_librarySelection;

	public CreateRingTerminalAction(ICapletController controller)
	{
		super(controller);
		if (m_ringTermCursor == null) {
			m_ringTermCursor = CAFUtils.getInstance().loadCursor(controller.getCaplet(),
					"chs/images/app/cur_ringterminal.gif", new Point(7, 7));
		}
		setSubType(RINGTERMINAL_CONNECTOR);
	}

	public Cursor getCursor()
	{
		return m_ringTermCursor;
	}

	@Override public String getActionUIClass()
	{
		return CreateRingTerminalActionUI.class.getName();
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		m_librarySelection = PartBrowserActionHelper.getSelectedBrowserPart();
		return super.onActivate(e);
	}

	@Override @Nullable public ILibraryPartSelection getLibrarySelectedObject()
	{
		return m_librarySelection;
	}

	@Override protected List<IPin> addPins(int width, int height, int verticalOffset, IGrid grid, IPinList schem_conn,
			IConnector connector, boolean topdown)
	{
		List<IPin> addedPins = new ArrayList<IPin>();

		//Add one pin to the ring terminal
		int ypos = 0;
		if (topdown) {
			ypos = height;
		}

		ILibraryPartSelection selObj = getLibrarySelectedObject();
		ILibraryCavity cavity = null;
		if (selObj != null) {
			Set<ILibraryCavity> cavities = LibraryHelper.getCavities(selObj.getSelectedObject());
			if (!cavities.isEmpty()) {
				cavity = cavities.iterator().next();
			}
		}

		IPin pin = AddPinHelper.generatePin(schem_conn, connector, (m_pinsOnLeft) ? 0 : width,
				ypos + verticalOffset, grid, null, PreferenceSetHelper.getStyleSet(getDiagram()));

		IAbstractPin connectivityPin = pin.getConnectivity();
		if (cavity != null) {
			connectivityPin.setName(cavity.getName());
		}
		LogicUtils.setMatchingShortDescriptionFromOTI(connectivityPin, connectivityPin.getProject());
		addedPins.add(pin);

		return addedPins;
	}

	@Override public void constrainExtent(IExtent constExtent)
	{
		super.constrainExtent(constExtent);
		constrainExtentByMaxPinCount(constExtent, 1);
	}

	@NotNull @Override protected DynamicRotationIndicator createRotationIndicator()
	{
		DynamicRotationIndicator indicator = new DynamicRingTerminalRotationIndicator(getIndicateBothEdges());
		setRotationIndicator(indicator);
		return indicator;
	}

	protected void refreshDesignPreConnect()
	{
		//Needs this refresh because the device pin's to which we mat connect the created ring terminal
		// type may have got converted in other session if it design is being edited in MU.
		ILogicModel logicModel = CommonUtils.cast(getCapletModel(), ILogicModel.class);
		if (logicModel != null) {
			ILogicDesign logicDesign = logicModel.getDesign();
			if (logicDesign.isUnderConcurrentEdit()) {
				logicDesign.refresh();
			}
		}
	}
	@Override public boolean isEnabled()
	{
		DesignAbstractionType designAbstraction = DesignAbstractionHelper.getTypeOfDesignAbstraction();

		Set<DesignAbstractionType> abstractionTypes =
				new LinkedHashSet<>(Arrays.asList(DesignAbstractionType.LOGICAL,
						DesignAbstractionType.SYTEM_BLOCK, DesignAbstractionType.SMART_FLOWS,
						DesignAbstractionType.FLUID));
		if (designAbstraction != null && abstractionTypes.contains(designAbstraction)) {
			return false;
		}

		return super.isEnabled();
	}
}