/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2005-2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.IFIB;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import chs.utility.ui.IconUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Optional;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Mar 14, 2005 Time: 3:28:27 PM
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.ArtisanFunction, Application.ArtisanArchitect},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
public class EditSharedPinListActionUI extends ActionUI implements ISharedObjectBrowserAction
{

	public static final String EDIT_DEFAULT_NAME =
			ResourceMgr.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.name.decl");
	public static final String EDIT_DEFAULT_SHORTDESC =
			ResourceMgr.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.shortDesc.decl");
	public static final String EDIT_DEFAULT_LONGDESC =
			ResourceMgr.getString(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.longDesc.decl");

	public static final String EDIT_DEVICE_NAME =
			ResourceMgr.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Device.name.decl");
	public static final String EDIT_DEVICE_SHORTDESC = ResourceMgr
			.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Device.shortDesc.decl");
	public static final String EDIT_DEVICE_LONGDESC =
			ResourceMgr.getString(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Device.longDesc.decl");

	public static final String EDIT_FUNCTION_NAME =
			ResourceMgr.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Function.name.decl");
	public static final String EDIT_FUNCTION_SHORTDESC = ResourceMgr
			.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Function.shortDesc.decl");
	public static final String EDIT_FUNCTION_LONGDESC =
			ResourceMgr.getString(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Function.longDesc.decl");

	public static final String EDIT_PLUG_NAME =
			ResourceMgr.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Plug.name.decl");
	public static final String EDIT_PLUG_SHORTDESC = ResourceMgr
			.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Plug.shortDesc.decl");
	public static final String EDIT_PLUG_LONGDESC =
			ResourceMgr.getString(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Plug.longDesc.decl");

	public static final String EDIT_JACK_NAME =
			ResourceMgr.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Jack.name.decl");
	public static final String EDIT_JACK_SHORTDESC = ResourceMgr
			.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Jack.shortDesc.decl");
	public static final String EDIT_JACK_LONGDESC =
			ResourceMgr.getString(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Jack.longDesc.decl");

	public static final String EDIT_INTERCONNECTDEVICE_NAME = ResourceMgr.getStringForMenu(
			EditSharedPinListActionUI.class, "EditSharedPinListActionUI.InterconnectDevice.name.decl");
	public static final String EDIT_INTERCONNECTDEVICE_SHORTDESC = ResourceMgr.getStringForMenu(
			EditSharedPinListActionUI.class, "EditSharedPinListActionUI.InterconnectDevice.shortDesc.decl");
	public static final String EDIT_INTERCONNECTDEVICE_LONGDESC = ResourceMgr
			.getString(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.InterconnectDevice.longDesc.decl");

	public static final String EDIT_INTERCONNECTCONNECTOR_NAME = ResourceMgr.getStringForMenu(
			EditSharedPinListActionUI.class, "EditSharedPinListActionUI.InterconnectConnector.name.decl");
	public static final String EDIT_INTERCONNECTCONNECTOR_SHORTDESC = ResourceMgr.getStringForMenu(
			EditSharedPinListActionUI.class, "EditSharedPinListActionUI.InterconnectConnector.shortDesc.decl");
	public static final String EDIT_INTERCONNECTCONNECTOR_LONGDESC = ResourceMgr.getString(
			EditSharedPinListActionUI.class, "EditSharedPinListActionUI.InterconnectConnector.longDesc.decl");

	public static final String EDIT_INLINE_NAME =
			ResourceMgr.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Inline.name.decl");
	public static final String EDIT_INLINE_SHORTDESC = ResourceMgr
			.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Inline.shortDesc.decl");
	public static final String EDIT_INLINE_LONGDESC =
			ResourceMgr.getString(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.Inline.longDesc.decl");

	public static final String EDIT_RINGTERMINAL_NAME =
			ResourceMgr.getStringForMenu(EditSharedPinListActionUI.class,
					"EditSharedPinListActionUI.RingTerminal.name.decl");
	public static final String EDIT_RINGTERMINAL_SHORTDESC = ResourceMgr
			.getStringForMenu(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.RingTerminal.shortDesc.decl");
	public static final String EDIT_RINGTERMINAL_LONGDESC =
			ResourceMgr.getString(EditSharedPinListActionUI.class,
					"EditSharedPinListActionUI.RingTerminal.longDesc.decl");

	public EditSharedPinListActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_sharedpinlist.gif");
		Integer iMnemonic = new Integer(
				ResourceMgr.getMnemonic(EditSharedPinListActionUI.class, "EditSharedPinListActionUI.mnemonic"));

		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, EDIT_DEFAULT_NAME);
		putValue(SHORT_DESCRIPTION, EDIT_DEFAULT_SHORTDESC);
		putValue(LONG_DESCRIPTION, EDIT_DEFAULT_LONGDESC);
		putValue(SMALL_ICON, icon);
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			IAction action = getAction();
			if (action != null) {
				action.setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
			}
			return false;
		}
		if (getFIB().isTaskActive(IFIB.TASK_SAVE)) {
			return false;
		}
		return ISharedObjectBrowserAction.isTreeConstructionComplete() && super.isEnabled();
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return EditSharedPinListAction.class.getName();
	}

	public void updateUI()
	{
		super.updateUI();
		EditSharedPinListAction action = (EditSharedPinListAction) getAction();
		if (action == null) {
			return;
		}
		ISharedPinList op = action.getOperand();
		if (op == null) {
			return;
		}
		setTextAndIconFromOperand(op);
	}

	public void setTextAndIconFromOperand(ISharedPinList op)
	{
		if (op != null) {
			Optional<Icon> iconForSharedPinList = Optional.empty();

			if (op.getType().equals(PinListTypeEnum.TypeInterconnectDevice)) {
				putValue(NAME, EDIT_INTERCONNECTDEVICE_NAME);
				putValue(SHORT_DESCRIPTION, EDIT_INTERCONNECTDEVICE_SHORTDESC);
				putValue(LONG_DESCRIPTION, EDIT_INTERCONNECTDEVICE_LONGDESC);
			}
			else if (op.getType().equals(PinListTypeEnum.TypeInterconnectConnector)) {
				putValue(NAME, EDIT_INTERCONNECTCONNECTOR_NAME);
				putValue(SHORT_DESCRIPTION, EDIT_INTERCONNECTCONNECTOR_SHORTDESC);
				putValue(LONG_DESCRIPTION, EDIT_INTERCONNECTCONNECTOR_LONGDESC);
			}
			else if (op.getType().equals(PinListTypeEnum.TypeDevice)) {
				putValue(NAME, EDIT_DEVICE_NAME);
				putValue(SHORT_DESCRIPTION, EDIT_DEVICE_SHORTDESC);
				putValue(LONG_DESCRIPTION, EDIT_DEVICE_LONGDESC);
			}
			else if(op.getType().equals(PinListTypeEnum.TypeFunction)){
				putValue(NAME, EDIT_FUNCTION_NAME);
				putValue(SHORT_DESCRIPTION, EDIT_FUNCTION_SHORTDESC);
				putValue(LONG_DESCRIPTION, EDIT_FUNCTION_LONGDESC);
			}
			else if (op.getType().equals(PinListTypeEnum.TypeInlineJack)
					|| op.getType().equals(PinListTypeEnum.TypeInlinePlug)) {
				putValue(NAME, EDIT_INLINE_NAME);
				putValue(SHORT_DESCRIPTION, EDIT_INLINE_SHORTDESC);
				putValue(LONG_DESCRIPTION, EDIT_INLINE_LONGDESC);
				iconForSharedPinList = Optional.ofNullable(IconUtils.getSharedInlineIcon(IconUtils.ACTIVE));
			}
			else if (op.getType().equals(PinListTypeEnum.TypePlug)) {
				putValue(NAME, EDIT_PLUG_NAME);
				putValue(SHORT_DESCRIPTION, EDIT_PLUG_SHORTDESC);
				putValue(LONG_DESCRIPTION, EDIT_PLUG_LONGDESC);
			}
			else if (op.getType().equals(PinListTypeEnum.TypeJack)) {
				putValue(NAME, EDIT_JACK_NAME);
				putValue(SHORT_DESCRIPTION, EDIT_JACK_SHORTDESC);
				putValue(LONG_DESCRIPTION, EDIT_JACK_LONGDESC);
			}
			else if (op.getType().equals(PinListTypeEnum.TypeRingTerminal)) {
				putValue(NAME, EDIT_RINGTERMINAL_NAME);
				putValue(SHORT_DESCRIPTION, EDIT_RINGTERMINAL_SHORTDESC);
				putValue(LONG_DESCRIPTION, EDIT_RINGTERMINAL_LONGDESC);
			}
			else {
				putValue(NAME, EDIT_DEFAULT_NAME);
				putValue(SHORT_DESCRIPTION, EDIT_DEFAULT_SHORTDESC);
				putValue(LONG_DESCRIPTION, EDIT_DEFAULT_LONGDESC);
			}

			Icon icon = iconForSharedPinList.orElse(IconUtils.getIconForSharedPinList(op.getType(), IconUtils.ACTIVE));
			setIcon(icon);
		}
	}

	private void setIcon(@Nullable Icon iconForSharedPinList)
	{
		if (iconForSharedPinList != null) {
			putValue(SMALL_ICON, iconForSharedPinList);
		}
	}
}
