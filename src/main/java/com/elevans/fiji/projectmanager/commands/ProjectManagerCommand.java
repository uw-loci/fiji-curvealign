package com.elevans.fiji.projectmanager.commands;

import com.elevans.fiji.projectmanager.services.ProjectManagerService;
import com.elevans.fiji.projectmanager.ui.ProjectManagerDialog;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import javax.swing.SwingUtilities;

/**
 * Main Fiji plugin command that opens the Project Manager dialog.
 * Registered in the Fiji Plugins menu.
 */
@Plugin(type = Command.class, menuPath = "Plugins>Project Manager")
public class ProjectManagerCommand implements Command {

	// Shared service instance so the same project state is maintained
	// across menu invocations.
	private static ProjectManagerService sharedService;
	private static ProjectManagerDialog dialog;

	@Override
	public void run() {
		SwingUtilities.invokeLater(() -> {
			if (dialog != null && dialog.isDisplayable()) {
				dialog.toFront();
				dialog.requestFocus();
				return;
			}
			if (sharedService == null) {
				sharedService = new ProjectManagerService();
			}
			dialog = new ProjectManagerDialog(sharedService);
			dialog.setVisible(true);
		});
	}
}
