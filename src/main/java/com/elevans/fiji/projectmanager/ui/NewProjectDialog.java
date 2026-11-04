package com.elevans.fiji.projectmanager.ui;

import com.elevans.fiji.projectmanager.models.Project.ExperimentType;
import com.elevans.fiji.projectmanager.services.ProjectManagerService;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for creating a new project.
 */
public class NewProjectDialog extends JDialog {

	private final ProjectManagerService service;
	private JTextField nameField;
	private JTextArea descriptionArea;
	private JComboBox<ExperimentType> typeCombo;

	public NewProjectDialog(JFrame parent, ProjectManagerService service) {
		super(parent, "New Project", true);
		this.service = service;
		initComponents();
	}

	private void initComponents() {
		setLayout(new BorderLayout(8, 8));
		setMinimumSize(new Dimension(400, 300));

		JPanel formPanel = new JPanel(new GridBagLayout());
		formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Name
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
		formPanel.add(new JLabel("Name:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		nameField = new JTextField(20);
		formPanel.add(nameField, gbc);

		// Experiment Type
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
		formPanel.add(new JLabel("Type:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0;
		typeCombo = new JComboBox<>(ExperimentType.values());
		typeCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof ExperimentType t) {
					setText(t.getDisplayName());
				}
				return this;
			}
		});
		formPanel.add(typeCombo, gbc);

		// Description
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
		formPanel.add(new JLabel("Description:"), gbc);
		gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
		descriptionArea = new JTextArea(4, 20);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		formPanel.add(new JScrollPane(descriptionArea), gbc);

		// Buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton createBtn = new JButton("Create");
		createBtn.addActionListener(e -> onCreateClicked());
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(e -> dispose());
		buttonPanel.add(createBtn);
		buttonPanel.add(cancelBtn);

		add(formPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(getParent());
	}

	private void onCreateClicked() {
		String name = nameField.getText().trim();
		if (name.isEmpty()) {
			JOptionPane.showMessageDialog(this,
				"Please enter a project name.", "Validation", JOptionPane.WARNING_MESSAGE);
			return;
		}
		ExperimentType type = (ExperimentType) typeCombo.getSelectedItem();
		String description = descriptionArea.getText().trim();
		service.createProject(name, description, type);
		dispose();
	}
}
