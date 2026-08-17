package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.shared.AddSharedPinDialog;
import chs.caplets.logic.shared.AddSharedPortDialog;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Point;

public class AddFunctionPinActionHelper extends AddPinActionHelper
{

	private static Cursor m_addFunctionPinValidCursor = null;
	private static Cursor m_addFunctionPinInvalidCursor = null;

	public AddFunctionPinActionHelper(ControllerActionRT action, boolean requirePlacement,
			boolean useBoundaryExtensions)
	{
		super(action, requirePlacement, useBoundaryExtensions);
	}

	@Override @NotNull protected IAddPinView getPlacePinDialog(IPinList cpl, @NotNull IPlacementOptionParams params)
	{
		return new PlacePortsDialog(CAFUtils.getInstance().getDialogFrame(), cpl, true, params);
	}

	@Override protected void createAddPinCursors()
	{
		if (m_addFunctionPinValidCursor == null) {
			//noinspection AssignmentToStaticFieldFromInstanceMethod,NonThreadSafeLazyInitialization
			m_addFunctionPinValidCursor = CAFUtils.getInstance()
					.loadCursor(m_controller.getCaplet(), getAddPinCursorIcon(), new Point(7, 7));
			//noinspection AssignmentToStaticFieldFromInstanceMethod
			m_addFunctionPinInvalidCursor = CAFUtils.getInstance()
					.loadCursor(m_controller.getCaplet(), getIconForIvalidLocation(), new Point(7, 7));
		}
	}

	@Override @NotNull protected String getDialogTitle()
	{
		return ResourceMgr.getString(AbstractPinActionHelper.class, "AddPinActionHelper.PortSelectDialogTitle.text");
	}

	@Override protected String getTooltipForGroupPlacement()
	{
		return ResourceMgr.getString(AddFunctionPinActionHelper.class,
				"AddFunctionPinActionHelper.GroupHint.tooltip");
	}

	@Override protected Cursor getAddPinInvalidCursor()
	{
		return m_addFunctionPinInvalidCursor;
	}

	@Override protected Cursor getAddPinValidCursor()
	{
		return m_addFunctionPinValidCursor;
	}

	@Override @NotNull protected String getAddPinCursorIcon()
	{
		return CHSImages.FUNCTIONPIN_ADD_CURSOR;
	}

	@Override @NotNull public String getStatusbarText()
	{
		return ResourceMgr.getString(AddFunctionPinActionHelper.class, "AddFunctionPinActionHelper.StatusBarMessage");
	}

	@NotNull @Override
	protected AddSharedPinDialog getSharedPinSelectionDialog(Frame parentFrame, ISharedPinList sharedPinList)
	{
		return new AddSharedPortDialog(parentFrame, getDialogTitle(), sharedPinList);
	}
}
