package chs.caplets.shared.actions;

import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.ICaplet;
import chs.caf.CAFUtils;
import chs.cof.logical.schem.IPin;

/**
 * Abstract class for action UIs for pin stack
 */
public abstract class AbstractStackPinActionUI extends ActionUI implements ISelectListener
{

	protected AbstractStackPinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void selectionChanged(SelectEvent e)
	{
		boolean bEnable = false;
		ISelectMgr activeSM = CAFUtils.getInstance().getActiveSelectMgr();
		if (activeSM != null) {
			for (SelectionIterator iter = activeSM.getPreSelections().getSelected(); iter.hasNext();) {

				bEnable = true;
				Selection sel = iter.getNext();

				if (!IPin.class.isAssignableFrom(sel.getSelectionClass())) {
					bEnable = false;
					break;
				}
			}
		}

		setEnabled(bEnable);
	}
}
