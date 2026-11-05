package sc.fiji.curvealign;

import java.io.File;

import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

@Plugin(type = Command.class, menuPath = "Plugins>CurveAlign>Configure")
public class ConfigurePythonEnvironmentCommand implements Command, Initializable {
	static final String PREFERENCE_KEY = "pythonEnvironment";

	static boolean isPythonEnvironment(final File directory) {
		return directory != null && directory.isDirectory() &&
			(new File(directory, "bin/python").isFile() ||
				new File(directory, "Scripts/python.exe").isFile());
	}

	private static boolean isTmeQuantCheckout(final File directory) {
		return directory != null && directory.isDirectory() &&
			new File(directory, "bin/install.sh").isFile();
	}

	@Parameter(label = "tme-quant checkout (.venv built by bin/install.sh)",
		style = "directory", description = "Select the repository directory containing bin/install.sh. Its hidden .venv directory will be used.")
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
			throw new IllegalArgumentException("Select the tme-quant checkout containing bin/install.sh.");
		}
		final File pythonEnvironment = new File(tmeQuantCheckout, ".venv");
		if (!isPythonEnvironment(pythonEnvironment)) {
			throw new IllegalArgumentException("The selected checkout has no valid .venv. Run bin/install.sh from this tme-quant checkout first.");
		}
		prefService.put(getClass(), PREFERENCE_KEY, pythonEnvironment.getAbsolutePath());
	}
}