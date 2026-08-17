package chs.caplets.logic.actions.ui;

/**
 * @author chandras on 01-04-2018.
 */
public enum FacetStature implements IFacetStature
{
	READ_ONLY_ATTR {
		@Override public boolean isEditable()
		{
			return false;
		}
	},
	READ_ONLY_PROP {
		@Override public boolean isEditable()
		{
			return false;
		}

		@Override public boolean isAttribute()
		{
			return false;
		}
	},
	EDITABLE_ATTR,
	EDITABLE_PROP {
		@Override public boolean isAttribute()
		{
			return false;
		}
	};

	@Override public boolean isEditable()
	{
		return true;
	}

	@Override public boolean isAttribute()
	{
		return true;
	}
}
