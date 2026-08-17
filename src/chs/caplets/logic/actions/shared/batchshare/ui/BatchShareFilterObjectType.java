/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.ShareableEntityTypeEnum;
import chs.common.IAttributePropertyProvider;
import chs.images.CHSImageLoader;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 *
 */
public enum BatchShareFilterObjectType implements IBatchShareFilterObjectType
{
	DEVICE("Device", "chs/images/javafx_ui/device-small.png",
			ResourceMgr.getString(BatchShareFilterObjectType.class, "BatchShareFilterObjectType.types.device"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.DEVICE)),
	CONNECTOR("Connector", "chs/images/app/ico_connector_active.gif",
			ResourceMgr.getString(BatchShareFilterObjectType.class, "BatchShareFilterObjectType.types.connector"),
			CollectionUtils.asSet(ObjectClass.Logic),
			Set.of(ShareableEntityTypeEnum.PLUG, ShareableEntityTypeEnum.JACK)),
	SINGLE_LINE("SingleLine", "chs/images/javafx_ui/singleline-small.png",
			ResourceMgr.getString(BatchShareFilterObjectType.class, "BatchShareFilterObjectType.types.singleline"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.SINGLE_LINE)),
	HIGHWAY("Highway", "chs/images/javafx_ui/highway-small.png",
			ResourceMgr.getString(BatchShareFilterObjectType.class, "BatchShareFilterObjectType.types.highway"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.HIGHWAY)),
	INLINE("Inline", "chs/images/app/ico_inline_active.gif",
			ResourceMgr.getString(BatchShareFilterObjectType.class, "BatchShareFilterObjectType.types.inline"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.INLINE)),
	MULTICORE("Multicore", "chs/images/javafx_ui/multicore-small.png",
			ResourceMgr.getString(BatchShareFilterObjectType.class, "BatchShareFilterObjectType.types.multicore"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.MULTICORE)),
	NET("Net", "chs/images/javafx_ui/net-small.png",
			ResourceMgr.getString(BatchShareFilterObjectType.class, "BatchShareFilterObjectType.types.net"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.NET)),
	RINGTERMINAL("RingTerminal", "chs/images/javafx_ui/ring-terminal-small.png",
			ResourceMgr.getString(BatchShareFilterObjectType.class, "BatchShareFilterObjectType.types.ringterminal"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.RING_TERMINAL)),
	OVERBRAID("Overbraid", "chs/images/javafx_ui/overbraid-small.png",
			ResourceMgr.getString(BatchShareFilterObjectType.class, "BatchShareFilterObjectType.types.overbraid"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.OVERBRAID)),
	SPLICE("Splice", "chs/images/javafx_ui/splice-small.png",
			ResourceMgr.getString(BatchShareFilterObjectType.class, "BatchShareFilterObjectType.types.splice"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.SPLICE)),
	WIRE("Wire", "chs/images/javafx_ui/wire-small.png",
			ResourceMgr.getString(BatchShareFilterObjectType.class, "BatchShareFilterObjectType.types.wire"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.WIRE));

	private String name;
	private String image;
	private String tooltip;
	private Collection<ObjectClass> objectClasses;
	@NotNull private Set<ShareableEntityTypeEnum> m_representedObjectTypes;

	@NotNull @Override public String getName()
	{
		return name;
	}

	@Override public boolean isObject(IAttributePropertyProvider provider)
	{
		return true;
	}

	@Nullable @Override public Image getImage()
	{
		return getImage(image);
	}

	@Nullable private Image getImage(String path)
	{
		return CHSImageLoader.loadJFXImage(path);
	}

	@NotNull @Override public String toolTipText()
	{
		return tooltip;
	}

	@NotNull @Override public Collection<ObjectClass> getObjectClass()
	{
		return objectClasses;
	}

	@NotNull @Override public Set<ShareableEntityTypeEnum> getRepresentedObjectTypes()
	{
		return m_representedObjectTypes;
	}

	BatchShareFilterObjectType(String name, String image, String tooltip,
			Collection<ObjectClass> objectClasses, @NotNull Set<ShareableEntityTypeEnum> representedObjectTypes)
	{
		this.name = name;
		this.image = image;
		this.tooltip = tooltip;
		this.objectClasses = objectClasses;
		m_representedObjectTypes = representedObjectTypes;
	}

	/**
	 * Returns filter object types applicable for unshare redundant objects.
	 *
	 * @return array of filter types valid for unshare
	 */
	@NotNull public static IBatchShareFilterObjectType[] getUnshareRedundantTypes()
	{
		return new IBatchShareFilterObjectType[]{DEVICE, CONNECTOR, INLINE, NET, RINGTERMINAL, SPLICE, WIRE, MULTICORE,
				OVERBRAID};
	}
}
