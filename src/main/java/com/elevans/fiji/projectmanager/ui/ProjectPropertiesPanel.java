package com.elevans.fiji.projectmanager.ui;

import com.elevans.fiji.projectmanager.models.Project.ExperimentType;
import com.elevans.fiji.projectmanager.services.ProjectManagerService;

import javax.swing.*;
import java.awt.*;

/**
 * Panel displaying and editing project properties (name, description, type).
 */
public class ProjectPropertiesPanel extends JPanel {

	private final ProjectManagerService service;
	private JTextField nameField;
	private JTextArea descriptionArea;
	private JComboBox<ExperimentType> typeCombo;
	private JLabel createdLabel;
	private JLabel modifiedLabel;
	private boolean updating = false;

	public ProjectPropertiesPanel(ProjectManagerService service) {
		this.service = service;
		initComponents();
		refresh();
	}

	private void initComponents() {
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 4, 2, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Name
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		add(new JLabel("Name:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		nameField = new JTextField(20);
		nameField.addActionListener(e -> applyNameChange());
		nameField.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusLost(java.awt.event.FocusEvent e) { applyNameChange(); }
		});
		add(nameField, gbc);

		// Type
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		add(new JLabel("Type:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		typeCombo = new JComboBox<>(ExperimentType.values());
		typeCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof ExperimentType t) setText(t.getDisplayName());
				return this;
			}
		});
		typeCombo.addActionListener(e -> {
			if (!updating) {
				service.updateExperimentType((ExperimentType) typeCombo.getSelectedItem());
			}
		});
		add(typeCombo, gbc);

		// Description
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
		add(new JLabel("Description:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
		descriptionArea = new JTextArea(3, 20);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		descriptionArea.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusLost(java.awt.event.FocusEvent e) { applyDescriptionChange(); }
		});
		add(new JScrollPane(descriptionArea), gbc);

		// Dates
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
		add(new JLabel("Created:"), gbc);
		gbc.gridx = 1;
		createdLabel = new JLabel("-");
		add(createdLabel, gbc);

		gbc.gridx = 0; gbc.gridy = 4;
		add(new JLabel("Modified:"), gbc);
		gbc.gridx = 1;
		modifiedLabel = new JLabel("-");
		add(modifiedLabel, gbc);
	}

	private void applyNameChange() {
		if (updating) return;
		String name = nameField.getText().trim();
		if (!name.isEmpty()) {
			service.updateName(name);
		}
	}

	private void applyDescriptionChange() {
		if (updating) return;
		service.updateDescription(descriptionArea.getText().trim());
	}

	/**
	 * Refresh panel from current project state.
	 */
	public void refresh() {
		updating = true;
		try {
			service.getCurrentProject().ifPresentOrElse(project -> {
				nameField.setText(project.name());
				descriptionArea.setText(project.description());
				typeCombo.setSelectedItem(project.experimentType());
				createdLabel.setText(project.createdDate().toString());
				modifiedLabel.setText(project.modifiedDate().toString());
				setFieldsEnabled(true);
			}, () -> {
				nameField.setText("");
				descriptionArea.setText("");
				typeCombo.setSelectedIndex(0);
				createdLabel.setText("-");
				modifiedLabel.setText("-");
				setFieldsEnabled(false);
			});
		} finally {
			updating = false;
		}
	}

	private void setFieldsEnabled(boolean enabled) {
		nameField.setEnabled(enabled);
		descriptionArea.setEnabled(enabled);
		typeCombo.setEnabled(enabled);
	}
}
