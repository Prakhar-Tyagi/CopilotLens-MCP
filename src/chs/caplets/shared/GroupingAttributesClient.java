/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2018-2023 Siemens
 */

package chs.caplets.shared;

import chs.caf.action.dragdrop.DragActionInvocationTransferHandler;
import chs.caf.action.dragdrop.IDragDropActionInvocationControl;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.browser.IBrowserTreeNode;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldBody;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utilities.CommonUtils;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.TransferHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupingAttributesClient extends LogicDesignBrowserClient
{

	private GroupAttributeConfigurator configurator;
	private IDesign design;

	public GroupingAttributesClient(ICapletController controller, IDesign design,
			GroupAttributeConfigurator configurator)
	{
		super(controller);
		this.configurator = configurator;
		this.design = design;
	}

	@Nullable @Override public String getToolTipText(IUID uid, @Nullable IUID parentUID)
	{
		return (configurator != null ? configurator.getTooltipText(uid) : null);
	}

	@Nullable public TransferHandler createTransferTreeHandler(
			@NotNull IDragDropActionInvocationControl actionInocationControl)
	{
		return new DragActionInvocationTransferHandler();
	}

	protected boolean isDimmedTreeNode(IBrowserTreeNode node)
	{
		ILogicObject logicObject = CommonUtils.cast(node.getUIDObject(), ILogicObject.class);
		if (logicObject != null && logicObject.getLogicDesign() instanceof ILayoutLogicDesign &&
				!(logicObject instanceof IMulticore) && !(logicObject instanceof IShieldBody) &&
				!LogicUtils.hasUsage(logicObject)) {
			return true;
		}
		return super.isDimmedTreeNode(node);
	}

	@Override public IUID getRoot()
	{
		return configurator.getUID();
	}

	@Override protected IUIDObject getRootObject()
	{
		return configurator;
	}

	@Override public boolean hasChildren(IUID uid, IUID parentUID)
	{
		return hasChildren(uid);
	}

	private boolean hasChildren(IUID uid)
	{
		if (uid.isEquiv(configurator.getUID())) {
			return !configurator.getAttributeConfiguratorNodes().isEmpty();
		}
		return !configurator.getChildren(uid).isEmpty();
	}

	@Override public List<IUID> getChildren(IUID uid)
	{
		if (uid.isEquiv(configurator.getUID())) {
			List<IUID> iuids = new ArrayList<>();

			for (GroupAttributeConfigurator.IGroupAttributeConfiguratorAttrNameNode pathNode : configurator
					.getAttributeConfiguratorNodes()) {
				iuids.addAll(configurator.getChildrenOfRoot(pathNode, design));
			}

			return iuids;
		}
		else {
			List<IUID> childrenUIDs = configurator.getChildren(uid);
			Collections.sort(childrenUIDs, new BrowserTreeNodeComparator());
			return childrenUIDs;
		}
	}

	@Override public void destroy()
	{
		super.destroy();
		configurator.destroy();
		design = null;
	}
}
