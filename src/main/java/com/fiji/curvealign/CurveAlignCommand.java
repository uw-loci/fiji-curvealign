package com.fiji.curvealign;

import net.imagej.Dataset;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.SwingUtilities;

@Plugin(type = Command.class, menuPath = "Plugins>CurveAlign>Analyze current plane")
public class CurveAlignCommand implements Command {
	@Parameter
	private Dataset image;

	@Override
	public void run() {
		if (image == null) {
			IJ.error("Fiji CurveAlign", "Open an image before starting an analysis.");
			return;
		}
		SwingUtilities.invokeLater(() -> new CurveAlignDialog(image).setVisible(true));
	}
}
