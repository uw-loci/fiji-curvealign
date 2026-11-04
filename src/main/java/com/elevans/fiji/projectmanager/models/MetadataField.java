package com.elevans.fiji.projectmanager.models;

import java.io.Serializable;

/**
 * Immutable record representing a single metadata field for UI display.
 * Organizes metadata by category for tree view presentation.
 */
public record MetadataField(
	String category,
	String name,
	String value
) implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum Category {
		DIMENSIONS("Dimensions"),
		CHANNELS("Channels"),
		ACQUISITION("Acquisition"),
		MICROSCOPE("Microscope"),
		EXPERIMENTER("Experimenter"),
		OTHER("Other");

		private final String displayName;

		Category(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}
	}

	@Override
	public String toString() {
		return String.format("%s: %s", name, value);
	}
}
