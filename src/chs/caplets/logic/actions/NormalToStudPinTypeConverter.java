package chs.caplets.logic.actions;


/**
 * Created with IntelliJ IDEA. User: nagamani Date: 14/2/14
 */
public class NormalToStudPinTypeConverter extends AbstractPinTypeConverter
{
	@Override protected boolean isStudAfterConversion()
	{
		return true;
	}
}
