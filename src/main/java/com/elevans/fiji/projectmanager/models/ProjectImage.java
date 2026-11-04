package com.elevans.fiji.projectmanager.models;

import java.io.Serializable;
import java.util.UUID;

/**
 * Immutable record representing an image in a Project.
 * Stores file path, descriptive metadata, OME metadata, and ordering information.
 */
public record ProjectImage(
	String id,
	String filePath,
	String imageName,
	ProjectMetadata omeMetadata,
	int orderIndex,
	String groupKey
) implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Factory method to create a new ProjectImage.
	 */
	public static ProjectImage create(
		String filePath,
		String imageName,
		ProjectMetadata omeMetadata,
		int orderIndex
	) {
		return new ProjectImage(
			UUID.randomUUID().toString(),
			filePath,
			imageName,
			omeMetadata,
			orderIndex,
			null
		);
	}

	/**
	 * Returns a copy of this ProjectImage with an updated order index.
	 */
	public ProjectImage withOrderIndex(int newIndex) {
		return new ProjectImage(id, filePath, imageName, omeMetadata, newIndex, groupKey);
	}

	/**
	 * Returns a copy of this ProjectImage with an updated group key (for grouping by channel/timepoint).
	 */
	public ProjectImage withGroupKey(String newGroupKey) {
		return new ProjectImage(id, filePath, imageName, omeMetadata, orderIndex, newGroupKey);
	}

	@Override
	public String toString() {
		return String.format("ProjectImage(id=%s, name=%s, path=%s, order=%d)", 
			id, imageName, filePath, orderIndex);
	}
}
