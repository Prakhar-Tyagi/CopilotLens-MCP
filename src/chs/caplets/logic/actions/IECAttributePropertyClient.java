package chs.caplets.logic.actions;

import chs.caf.caplet.helpers.SectorHierarchyFinder;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemSector;
import chs.common.IReadOnlyValue;
import chs.common.attr.IAttribute;
import chs.common.attr.IAttributeProvider;
import chs.ctf.editui.SingleAttributeClient;
import chs.utilities.CommonUtils;
import chs.utilities.StringUtils;
import chs.utility.DiagramHelper;
import org.jetbrains.annotations.NotNull;

public class IECAttributePropertyClient extends SingleAttributeClient
{

	@NotNull private IAttributeProvider mAttrProvider;
	private IAttribute holder;
	private boolean designChangesPropagated = false;

	public IECAttributePropertyClient(@NotNull IAttribute attribute, @NotNull ISchemSector attProvider)
	{
		super(attribute);
		mAttrProvider = attProvider;
	}

	public IECAttributePropertyClient(@NotNull IAttribute attribute, @NotNull ILogicDesign attProvider)
	{
		super(attribute);
		holder = attribute;
		mAttrProvider = attProvider;
	}

	public boolean getDesignChangesPropagated()
	{
		return designChangesPropagated;
	}

	@Override protected void applyValue(@NotNull IReadOnlyValue value)
	{
		if (mAttrProvider instanceof ILogicDesign) {
			ILogicDesign design = CommonUtils.cast(mAttrProvider, ILogicDesign.class);
			if (design != null) {
				IAttribute attribute = mAttrProvider.getAttribute(holder.getName());
				if (attribute != null) {
					String oldValue = StringUtils.ensureNotNull(attribute.getString());
					super.applyValue(value);
					String newValue = StringUtils.ensureNotNull(attribute.getString());
					designChangesPropagated = design.propagateAttributeChange(holder.getName(), oldValue, newValue);
				}
			}
		}
		else if (mAttrProvider instanceof ISchemSector) {
			ISchemSector sector = CommonUtils.cast(mAttrProvider, ISchemSector.class);
			if (sector != null) {
				ISchemDiagram diagram = DiagramHelper.getDiagram(sector);
				if (diagram != null) {
					SectorHierarchyFinder finder = new SectorHierarchyFinder(diagram);
					IECAttributeResolver attributeResolver = new IECAttributeResolver(finder, true);
					finder.getObjectsContainedInSector(sector).stream()
							.forEach(gfxObject -> attributeResolver.addGfxObject(gfxObject, false));
					super.applyValue(value);
					ILogicDesign design = diagram.getDesign();
					assert design != null;
					attributeResolver.resolveAttributes(design);
				}
			}
		}
	}
}
