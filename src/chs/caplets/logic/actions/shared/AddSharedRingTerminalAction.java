package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caplets.logic.shared.AddSharedPinListDialog;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.common.IExtent;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Point;
import java.util.List;
import java.util.function.Consumer;

public class AddSharedRingTerminalAction extends AddSharedPlugConnectorAction
{

	private static Cursor m_ringTerminalCursor = null;

	public AddSharedRingTerminalAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller, sharedSelectMgr);
		setSubType(getType());
		if (m_ringTerminalCursor == null) {
			m_ringTerminalCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_ringterminal.gif", new Point(8, 8));
		}
	}

	protected PinListTypeEnum getType()
	{
		return RINGTERMINAL_CONNECTOR;
	}

	public String getActionUIClass()
	{
		return AddSharedRingTerminalActionUI.class.getName();
	}

	public Cursor getCursor()
	{
		return m_ringTerminalCursor;
	}

	public String getStatusbarText()
	{
		switch (getState()) {
			case STATE_PARAM:
				return ResourceMgr.getString(AddSharedRingTerminalAction.class,
						"AddSharedRingTerminalAction.StatusBar.text");
			case STATE_PINS:
				return m_addPinActionHelper.getStatusbarText();
			default:
				return null;
		}
	}

	@Override public void constrainExtent(IExtent constExtent)
	{
		super.constrainExtent(constExtent);
		constrainExtentByMaxPinCount(constExtent, 1);
	}

	@NotNull @Override protected AddSharedPinListDialog getAddSharedPinListDialog(@Nullable Frame owner)
	{

		return new AddSharedRingTerminalDialog(owner,
				CAFUtils.getInstance().getDialogTitleByAction(this, true), PinListTypeEnum.TypeRingTerminal);
	}

	protected static class AddSharedRingTerminalDialog extends AddSharedPinListDialog
	{

		AddSharedRingTerminalDialog(@Nullable Frame frame, String title, @Nullable PinListTypeEnum plType)
		{
			super(frame, title, plType);
		}

		@Override protected void createAutoGenerateOption()
		{
			//
		}

		@Override public boolean getAutoGenerate()
		{
			return false;
		}

		@Override protected void createAsStackOption()
		{
			//
		}

		@Override protected void createAsGroupOption()
		{
			//
		}

		@Override protected void createIndividualOption()
		{
		}

		public Consumer<List<?>> getSelectedPinsHandler()
		{
			return new Consumer<List<?>>()
			{
				@Override public void accept(List<?> objects)
				{
					boolean canAdd = canAddCurrentPinListToDesign();
					if (!objects.isEmpty() && canAdd) {
						getOkButton().setEnabled(true);
						getOkButton().setToolTipText(null);
					}
					else {
						getOkButton().setEnabled(false);
						String toolTipText;
						if (!canAdd) {
							toolTipText = ResourceMgr.getString(AddSharedPinListDialog.class,
									"AddSharedPinListDialog.OKButton.Tooltip.text");
						}
						else {
							toolTipText = ResourceMgr.getString(AddSharedRingTerminalAction.class,
									"AddSharedRingTerminalAction.tooltip.text");
						}
						getOkButton().setToolTipText(toolTipText);
					}
				}
			};
		}

		@NotNull @Override public String getHelpID()
		{
			return AddSharedPinListDialog.class.getName();
		}
	}
}
