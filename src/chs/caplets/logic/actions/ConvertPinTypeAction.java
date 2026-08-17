package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.AppAction;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionFilter;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.ConvertPinTypeLogEnum;
import chs.cof.logical.IPinTypeConverter;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.LogTabType;

import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Created with IntelliJ IDEA. User: nagamani Date: 6/2/14
 */
public class ConvertPinTypeAction extends ControllerActionRT implements ICtxMenuProvider
{

	protected IPinTypeConverter m_pinTypeConveter;
	protected boolean bNormalToStud = false;
	protected boolean bStudToNormal = false;

	public ConvertPinTypeAction(ICapletController controller)
	{
		super(controller);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		registerPinTypeConverter();
		return IActionEnum.eCompleted;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		LogHelper.clearTab(LogTabType.TAB_CONVERT_PIN_TYPE);

		ConvertPinTypeLogEnum result = m_pinTypeConveter.doConversion();
		if (result != ConvertPinTypeLogEnum.NO_ERROR) {
			displayError(result);
		}
		if (m_pinTypeConveter.isSharedContext()) {
			// Clear the undo stack to avoid problems with trying to undo editing shared objects
			getController().getUndoableContainer().endEdit();
			getController().getUndoableContainer().clear();
		}

		return true;
	}

	protected boolean registerPinTypeConverter()
	{
		resetOperands();
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		SelectionFilter studPinSelectionFilter = new StudPinSelectionFilter();
		SelectionFilter normalPinSelectionFilter = new NormalPinSelectionFilter();

		int selectionCount = selections.getSelectCount();
		boolean bValidSelections = false;
		if (selectionCount > 0) {
			SelectedUIDObjectIterator selectedObjIter = selections.getSelectedUIDObjects();
			if (selectionCount == selections.getSelectCount(normalPinSelectionFilter)) {
				//all selections are stud pins
				bNormalToStud = true;
				m_pinTypeConveter = new NormalToStudPinTypeConverter();
			}
			else if (selectionCount == selections.getSelectCount(studPinSelectionFilter)) {
				//all selections are stud pins
				bStudToNormal = true;
				m_pinTypeConveter = new StudPinToNormalPinTypeConverter();
			}
			if (m_pinTypeConveter != null) {
				while (selectedObjIter.hasNext()) {
					IUIDObject obj = selectedObjIter.getNext();
					if (obj instanceof IPin) {
						obj = ((IConnectivityRef) obj).getConnectivity();
					}
					if (obj instanceof IDevicePin) {
						m_pinTypeConveter.addPinForConversion((IDevicePin) obj);
						bValidSelections = true;
					}
				}
			}
		}
		return bValidSelections && (bNormalToStud != bStudToNormal);
	}

	private void resetOperands()
	{
		bNormalToStud = false;
		bStudToNormal = false;
		m_pinTypeConveter = null;
	}

	@Override public String getActionUIClass()
	{
		return ConvertPinTypeActionUI.class.getName();
	}

	@Override public boolean isEnabled()
	{
		m_disabledReason = "";
		if (super.isEnabled() && registerPinTypeConverter()) {
			ConvertPinTypeLogEnum log = m_pinTypeConveter.isEnabled();
			if (log != ConvertPinTypeLogEnum.NO_ERROR) {
				m_disabledReason = ResourceMgr
						.getString(ConvertPinTypeAction.class, "ConvertPinTypeAction." + log.toString() + ".Info");
				return false;
			}
			return true;
		}
		return false;
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (registerPinTypeConverter()) {
			AppAction aui = (AppAction) getActionUI();
			if (bStudToNormal) {
				aui.setResources("name1.decl", false, "shortDesc1.decl", "name1.longDesc", null, null,
						null);
			}
			if (bNormalToStud) {
				aui.setResources("name2.decl", false, "shortDesc2.decl", "name2.longDesc", null, null,
						null);
			}
			container.add(new ActionEntry(aui));
			if (!isEnabled()) {
				aui.setEnabled(false);
				aui.updateUI();
			}
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	private static class StudPinSelectionFilter extends SelectionFilter
	{

		public boolean selectionAllowed(Selection sel)
		{
			IUIDObject object = sel.getObject();
			if (object instanceof IRepresentedObject) {
				object = ((IRepresentedObject) object).getRawConnectivity();
			}
			return object instanceof IDevicePin && ((IDevicePin) object).isStud();
		}
	}

	private static class NormalPinSelectionFilter extends SelectionFilter
	{

		public boolean selectionAllowed(Selection sel)
		{
			IUIDObject object = sel.getObject();
			if (object instanceof IRepresentedObject) {
				object = ((IRepresentedObject) object).getRawConnectivity();
			}
			if (object instanceof IDevicePin) {
				//pin connected to device connector and stud pin itself cannot be converted to studpin.
				return !((IDevicePin) object).isStud() && ((IDevicePin) object).getDeviceConnectorPin() == null;
			}
			return false;
		}
	}

    protected void displayError(ConvertPinTypeLogEnum logEnum) {
		String err = logEnum.toString();
		List<String> additionalsArgs = logEnum.getAdditionalsArgs();
		LogHelper.clearTab(LogTabType.TAB_CONVERT_PIN_TYPE);
		String infoMessage =
				ResourceMgr.getString(ConvertPinTypeAction.class, "ConvertPinTypeAction." + err + ".Info",
						additionalsArgs.toArray());
		LogHelper.printMsgSafe(LogTabType.TAB_CONVERT_PIN_TYPE, infoMessage);
	}
}