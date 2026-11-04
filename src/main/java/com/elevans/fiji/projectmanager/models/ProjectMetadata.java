package com.elevans.fiji.projectmanager.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable record representing serializable OME metadata for a ProjectImage.
 * Contains only JSON-serializable fields extracted from full OME metadata.
 */
public record ProjectMetadata(
	// Dimensions
	Integer sizeX,
	Integer sizeY,
	Integer sizeZ,
	Integer sizeC,
	Integer sizeT,
	String pixelType,
	Double pixelPhysicalSizeX,
	Double pixelPhysicalSizeY,
	Double pixelPhysicalSizeZ,
	
	// Channel Information
	List<ChannelMetadata> channels,
	
	// Acquisition Information
	Map<String, String> acquisitionMetadata,
	
	// Microscope Information
	Map<String, String> microscopeInfo,
	
	// Experimenter Information
	Map<String, String> experimenterInfo
) implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Immutable record for channel-specific metadata.
	 */
	public record ChannelMetadata(
		Integer channelIndex,
		String channelName,
		String fluor,
		Integer emissionWavelength,
		Integer excitationWavelength
	) implements Serializable {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Factory method to create an empty ProjectMetadata with basic dimensions.
	 */
	public static ProjectMetadata create(
		int sizeX, int sizeY, int sizeZ, int sizeC, int sizeT, String pixelType
	) {
		return new ProjectMetadata(
			sizeX, sizeY, sizeZ, sizeC, sizeT,
			pixelType,
			null, null, null,
			new ArrayList<>(),
			new HashMap<>(),
			new HashMap<>(),
			new HashMap<>()
		);
	}

	/**
	 * Returns a copy with updated channels.
	 */
	public ProjectMetadata withChannels(List<ChannelMetadata> newChannels) {
		return new ProjectMetadata(
			sizeX, sizeY, sizeZ, sizeC, sizeT,
			pixelType,
			pixelPhysicalSizeX, pixelPhysicalSizeY, pixelPhysicalSizeZ,
			newChannels,
			acquisitionMetadata,
			microscopeInfo,
			experimenterInfo
		);
	}

	/**
	 * Returns a copy with updated acquisition metadata.
	 */
	public ProjectMetadata withAcquisitionMetadata(Map<String, String> newMetadata) {
		return new ProjectMetadata(
			sizeX, sizeY, sizeZ, sizeC, sizeT,
			pixelType,
			pixelPhysicalSizeX, pixelPhysicalSizeY, pixelPhysicalSizeZ,
			channels,
			newMetadata,
			microscopeInfo,
			experimenterInfo
		);
	}

	/**
	 * Returns a copy with updated microscope info.
	 */
	public ProjectMetadata withMicroscopeInfo(Map<String, String> newInfo) {
		return new ProjectMetadata(
			sizeX, sizeY, sizeZ, sizeC, sizeT,
			pixelType,
			pixelPhysicalSizeX, pixelPhysicalSizeY, pixelPhysicalSizeZ,
			channels,
			acquisitionMetadata,
			newInfo,
			experimenterInfo
		);
	}

	/**
	 * Returns a copy with updated experimenter info.
	 */
	public ProjectMetadata withExperimenterInfo(Map<String, String> newInfo) {
		return new ProjectMetadata(
			sizeX, sizeY, sizeZ, sizeC, sizeT,
			pixelType,
			pixelPhysicalSizeX, pixelPhysicalSizeY, pixelPhysicalSizeZ,
			channels,
			acquisitionMetadata,
			microscopeInfo,
			newInfo
		);
	}

	@Override
	public String toString() {
		return String.format("ProjectMetadata(dims=%dx%dx%dx%dx%d, type=%s, channels=%d)",
			sizeX, sizeY, sizeZ, sizeC, sizeT, pixelType, channels.size());
	}
}
