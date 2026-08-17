package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.caplets.logic.actions.shared.helper.IShareMessageContextReporter;
import chs.caplets.logic.actions.shared.helper.ModularConnectorHandler;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.utilities.CHSConstants;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class AutoModularConnectorView
{

	@NotNull private final ModularConnectorHandler mHandler;
	@NotNull private Collection<ModularConnectorHandler.IConnectorNode> createdConnectorNodes = new ArrayList<>();

	public AutoModularConnectorView(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design,
			@NotNull IConnector connector, @NotNull IShareMessageContextReporter reporter, boolean isBulkShare)
	{
		mHandler = new ModularConnectorHandler(model, design, connector, () -> createdConnectorNodes, reporter,
				isBulkShare);
	}

	public void setupModularHierarchy()
	{
		init1ModularHierarchy();
		mHandler.evaluateModularHierarchyValidity(this::getMessage);
	}

	public void init1ModularHierarchy()
	{
		final ModularConnectorHandler.IConnectorNode rootNode = mHandler.createRootNode();
		createdConnectorNodes.add(rootNode);
		populateModularHierarchy(mHandler.getRootConnector());
	}

	private void populateModularHierarchy(@NotNull IConnector connector)
	{
		for (ModularConnectorHandler.IConnectorNode rNode : mHandler.createChildrenNodes(connector)) {
			mHandler.onConnectorNodeAdd(rNode);
			createdConnectorNodes.add(rNode);
			populateModularHierarchy(rNode.getConnector());
		}
	}

	@Nullable private String getMessage(@NotNull ModularConnectorHandler.ModularConnectorShareErrors shareMessageEnum)
	{
		switch (shareMessageEnum) {
			case DuplicateNameInModularHierarchy:
				return ResourceMgr.getString(AutoModularConnectorView.class,
						"AutoModularConnectorView.DuplicateNameInModularHierarchy.text");
			case ModularHierarchySharedNameConflict:
				return ResourceMgr.getString(AutoModularConnectorView.class,
						"AutoModularConnectorView.ModularHierarchySharedNameConflict.text");
			case ModularConnectorInvalidName:
				return ResourceMgr.getString(AutoModularConnectorView.class,
						"AutoModularConnectorView.InvalidName.text");
			case ModularConnectorNameTooLong:
				return ResourceMgr.getString(AutoModularConnectorView.class,
						"AutoModularConnectorView.nametoolong.text",
						String.valueOf(CHSConstants.MAX_NAME_LENGTH));
		}
		return null;
	}
}
