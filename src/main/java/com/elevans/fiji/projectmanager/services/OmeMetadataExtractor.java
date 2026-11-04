package com.elevans.fiji.projectmanager.services;

import com.elevans.fiji.projectmanager.models.ProjectMetadata;
import com.elevans.fiji.projectmanager.models.ProjectMetadata.ChannelMetadata;

import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;

import ome.xml.model.primitives.PositiveInteger;
import ome.units.quantity.Length;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts OME metadata from image files using Bio-Formats.
 * Returns a serializable ProjectMetadata record.
 */
public class OmeMetadataExtractor {

	private static final Logger log = LoggerFactory.getLogger(OmeMetadataExtractor.class);

	/**
	 * Extract OME metadata from an image file.
	 *
	 * @param filePath absolute path to the image file
	 * @return ProjectMetadata with extracted fields, or a minimal fallback on failure
	 */
	public ProjectMetadata extract(String filePath) {
		try (IFormatReader reader = createReader(filePath)) {
			IMetadata meta = (IMetadata) reader.getMetadataStore();
			return buildMetadata(meta, 0);
		} catch (Exception e) {
			log.warn("Failed to extract OME metadata from {}: {}", filePath, e.getMessage());
			return buildFallbackMetadata(filePath);
		}
	}

	/**
	 * Creates and initializes a Bio-Formats reader for the given file.
	 */
	private IFormatReader createReader(String filePath) throws Exception {
		IFormatReader reader = new ImageReader();
		IMetadata meta = MetadataTools.createOMEXMLMetadata();
		reader.setMetadataStore(meta);
		reader.setId(filePath);
		return reader;
	}

	/**
	 * Builds ProjectMetadata from parsed OME IMetadata for a given image series.
	 */
	private ProjectMetadata buildMetadata(IMetadata meta, int series) {
		// Dimensions
		Integer sizeX = positiveIntOrNull(meta.getPixelsSizeX(series));
		Integer sizeY = positiveIntOrNull(meta.getPixelsSizeY(series));
		Integer sizeZ = positiveIntOrNull(meta.getPixelsSizeZ(series));
		Integer sizeC = positiveIntOrNull(meta.getPixelsSizeC(series));
		Integer sizeT = positiveIntOrNull(meta.getPixelsSizeT(series));
		String pixelType = meta.getPixelsType(series) != null ? meta.getPixelsType(series).getValue() : null;

		// Physical pixel sizes
		Double physX = lengthToMicrons(meta.getPixelsPhysicalSizeX(series));
		Double physY = lengthToMicrons(meta.getPixelsPhysicalSizeY(series));
		Double physZ = lengthToMicrons(meta.getPixelsPhysicalSizeZ(series));

		// Channels
		List<ChannelMetadata> channels = extractChannels(meta, series);

		// Acquisition metadata
		Map<String, String> acquisitionMeta = extractAcquisitionMetadata(meta, series);

		// Microscope info
		Map<String, String> microscopeInfo = extractMicroscopeInfo(meta);

		// Experimenter info
		Map<String, String> experimenterInfo = extractExperimenterInfo(meta);

		return new ProjectMetadata(
			sizeX, sizeY, sizeZ, sizeC, sizeT,
			pixelType,
			physX, physY, physZ,
			channels,
			acquisitionMeta,
			microscopeInfo,
			experimenterInfo
		);
	}

	private List<ChannelMetadata> extractChannels(IMetadata meta, int series) {
		List<ChannelMetadata> channels = new ArrayList<>();
		int channelCount = meta.getChannelCount(series);
		for (int c = 0; c < channelCount; c++) {
			String name = meta.getChannelName(series, c);
			String fluor = meta.getChannelFluor(series, c);

			Integer emWave = null;
			try {
				Length emLen = meta.getChannelEmissionWavelength(series, c);
				if (emLen != null) emWave = (int) emLen.value().doubleValue();
			} catch (Exception ignored) {}

			Integer exWave = null;
			try {
				Length exLen = meta.getChannelExcitationWavelength(series, c);
				if (exLen != null) exWave = (int) exLen.value().doubleValue();
			} catch (Exception ignored) {}

			channels.add(new ChannelMetadata(c, name, fluor, emWave, exWave));
		}
		return channels;
	}

