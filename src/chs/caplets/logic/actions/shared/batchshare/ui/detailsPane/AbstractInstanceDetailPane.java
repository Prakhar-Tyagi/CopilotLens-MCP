/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.cof.logical.cable.ILogicObject;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.ITableSelectionListener;
import com.mentor.capital.javafx.table.Selection;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.ImageView;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract instance detail pane
 */
public abstract class AbstractInstanceDetailPane extends BaseDetailPane
{

	protected static final String PANE_TITLE =
			ResourceMgr.getString(AbstractInstanceDetailPane.class, "InstanceDetailPane.tableTitle.text");
	protected static final String TABLE_ID = "instance_details_table";
	protected Button zoomButton;

	protected AbstractInstanceDetailPane(@Nullable ILogicObject selectedObject)
	{
		super(PANE_TITLE, TABLE_ID);
		setId("InstanceDetailPane");
		addZoomButton();
		updateContent(selectedObject);
	}

	protected void addZoomButton()
	{
		initZoomButton();

		//Make zoom button visible when Titled Pane expanded
		expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> zoomButton.setVisible(isNowExpanded));
		zoomButton.setOnAction(e -> executeZoomAction());
		zoomButton.setDisable(true);
	}

	private void addTableListenerEnableZoomButton()
	{
		table.addSelectionListener(new ITableSelectionListener<DetailsTableInfo>()
		{
			@Override
			public void changed(Selection<DetailsTableInfo> selection, int lastSelectionIndex)
			{
				selection.getSelectedCells();
				if (selection.getSelectedCells().iterator().hasNext()) {
					zoomButton.setDisable(false);
				}
				else {
					zoomButton.setDisable(true);
				}
			}
		});
	}

	private void initZoomButton()
	{
		zoomButton = new Button();
		zoomButton.setId("ZoomToInstanceButton");
		//set button graphics
		ImageView image = new ImageView(CHSImageLoader.loadJFXImage("chs/images/javafx_ui/zoom-selection-small.png"));
		zoomButton.setGraphic(image);
		setGraphic(zoomButton);

		//add zoom button on right end of the Titled pane header
		setContentDisplay(ContentDisplay.RIGHT);
		final double graphicMarginRight = 110;
		zoomButton.translateXProperty().bind(Bindings.createDoubleBinding(
				() -> getWidth() - getLayoutX() - zoomButton.getWidth() - graphicMarginRight,
				widthProperty())
		);
	}

	private void executeZoomAction()
	{
		InstanceDetailsTableInfo tableInfo = getSelectedTableRow();
		if (tableInfo != null) {
			tableInfo.zoomToInstance();
		}
	}

	@Nullable protected InstanceDetailsTableInfo getSelectedTableRow()
	{
		Selection<DetailsTableInfo> selection = table.getSelection();

		List<DetailsTableInfo> selectedInfos = selection.getSelectedCells().stream()
				.map(cellSelection -> cellSelection.getSelectedItem())
				.collect(Collectors.toList());

		if (selectedInfos.isEmpty()) {
			return null;
		}

		return (InstanceDetailsTableInfo) selectedInfos.iterator().next();
	}

	public void updateContent(@Nullable ILogicObject selectedObject)
	{
		super.updateContent(selectedObject);
		zoomButton.setDisable(true);
		addTableListenerEnableZoomButton();
	}
}
