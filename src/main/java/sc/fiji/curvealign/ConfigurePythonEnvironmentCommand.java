package sc.fiji.curvealign;

import java.io.File;

import org.scijava.Initializable;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

@Plugin(type = Command.class, menuPath = "Plugins>CurveAlign>Configure")
public class ConfigurePythonEnvironmentCommand implements Command, Initializable {
	static final String PREFERENCE_KEY = "pythonEnvironment";

	@Parameter(label = "", visibility = ItemVisibility.MESSAGE, persist = false)
	private final String instructions = "<html><b>Python environment</b><br>" +
		"Select the tme-quant checkout that contains a .venv subdirectory.<br>" +
		"bin/install.sh must be run first to build the environment.</html>";

	@Parameter(label = "tme-quant checkout",
		style = "directory", description = "Select the tme-quant repository to use for curvelet analysis.")
	private File tmeQuantCheckout;

	@Parameter
	private PrefService prefService;

	@Override
	public void initialize() {
		final String path = prefService.get(getClass(), PREFERENCE_KEY);
		if (path == null || path.isBlank()) return;
		final File savedPath = new File(path);
		if (isTmeQuantCheckout(savedPath)) {
			tmeQuantCheckout = savedPath;
		}
		else if (".venv".equals(savedPath.getName()) &&
			isTmeQuantCheckout(savedPath.getParentFile()))
		{
			tmeQuantCheckout = savedPath.getParentFile();
		}
	}

	@Override
	public void run() {
		if (!isTmeQuantCheckout(tmeQuantCheckout)) {
			throw new IllegalArgumentException("Select a tme-quant checkout containing bin/install.sh.");
		}
		final File pythonEnvironment = new File(tmeQuantCheckout, ".venv");
		if (!isPythonEnvironment(pythonEnvironment)) {
			throw new IllegalArgumentException("The selected checkout has no valid .venv. Run bin/install.sh from this tme-quant checkout first.");
		}
		prefService.put(getClass(), PREFERENCE_KEY, pythonEnvironment.getAbsolutePath());
	}

	private static boolean isTmeQuantCheckout(final File directory) {
		return directory != null && directory.isDirectory() &&
			new File(directory, "bin/install.sh").isFile();
	}

	static boolean isPythonEnvironment(final File directory) {
		return directory != null && directory.isDirectory() &&
			(new File(directory, "bin/python").isFile() ||
				new File(directory, "Scripts/python.exe").isFile());
	}
}