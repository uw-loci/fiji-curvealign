package com.fiji.curvealign;

import ij.ImagePlus;
import ij.measure.ResultsTable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

final class CurveAlignDialog extends JFrame {

	private final ImagePlus image;
	private final JSpinner keepField = new JSpinner(new SpinnerNumberModel(0.05, 0.001, 1.0, 0.01));
	private final JSpinner scaleField;
	private final JSpinner radiusField = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 1000.0, 1.0));
	private final JSpinner neighborsField = new JSpinner(new SpinnerNumberModel(1, 1, 32, 1));
	private final JSpinner boxSizeField = new JSpinner(new SpinnerNumberModel(32, 1, 4096, 1));
	private final JLabel status = new JLabel("Ready");
	private final JButton analyzeButton = new JButton("Extract fiber features");

	CurveAlignDialog(final ImagePlus image) {
		super("Fiji CurveAlign");
		this.image = image;
		int minimumDimension = Math.min(image.getWidth(), image.getHeight());
		int maximumScale = Math.max(1, (int) (Math.log(minimumDimension) / Math.log(2)) - 4);
		scaleField = new JSpinner(new SpinnerNumberModel(1, 1, maximumScale, 1));
		buildUi();
	}

	private void buildUi() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(380, 300));
		setResizable(false);

		JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
		addField(form, 0, "Active image", new JLabel(image.getShortTitle()));
		addField(form, 1, "Keep coefficient fraction", keepField);
		addField(form, 2, "Curvelet scale", scaleField);
		addField(form, 3, "Grouping radius (px)", radiusField);
		addField(form, 4, "Nearest fibers", neighborsField);
		addField(form, 5, "Feature box size (px)", boxSizeField);

		analyzeButton.addActionListener(event -> runAnalysis());
		JPanel footer = new JPanel(new BorderLayout(8, 0));
		footer.setBorder(BorderFactory.createEmptyBorder(8, 14, 12, 14));
		footer.add(status, BorderLayout.CENTER);
		footer.add(analyzeButton, BorderLayout.EAST);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(form, BorderLayout.CENTER);
		getContentPane().add(footer, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(null);
	}

	private void addField(final JPanel form, final int row, final String label, final java.awt.Component field) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridy = row;
		constraints.insets = new Insets(5, 0, 5, 8);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridx = 0;
		form.add(new JLabel(label), constraints);
		constraints.gridx = 1;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(5, 0, 5, 0);
		form.add(field, constraints);
	}

	private void runAnalysis() {
		analyzeButton.setEnabled(false);
		status.setText("Starting Python environment...");
		double keep = ((Number) keepField.getValue()).doubleValue();
		int scale = ((Number) scaleField.getValue()).intValue();
		double radius = ((Number) radiusField.getValue()).doubleValue();
		int minimumNearestFibers = ((Number) neighborsField.getValue()).intValue();
		int minimumBoxSize = ((Number) boxSizeField.getValue()).intValue();

		new SwingWorker<List<Map<String, Object>>, Void>() {
			@Override
			protected List<Map<String, Object>> doInBackground() throws Exception {
				return CurveAlignService.analyze(image, keep, scale, radius, minimumNearestFibers, minimumBoxSize);
			}

			@Override
			protected void done() {
				analyzeButton.setEnabled(true);
				try {
					List<Map<String, Object>> rows = get();
					if (rows.isEmpty()) {
						status.setText("No fibers were detected.");
						return;
					}
					showResults(rows);
					status.setText("Completed: " + rows.size() + " fibers");
				} catch (InterruptedException error) {
					Thread.currentThread().interrupt();
					status.setText("Analysis interrupted");
				} catch (ExecutionException error) {
					Throwable cause = error.getCause() == null ? error : error.getCause();
					status.setText("Analysis failed");
					javax.swing.JOptionPane.showMessageDialog(CurveAlignDialog.this,
						cause.getMessage(), "Fiji CurveAlign", javax.swing.JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	private void showResults(final List<Map<String, Object>> rows) {
		ResultsTable table = new ResultsTable();
		for (Map<String, Object> row : rows) {
			table.incrementCounter();
			row.forEach((column, value) -> {
				if (value instanceof Number number) table.addValue(column, number.doubleValue());
				else if (value != null) table.addValue(column, String.valueOf(value));
			});
		}
		table.show("CurveAlign - " + image.getShortTitle());
	}
}