package sc.fiji.curvealign;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.apposed.appose.Appose;
import org.apposed.appose.NDArray;
import org.apposed.appose.Service;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;
import org.apposed.appose.TaskEvent;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.task.TaskService;
import org.scijava.ui.UIService;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.ShmImg;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ImgUtil;

@Plugin(type = Command.class, menuPath = "Plugins>CurveAlign>Curvelets")
public class CurveletsCommand implements Command {
	@Parameter
	private Dataset inputImage;

	@Parameter(label = "Use boundary mask")
	private boolean useBoundaryMask;

	@Parameter(label = "Boundary mask", required = false,
		description = "Only used when Use boundary mask is enabled.")
	private Dataset boundaryMask;

	@Parameter(label = "Analysis output directory (tables and plots)", style = "directory")
	private File outputDirectory;

	@Parameter(label = "Curvelet coefficients to keep", min = "0.0", max = "1.0")
	private double keep = 0.1;

	@Parameter(label = "Fiber mode (0=curvelet, 1-3=FIRE)", min = "0", max = "3")
	private int fiberMode;

	@Parameter(label = "CT-FIRE results directory (FIRE modes 1-3 only)", style = "directory", required = false)
	private File fireDirectory;

	@Parameter(label = "Boundary distance threshold (pixels)", min = "0")
	private double distanceThreshold = 100.0;

	@Parameter(type = ItemIO.OUTPUT, label = "Overlay")
	private Dataset overlay;

	@Parameter(type = ItemIO.OUTPUT, label = "Heatmap")
	private Dataset heatmap;

	@Parameter(type = ItemIO.OUTPUT, label = "Reconstruction", required = false)
	private Dataset reconstruction;

	@Parameter(type = ItemIO.OUTPUT, label = "Fiber summary")
	private String summary;

	@Parameter
	private DatasetService datasetService;

	@Parameter(required = false)
	private UIService uiService;

	@Parameter
	private PrefService prefService;

	@Parameter
	private TaskService taskService;

	@Parameter
	private CommandService commandService;

	@Override
	public void run() {
		final File pythonEnvironment = configuredPythonEnvironment();
		validateInputs(pythonEnvironment);
		final Img<FloatType> image = asFloatImage(inputImage);
		final ShmImg<FloatType> sharedImage = ShmImg.copyOf(image);
		final ShmImg<FloatType> sharedMask = useBoundaryMask ?
			ShmImg.copyOf(asFloatImage(boundaryMask)) : null;
		final org.scijava.task.Task statusTask = taskService.createTask("Curvelets");
		statusTask.setStatusMessage("Starting CurveAlign...");
		statusTask.start();
		try (Service python = Appose.wrap(pythonEnvironment).python()) {
			final Map<String, Object> inputs = new HashMap<>();
			inputs.put("img", sharedImage.ndArray());
			inputs.put("boundary_mask", sharedMask == null ? null : sharedMask.ndArray());
			inputs.put("img_name", imageName());
			inputs.put("output_dir", outputDirectory.getAbsolutePath());
			inputs.put("keep", keep);
			inputs.put("fiber_mode", fiberMode);
			inputs.put("fire_dir", fireDirectory == null ? null : fireDirectory.getAbsolutePath());
			inputs.put("distance_threshold", distanceThreshold);

			final Task task = python.task(readWorkerScript(), inputs);
			task.listen((final TaskEvent event) -> {
				if (event.message != null && !event.message.isBlank()) {
					statusTask.setStatusMessage(event.message);
				}
				if (event.maximum > 0) {
					statusTask.setProgressMaximum(event.maximum);
					statusTask.setProgressValue(event.current);
				}
			});
			statusTask.setCancelCallBack(task::cancel);
			statusTask.setStatusMessage("Running CurveAlign...");
			task.start();
			task.waitFor();
			if (task.status != TaskStatus.COMPLETE) {
				throw new IllegalStateException("CurveAlign Python task failed: " + task.error);
			}
			statusTask.setStatusMessage("Copying CurveAlign results into Fiji...");
			overlay = copyColorImage((NDArray) task.outputs.get("overlay"), "Curvelets overlay");
			heatmap = copyColorImage((NDArray) task.outputs.get("heatmap"), "Curvelets heatmap");
			final NDArray reconstructionArray = (NDArray) task.outputs.get("reconstruction");
			if (reconstructionArray != null) {
				reconstruction = copyFloatImage(reconstructionArray, "Curvelet reconstruction");
			}
			summary = (String) task.outputs.get("summary");
			if (uiService != null) {
				uiService.show(overlay);
				uiService.show(heatmap);
				if (reconstruction != null) uiService.show(reconstruction);
			}
			statusTask.setStatusMessage("CurveAlign complete");
		}
		catch (final InterruptedException e) {
			statusTask.setStatusMessage("CurveAlign interrupted");
			Thread.currentThread().interrupt();
			throw new RuntimeException("CurveAlign was interrupted", e);
		}
		catch (final Exception e) {
			statusTask.setStatusMessage("CurveAlign failed");
			throw new RuntimeException("CurveAlign failed", e);
		}
		finally {
			statusTask.finish();
			sharedImage.close();
			if (sharedMask != null) sharedMask.close();
		}
	}

