package com.elevans.fiji.projectmanager.ui;

import com.elevans.fiji.projectmanager.models.ProjectImage;
import com.elevans.fiji.projectmanager.models.ProjectMetadata;
import com.elevans.fiji.projectmanager.models.ProjectMetadata.ChannelMetadata;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.Map;

/**
 * Panel displaying OME metadata for a selected image in a tree view.
 * Organizes metadata by category: Dimensions, Channels, Acquisition, Microscope, Experimenter.
 */
public class MetadataViewerPanel extends JPanel {

	private final DefaultMutableTreeNode rootNode;
	private final DefaultTreeModel treeModel;
	private final JTree tree;
	private final JLabel headerLabel;

	public MetadataViewerPanel() {
		rootNode = new DefaultMutableTreeNode("Metadata");
		treeModel = new DefaultTreeModel(rootNode);
		tree = new JTree(treeModel);
		headerLabel = new JLabel("Select an image to view metadata");
		initComponents();
	}

	private void initComponents() {
		setLayout(new BorderLayout());

		headerLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
		add(headerLabel, BorderLayout.NORTH);

		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		add(new JScrollPane(tree), BorderLayout.CENTER);
	}

	/**
	 * Display metadata for the given image.
	 */
	public void displayMetadata(ProjectImage image) {
		rootNode.removeAllChildren();
		headerLabel.setText(image.imageName());

		ProjectMetadata meta = image.omeMetadata();
		if (meta == null) {
			rootNode.add(new DefaultMutableTreeNode("No metadata available"));
			treeModel.reload();
			return;
		}

		// Dimensions
		DefaultMutableTreeNode dimsNode = new DefaultMutableTreeNode("Dimensions");
		addField(dimsNode, "Size X", meta.sizeX());
		addField(dimsNode, "Size Y", meta.sizeY());
		addField(dimsNode, "Size Z", meta.sizeZ());
		addField(dimsNode, "Size C (Channels)", meta.sizeC());
		addField(dimsNode, "Size T (Timepoints)", meta.sizeT());
		addField(dimsNode, "Pixel Type", meta.pixelType());
		addField(dimsNode, "Physical Size X (µm)", meta.pixelPhysicalSizeX());
		addField(dimsNode, "Physical Size Y (µm)", meta.pixelPhysicalSizeY());
		addField(dimsNode, "Physical Size Z (µm)", meta.pixelPhysicalSizeZ());
		if (dimsNode.getChildCount() > 0) rootNode.add(dimsNode);

		// Channels
		if (meta.channels() != null && !meta.channels().isEmpty()) {
			DefaultMutableTreeNode chsNode = new DefaultMutableTreeNode("Channels");
			for (ChannelMetadata ch : meta.channels()) {
				String label = ch.channelName() != null
					? String.format("Channel %d: %s", ch.channelIndex(), ch.channelName())
					: String.format("Channel %d", ch.channelIndex());
				DefaultMutableTreeNode chNode = new DefaultMutableTreeNode(label);
				addField(chNode, "Fluorophore", ch.fluor());
				addField(chNode, "Emission (nm)", ch.emissionWavelength());
				addField(chNode, "Excitation (nm)", ch.excitationWavelength());
				chsNode.add(chNode);
			}
			rootNode.add(chsNode);
		}

		// Acquisition
		addMapSection("Acquisition", meta.acquisitionMetadata());

		// Microscope
		addMapSection("Microscope", meta.microscopeInfo());

		// Experimenter
		addMapSection("Experimenter", meta.experimenterInfo());

		// File info
		DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode("File");
		fileNode.add(new DefaultMutableTreeNode("Path: " + image.filePath()));
		addField(fileNode, "Group", image.groupKey());
		rootNode.add(fileNode);

		treeModel.reload();
		expandAllNodes();
	}

	/**
	 * Clear the metadata display.
	 */
	public void clear() {
		rootNode.removeAllChildren();
		headerLabel.setText("Select an image to view metadata");
		treeModel.reload();
	}

	private void addMapSection(String title, Map<String, String> map) {
		if (map != null && !map.isEmpty()) {
			DefaultMutableTreeNode section = new DefaultMutableTreeNode(title);
			for (Map.Entry<String, String> entry : map.entrySet()) {
				section.add(new DefaultMutableTreeNode(
					formatKey(entry.getKey()) + ": " + entry.getValue()));
			}
			rootNode.add(section);
		}
	}

	private void addField(DefaultMutableTreeNode parent, String name, Object value) {
		if (value != null) {
			parent.add(new DefaultMutableTreeNode(name + ": " + value));
		}
	}

	private String formatKey(String camelCase) {
		// Convert camelCase to Title Case with spaces
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < camelCase.length(); i++) {
			char c = camelCase.charAt(i);
			if (i > 0 && Character.isUpperCase(c)) {
				sb.append(' ');
			}
			if (i == 0) {
				sb.append(Character.toUpperCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private void expandAllNodes() {
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
	}
}
