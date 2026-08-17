package chs.caplets.logic.actions.ui;

/**
 * @author chandras on 25-03-2018.
 */
public enum ValueOption
{
	Source {
		public ValueOption toggle()
		{
			return Target;
		}
	},

	Target {
		public ValueOption toggle()
		{
			return Source;
		}
	},

	User;

	public ValueOption toggle()
	{
		return User;
	}
}
