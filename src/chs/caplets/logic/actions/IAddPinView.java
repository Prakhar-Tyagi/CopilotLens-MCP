package chs.caplets.logic.actions;

import chs.ctf.caf.utils.IPinProxy;

import java.util.List;

/**
 * Created with IntelliJ IDEA. User: brangan Date: 1/9/14 Time: 3:20 PM To change this template use File | Settings |
 * File Templates.
 */
public interface IAddPinView
{
	/**
	 * The result of invoking the dialog.
	 */
	enum Result
	{

		/**
		 * Create a single new connectivity and schem pin.
		 */
		CREATE,

		/**
		 * Create schematic (and possibly connectivity) instances of the selected pins in the dialog.
		 */
		PLACE,

		/**
		 * Cancelled (closed)
		 */
		CANCEL
	}

	List<IPinProxy> getPins();
	boolean isReference();
	boolean isPlaceAsStack();
	boolean isPlaceAsGroup();
	boolean isWithConductor();
	Result showDialog();
}