	private Dataset copyColorImage(final NDArray array, final String name) {
		final ShmImg<UnsignedIntType> shared = new ShmImg<>(array);
		try {
			if (shared.numDimensions() != 2) {
				throw new IllegalArgumentException("Expected a 2D packed color image.");
			}
			final Img<ARGBType> copy = ArrayImgs.argbs(shared.dimension(0), shared.dimension(1));
			final Cursor<UnsignedIntType> source = shared.cursor();
			final Cursor<ARGBType> destination = copy.cursor();
			while (source.hasNext()) {
				source.fwd();
				destination.fwd();
				destination.get().set((int) source.get().getIntegerLong());
			}
			return datasetService.create(new ImgPlus<ARGBType>(
				copy, name, new AxisType[] { Axes.X, Axes.Y }));
		}
		finally {
			releaseOutputArray(array, shared);
		}
	}

	private Dataset copyFloatImage(final NDArray array, final String name) {
		final ShmImg<FloatType> shared = new ShmImg<>(array);
		try {
			final Img<FloatType> copy = ArrayImgs.floats(shared.dimensionsAsLongArray());
			ImgUtil.copy(shared, copy);
			return datasetService.create(new ImgPlus<FloatType>(
				copy, name, new AxisType[] { Axes.X, Axes.Y }));
		}
		finally {
			releaseOutputArray(array, shared);
		}
	}

	private static void releaseOutputArray(final NDArray array, final ShmImg<?> shared) {
		array.shm().unlinkOnClose(true);
		shared.close();
	}

	private File configuredPythonEnvironment() {
		String path = prefService.get(ConfigurePythonEnvironmentCommand.class,
			ConfigurePythonEnvironmentCommand.PREFERENCE_KEY);
		if (path == null || path.isBlank() ||
			!ConfigurePythonEnvironmentCommand.isPythonEnvironment(new File(path)))
		{
			try {
				commandService.run(ConfigurePythonEnvironmentCommand.class, true).get();
			}
			catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while configuring the tme-quant environment.", e);
			}
			catch (final ExecutionException e) {
				throw new IllegalStateException("Could not configure the tme-quant environment.", e.getCause());
			}
			catch (final CancellationException e) {
				// Check the preference below to report cancellation consistently.
			}
			path = prefService.get(ConfigurePythonEnvironmentCommand.class,
				ConfigurePythonEnvironmentCommand.PREFERENCE_KEY);
		}
		if (path == null || path.isBlank() ||
			!ConfigurePythonEnvironmentCommand.isPythonEnvironment(new File(path)))
		{
			throw new IllegalStateException("No valid Python environment configured. Select the tme-quant .venv in Plugins > CurveAlign > Configure.");
		}
		return new File(path);
	}

	private void validateInputs(final File pythonEnvironment) {
		if (inputImage.numDimensions() != 2 || !(inputImage.getType() instanceof RealType<?>)) {
			throw new IllegalArgumentException("Input must be a 2D scalar image.");
		}
		if (useBoundaryMask && boundaryMask == null) {
			throw new IllegalArgumentException("Select a boundary mask or disable Use boundary mask.");
		}
		if (useBoundaryMask && (boundaryMask.numDimensions() != 2 ||
			!(boundaryMask.getType() instanceof RealType<?>) ||
			boundaryMask.dimension(0) != inputImage.dimension(0) ||
			boundaryMask.dimension(1) != inputImage.dimension(1)))
		{
			throw new IllegalArgumentException("Boundary mask must be a 2D scalar image matching the input dimensions.");
		}
		if (!ConfigurePythonEnvironmentCommand.isPythonEnvironment(pythonEnvironment)) {
			throw new IllegalArgumentException("Python environment directory does not exist: " + pythonEnvironment);
		}
		if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()) {
			throw new IllegalArgumentException("Could not create output directory: " + outputDirectory);
		}
		if (fiberMode != 0 && (fireDirectory == null || !fireDirectory.isDirectory())) {
			throw new IllegalArgumentException("FIRE modes require a CT-FIRE results directory.");
		}
	}

	private static Img<FloatType> asFloatImage(final Dataset dataset) {
		final RandomAccessibleInterval<?> source = dataset.getImgPlus();
		final Img<FloatType> copy = ArrayImgs.floats(source.dimensionsAsLongArray());
		final Cursor<?> cursor = dataset.getImgPlus().localizingCursor();
		final RandomAccess<FloatType> access = copy.randomAccess();
		while (cursor.hasNext()) {
			cursor.fwd();
			for (int d = 0; d < source.numDimensions(); d++) {
				access.setPosition(cursor.getLongPosition(d) - source.min(d), d);
			}
			access.get().setReal(((RealType<?>) cursor.get()).getRealDouble());
		}
		return copy;
	}

	private String imageName() {
		String name = inputImage.getName();
		if (name == null || name.isBlank()) return "image";
		final int extension = name.lastIndexOf('.');
		if (extension > 0) name = name.substring(0, extension);
		return new File(name).getName();
	}

	private static String readWorkerScript() throws IOException {
		try (InputStream stream = CurveletsCommand.class.getResourceAsStream("run_curvealign.py")) {
			if (stream == null) throw new IOException("Missing run_curvealign.py resource");
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}