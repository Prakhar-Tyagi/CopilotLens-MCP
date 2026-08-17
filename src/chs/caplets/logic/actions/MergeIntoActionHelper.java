package chs.caplets.logic.actions;

import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.common.IUIDObject;
import chs.utility.helpers.ReferenceHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 24-Mar-2010 Time: 13:55:25 To change this template use File | Settings
 * | File Templates.
 */
public class MergeIntoActionHelper
{

	private MergeIntoActionHelper()
	{
	}

	@Nullable public static ILogicObject getOperand(SelectSet selections)
	{
		ILogicObject connectivityObject = null;

		SelectedUIDObjectIterator selectedObjects = selections.getSelectedUIDObjects();
		boolean matesSelected = true;
		while (selectedObjects.hasNext()) {
			IUIDObject uidObject = selectedObjects.getNext();
			ILogicObject connObject = ReferenceHelper.reduceToLogicObject(uidObject);
			if (connObject == null) {
				continue;
			}
			if (connObject instanceof IDeviceConnector) {
				//LOGIC-8180 Merge into option is available on Device side connector.
				return null;
			}
			if (connObject instanceof IConnector) {
				if (((IConnector) connObject).getOccupiedPosition() != null) {
					return null; //cannot perform merge-Into on a child connector
				}
				if (!((IConnector) connObject).getPositionedObjects().isEmpty()) {
					return null; //cannot perform merge-Into on a modular connector with atleast one filled position
				}
			}
			if (connectivityObject == null) {
				connectivityObject = connObject;
				if (connectivityObject instanceof IGenericInlineConnector) {
					matesSelected = false;
				}
			}
			else if (connectivityObject != connObject && !areInlineMates(connectivityObject, connObject)) {
				return null;
			}

			if (areInlineMates(connectivityObject, connObject)) {
				matesSelected = true;
			}
		}
		return matesSelected ? connectivityObject : null;
	}

	private static boolean areInlineMates(ILogicObject sideOne, ILogicObject sideTwo)
	{
		if (sideOne instanceof IGenericInlineConnector && sideTwo instanceof IGenericInlineConnector) {
			return ((IGenericInlineConnector) sideOne).getMatedInlines().contains(sideTwo);
		}
		else {
			return false;
		}
	}

	public static boolean isMergeable(@Nullable ILogicObject object)
	{
		return object != null && object.isMergeable();
	}
}
