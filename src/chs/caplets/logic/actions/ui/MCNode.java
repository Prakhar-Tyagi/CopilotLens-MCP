package chs.caplets.logic.actions.ui;

import chs.cof.logical.IAbstractMulticore;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.shared.ISharedConductor;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MCNode extends AbstractMCProxyTree<ILogicObject>
{

	@Nullable private MCSharedNode m_sharedProxy;

	public MCNode(ILogicObject ref)
	{
		this(null, ref);
	}

	public MCNode(@Nullable String name, @Nullable ILogicObject ref)
	{
		super(name, ref);
	}

	@Nullable public String getSheathType()
	{
		if (m_ref instanceof IMulticore) {
			return ((IAbstractMulticore) m_ref).getSheathType();
		}
		return null;
	}

	@NotNull public String getConductorRole()
	{
		if (m_ref instanceof IShieldConductor) {
			return ISharedConductor.SHIELD_TYPE;
		}
		if (m_ref instanceof IWireConductor) {
			return ISharedConductor.WIRE_TYPE;
		}
		if (m_ref instanceof INetConductor) {
			return ISharedConductor.NET_TYPE;
		}
		return "";
	}

	protected String getDefaultName(String name, ILogicObject ref)
	{
		// Get the appropriate name based on the ref object
		if (ref != null) {
			return ref.getName();
		}
		return super.getDefaultName(name, null);
	}

	@Nullable public MCNode getParentMcNode()
	{
		return (MCNode) getParent();
	}

	public List<String> getAttributes()
	{
		List<String> attributeValues = new ArrayList<>();
		if (!hasRef()) {
			return attributeValues;
		}
		IUIDObject aRefObj = getRef();
		if (aRefObj instanceof IConductor) {
			IConductor conductor = (IConductor) aRefObj;
			String materialCode = conductor.getMaterialCode();
			if (materialCode != null && !materialCode.trim().isEmpty()) {
				attributeValues.add(materialCode);
			}
			String wireSpecification = conductor.getWireSpecification();
			if (wireSpecification != null && !wireSpecification.trim().isEmpty()) {
				attributeValues.add(wireSpecification);
			}
			String wireColor = conductor.getWireColor();
			if (wireColor != null && !wireColor.trim().isEmpty()) {
				attributeValues.add(wireColor);
			}
		}
		return attributeValues;
	}

	@Override public boolean isAssigned()
	{
		return hasSharedRef();
	}

	@Override public boolean isOverbraid()
	{
		return getRef() instanceof IOverbraid;
	}

	public int getUnassignedChildCount()
	{
		int count = 0;
		for (Enumeration<?> enumerator = children(); enumerator.hasMoreElements(); ) {
			MCNode node = (MCNode) enumerator.nextElement();
			if (!node.isAssigned()) {
				count++;
			}
		}
		return count;
	}

	public Enumeration<MCNode> unassignedChildren()
	{
		Collection<MCNode> unassignedVector = new ArrayList<>();
		for (Enumeration<?> enumerator = children(); enumerator.hasMoreElements(); ) {
			MCNode node = (MCNode) enumerator.nextElement();
			if (!node.isAssigned()) {
				unassignedVector.add(node);
			}
		}
		return (Collections.enumeration(unassignedVector));
	}

	public void setSharedProxy(MCSharedNode sharedProxy)
	{
		m_sharedProxy = sharedProxy;
		m_sharedProxy.setMCNodeProxy(this);
	}

	public void removeAssociation()
	{
		if (m_sharedProxy != null) {
			m_sharedProxy.setMCNodeProxy(null);
			m_sharedProxy = null;
		}
	}

	public void removeAssociationAllChildren()
	{
		for (Enumeration<TreeNode> enumerator = children();
				enumerator.hasMoreElements(); ) {
			MCNode mcNode = (MCNode) enumerator.nextElement();
			mcNode.removeAssociationAllChildren();
		}
		if (m_sharedProxy != null) {
			m_sharedProxy.setMCNodeProxy(null);
			m_sharedProxy = null;
		}
	}

	public boolean hasSharedRef()
	{
		return (m_sharedProxy != null);
	}

	@Nullable public MCSharedNode getSharedProxy()
	{
		return m_sharedProxy;
	}
}