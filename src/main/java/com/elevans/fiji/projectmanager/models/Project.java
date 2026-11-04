package com.elevans.fiji.projectmanager.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable record representing a Project.
 * Contains project metadata and a list of associated images.
 */
public record Project(
	String id,
	String name,
	String description,
	ExperimentType experimentType,
	LocalDateTime createdDate,
	LocalDateTime modifiedDate,
	List<ProjectImage> images
) implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum ExperimentType {
		FRET("FRET"),
		TIMELAPSE("TimeLapse"),
		FLUORESCENCE_LIFETIME("FluorescenceLifetime"),
		IMMUNO("Immuno"),
		FISH("FISH"),
		OTHER("Other");

		private final String displayName;

		ExperimentType(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}
	}

	/**
	 * Factory method to create a new Project with default values.
	 */
	public static Project create(String name, String description, ExperimentType experimentType) {
		return new Project(
			UUID.randomUUID().toString(),
			name,
			description,
			experimentType,
			LocalDateTime.now(),
			LocalDateTime.now(),
			new ArrayList<>()
		);
	}

	/**
	 * Returns a copy of this Project with updated properties.
	 */
	public Project withName(String newName) {
		return new Project(id, newName, description, experimentType, createdDate, LocalDateTime.now(), images);
	}

	public Project withDescription(String newDescription) {
		return new Project(id, name, newDescription, experimentType, createdDate, LocalDateTime.now(), images);
	}

	public Project withExperimentType(ExperimentType newType) {
		return new Project(id, name, description, newType, createdDate, LocalDateTime.now(), images);
	}

	public Project withImages(List<ProjectImage> newImages) {
		return new Project(id, name, description, experimentType, createdDate, LocalDateTime.now(), newImages);
	}

	@Override
	public String toString() {
		return String.format("Project(id=%s, name=%s, type=%s, images=%d)", 
			id, name, experimentType.getDisplayName(), images.size());
	}
}
