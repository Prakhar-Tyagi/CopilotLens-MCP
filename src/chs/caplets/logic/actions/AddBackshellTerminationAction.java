/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedBackshellOwner;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryCavity;
import chs.cof.symbol.ISymbolRef;
import chs.common.INamedPropertiedObject;
import chs.ctf.caf.utils.IPinProxy;
import chs.utility.ui.BaseDialog;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AddBackshellTerminationAction extends AbstractBackshellAction
{

	public AddBackshellTerminationAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return AddBackshellTerminationActionUI.class.getName();
	}

	@Override public boolean isEnabled()
	{
		return super.isEnabled() && !hasFrozenSharedPinlistWthNoTerminations();
	}

	private boolean hasFrozenSharedPinlistWthNoTerminations()
	{
		final IPinList pinlist = getOperand(getController().getSelectMgr().getPreSelections());
		if (pinlist != null) {
			final chs.cof.logical.cable.IPinList connectivity = pinlist.getConnectivity();
			if (connectivity != null) {
				final ISharedPinList sharedPinList = connectivity.getSharedPinList();
				if (sharedPinList != null) {
					// CTRL+B should not allow termination addition when shared pinlist is frozen and has no terminations.
					return sharedPinList.isFrozen() && !hasTerminations(sharedPinList);
				}
			}
		}
		return false;
	}

	@Override protected boolean hasAllRequiredDataForActionActivation()
	{
		if (!super.hasAllRequiredDataForActionActivation()) {
			return false;
		}

		if (spl != null) {
			// CTRL+B should not allow termination addition when shared pinlist is frozen and has no terminations.
			return !spl.isFrozen() || hasTerminations(spl);
		}
		return true;
	}

	@Override protected boolean hasCollectedAllRequiredDataForActivation()
	{

		// Allow termination addition even if the pinlist is shared or has a part assigned.
		// If only one backshell termination exists, select it automatically.
		// If multiple terminations exist, prompt the user to select which to add.

		// Retrieve all available terminations (existing and from the library).
		final Collection<Object> terminations = getExistingTerminations();
		// If no terminations exist, select the next default termination name.
		if (terminations.isEmpty()) {
			//Pickup next default name!
			boolean selected = selectNextBackshellTermination();
			if (!selected) {
				return false;
			}
		}
		else {
			if (terminations.size() == 1) {
				// Only one termination available; select it automatically.
				selectTermination(terminations.iterator().next());
			}
			else {
				// Multiple terminations available; show dialog for user selection.
				AddBackshellTerminationDialog dialog = createAddBackshellTerminationDialog();

				if (dialog.isCancelled()) {
					return false;
				}
			}
		}

		final IBackshell backshell = getExistingOrTemporaryBackshell();

		selectedBackshellName(backshell.getName());

		final ISymbolRef symbolRef = backshell.getSymbolRef();

		selectedBackshellSymbol(symbolRef);

		return true;
	}

	@NotNull
	protected AddBackshellTerminationDialog createAddBackshellTerminationDialog()
	{
		AddBackshellTerminationDialog dialog = new AddBackshellTerminationDialog(getFrame(), getTitle(), true, this);
		dialog.setVisible(true);
		return dialog;
	}

	protected boolean selectNextBackshellTermination()
	{
		selectTermination(getNextBackshellTermination());
		return true;
	}

	@NotNull private Collection<Object> getExistingTerminations()
	{

		//Collect existing backshell terminations
		final Set<INamedPropertiedObject> existingTerminations = getExistingBackshellTerminations();
		final Collection<Object> terminations = new HashSet<>(existingTerminations);

		final Set<String> existingNames = existingTerminations
				.stream()
				.map(INamedPropertiedObject::getName)
				.collect(Collectors.toSet());

		//Collect non-existing library backshell terminations
		for (String termination : getLibraryBackshellTerminations()) {
			if (!existingNames.contains(termination)) {
				terminations.add(termination);
			}
		}

		return terminations;
	}

	protected void selectTermination(Object termination)
	{
		final List<IPinProxy> selectedTerminations = new ArrayList<>();
		selectedTerminations.add(createPinProxy(termination));
		selectedBackshellTerminations(selectedTerminations);
	}

	private Set<String> getLibraryBackshellTerminations()
	{
		return getLibraryTerminations().stream().map(ILibraryCavity::getName).collect(Collectors.toSet());
	}

	private boolean hasTerminations(@NotNull ISharedPinList sharedPinList)
	{
		if (sharedPinList instanceof ISharedBackshellOwner sharedBackshellOwner) {
			final ISharedBackshell backshell = sharedBackshellOwner.getBackshell();
			if (backshell != null) {
				return backshell.getNumBackshellTerminations() > 0;
			}
		}
		return false;
	}
}

