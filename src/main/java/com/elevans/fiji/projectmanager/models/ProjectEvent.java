package com.elevans.fiji.projectmanager.models;

import java.util.EventObject;

/**
 * Base event for project state changes.
 * Subclasses provide specific event information.
 */
public abstract sealed class ProjectEvent extends EventObject 
	permits ProjectEvent.ProjectCreatedEvent, ProjectEvent.ProjectClosedEvent, 
	        ProjectEvent.ImageAddedEvent, ProjectEvent.ImageRemovedEvent, 
	        ProjectEvent.ProjectUpdatedEvent, ProjectEvent.ImagesReorderedEvent {

	private static final long serialVersionUID = 1L;

	public ProjectEvent(Object source) {
		super(source);
	}

	/**
	 * Event fired when a new project is created.
	 */
	public static final class ProjectCreatedEvent extends ProjectEvent {
		private static final long serialVersionUID = 1L;
		private final Project project;

		public ProjectCreatedEvent(Object source, Project project) {
			super(source);
			this.project = project;
		}

		public Project getProject() {
			return project;
		}
	}

	/**
	 * Event fired when a project is closed.
	 */
	public static final class ProjectClosedEvent extends ProjectEvent {
		private static final long serialVersionUID = 1L;
		private final String projectId;

		public ProjectClosedEvent(Object source, String projectId) {
			super(source);
			this.projectId = projectId;
		}

		public String getProjectId() {
			return projectId;
		}
	}

	/**
	 * Event fired when an image is added to the project.
	 */
	public static final class ImageAddedEvent extends ProjectEvent {
		private static final long serialVersionUID = 1L;
		private final ProjectImage image;

		public ImageAddedEvent(Object source, ProjectImage image) {
			super(source);
			this.image = image;
		}

		public ProjectImage getImage() {
			return image;
		}
	}

	/**
	 * Event fired when an image is removed from the project.
	 */
	public static final class ImageRemovedEvent extends ProjectEvent {
		private static final long serialVersionUID = 1L;
		private final String imageId;

		public ImageRemovedEvent(Object source, String imageId) {
			super(source);
			this.imageId = imageId;
		}

		public String getImageId() {
			return imageId;
		}
	}

	/**
	 * Event fired when project properties are updated.
	 */
	public static final class ProjectUpdatedEvent extends ProjectEvent {
		private static final long serialVersionUID = 1L;
		private final Project project;

		public ProjectUpdatedEvent(Object source, Project project) {
			super(source);
			this.project = project;
		}

		public Project getProject() {
			return project;
		}
	}

	/**
	 * Event fired when images are reordered within the project.
	 */
	public static final class ImagesReorderedEvent extends ProjectEvent {
		private static final long serialVersionUID = 1L;
		private final java.util.List<ProjectImage> reorderedImages;

		public ImagesReorderedEvent(Object source, java.util.List<ProjectImage> reorderedImages) {
			super(source);
			this.reorderedImages = reorderedImages;
		}

		public java.util.List<ProjectImage> getReorderedImages() {
			return reorderedImages;
		}
	}
}
