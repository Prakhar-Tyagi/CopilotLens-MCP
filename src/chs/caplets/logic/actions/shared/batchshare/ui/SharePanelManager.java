/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.ctf.caf.ui.SimpleOkCancelDialog;
import com.mentor.capital.javafx.table.helpers.IgnoreEscapeKeyListener;
import com.mentor.lookandfeel.JavaFXLib.JFXFlatUIUtils;
import com.sun.javafx.application.PlatformImpl;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manager for creating and configuring JavaFX panels for share/unshare UI operations.
 * <p>
 * It provides a centralized approach to managing JavaFX scenes with consistent styling and keyboard event handling.
 */
public class SharePanelManager
{

	private final JFXPanel jfxPanel;
	private final SimpleOkCancelDialog dialog;

	public SharePanelManager(@NotNull SimpleOkCancelDialog dialog, @NotNull String panelName)
	{
		this.dialog = dialog;
		jfxPanel = new JFXPanel();
		jfxPanel.setName(panelName);
		jfxPanel.addKeyListener(new IgnoreEscapeKeyListener());
	}

	public void initializeScene(@NotNull Supplier<Pane> rootPaneSupplier, @NotNull Consumer<Scene> stylesheetApplier)
	{
		PlatformImpl.runAndWait(() -> {
			Pane rootPane = rootPaneSupplier.get();
			Scene scene = new Scene(rootPane);
			JFXFlatUIUtils.getInstance().setFlatUIFor(scene);

			scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
				if (KeyCode.ESCAPE.equals(e.getCode())) {
					dialog.getCancelButton().doClick();
				}
			});

			stylesheetApplier.accept(scene);
			jfxPanel.setScene(scene);
		});
	}

	@NotNull public JFXPanel getPanel()
	{
		return jfxPanel;
	}
}
