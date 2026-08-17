package chs.caplets.logic.actions.serviceDocumentation.offPage.messages;

import chs.caf.helpers.ui.common.statusmessage.PublisherStatusMessageTableModel;
import chs.utilities.ResourceMgr;
import chs.utility.STATUS_MESSAGE_COLUMN_TYPE;
import chs.utility.ui.ITableProvider;
import org.jetbrains.annotations.NotNull;

public class FetchActionStatusMessageTableModel extends PublisherStatusMessageTableModel
{

	public FetchActionStatusMessageTableModel(@NotNull ITableProvider aTable)
	{
		super(aTable);
	}

	@Override protected void addAllColumns()
	{
		addColumn(0, STATUS_MESSAGE_COLUMN_TYPE.STATUS);
		addColumn(1, STATUS_MESSAGE_COLUMN_TYPE.MESSAGE);
		addColumn(2, STATUS_MESSAGE_COLUMN_TYPE.DESIGNBUILDLIST);
		addColumn(3, STATUS_MESSAGE_COLUMN_TYPE.OBJECT);
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		if (getColumnTypeAt(columnIndex) == STATUS_MESSAGE_COLUMN_TYPE.DESIGNBUILDLIST) {
			return ResourceMgr
					.getString(FetchActionStatusMessageTableModel.class, "FetchActionStatusMessageTableModel.scope");
		}
		return super.getColumnName(columnIndex);
	}
}
