package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.helpers.ui.std.UIManager;
import chs.caplets.logic.actions.shared.SharedObjectAvailabilityReporter;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.IUIDObject;
import chs.utility.DiagramHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class AddFunctionPortAction extends AbstractAddPinAction
{

	//private String ctxCommand;

	public AddFunctionPortAction(ICapletController controller)
	{
		super(controller);
		setupActionHelper();
	}

	protected void setupActionHelper()
	{
		m_addPinActionPresenter = new AddFunctionPinActionHelper(this, false, true);
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		if (!initAddPinModel()) {
			return IActionEnum.eCanceled;
		}
		initAddPinModel();
		boolean altPress = (e.getSource() instanceof UIManager) && (e.getModifiers() & KeyEvent.ALT_MASK) != 0;
		boolean shiftNotPressed =
				(!(e.getSource() instanceof UIManager) && !((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0));
		boolean retState = m_addPinActionPresenter.initialize(m_addPinActionModel, altPress, shiftNotPressed);

		return retState ? IActionEnum.eActivated : IActionEnum.eCanceled;
	}

	protected boolean initAddPinModel()
	{
		// show a dialog to select the pins
		IPinList function = getOperand(getController().getSelectMgr().getPreSelections());
		if (function == null) {
			return false;
		}

		chs.cof.logical.cable.IPinList connectivity = function.getConnectivity();
		if (!isConnectivityEditable(connectivity)) {
			return false;
		}

		ISharedObject sharedFunction = connectivity.getSharedObject();
		if (sharedFunction != null) {
			final ILogicDesign functionDesign = connectivity.getLogicDesign();
			// Cancel 'Add Port' action on a shared object (all types) instance which is restricted to current user
			if (!isSharedObjectAvailable(sharedFunction, functionDesign)) {
				return false;
			}
		}
		m_addPinActionModel = setupPinActionModel(function);

		return true;
	}

	@NotNull @Override protected AddPinActionModel setupPinActionModel(@NotNull IPinList function)
	{
		return new AddPinActionModel(function);
	}

	private boolean isSharedObjectAvailable(@NotNull ISharedObject sharedFunction, @Nullable IDesign functionDesign)
	{
		final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
		return new SharedObjectAvailabilityChecker().check(sharedFunction, functionDesign, reporter, false);
	}

	private boolean isConnectivityEditable(chs.cof.logical.cable.IPinList connectivity)
	{
		return LogicObjectLockFinder.tryEdit(connectivity);
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return AddFunctionPortActionUI.class.getName();
	}

	@Nullable
	protected IPinList getOperand(SelectSet selections)
	{
		IPinList selectedPinList = null;

		int plCount = 0;
		for (SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();

			if (uidObj instanceof IPinList) {
				IPinList pl = (IPinList) uidObj;
				if (pl.getParameterized() != null) {

					plCount++;
					if (plCount == 1) {
						selectedPinList = (IPinList) uidObj;
					}
					else {
						break;
					}
				}
			}
		}

		IPinList operand;
		if (plCount == 1 && selectedPinList.getConnectivity() instanceof IFunction) {
			operand = selectedPinList;
		}
		else {
			return null;
		}

		// Add Port currently expects the pinlist to be on the active diagram
		if (DiagramHelper.getDiagram(operand) != CAFUtils.getInstance().getActiveDiagram()) {
			return null;
		}

		chs.cof.logical.cable.IPinList capitalLogicalPinList = operand.getConnectivity();
		final ISharedPinList sharedPinList = capitalLogicalPinList.getSharedPinList();
		if (sharedPinList != null && sharedPinList.getPins().getSize() == 0) {
			return null;
		}
		return operand;
	}
}

