package chs.caplets.logic.actions.ui;

import chs.cof.logical.IAbstractMulticore;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedOverbraid;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MCSharedNode extends AbstractMCProxyTree<ISharedObject>
{

	@Nullable private MCNode m_mcNodeProxy;

	public MCSharedNode(ISharedObject ref)
	{
		this(null, ref);
	}

	public MCSharedNode(@Nullable String name, @Nullable ISharedObject ref)
	{
		super(name, ref);
	}

	@Nullable public String getSheathType()
	{
		if (m_ref instanceof ISharedMulticore) {
			return ((IAbstractMulticore) m_ref).getSheathType();
		}
		return null;
	}

	@NotNull public String getConductorRole()
	{
		if (m_ref instanceof ISharedConductor) {
			return ((ISharedConductor) m_ref).getType();
		}
		return "";
	}

	protected String getDefaultName(String name, ISharedObject ref)
	{
		// Get the appropriate name based on the ref object
		if (ref != null) {
			return ref.getName();
		}
		return super.getDefaultName(name, null);
	}

	@Nullable public MCSharedNode getParentMcSharedNode()
	{
		return (MCSharedNode) getParent();
	}

	public List<String> getAttributes()
	{

		List<String> attributeValues = new ArrayList<>();
		if (!hasRef()) {
			return attributeValues;
		}
		IUIDObject aRefObj = getRef();
		if (aRefObj instanceof ISharedConductor) {
			ISharedConductor sharedConductor = (ISharedConductor) aRefObj;
			String materialCode = sharedConductor.getMaterialCode();
			if (materialCode != null && !materialCode.trim().isEmpty()) {
				attributeValues.add(materialCode);
			}
			String wireSpecification = sharedConductor.getWireSpecification();
			if (wireSpecification != null &&
					!wireSpecification.trim().isEmpty()) {
				attributeValues.add(wireSpecification);
			}
			String wireColor = sharedConductor.getWireColor();
			if (wireColor != null && !wireColor.trim().isEmpty()) {
				attributeValues.add(wireColor);
			}
		}
		return attributeValues;
	}

	public void setMCNodeProxy(@Nullable MCNode mcNodeProxy)
	{
		m_mcNodeProxy = mcNodeProxy;
	}

	public boolean hasMCProxy()
	{
		return (m_mcNodeProxy != null);
	}

	@Nullable public MCNode getMCProxy()
	{
		return m_mcNodeProxy;
	}

	@Override public boolean isAssigned()
	{
		return hasMCProxy();
	}

	@Override public boolean isOverbraid()
	{
		return getRef() instanceof ISharedOverbraid;
	}

	public int getUnassignedChildCount()
	{
		Enumeration<?> sharedNodeEnumeration = children();
		int count = 0;
		if (sharedNodeEnumeration != null) {
			while (sharedNodeEnumeration.hasMoreElements()) {
				MCSharedNode shareNode = (MCSharedNode) sharedNodeEnumeration.nextElement();
				if (!shareNode.isAssigned()) {
					count++;
				}
			}
		}
		return count;
	}

	public Enumeration<MCSharedNode> unassignedChildren()
	{
		Collection<MCSharedNode> unassignedVector = new ArrayList<>();
		for (Enumeration<?> enumerator = children(); enumerator.hasMoreElements(); ) {
			MCSharedNode node = (MCSharedNode) enumerator.nextElement();
			if (!node.isAssigned()) {
				unassignedVector.add(node);
			}
		}
		return (Collections.enumeration(unassignedVector));
	}
}