package sc.fiji.curvealign;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apposed.appose.Appose;
import org.apposed.appose.Service;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import net.imagej.Dataset;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.ShmImg;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

@Plugin(type = Command.class, menuPath = "Plugins>CurveAlign>Run")
public class CurveAlignCommand implements Command {
	@Parameter
	private Dataset inputImage;

	@Parameter(label = "Boundary mask", required = false)
	private Dataset boundaryMask;

	@Parameter(label = "tme-quant Python environment", style = "directory")
	private File pythonEnvironment;

	@Parameter(label = "Output directory", style = "directory")
	private File outputDirectory;

	@Parameter(label = "Curvelet coefficients to keep", min = "0.0", max = "1.0")
	private double keep = 0.1;

	@Parameter(label = "Fiber mode (0=curvelet, 1-3=FIRE)", min = "0", max = "3")
	private int fiberMode;

	@Parameter(label = "CT-FIRE results directory", style = "directory", required = false)
	private File fireDirectory;

	@Parameter(label = "Boundary distance threshold (pixels)", min = "0")
	private double distanceThreshold = 100.0;

	@Parameter(type = ItemIO.OUTPUT)
	private Dataset overlay;

	@Parameter(type = ItemIO.OUTPUT, label = "Fiber summary")
	private String summary;

	@Parameter
	private IOService ioService;

	@Parameter(required = false)
	private UIService uiService;

	@Override
	public void run() {
		validateInputs();
		final Img<FloatType> image = asFloatImage(inputImage);
		final ShmImg<FloatType> sharedImage = ShmImg.copyOf(image);
		final ShmImg<FloatType> sharedMask = boundaryMask == null ? null : ShmImg.copyOf(asFloatImage(boundaryMask));
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
			task.start();
			task.waitFor();
			if (task.status != TaskStatus.COMPLETE) {
				throw new IllegalStateException("CurveAlign Python task failed: " + task.error);
			}
			overlay = (Dataset) ioService.open((String) task.outputs.get("overlay_path"));
			summary = (String) task.outputs.get("summary");
			if (uiService != null) uiService.show(overlay);
		}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("CurveAlign was interrupted", e);
		}
		catch (final Exception e) {
			throw new RuntimeException("CurveAlign failed", e);
		}
		finally {
			sharedImage.close();
			if (sharedMask != null) sharedMask.close();
		}
	}

	private void validateInputs() {
		if (inputImage.numDimensions() != 2 || !(inputImage.getType() instanceof RealType<?>)) {
			throw new IllegalArgumentException("Input must be a 2D scalar image.");
		}
		if (boundaryMask != null && (boundaryMask.numDimensions() != 2 ||
			!(boundaryMask.getType() instanceof RealType<?>) ||
			boundaryMask.dimension(0) != inputImage.dimension(0) ||
			boundaryMask.dimension(1) != inputImage.dimension(1)))
		{
			throw new IllegalArgumentException("Boundary mask must be a 2D scalar image matching the input dimensions.");
		}
		if (!pythonEnvironment.isDirectory()) {
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
		try (InputStream stream = CurveAlignCommand.class.getResourceAsStream("run_curvealign.py")) {
			if (stream == null) throw new IOException("Missing run_curvealign.py resource");
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}