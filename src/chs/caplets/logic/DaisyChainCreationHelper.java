/*
* Copyright 2019 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.
*/

package chs.caplets.logic;

import chs.caplets.logic.actions.AddChainAction;
import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IShieldConductor;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author pbhawsar on 02-02-2016
 */
public class DaisyChainCreationHelper
{

	private DaisyChainCreationHelper()
	{
	}

	protected static void displayErrorMessage(Set<IUID> failedObjects)
	{
		final List<IUIDObject> failedNonDeletedObjects = UIDUtils.convertToNonDeletedUIDObjects(failedObjects);
		if (!failedNonDeletedObjects.isEmpty()) {
			ResourceBasedMessageContent content =
					new ResourceBasedMessageContent(AddChainAction.class, "AddChainAction.LockFailures");
			content.setImplicationsParameters(getDisplayableObjects(failedNonDeletedObjects));
			Message.show(PromptSeverity.ERROR, content);
		}
	}

	private static String getDisplayableObjects(Collection<IUIDObject> failedObjects)
	{
		StringBuilder objs = new StringBuilder();

		List<IMulticore> multicores = CollectionUtils.getObjectList(failedObjects, IMulticore.class);
		List<IShieldConductor> shields = CollectionUtils.getObjectList(failedObjects, IShieldConductor.class);
		List<IPinList> pinlists = CollectionUtils.getObjectList(failedObjects, IPinList.class);

		if (!multicores.isEmpty()) {
			return buildLockMessage(objs, multicores, COFTypeEnum.Multicore.toString());
		}

		if (!shields.isEmpty()) {
			return buildLockMessage(objs, shields, COFTypeEnum.Multicore.toString());
		}

		if (!pinlists.isEmpty()) {
			return buildLockMessage(objs, pinlists, COFTypeEnum.PinList.toString());
		}

		List<ILogicObject> logicObjects = CollectionUtils.getObjectList(failedObjects, ILogicObject.class);
		return buildLockMessage(objs, logicObjects, "Objects");
	}

	@NotNull private static <T extends ILogicObject> String buildLockMessage(StringBuilder objs, List<T> uidObjects,
			String str)
	{
		objs.append(str).append(" ");
		if (uidObjects.size() < 2) {
			objs.append(uidObjects.get(0).getName());
		}
		else {
			objs.append(uidObjects.get(0).getName()).append(", ").append(uidObjects.get(1).getName());
		}
		return objs.toString();
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(AddChainAction.class, "AddChainAction.statusbar.text");
	}

}
