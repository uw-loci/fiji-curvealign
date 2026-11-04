package loci.plugins.curvealign;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Fiji menu entry for CurveAlign.
 *
 * <p>Registered as {@code Plugins > CurveAlign}. This placeholder only confirms
 * that Fiji loaded the plugin. The tme-quant workflow from
 * uw-loci/tme-quant#66 is not called yet.
 */
@Plugin(type = Command.class, menuPath = "Plugins>CurveAlign")
public class CurveAlignCommand implements Command {

	static final String PLACEHOLDER_MESSAGE =
		"CurveAlign placeholder. The tme-quant workflow is not connected yet.";

	@Parameter
	private LogService log;

	/** Absent when Fiji is running headless. */
	@Parameter(required = false)
	private UIService ui;

	@Override
	public void run() {
		log.info(PLACEHOLDER_MESSAGE);
		if (ui != null) {
			ui.showDialog(PLACEHOLDER_MESSAGE, "CurveAlign");
		}
	}
}
