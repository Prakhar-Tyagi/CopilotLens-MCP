package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.cof.logical.cable.IFunctionPin;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction})
public class MoveFunctionPortActionUI extends ActionUI implements ISelectListener
{

	public MoveFunctionPortActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
		Integer iMnemonic = KeyEvent.VK_M;

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(MoveFunctionPortActionUI.class, "MoveFunctionPortActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(MoveFunctionPortActionUI.class, "MoveFunctionPortActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(MoveFunctionPortActionUI.class, "MoveFunctionPortActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);

		getCaplet().getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public Icon getInactiveIcon()
	{

		return CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
	}

	public String getActionClass()
	{
		return MoveFunctionPortAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		boolean bEnable = false;
		ISelectMgr activeSM = CAFUtils.getInstance().getActiveSelectMgr();
		if (activeSM != null) {
			for (SelectionIterator iter = activeSM.getPreSelections().getSelected(); iter.hasNext(); ) {

				bEnable = true;
				Selection sel = iter.getNext();

				if (!IFunctionPin.class.isAssignableFrom(sel.getSelectionClass())) {
					bEnable = false;
					break;
				}
			}
		}

		setEnabled(bEnable);
	}
}
