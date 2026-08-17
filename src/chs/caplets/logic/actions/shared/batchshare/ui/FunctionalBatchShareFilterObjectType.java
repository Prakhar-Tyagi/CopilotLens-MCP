/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2024 Siemens
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
 * Functional object type filter
 */
public enum FunctionalBatchShareFilterObjectType implements IBatchShareFilterObjectType
{

	SIGNAL("Signal", "chs/images/javafx_ui/functional-signal-small.png",
			ResourceMgr.getString(FunctionalBatchShareFilterObjectType.class,
					"BatchShareFilterObjectType.types.signal"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.FUNCTION_SIGNAL)),

	MESSAGE("Message", "chs/images/javafx_ui/message-small.png",
			ResourceMgr.getString(FunctionalBatchShareFilterObjectType.class,
					"BatchShareFilterObjectType.types.message"),
			CollectionUtils.asSet(ObjectClass.Logic), Set.of(ShareableEntityTypeEnum.FUNCTION_MESSAGE));

	@NotNull private String name;
	@NotNull private String image;
	@NotNull private String tooltip;
	@NotNull private Collection<ObjectClass> objectClasses;
	@NotNull private Set<ShareableEntityTypeEnum> m_representedObjectTypes;

	FunctionalBatchShareFilterObjectType(@NotNull String name, @NotNull String image, @NotNull String tooltip,
			@NotNull Collection<ObjectClass> objectClasses,
			@NotNull Set<ShareableEntityTypeEnum> representedObjectTypes)
	{
		this.name = name;
		this.image = image;
		this.tooltip = tooltip;
		this.objectClasses = objectClasses;
		m_representedObjectTypes = representedObjectTypes;
	}

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

	@Nullable private Image getImage(@NotNull String path)
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
}