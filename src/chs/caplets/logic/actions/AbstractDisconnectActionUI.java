package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemStackPin;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

public abstract class AbstractDisconnectActionUI extends ActionUI implements ISelectListener
{

	AbstractDisconnectActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_D, 0);
		putValue(MNEMONIC_KEY,
				(int) ResourceMgr.getMnemonic(AbstractDisconnectActionUI.class, "DisconnectActionUI.mnemonic"));

		putValue(NAME, ResourceMgr.getString(AbstractDisconnectActionUI.class, "DisconnectActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AbstractDisconnectActionUI.class, "DisconnectActionUI.name.decl"));
		putValue(LONG_DESCRIPTION, getLongDescription());
		putValue(SMALL_ICON, getActiveIcon());
		putValue(ACCELERATOR_KEY, accel);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	abstract String getLongDescription();

	private Icon getActiveIcon()
	{
		ICaplet caplet = getCaplet();
		if (caplet.isFunctionCaplet()) {
			return CHSImageLoader.loadImageIcon(CHSImages.FUNCTION_DISCONNECT_SELECTED_ICON);
		}
		return CHSImageLoader.loadImageIcon(CHSImages.DEVICE_DISCONNECT_SELECTED_ICON);
	}

	public Icon getInactiveIcon()
	{
		if (getCaplet().isFunctionCaplet()) {
			return CHSImageLoader.loadImageIcon("chs/images/app/ico_disconnect_selected_inactive.gif");
		}
		return CHSImageLoader.loadImageIcon("chs/images/general/ico_disconnect_selected_inactive.gif");
	}

	public void selectionChanged(SelectEvent e)
	{
		boolean bEnable = false;
		//
		// Only if the super says yes, we have an option of making this enabled.
		//
		if (super.isEnabled()) {
			ISelectMgr activeSM = CAFUtils.getInstance().getActiveSelectMgr();
			if (activeSM != null) {
				SelectSet presel = activeSM.getPreSelections();
				if (presel.getSelectCount() >= 1) {
					SelectionIterator iter = presel.getSelected();
					Selection sel = iter.getNext();
					if (IPin.class.isAssignableFrom(sel.getSelectionClass())) {
						IPin pin = (IPin) UIDMgr.getObject(sel.getUID());
						IAbstractPin cPin = pin != null ? pin.getConnectivity() : null;
						if (cPin != null) {
							if (cPin.getConductors() != null) {
								if (cPin.getConductors().getSize() >= 1) {
									bEnable = true;
								}
							}
						}
					}
					else if (ISchemStackPin.class.isAssignableFrom(sel.getSelectionClass())) {
						ISchemStackPin pin = (ISchemStackPin) UIDMgr.getObject(sel.getUID());
						if (pin != null && !pin.getConnectedHighways().isEmpty()) {
							bEnable = true;
						}
					}
					else if (IPinList.class.isAssignableFrom(sel.getSelectionClass())) {
						bEnable = true;
					}
				}
			}
		}
		setEnabled(bEnable);
	}
}
