/*
 * Copyright 2002-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.common.IUIDObject;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * Description of the Class
 *
 * @author Darin Jackson
 * @created August 1, 2001
 */
public abstract class DeleteActionUI extends ActionUI implements ISelectListener
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public DeleteActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		String name = ResourceMgr.getString(DeleteActionUI.class, "DeleteActionUI.Delete.Name");
		String shortDesc = ResourceMgr.getString(DeleteActionUI.class, "DeleteActionUI.Delete.ShortDesc");
		String longDesc = ResourceMgr.getString(DeleteActionUI.class, "DeleteActionUI.Delete.LongDesc");
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		KeyStroke accel = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0);
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_D);

		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, shortDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, icon);
		putValue(ACCELERATOR_KEY, accel);
		putValue(MNEMONIC_KEY, iMnemonic);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public Icon getInactiveIcon()
	{
		return null;
		//return CHSImageLoader.loadImageIcon("chs/images/general/ico_rotate_inactive.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public abstract String getActionClass();

	public void selectionChanged(SelectEvent e)
	{
		boolean bEnable = false;
		ISelectMgr activeSM = CAFUtils.getInstance().getActiveSelectMgr();
		if (activeSM != null) {
			for (SelectedUIDObjectIterator iter = activeSM.getPreSelections().getSelectedUIDObjects();
					iter.hasNext();) {
				IUIDObject obj = iter.getNext();
				//
				// Don't allow deletion of pins from a symbol.
				//
				if (obj instanceof chs.cof.logical.schem.IPin) {
					chs.cof.logical.schem.IPin pin = (chs.cof.logical.schem.IPin) obj;
					if (pin.getParent() instanceof chs.cof.logical.schem.IPinList) {
						chs.cof.logical.schem.IPinList pl = (chs.cof.logical.schem.IPinList) pin.getParent();
						if (pl.getSymbolRef() != null) {
							continue;
						}
					}
				}
				bEnable = true;
				break;
			}
		}
		setEnabled(bEnable);
	}
}
