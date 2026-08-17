package chs.caplets.logic.helpers.tabulareditor;

import chs.caf.caplet.helpers.PropertiedSetHelper;
import chs.cof.logical.cable.ILogicObject;
import chs.common.IAttributePropertyProvider;
import chs.common.IProperty;
import chs.utilities.CommonUtils;
import chs.utilities.ui.tabulareditor.PropertyColumnProvider;
import chs.utility.helpers.PropertyHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class LogicPropertyColumnProvider extends PropertyColumnProvider
{
	@Override protected void removeProperty(@Nullable IProperty property, IAttributePropertyProvider propertiedObject)
	{
		if (property != null) {
			ILogicObject logicObject = CommonUtils.cast(propertiedObject, ILogicObject.class);
			if (logicObject != null && logicObject.isShared()) {
				PropertiedSetHelper.removeProperty(logicObject, property);
				PropertyHelper.deletePersistedProperties(logicObject.getSharedObject(),
						Collections.singletonList(property));
			}
		}
		super.removeProperty(property, propertiedObject);
	}
}
