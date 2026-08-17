package chs.caplets.logic.actions.shared.helper;

import chs.caplets.logic.shared.AbstractLockedSharedObjectFilter;
import chs.cof.project.naming.INameMgr;
import chs.common.IReadOnlyNamedObject;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utilities.ui.property.IPropertyValidator;
import chs.utilities.ui.property.IStringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ISelectSharedAdapter
{

	@NotNull IStringProperty createNameProperty(boolean isInlineJack);

	@NotNull IBooleanProperty createGeneratedProperty(boolean defaultGeneratedValue, boolean isEnabled);

	@NotNull IStringProperty createMateNameProperty();

	@NotNull IBooleanProperty createMateGeneratedProperty(boolean defaultMateGeneratedvalue, boolean isEnabled);

	@NotNull IStringProperty createRevisionProperty();

	@NotNull AbstractLockedSharedObjectFilter getLockedSharedObjectFilter(boolean isBulkShare);

	@NotNull IPropertyValidator createNamePropertyValidator(@Nullable IReadOnlyNamedObject namedObject,
			@Nullable INameMgr nameMgr);

	boolean shouldReportNameValidation();
}
