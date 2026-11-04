package com.elevans.fiji.projectmanager.ui;

import com.elevans.fiji.projectmanager.models.ProjectImage;
import com.elevans.fiji.projectmanager.services.ProjectManagerService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel displaying the list of images in the current project.
 * Supports selection, removal, and drag-to-reorder.
 */
public class ImageListPanel extends JPanel {

	private final ProjectManagerService service;
	private final ImageTableModel tableModel;
	private final JTable table;
	private final List<Consumer<ProjectImage>> selectionListeners = new ArrayList<>();

	public ImageListPanel(ProjectManagerService service) {
		this.service = service;
		this.tableModel = new ImageTableModel();
		this.table = new JTable(tableModel);
		initComponents();
		refresh();
	}

	private void initComponents() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Images"));

		// Table setup
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(24);
		table.getColumnModel().getColumn(0).setPreferredWidth(30);   // #
		table.getColumnModel().getColumn(1).setPreferredWidth(200);  // Name
		table.getColumnModel().getColumn(2).setPreferredWidth(100);  // Dimensions
		table.getColumnModel().getColumn(3).setPreferredWidth(60);   // Channels
		table.getColumnModel().getColumn(4).setPreferredWidth(80);   // Group

		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				int row = table.getSelectedRow();
				ProjectImage selected = (row >= 0) ? tableModel.getImageAt(row) : null;
				for (Consumer<ProjectImage> listener : selectionListeners) {
					listener.accept(selected);
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(table);
		add(scrollPane, BorderLayout.CENTER);

		// Button bar
		JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton addBtn = new JButton("Add...");
		addBtn.addActionListener(e -> addImages());

		JButton removeBtn = new JButton("Remove");
		removeBtn.addActionListener(e -> removeSelectedImage());

		JButton moveUpBtn = new JButton("Move Up");
		moveUpBtn.addActionListener(e -> moveSelected(-1));

		JButton moveDownBtn = new JButton("Move Down");
		moveDownBtn.addActionListener(e -> moveSelected(1));

		JButton setGroupBtn = new JButton("Set Group...");
		setGroupBtn.addActionListener(e -> setGroupForSelected());

		buttonBar.add(addBtn);
		buttonBar.add(removeBtn);
		buttonBar.add(Box.createHorizontalStrut(12));
		buttonBar.add(moveUpBtn);
		buttonBar.add(moveDownBtn);
		buttonBar.add(Box.createHorizontalStrut(12));
		buttonBar.add(setGroupBtn);

		add(buttonBar, BorderLayout.SOUTH);
	}

	public void addSelectionListener(Consumer<ProjectImage> listener) {
		selectionListeners.add(listener);
	}

	public void refresh() {
		List<ProjectImage> images = service.getCurrentProject()
			.map(p -> p.images())
			.orElse(List.of());
		tableModel.setImages(images);
	}

	public void removeSelectedImage() {
		int row = table.getSelectedRow();
		if (row < 0) return;
		ProjectImage img = tableModel.getImageAt(row);
		service.removeImage(img.id());
	}

	private void addImages() {
		if (service.getCurrentProject().isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Create or load a project first.", "No Project", JOptionPane.WARNING_MESSAGE);
			return;
		}
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Add Images to Project");
		chooser.setMultiSelectionEnabled(true);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			for (java.io.File file : chooser.getSelectedFiles()) {
				service.addImage(file.getAbsolutePath(), file.getName());
			}
		}
	}

	private void moveSelected(int direction) {
		int row = table.getSelectedRow();
		if (row < 0) return;
		int newRow = row + direction;
		if (newRow < 0 || newRow >= tableModel.getRowCount()) return;

		List<String> ids = new ArrayList<>();
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			ids.add(tableModel.getImageAt(i).id());
		}
		Collections.swap(ids, row, newRow);
		service.reorderImages(ids);

		// Keep selection on the moved row
		SwingUtilities.invokeLater(() -> table.setRowSelectionInterval(newRow, newRow));
	}

	private void setGroupForSelected() {
		int row = table.getSelectedRow();
		if (row < 0) return;

		ProjectImage img = tableModel.getImageAt(row);
		String current = img.groupKey() != null ? img.groupKey() : "";
		String newGroup = JOptionPane.showInputDialog(this,
			"Enter group name for \"" + img.imageName() + "\":", current);
		if (newGroup != null) {
			service.setImageGroup(img.id(), newGroup.trim().isEmpty() ? null : newGroup.trim());
		}
	}

	// -- Table Model --

	private static class ImageTableModel extends AbstractTableModel {
		private static final String[] COLUMNS = {"#", "Name", "Dimensions", "Channels", "Group"};
		private List<ProjectImage> images = new ArrayList<>();

		public void setImages(List<ProjectImage> images) {
			this.images = new ArrayList<>(images);
			fireTableDataChanged();
		}

		public ProjectImage getImageAt(int row) {
			return images.get(row);
		}

		@Override
		public int getRowCount() { return images.size(); }

		@Override
		public int getColumnCount() { return COLUMNS.length; }

		@Override
		public String getColumnName(int col) { return COLUMNS[col]; }

		@Override
		public Object getValueAt(int row, int col) {
			ProjectImage img = images.get(row);
			return switch (col) {
				case 0 -> img.orderIndex() + 1;
				case 1 -> img.imageName();
				case 2 -> formatDimensions(img);
				case 3 -> formatChannels(img);
				case 4 -> img.groupKey() != null ? img.groupKey() : "";
				default -> "";
			};
		}

		private String formatDimensions(ProjectImage img) {
			if (img.omeMetadata() == null) return "N/A";
			var m = img.omeMetadata();
			if (m.sizeX() == null) return "N/A";
			StringBuilder sb = new StringBuilder();
			sb.append(m.sizeX()).append("x").append(m.sizeY());
			if (m.sizeZ() != null && m.sizeZ() > 1) sb.append("x").append(m.sizeZ());
			return sb.toString();
		}

		private String formatChannels(ProjectImage img) {
			if (img.omeMetadata() == null) return "N/A";
			var m = img.omeMetadata();
			if (m.sizeC() == null) return "N/A";
			return String.valueOf(m.sizeC());
		}
	}
}
