/*
 * Copyright 2007-2008 Mentor Graphics Corporation
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
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.cof.logical.schem.IPin;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Description of the Class
 *
 * @author Darin Jackson
 * @created August 1, 2001
 */
public class AbstractMovePinActionUI extends ActionUI implements ISelectListener
{

	/**
	 * Constructor for the CreateCircleActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public AbstractMovePinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_M);

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(AbstractMovePinActionUI.class, "MovePinActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AbstractMovePinActionUI.class, "MovePinActionUI.name.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AbstractMovePinActionUI.class, "MovePinActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public Icon getInactiveIcon()
	{
		//return new ImageIcon(CHSImageLoader.loadImage("chs/images/general/ico_transparent.gif"));
		return CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AbstractMovePinAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		boolean bEnable = false;
		ISelectMgr activeSM = CAFUtils.getInstance().getActiveSelectMgr();
		if (activeSM != null) {
			for (SelectionIterator iter = activeSM.getPreSelections().getSelected(); iter.hasNext();) {

				bEnable = true;
				Selection sel = iter.getNext();

				if (IPin.class.isAssignableFrom(sel.getSelectionClass()) == false) {
					bEnable = false;
					break;
				}
			}
		}

		setEnabled(bEnable);
	}
}
