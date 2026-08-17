package chs.caplets.symbol.actions;

import chs.caf.CAFUtils;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.schem.IPinList;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IBlockIterator;
import chs.cof.symbol.ISymbolDef;
import chs.utilities.ui.MessageHelper;

import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.List;

public abstract class AddBlockActionHelper
{

	protected IBlock m_compositeBlock = null;

	protected AddBlockActionHelper(IBlock m_block)
	{
		m_compositeBlock = m_block;
	}

	public void setPinNames()
	{
		chs.cof.logical.cable.IPinList blockConnectivity = m_compositeBlock.getConnectivity();
		IAbstractPinIterator iter = blockConnectivity.getPins();
		while (iter.hasNext()) {
			IAbstractPin pin = iter.getNext();
			// set the name so that it isn't ever under default naming
			pin.setName(pin.getName());
		}
	}

	public void connectInternalLinks(IPinList pinList)
	{
	}

	public void checkforDuplicatePinNames(ISymbolDef symbolDef)
	{
		//Check whether current block pin names are already used in Symbol - If so , warn the user about Duplicate Pin Names
		List<String> pinNames = new ArrayList<String>();
		for (IBlockIterator bitr = symbolDef.getBlocks(); bitr.hasNext(); ) {
			IBlock sblk = bitr.getNext();
			if (sblk.getConnectivity() != null && sblk != m_compositeBlock) {
				for (IAbstractPinIterator pinIter = sblk.getConnectivity().getPins(); pinIter.hasNext(); ) {
					IAbstractPin curPin = pinIter.getNext();
					pinNames.add(curPin.getName());
				}
			}
		}
		if (symbolDef.getConnectivity() != null) {
			for (IAbstractPinIterator pinIter = symbolDef.getConnectivity().getPins(); pinIter.hasNext(); ) {
				IAbstractPin curPin = pinIter.getNext();
				pinNames.add(curPin.getName());
			}
		}
		IAbstractPinIterator pinIter = m_compositeBlock.getConnectivity().getPins();
		boolean duplicatePin = false;
		List<String> duplicatePinNames = new ArrayList<String>();
		while (pinIter.hasNext()) {
			IAbstractPin pin = pinIter.getNext();
			if (pinNames.contains(pin.getName())) {
				duplicatePin = true;
				duplicatePinNames.add(pin.getName());
			}
		}
		if (duplicatePin) {
			CreateAndShowDuplicatePinNamesWarningMsg(duplicatePinNames);
		}
	}

	protected abstract void CreateAndShowDuplicatePinNamesWarningMsg(List<String> duplicatePinNames);

	protected void showWarningMessage(JLabel actionLabel, String title, String heading, String message)
	{
		MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				actionLabel,
				title,
				heading,
				message);
	}
}
