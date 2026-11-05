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

	@Parameter(label = "Python environment (.venv directory)", style = "directory")
	private File pythonEnvironment;

	@Parameter
	private PrefService prefService;

	@Override
	public void initialize() {
		final String path = prefService.get(getClass(), PREFERENCE_KEY);
		if (path != null && !path.isBlank()) pythonEnvironment = new File(path);
	}

	@Override
	public void run() {
		if (!isPythonEnvironment(pythonEnvironment)) {
			throw new IllegalArgumentException("Select the installed tme-quant .venv, not the source checkout.");
		}
		prefService.put(getClass(), PREFERENCE_KEY, pythonEnvironment.getAbsolutePath());
	}
}