	private Map<String, String> extractAcquisitionMetadata(IMetadata meta, int series) {
		Map<String, String> acq = new HashMap<>();
		try {
			if (meta.getImageAcquisitionDate(series) != null) {
				acq.put("acquisitionDate", meta.getImageAcquisitionDate(series).getValue());
			}
			if (meta.getImageDescription(series) != null) {
				acq.put("description", meta.getImageDescription(series));
			}
			if (meta.getImageName(series) != null) {
				acq.put("imageName", meta.getImageName(series));
			}
		} catch (Exception e) {
			log.debug("Error extracting acquisition metadata: {}", e.getMessage());
		}
		return acq;
	}

	private Map<String, String> extractMicroscopeInfo(IMetadata meta) {
		Map<String, String> info = new HashMap<>();
		try {
			int instrumentCount = meta.getInstrumentCount();
			if (instrumentCount > 0) {
				if (meta.getMicroscopeModel(0) != null) {
					info.put("microscopeModel", meta.getMicroscopeModel(0));
				}
				if (meta.getMicroscopeManufacturer(0) != null) {
					info.put("microscopeManufacturer", meta.getMicroscopeManufacturer(0));
				}
				int objCount = meta.getObjectiveCount(0);
				if (objCount > 0) {
					if (meta.getObjectiveModel(0, 0) != null) {
						info.put("objectiveModel", meta.getObjectiveModel(0, 0));
					}
					if (meta.getObjectiveNominalMagnification(0, 0) != null) {
						info.put("objectiveMagnification",
							meta.getObjectiveNominalMagnification(0, 0).toString());
					}
					if (meta.getObjectiveLensNA(0, 0) != null) {
						info.put("objectiveNA", meta.getObjectiveLensNA(0, 0).toString());
					}
				}
				int detectorCount = meta.getDetectorCount(0);
				if (detectorCount > 0) {
					if (meta.getDetectorModel(0, 0) != null) {
						info.put("detectorModel", meta.getDetectorModel(0, 0));
					}
					if (meta.getDetectorType(0, 0) != null) {
						info.put("detectorType", meta.getDetectorType(0, 0).getValue());
					}
				}
			}
		} catch (Exception e) {
			log.debug("Error extracting microscope info: {}", e.getMessage());
		}
		return info;
	}

	private Map<String, String> extractExperimenterInfo(IMetadata meta) {
		Map<String, String> info = new HashMap<>();
		try {
			int expCount = meta.getExperimenterCount();
			if (expCount > 0) {
				if (meta.getExperimenterFirstName(0) != null) {
					info.put("firstName", meta.getExperimenterFirstName(0));
				}
				if (meta.getExperimenterLastName(0) != null) {
					info.put("lastName", meta.getExperimenterLastName(0));
				}
				if (meta.getExperimenterEmail(0) != null) {
					info.put("email", meta.getExperimenterEmail(0));
				}
				if (meta.getExperimenterInstitution(0) != null) {
					info.put("institution", meta.getExperimenterInstitution(0));
				}
			}
		} catch (Exception e) {
			log.debug("Error extracting experimenter info: {}", e.getMessage());
		}
		return info;
	}

	/**
	 * Fallback metadata when Bio-Formats extraction fails.
	 */
	private ProjectMetadata buildFallbackMetadata(String filePath) {
		return ProjectMetadata.create(0, 0, 0, 0, 0, "unknown");
	}

	private Integer positiveIntOrNull(PositiveInteger pi) {
		return pi != null ? pi.getValue() : null;
	}

	private Double lengthToMicrons(Length length) {
		if (length == null) return null;
		return length.value().doubleValue();
	}
}
