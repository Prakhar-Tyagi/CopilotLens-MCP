/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */
package chs.caplets.shared;

import chs.ans.ContainerType;
import chs.caf.IFIB;
import chs.caf.IResource;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.helpers.CapletHelper;
import chs.caf.caplet.helpers.quickedit.QuickEditAttrInfo;
import chs.caf.caplet.helpers.quickedit.ui.basic.ComboBoxUI;
import chs.caf.caplet.helpers.quickedit.ui.basic.EquipotentialComboBoxUI;
import chs.caf.caplet.helpers.quickedit.ui.basic.GlobalEquipotentialTextBoxUI;
import chs.caf.caplet.helpers.quickedit.ui.helpers.EquipotentialRelationshipPropagator;
import chs.caplets.logic.ILogicCaplet;
import chs.cof.project.IProject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.common.IRealm;
import chs.common.attr.IAttributeTypes;
import chs.ctf.drc.IDRCDomainManager;
import chs.utilities.ResourceMgr;
import chs.utilities.suite.DesignType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public abstract class BaseCaplet extends CapletHelper implements ILogicCaplet
{

	private IFIB m_fib = null;
	private ICapletLifecycle m_lifeCycle = null;
	private IResource m_resource = null;
	private Collection<IDRCDomainManager> m_drcDomainManagers = null;

	protected BaseCaplet(DataTransfer dt)
	{
		super(dt);
	}

	/**
	 * @return name of the designs created by this caplet
	 */
	public String getDesignType()
	{
		return ResourceMgr.getString(BaseCaplet.class, "Caplet.Design.Type");
	}

	@Override public Collection<ContainerType> getOpenableContainerTypes()
	{
		return Collections.singleton(ContainerType.LOGICAL);
	}

	@NotNull
	@Override
	public Collection<DesignType> getOpenAbleDesignTypes() {
		return Collections.singleton(DesignType.LOGICAL);
	}

	/**
	 * @return the functional permissions used for editing
	 */
	public FunctionalPermissionEnum getEditFunctionalPermissionEnum()
	{
		return FunctionalPermissionEnum.EditLogicDesigns;
	}

	@NotNull public IFIB getFIB()
	{
		return m_fib;
	}

	public ICapletLifecycle getLifecycle()
	{
		return m_lifeCycle;
	}

	public IResource getResource()
	{
		return m_resource;
	}

	public void initializeCaplet(@NotNull IFIB fib)
	{
		// Remember the FIB so we can communicate with CAF
		m_fib = fib;

		// Create Interface Implementations
		m_lifeCycle = createLifecycle();
		m_resource = createResource();
		initializeQuickEditAttributes();
	}

	private void initializeQuickEditAttributes()
	{
		instance.registerAttribute(new QuickEditAttrInfo(IAttributeTypes.INDICATOR_TYPE));
		EquipotentialRelationshipPropagator equipotentialRelationshipPropagator =
				new EquipotentialRelationshipPropagator();
		instance.registerAttribute(
				new QuickEditAttrInfo(IAttributeTypes.EQUIPOTENTIAL,
						quickEditFacet -> new EquipotentialComboBoxUI(quickEditFacet,
								equipotentialRelationshipPropagator)));
		instance.registerAttribute(new QuickEditAttrInfo(IAttributeTypes.GLOBALEQUIPOTENTIAL,
				QuickEditFacet -> new GlobalEquipotentialTextBoxUI(QuickEditFacet,
						equipotentialRelationshipPropagator)));
		instance.registerAttribute(new QuickEditAttrInfo(IAttributeTypes.CONTACT_MATERIAL,
				quickEditFacet -> new ComboBoxUI(quickEditFacet)));
		instance.registerAttribute(new QuickEditAttrInfo(IAttributeTypes.DEVICE_CONNECTOR_PIN));
		instance.registerAttribute(
				new QuickEditAttrInfo(IAttributeTypes.PIN_TYPE, quickEditFacet -> new ComboBoxUI(quickEditFacet)));
	}

	/**
	 * Returns the class used for resource string lookups.
	 */
	protected abstract Class<? extends ICaplet> getResourceClass();

	protected abstract ICapletLifecycle createLifecycle();

	protected abstract IResource createResource();

	@NotNull public Class<? extends IRealm> getRealm()
	{
		return IProject.class;
	}

	protected abstract void addDRCDomainManagers(Collection<IDRCDomainManager> drcs);

	public Collection<IDRCDomainManager> getDRCDomainManagers()
	{
		if (m_drcDomainManagers == null) {
			m_drcDomainManagers = new ArrayList<IDRCDomainManager>();
			addDRCDomainManagers(m_drcDomainManagers);
		}
		return m_drcDomainManagers;
	}

	public boolean isAnalysisSupported()
	{
		return true;
	}

	public boolean isBridgesSupported()
	{
		return true;
	}

	/**
	 * @return does this caplet edit schematics?
	 */
	public boolean isSchematicEditor()
	{
		return true;
	}

	@Override public int getToolFlowOrder()
	{
		return 0;
	}
}
