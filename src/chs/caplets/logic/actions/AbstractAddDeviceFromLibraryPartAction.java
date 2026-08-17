package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.creation.CreateByPointAction;
import chs.cof.logical.cable.IDevice;
import chs.cof.parts.ILibraryGraphic;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.Library;
import chs.cof.parts.LibraryCriteriaHelper;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.parts.partselector.ILibraryPartSelector;
import chs.cof.parts.partselector.ILibrarySelectionFilter;
import chs.cof.parts.partselector.PartSelectionContext;
import chs.cof.project.IProject;
import chs.common.IDesignContainer;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.criteria.ICriteria;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LibraryHelper;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractAddDeviceFromLibraryPartAction extends ControllerActionRT
		implements MouseListener, MouseMotionListener, KeyListener
{

	protected ControllerActionRT subAction;
	protected Set<IUIDObject> newObjects = new HashSet<IUIDObject>();


	protected AbstractAddDeviceFromLibraryPartAction(ICapletController controller)
	{
		super(controller);
	}

	/**
	 * On activation, we choose a part via the PSD and setup the appropriate "sub-action" to do it's own activation,
	 * depending on whether the part has a symbol or not.
	 */
	protected IActionEnum onActivate(ActionEvent e)
	{
		// pick a library part
		subAction = null;
		newObjects.clear();
		ILibraryPartSelection libraryPart = pickLibraryPart();
		IActionEnum result = IActionEnum.eCanceled;
		if (libraryPart != null) {
			if (libraryPart.getSelectedObject() != null) {
				if (isSelectionAssociatedWithSymbol(libraryPart)) {
					result = activateAddWithSymbol(e, libraryPart);
				}
				else {
					result = activateAddWithoutSymbol(e, libraryPart);
				}
			}
			else {
				result = IActionEnum.eCanceled;
				assert false : "library selection contains a null ILibraryObject";
			}
		}

		return result;
	}

	public boolean isSelectionAssociatedWithSymbol(ILibraryPartSelection iLibraryPartSelection)
	{
		if (iLibraryPartSelection != null && iLibraryPartSelection.getSelectedObject() != null) {
			ILibraryGraphic selectedSymbol = iLibraryPartSelection.getSelectedSymbol();
			if (selectedSymbol != null && LibraryHelper.getLogicalSymbol(selectedSymbol) != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Activates the sub-action for adding a library part with a symbol to create an instance with that symbol.
	 */
	protected IActionEnum activateAddWithSymbol(ActionEvent e, ILibraryPartSelection libraryPart)
	{
		AddLibraryPartWithSymbolAction action = getAddWithSymbolAction(libraryPart);
		subAction = action; // must set subAction before activating it so events are forwarded
		return action.onActivate(e);
	}

	protected AddLibraryPartWithSymbolAction getAddWithSymbolAction(ILibraryPartSelection libraryPart)
	{
		return new AddLibraryPartWithSymbolAction(getController(), libraryPart);
	}

	/**
	 * Activates the sub-action for adding a library part without a symbol to create a parameterized device and prompt the
	 * user for pin positions.
	 */
	protected IActionEnum activateAddWithoutSymbol(ActionEvent e, ILibraryPartSelection libraryPart)
	{
		AddParameterizedDeviceFromLibraryPartAction action =
				new AddParameterizedDeviceFromLibraryPartAction(getController(), libraryPart);
		subAction = action; // must set subAction before activating it so events are forwarded
		return action.onActivate(e);
	}

	/**
	 * Terminates the appropriate "sub-action" which has been setup according to whether the part has a symbol or not.
	 */
	protected boolean onTerminate(boolean successful)
	{
		// now we can really create the objects that were created a bit early
		if (successful) {
			CreationDeletionHelper.getTheCreationHelper().addCreationObjects(newObjects);
		}

		// terminate the appropriate sub-action
		// TODO jacobt FEAT3099.1 : Would it really be that bad to make ControllerActionRT.onTerminate public?
		boolean result = false;
		if (subAction instanceof AddInstanceAction) {
			// add as symbol
			result = ((AddInstanceAction) subAction).onTerminate(successful);
		}
		else if (subAction instanceof CreateByPointAction) {
			result = ((CreateByPointAction) subAction).onTerminate(successful);
		}
		else if (subAction instanceof CreateSpliceAction) {
			result = ((CreateSpliceAction) subAction).onTerminate(successful);
			if (result) {
				// the other sub-actions already do this.  We have to do it e
			}
		}
		else if(subAction instanceof AddPinListAction){
			result=((AddPinListAction)subAction).onTerminate(successful);
		}
		else if (subAction != null) {
			assert false : "Unknown sub-action " + subAction;
		}

		if (!successful) {
			// if we cancelled the action part way through we might have this stuff hanging around in the UIDMgr
			for (IUIDObject obj : newObjects) {
				if (obj instanceof IDevice) {
					// this temp device must be removed from the connectivity!
					IDevice tempDevice = (IDevice) obj;
					if (tempDevice.getConnectivity() != null) {
						tempDevice.getConnectivity().removeDevice(tempDevice);
					}
				}
				UIDMgr.removeObject(obj.getUID());
			}
		}
		newObjects.clear();

		subAction = null;
		return result;
	}

	@Override public boolean shouldDisableUndoForNonUndoableChanges()
	{
		if(subAction != null) {
			return subAction.shouldDisableUndoForNonUndoableChanges();
		}
		return super.shouldDisableUndoForNonUndoableChanges();
	}

	public boolean isEnabled()
	{
		return getController().getCapletModel().isEditable() && super.isEnabled();
	}

	public abstract String getActionUIClass();

	public String getStatusbarText()
	{
		if (subAction != null) {
			return subAction.getStatusbarText();
		}

		return "";
	}

	public Cursor getCursor()
	{
		if (subAction != null) {
			return subAction.getCursor();
		}
		return null;
	}

	/**
	 * Pick a library part from the PSD, used to create the device
	 *
	 * @return The library part, or null if none was chosen.
	 */
	@Nullable protected ILibraryPartSelection pickLibraryPart()
	{
		ILibraryPartSelection partSel = getPartSelection();
		if (partSel != null) {
			return partSel;
		}
		ILibraryObject.GroupType filter = getPartFilter();

		ICriteria<? extends ILibraryObject> criteria = LibraryCriteriaHelper.createCriteria(
				filter.getLibraryObjectClass());
		ILibraryPartSelector partSelector =
				Library.getInstance().getLibraryPartSelector(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
		IDesignContainer activeDesignContainer = CAFUtils.getInstance().getActiveDesignContainer();
		IProject currProject = CAFUtils.getInstance().getCurrentProject();
		List<IUID> customers = LibraryCriteriaHelper.getCustomerDetailsFromScopes(activeDesignContainer, currProject);
		ILibrarySelectionFilter electricalSymbolFilter =
				LibraryCriteriaHelper.getSelectionFilterForElectricalSymbols(null, null, customers);
		PartSelectionContext partSelectionContext = new PartSelectionContext();
		partSelectionContext.setSelectionFilter(electricalSymbolFilter);

		//@todo used library configuration context directly, needs confirmation - kjuthi
		return partSelector.selectPart(criteria, CAFUtils.getInstance().getCurrentProject(),
				partSelectionContext, ConfigurationTypeEnum.LOGICAL, CAFUtils.getInstance().getActiveDesignContainer());
	}

	@Nullable protected abstract ILibraryPartSelection getPartSelection();

	protected ILibraryObject.GroupType getPartFilter()
	{
		return ILibraryObject.GroupType.DEVICE;
	}

	/**
	 * Forward all mouse and keyboard events to the appropriate "sub-actions".
	 */
	public void mouseClicked(MouseEvent e)
	{
		if (subAction instanceof MouseListener) {
			((MouseListener) subAction).mouseClicked(e);
		}
	}

	public void mousePressed(MouseEvent e)
	{
		if (subAction instanceof MouseListener) {
			((MouseListener) subAction).mousePressed(e);
		}
	}

	public void mouseReleased(MouseEvent e)
	{
		if (subAction instanceof MouseListener) {
			((MouseListener) subAction).mouseReleased(e);

			// sometimes this action creates stuff a bit too early
			// need to mess with CDH to avoid warnings about creating stuff before onTerminate
			CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
			if (cdh.getPendingCount() > 0) {
				newObjects.addAll(CollectionUtils.createList(cdh.getNewObjectsToProcess()));
				cdh.clearNewObjects();
			}
		}
	}

	public void mouseEntered(MouseEvent e)
	{
		if (subAction instanceof MouseListener) {
			((MouseListener) subAction).mouseEntered(e);
		}
	}

	public void mouseExited(MouseEvent e)
	{
		if (subAction instanceof MouseListener) {
			((MouseListener) subAction).mouseExited(e);
		}
	}

	public void mouseDragged(MouseEvent e)
	{
		if (subAction instanceof MouseMotionListener) {
			((MouseMotionListener) subAction).mouseDragged(e);
		}
	}

	public void mouseMoved(MouseEvent e)
	{
		if (subAction instanceof MouseMotionListener) {
			((MouseMotionListener) subAction).mouseMoved(e);
		}
	}

	public void keyTyped(KeyEvent e)
	{
		if (subAction instanceof KeyListener) {
			((KeyListener) subAction).keyTyped(e);
		}
	}

	public void keyPressed(KeyEvent e)
	{
		if (subAction instanceof KeyListener) {
			((KeyListener) subAction).keyPressed(e);
		}
	}

	public void keyReleased(KeyEvent e)
	{
		if (subAction instanceof KeyListener) {
			((KeyListener) subAction).keyReleased(e);
		}
	}

}