/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.cafmain.actions.CAFCommandListener;
import chs.caf.cafmain.actions.ReplaceSymbolInstanceHelper;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.CommandClientAction;
import chs.caf.caplet.helpers.CommandClientCtxMenuAction;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.symbol.Model;
import chs.cof.drawplus.IBaseDiagram;
import chs.common.cmd.replacesymbol.ReplaceInstanceBlockOptions;
import chs.common.cmd.replacesymbol.ReplaceInstanceBlockParams;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolOptions;
import chs.common.cmd.replacesymbol.ReplaceInstanceSymbolParams;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Class UpdateInstanceAction - Replaces an chs.cof.logical.schem.IPinList symbol with different compatible one on a
 * Symbol
 * <p/>
 * Responsibilites- Gather parameters via OptionsDialog if needed and invoke SymUpdateInstanceSymbolCmd
 * <p/>
 * Collaborators- SymUpdateInstanceSymbolCmd
 */
public class UpdateInstanceAction extends CommandClientCtxMenuAction<SymUpdateInstanceSymbolCmd>
{
	//
	// Public methods
	//

	public UpdateInstanceAction(ICapletController controller)
	{
		super(controller);
	}

	/**
	 * @see ActionRT#getActionUIClass()
	 */
	public String getActionUIClass()
	{
		return UpdateInstanceActionUI.class.getName();
	}

	/**
	 * @see IAction#isEnabled()
	 */
	public boolean isEnabled()
	{
		if (!super.isEnabled()) {
			return false;
		}
		m_disabledReason = chs.caf.cafmain.actions.UpdateInstanceAction.getDisabledReason(getController());
		if (m_disabledReason != null) {
			return false;
		}
		return !getCmdParams().isEmpty();
	}

	//
	// Protected methods
	//

	/**
	 * @see CommandClientAction#doOnActivateCmdInitialize()
	 */
	protected boolean doOnActivateCmdInitialize()
	{
		setCommand(new SymUpdateInstanceSymbolCmd(new CAFCommandHelper()));

		List<? extends ReplaceInstanceSymbolParams> params = getCmdParams();
		if (!params.isEmpty()) {
			final ReplaceInstanceSymbolOptions userOptions = getOptions(params);
			if (userOptions != null) {

				CAFCommandListener<UpdateInstanceAction> cmdListener =
						new CAFCommandListener<UpdateInstanceAction>(UpdateInstanceAction.class);
				getCommand().setCommandListener(cmdListener);
				getCommand().setParams(params);
				return true;
			}
		}
		return false;
	}

	@Nullable protected ReplaceInstanceSymbolOptions getOptions(List<? extends ReplaceInstanceSymbolParams> params)
	{
		final ReplaceInstanceSymbolOptions userOptions = ReplaceSymbolInstanceHelper.getOptions(params, true, true);
		ReplaceInstanceBlockOptions.setisPorts(false);
		return userOptions;
	}

	/**
	 * Creates ReplaceInstanceSymbolParams for updateable objects from the current selection
	 *
	 * @return List<ReplaceInstanceSymbolParams>
	 */
	private List<? extends ReplaceInstanceSymbolParams> getCmdParams()
	{
		SelectSet selectSet = getController().getSelectMgr().getCurrentSelections();
		List<ReplaceInstanceBlockParams> params =
				new ArrayList<ReplaceInstanceBlockParams>(selectSet.getSelectCount());
		Model model = CommonUtils.cast(getController().getCapletModel(), Model.class);
		if (model != null) {
			IBaseDiagram diagram = model.getDiagram();
			for (SelectionIterator iter = selectSet.getSelected(); iter.hasNext(); ) {
				ReplaceInstanceBlockParams tempParam =
						new ReplaceInstanceBlockParams(diagram, iter.getNext().getUID());
				if (tempParam.isValidForUpdateInSymbol()) {
					params.add(tempParam);
				}
			}
		}
		return params;
	}

	protected void postCmdExecute()
	{
		super.postCmdExecute();
		CAFUtils.getInstance().getSymbolLibraryMgr().refreshUI(this);
	}
}
