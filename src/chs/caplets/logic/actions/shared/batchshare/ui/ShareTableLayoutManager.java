/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import com.mentor.capital.javafx.standardComponents.StandardSplitPane;
import com.mentor.capital.javafx.table.Table;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for constructing and configuring the layout of share/unshare table dialogs.
 * <p>
 * This class is responsible for creating the JavaFX UI layout structure used in batch share
 * and unshare operations.
 */
public class ShareTableLayoutManager
{

	private ShareTableLayoutManager() {
		// Private constructor to prevent instantiation of this utility class
	}

	@NotNull
	public static VBox buildLayout(@NotNull Table<?> table, @NotNull Pane controlPane, @NotNull TitledPane detailsPane,
			@NotNull Label selectionLabel)
	{
		VBox rootVBox = new VBox();
		StandardSplitPane splitPane = new StandardSplitPane();
		splitPane.setOrientation(Orientation.VERTICAL);

		HBox labelBox = new HBox();
		labelBox.setPadding(new Insets(6));
		labelBox.getChildren().add(selectionLabel);

		rootVBox.getChildren().add(controlPane);
		rootVBox.getChildren().add(splitPane);

		table.prefWidthProperty().bind(rootVBox.widthProperty());
		table.prefHeightProperty().bind(rootVBox.heightProperty());

		VBox mainTableVBox = new VBox(table);
		final int mainTableMinHeight = 120;
		mainTableVBox.setMinHeight(mainTableMinHeight);
		mainTableVBox.getChildren().add(labelBox);
		splitPane.getItems().add(mainTableVBox);
		splitPane.getItems().add(detailsPane);

		return rootVBox;
	}
}
