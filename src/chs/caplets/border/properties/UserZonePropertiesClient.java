/*
 * Copyright 2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.border.properties;

import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.symbol.Model;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.symbol.IUserDefinedZone;
import chs.common.IUIDObject;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.Nullable;

public class UserZonePropertiesClient extends BorderPropertiesClient
{

	public UserZonePropertiesClient(Model model)
	{
		super(model);
	}

	/**
	 * @param selections - selection to check for user defined zones.
	 *
	 * @return - It will return a selection of user defined zones only if the incoming selection contains their
	 * * name attribute text only and the selection is not empty. Otherwise NULL is returned.
	 */
	@Nullable public static SelectSet convertToSelectionOfUserDefinedZones(@Nullable SelectSet selections)
	{
		if (selections == null) {
			return null;
		}
		SelectSet selectedUserDefinedZones = new SelectSet();
		SelectedUIDObjectIterator selectedUIDObjects = selections.getSelectedUIDObjects();
		while (selectedUIDObjects.hasNext()) {
			IUIDObject obj = selectedUIDObjects.getNext();
			IUserDefinedZone userDefinedZone =
					CommonUtils.cast(obj instanceof IAttributeText ? ((IDiagramObject) obj).getParent() : null,
							IUserDefinedZone.class);
			if (userDefinedZone == null) {
				return null;
			}
			selectedUserDefinedZones.add(userDefinedZone, false);
		}
		return selectedUserDefinedZones.isEmpty() ? null : selectedUserDefinedZones;
	}

	@Override protected boolean doStartEditingProperties(SelectSet selections)
	{
		SelectSet selectedUserDefinedZones = convertToSelectionOfUserDefinedZones(selections);
		return super.doStartEditingProperties(selectedUserDefinedZones != null ? selectedUserDefinedZones : selections);
	}
}
