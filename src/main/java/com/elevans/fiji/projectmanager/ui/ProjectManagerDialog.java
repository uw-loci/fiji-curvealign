package com.elevans.fiji.projectmanager.ui;

import com.elevans.fiji.projectmanager.models.Project;
import com.elevans.fiji.projectmanager.models.ProjectEvent;
import com.elevans.fiji.projectmanager.models.ProjectImage;
import com.elevans.fiji.projectmanager.io.ProjectSerializer;
import com.elevans.fiji.projectmanager.services.ProjectManagerService;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Main dialog window for the Project Manager.
 * Contains panels for project properties, image list, and metadata viewing.
 */
public class ProjectManagerDialog extends JFrame {

	private final ProjectManagerService service;
	private final ProjectSerializer serializer;

	private ProjectPropertiesPanel propertiesPanel;
	private ImageListPanel imageListPanel;
	private MetadataViewerPanel metadataPanel;
	private JLabel statusLabel;

	public ProjectManagerDialog(ProjectManagerService service) {
		super("Fiji Project Manager");
		this.service = service;
		this.serializer = new ProjectSerializer();
		initComponents();
		registerListeners();
		updateStatus();
	}

	private void initComponents() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(900, 600));
		setPreferredSize(new Dimension(1000, 700));

		// Menu bar
		setJMenuBar(createMenuBar());

		// Main layout: left panel (properties + image list) | right panel (metadata)
		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainSplit.setResizeWeight(0.6);

		// Left: properties on top, image list on bottom
		JPanel leftPanel = new JPanel(new BorderLayout());
		propertiesPanel = new ProjectPropertiesPanel(service);
		imageListPanel = new ImageListPanel(service);

		// Properties panel in a collapsible titled border
		JPanel propsWrapper = new JPanel(new BorderLayout());
		propsWrapper.setBorder(BorderFactory.createTitledBorder("Project Properties"));
		propsWrapper.add(propertiesPanel, BorderLayout.CENTER);

		leftPanel.add(propsWrapper, BorderLayout.NORTH);
		leftPanel.add(imageListPanel, BorderLayout.CENTER);

		// Right: metadata viewer
		metadataPanel = new MetadataViewerPanel();
		JPanel metaWrapper = new JPanel(new BorderLayout());
		metaWrapper.setBorder(BorderFactory.createTitledBorder("Image Metadata (OME)"));
		metaWrapper.add(metadataPanel, BorderLayout.CENTER);

		mainSplit.setLeftComponent(leftPanel);
		mainSplit.setRightComponent(metaWrapper);

		// Listen for image selection changes to update metadata panel
		imageListPanel.addSelectionListener(this::onImageSelected);

		// Status bar
		statusLabel = new JLabel("No project open");
		statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(mainSplit, BorderLayout.CENTER);
		getContentPane().add(statusLabel, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(null);
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		// Project menu
		JMenu projectMenu = new JMenu("Project");
		JMenuItem newProject = new JMenuItem("New Project...");
		newProject.addActionListener(e -> showNewProjectDialog());

		JMenuItem saveProject = new JMenuItem("Save Project...");
		saveProject.addActionListener(e -> saveProject());

		JMenuItem loadProject = new JMenuItem("Load Project...");
		loadProject.addActionListener(e -> loadProject());

		JMenuItem closeProject = new JMenuItem("Close Project");
		closeProject.addActionListener(e -> closeProject());

		projectMenu.add(newProject);
		projectMenu.addSeparator();
		projectMenu.add(saveProject);
		projectMenu.add(loadProject);
		projectMenu.addSeparator();
		projectMenu.add(closeProject);

		menuBar.add(projectMenu);

		// Image menu
		JMenu imageMenu = new JMenu("Images");
		JMenuItem addImage = new JMenuItem("Add Image...");
		addImage.addActionListener(e -> addImage());

		JMenuItem removeImage = new JMenuItem("Remove Selected");
		removeImage.addActionListener(e -> imageListPanel.removeSelectedImage());

		imageMenu.add(addImage);
		imageMenu.add(removeImage);

		menuBar.add(imageMenu);

		return menuBar;
	}

	private void registerListeners() {
		service.addListener(event -> SwingUtilities.invokeLater(() -> {
			updateStatus();
			switch (event) {
				case ProjectEvent.ProjectCreatedEvent e -> {
					propertiesPanel.refresh();
					imageListPanel.refresh();
					metadataPanel.clear();
				}
				case ProjectEvent.ProjectClosedEvent e -> {
					propertiesPanel.refresh();
					imageListPanel.refresh();
					metadataPanel.clear();
				}
				case ProjectEvent.ImageAddedEvent e -> imageListPanel.refresh();
				case ProjectEvent.ImageRemovedEvent e -> {
					imageListPanel.refresh();
					metadataPanel.clear();
				}
				case ProjectEvent.ProjectUpdatedEvent e -> propertiesPanel.refresh();
				case ProjectEvent.ImagesReorderedEvent e -> imageListPanel.refresh();
			}
		}));
	}

	private void onImageSelected(ProjectImage image) {
		if (image != null && image.omeMetadata() != null) {
			metadataPanel.displayMetadata(image);
		} else {
			metadataPanel.clear();
		}
	}

	private void updateStatus() {
		service.getCurrentProject().ifPresentOrElse(
			p -> statusLabel.setText(String.format("Project: %s  |  Images: %d  |  Type: %s",
				p.name(), p.images().size(), p.experimentType().getDisplayName())),
			() -> statusLabel.setText("No project open")
		);
	}

	// -- Dialog Actions --

	private void showNewProjectDialog() {
		NewProjectDialog dlg = new NewProjectDialog(this, service);
		dlg.setVisible(true);
	}

	private void saveProject() {
		service.getCurrentProject().ifPresentOrElse(project -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Save Project");
			chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
			chooser.setSelectedFile(new java.io.File(project.name().replaceAll("\\s+", "_") + ".json"));
			if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				Path path = chooser.getSelectedFile().toPath();
				if (!path.toString().endsWith(".json")) {
					path = Path.of(path + ".json");
				}
				try {
					serializer.save(project, path);
					JOptionPane.showMessageDialog(this,
						"Project saved to:\n" + path, "Saved", JOptionPane.INFORMATION_MESSAGE);
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(this,
						"Failed to save project:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}, () -> JOptionPane.showMessageDialog(this,
			"No project is open.", "Save", JOptionPane.WARNING_MESSAGE));
	}

	private void loadProject() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Load Project");
		chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				Project project = serializer.load(chooser.getSelectedFile().toPath());
				// Validate image paths
				List<String> missing = serializer.validateImagePaths(project);
				if (!missing.isEmpty()) {
					String msg = "The following image files were not found:\n\n"
						+ String.join("\n", missing)
						+ "\n\nLoad project anyway?";
					int result = JOptionPane.showConfirmDialog(this, msg,
						"Missing Images", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (result != JOptionPane.YES_OPTION) return;
				}
				service.setProject(project);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this,
					"Failed to load project:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void closeProject() {
		if (service.getCurrentProject().isPresent()) {
			int result = JOptionPane.showConfirmDialog(this,
				"Close the current project? Unsaved changes will be lost.",
				"Close Project", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				service.closeProject();
			}
		}
	}

	private void addImage() {
		if (service.getCurrentProject().isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Create or load a project first.", "No Project", JOptionPane.WARNING_MESSAGE);
			return;
		}
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Add Image to Project");
		chooser.setMultiSelectionEnabled(true);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			for (java.io.File file : chooser.getSelectedFiles()) {
				service.addImage(file.getAbsolutePath(), file.getName());
			}
		}
	}
}
