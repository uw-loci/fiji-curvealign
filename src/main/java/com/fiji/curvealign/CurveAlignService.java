package com.fiji.curvealign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ImageProcessor;
import org.apache.commons.io.IOUtils;
import org.apposed.appose.Appose;
import org.apposed.appose.BuildException;
import org.apposed.appose.Environment;
import org.apposed.appose.Service;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;
import org.apposed.appose.TaskException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CurveAlignService {

	private static final ObjectMapper JSON = new ObjectMapper();
	private static final String RESOURCE_ROOT = "/com/fiji/curvealign/";

	private CurveAlignService() {
	}

	static List<Map<String, Object>> analyze(final ImagePlus image, final double keep,
			final int scale, final double radius, final int minimumNearestFibers,
			final int minimumBoxSize) throws IOException, InterruptedException, TaskException {
		Path temporaryDirectory = Files.createTempDirectory("fiji-curvealign-");
		Path imageFile = temporaryDirectory.resolve("input.tif");
		Path resultsFile = temporaryDirectory.resolve("results.json");
		try {
			ImageProcessor processor = image.getProcessor().convertToFloatProcessor();
			ImagePlus plane = new ImagePlus(image.getShortTitle(), processor);
			if (!new FileSaver(plane).saveAsTiff(imageFile.toString())) {
				throw new IOException("Could not write the active image plane to a temporary TIFF.");
			}

			String pixiToml = IOUtils.toString(CurveAlignService.class.getResource(RESOURCE_ROOT + "pixi.toml"), StandardCharsets.UTF_8);
			String script = IOUtils.toString(CurveAlignService.class.getResource(RESOURCE_ROOT + "analyze.py"), StandardCharsets.UTF_8);
			Environment environment;
			try {
				environment = Appose.pixi().content(pixiToml).build();
			} catch (BuildException error) {
				throw new IOException("Could not build the tme-quant Python environment.", error);
			}
			try {
				try (Service python = environment.activate("default").python()) {
					Map<String, Object> inputs = new HashMap<>();
					inputs.put("image_path", imageFile.toString());
					inputs.put("results_path", resultsFile.toString());
					inputs.put("keep", keep);
					inputs.put("scale", scale);
					inputs.put("radius", radius);
					inputs.put("minimum_nearest_fibers", minimumNearestFibers);
					inputs.put("minimum_box_size", minimumBoxSize);

					Task task = python.task(script, inputs);
					task.start();
					task.waitFor();
					if (task.status != TaskStatus.COMPLETE) {
						throw new TaskException("tme-quant analysis failed: " + task.error, task);
					}
					return JSON.readValue(resultsFile.toFile(), new TypeReference<>() {});
				}
			} catch (BuildException error) {
				throw new IOException("Could not activate the tme-quant Python environment.", error);
			}
		} finally {
			Files.deleteIfExists(resultsFile);
			Files.deleteIfExists(imageFile);
			Files.deleteIfExists(temporaryDirectory);
		}
	}
}