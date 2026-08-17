/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.bridges.adaptors.tcmbse.ISysMLProjectNode;
import chs.bridges.adaptors.tcmbse.generated.ConnectivityJson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Stores SysML node related information
 */
public class SysMLProjectNode implements ISysMLProjectNode
{

	@NotNull private final String projectId;
	@Nullable private String json;
	@NotNull private Set<ISysMLProjectNode> child;
	@Nullable private ConnectivityJson connectivityJson;

	public SysMLProjectNode(@NotNull String projectId)
	{
		this.projectId = projectId;
		child = new LinkedHashSet<>();
	}

	@Override public void addChild(@NotNull ISysMLProjectNode childNode)
	{
		child.add(childNode);
	}

	@NotNull public Set<ISysMLProjectNode> getChildren()
	{
		return child;
	}

	@NotNull public String getProjectId()
	{
		return projectId;
	}

	@Nullable @Override public ConnectivityJson getConnectivityJson()
	{
		return connectivityJson;
	}

	@Override public void setConnectivityJson(@NotNull ConnectivityJson connectivityJson)
	{
		this.connectivityJson = connectivityJson;
	}

	@Nullable public String getJson()
	{
		return json;
	}

	public void setJson(@NotNull String json)
	{
		this.json = json;
	}

	@Override public boolean equals(Object obj)
	{
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SysMLProjectNode otherObj = (SysMLProjectNode) obj;
		return Objects.equals(projectId, otherObj.projectId);
	}

	@Override public int hashCode()
	{
		return Objects.hashCode(projectId);
	}
}
