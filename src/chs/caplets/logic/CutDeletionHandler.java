/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic;

import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUIDObject;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CollectionUtils;
import chs.utilities.StringUtils;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class to assist delete during cut
 */
public class CutDeletionHandler
{

	private Collection<IUIDObject> deletionObjects;
	private ISchemDiagram sourceDiagram;

	public CutDeletionHandler(Collection<IUIDObject> deletionObjects, ISchemDiagram sourceDiagram)
	{
		this.deletionObjects = deletionObjects;
		this.sourceDiagram = sourceDiagram;
	}

	public boolean delete()
	{
		DeleteContext deleteContext = DeleteHelper.getInstance().delete(sourceDiagram, deletionObjects, false);
		Set<String> failedObjects = deleteContext.getErrorCollector().getFailedObjects();

		if (failedObjects.isEmpty()) {
			return true;
		}

		return notifyUser(failedObjects);
	}

	protected boolean notifyUser(Set<String> failedObjects)
	{
		Set<String> sortedFailedObjects =
				CollectionUtils.createAndSortSet(failedObjects.iterator(), new AlphaNumComparator<String>());

		String failedObjsForDisplay = CutDeletionUtils.getStringForDisplay(sortedFailedObjects);

		showErrorDialog(failedObjsForDisplay);

		return false;
	}

	protected void showErrorDialog(String objs)
	{
		ResourceBasedMessageContent content = getContent(objs);
		Message.show(PromptSeverity.ERROR, content);
	}

	@NotNull protected ResourceBasedMessageContent getContent(String objs)
	{
		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(CutDeletionHandler.class, "CutDeletionHandler.cutFailureError");
		content.setMessageParameters(objs);
		return content;
	}

	protected static class CutDeletionUtils
	{

		private static final int MAX_NUM_OF_OBJS = 3;
		private static final int MAX_LENGTH_OF_NAMES = 30;
		private static final String ELLIPSIS = "...";

		private CutDeletionUtils()
		{
		}

		@NotNull public static String getStringForDisplay(Collection<String> names)
		{
			StringBuilder builder = new StringBuilder();
			List<String> limitedNames = names.stream().filter(str -> !StringUtils.isEmpty(str)).limit(MAX_NUM_OF_OBJS)
					.collect(Collectors.toList());
			String nameString = StringUtils.convertCollectionToString(limitedNames, ",");

			StringBuilder nameStr = new StringBuilder();
			if (nameString.length() > MAX_LENGTH_OF_NAMES) {
				nameStr.append(nameString, 0, MAX_LENGTH_OF_NAMES).append(ELLIPSIS);
			}
			else {
				nameStr.append(nameString);
			}

			return builder.append("(").append(nameStr).append(")").toString();
		}
	}
}
