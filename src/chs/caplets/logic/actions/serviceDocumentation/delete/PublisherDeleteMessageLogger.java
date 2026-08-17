package chs.caplets.logic.actions.serviceDocumentation.delete;

import chs.caf.CAFUtils;
import chs.caplets.logic.actions.serviceDocumentation.PublisherDeleteAction;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.cable.ILogicObject;
import chs.common.IDesignContainer;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.LogHelper;
import chs.utility.helpers.LogTabType;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

class PublisherDeleteMessageLogger
{

	void logMessages(SelectionObjects selectionObjects)
	{
		LogHelper.clearTab(LogTabType.DELETE_PUB);
		selectionObjects
				.getLastInstances()
				.forEach(o -> {
					logMsg(o, "PublisherDeleteAction.cannotDelete.lastInstance");
				});
		selectionObjects
				.getNotFetchedObjects()
				.forEach(o -> {
					logMsg(o, "PublisherDeleteAction.cannotDelete.notFetched");
				});
		selectionObjects
				.getObjectsWhoseDeleteLeadsToConnectivityChange()
				.forEach(o -> {
					logMsg(o, "PublisherDeleteAction.cannotDelete.connChange");
				});
		selectionObjects
				.getNotDeletablesDueToOtherReasons()
				.forEach(o -> {
					logMsg(o, "PublisherDeleteAction.cannotDelete.otherReasons");
				});
	}

	private void logMsg(@NotNull IDiagramObject o, String key)
	{
		IDesignContainer activeDesignContainer = CAFUtils.getInstance().getActiveDesignContainer();
		Collection<ILogicObject> connectivities = new ConnectivityObjectProvider().getConnectivity(o);
		String name = getName(connectivities);
		String link = HTMLHelper.linkCheckParams(activeDesignContainer, o, name);
		String message = ResourceMgr.getString(PublisherDeleteAction.class, key, link);
		LogHelper.printMsg(LogTabType.DELETE_PUB, message);
	}

	@NotNull static String getName(Collection<ILogicObject> connectivities){
		return connectivities
				.stream()
				.map(ILogicObject::getName)
				.collect(Collectors.joining(","));
	}
}
