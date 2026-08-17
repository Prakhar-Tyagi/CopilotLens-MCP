/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023-2025 Siemens
 */

package chs.caplets.logic.actions.prototype;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.actions.prototype.AbstractPrototypeWireExpressionAction;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.SimpleActionUI;
import chs.caplets.logic.Model;
import chs.cof.logical.cable.IPrototypeWireExpressionObject;
import chs.common.IReadOnlyOptionedDesign;
import chs.ctf.ui.prototype.PrototypeWireExpressionUIContext;
import chs.ctf.ui.prototype.PrototypeWireExpressionViewController;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Objects;
import java.util.Set;

/**
 * Prototype option edit action
 */
public class LogicPrototypeWireExpressionEditAction extends AbstractPrototypeWireExpressionAction
{

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner}, immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
	public static class UI extends SimpleActionUI
	{

		public UI(@NotNull ICaplet caplet)
		{
			super(caplet);
			Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");
			putValue(SMALL_ICON, icon);
		}

		@NotNull @Override protected Class<? extends IAction> getOwningClass()
		{
			return LogicPrototypeWireExpressionEditAction.class;
		}

		@Override
		public boolean isEnabled()
		{
			IAction action = getAction();
			return action != null && action.isEnabled();
		}

		@Nullable @Override public String getToolTipText()
		{
			IAction action = getAction();
			if (action != null && action.isEnabled()) {
				return ResourceMgr.getString(LogicPrototypeWireExpressionEditAction.class,
						"LogicPrototypeWireExpressionEditAction.long.decl");
			}
			return getActionToolTip();
		}
	}

	@NotNull @Override public String getActionUIClass()
	{
		return UI.class.getName();
	}

	public LogicPrototypeWireExpressionEditAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@NotNull protected PrototypeWireExpressionViewController getViewController(
			@NotNull Set<IPrototypeWireExpressionObject> selections)
	{
		PrototypeWireExpressionUIContext context =
				new PrototypeWireExpressionUIContext(getDesign(), selections)
						.setParentFrame(getParentFrame())
						// if the design was opened in read only mode, reflect this in the dialog
						.setReadOnly(!getCapletModel().isEditable());

		return new PrototypeWireExpressionViewController(context);
	}

	@Override protected boolean shouldReportLockFailures()
	{
		return false;
	}

	@NotNull protected IReadOnlyOptionedDesign getDesign()
	{
		Model model = (Model) getCapletModel();
		return Objects.requireNonNull(model.getDesign());
	}
}
