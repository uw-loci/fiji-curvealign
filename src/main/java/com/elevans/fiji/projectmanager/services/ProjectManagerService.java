package com.elevans.fiji.projectmanager.services;

import com.elevans.fiji.projectmanager.models.Project;
import com.elevans.fiji.projectmanager.models.Project.ExperimentType;
import com.elevans.fiji.projectmanager.models.ProjectEvent;
import com.elevans.fiji.projectmanager.models.ProjectImage;
import com.elevans.fiji.projectmanager.models.ProjectMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Central service managing project state.
 * Maintains the current project and notifies listeners of state changes.
 */
public class ProjectManagerService {

	private static final Logger log = LoggerFactory.getLogger(ProjectManagerService.class);

	private Project currentProject;
	private final OmeMetadataExtractor metadataExtractor;
	private final List<Consumer<ProjectEvent>> listeners = new CopyOnWriteArrayList<>();

	public ProjectManagerService() {
		this.metadataExtractor = new OmeMetadataExtractor();
	}

	// -- Listeners --

	public void addListener(Consumer<ProjectEvent> listener) {
		listeners.add(listener);
	}

	public void removeListener(Consumer<ProjectEvent> listener) {
		listeners.remove(listener);
	}

	private void fireEvent(ProjectEvent event) {
		for (Consumer<ProjectEvent> listener : listeners) {
			try {
				listener.accept(event);
			} catch (Exception e) {
				log.error("Error in project event listener", e);
			}
		}
	}

	// -- Project Lifecycle --

	/**
	 * Create a new project, replacing any existing one.
	 */
	public Project createProject(String name, String description, ExperimentType type) {
		currentProject = Project.create(name, description, type);
		log.info("Created project: {}", currentProject);
		fireEvent(new ProjectEvent.ProjectCreatedEvent(this, currentProject));
		return currentProject;
	}

	/**
	 * Close the current project.
	 */
	public void closeProject() {
		if (currentProject != null) {
			String id = currentProject.id();
			currentProject = null;
			log.info("Closed project: {}", id);
			fireEvent(new ProjectEvent.ProjectClosedEvent(this, id));
		}
	}

	/**
	 * Set the current project (used when loading from file).
	 */
	public void setProject(Project project) {
		this.currentProject = project;
		log.info("Loaded project: {}", project);
		fireEvent(new ProjectEvent.ProjectCreatedEvent(this, project));
	}

	/**
	 * Get the current project, if any.
	 */
	public Optional<Project> getCurrentProject() {
		return Optional.ofNullable(currentProject);
	}

	// -- Image Management --

	/**
	 * Add an image to the current project by file path.
	 * Extracts OME metadata from the file.
	 *
	 * @param filePath  absolute path to the image file
	 * @param imageName display name for the image
	 * @return the created ProjectImage, or empty if no project is open
	 */
	public Optional<ProjectImage> addImage(String filePath, String imageName) {
		if (currentProject == null) {
			log.warn("Cannot add image: no project is open.");
			return Optional.empty();
		}

		ProjectMetadata metadata = metadataExtractor.extract(filePath);
		int nextIndex = currentProject.images().size();
		ProjectImage image = ProjectImage.create(filePath, imageName, metadata, nextIndex);

		List<ProjectImage> updatedImages = new ArrayList<>(currentProject.images());
		updatedImages.add(image);
		currentProject = currentProject.withImages(updatedImages);

		log.info("Added image to project: {}", image);
		fireEvent(new ProjectEvent.ImageAddedEvent(this, image));
		return Optional.of(image);
	}

	/**
	 * Remove an image from the current project by ID.
	 */
	public boolean removeImage(String imageId) {
		if (currentProject == null) return false;

		List<ProjectImage> updatedImages = new ArrayList<>(currentProject.images());
		boolean removed = updatedImages.removeIf(img -> img.id().equals(imageId));
		if (removed) {
			// Re-index remaining images
			for (int i = 0; i < updatedImages.size(); i++) {
				updatedImages.set(i, updatedImages.get(i).withOrderIndex(i));
			}
			currentProject = currentProject.withImages(updatedImages);
			log.info("Removed image {} from project", imageId);
			fireEvent(new ProjectEvent.ImageRemovedEvent(this, imageId));
		}
		return removed;
	}

	/**
	 * Reorder images in the project. The list should contain all image IDs in the desired order.
	 */
	public void reorderImages(List<String> imageIdsInOrder) {
		if (currentProject == null) return;

		List<ProjectImage> current = currentProject.images();
		List<ProjectImage> reordered = new ArrayList<>();
		for (int i = 0; i < imageIdsInOrder.size(); i++) {
			String id = imageIdsInOrder.get(i);
			final int index = i;
			current.stream()
				.filter(img -> img.id().equals(id))
				.findFirst()
				.ifPresent(img -> reordered.add(img.withOrderIndex(index)));
		}
		currentProject = currentProject.withImages(reordered);
		log.info("Reordered {} images", reordered.size());
		fireEvent(new ProjectEvent.ImagesReorderedEvent(this, reordered));
	}

	/**
	 * Set the group key for an image.
	 */
	public void setImageGroup(String imageId, String groupKey) {
		if (currentProject == null) return;

		List<ProjectImage> updated = new ArrayList<>(currentProject.images());
		for (int i = 0; i < updated.size(); i++) {
			if (updated.get(i).id().equals(imageId)) {
				updated.set(i, updated.get(i).withGroupKey(groupKey));
				break;
			}
		}
		currentProject = currentProject.withImages(updated);
		fireEvent(new ProjectEvent.ProjectUpdatedEvent(this, currentProject));
	}

	// -- Project Properties --

	public void updateName(String name) {
		if (currentProject == null) return;
		currentProject = currentProject.withName(name);
		fireEvent(new ProjectEvent.ProjectUpdatedEvent(this, currentProject));
	}

	public void updateDescription(String description) {
		if (currentProject == null) return;
		currentProject = currentProject.withDescription(description);
		fireEvent(new ProjectEvent.ProjectUpdatedEvent(this, currentProject));
	}

	public void updateExperimentType(ExperimentType type) {
		if (currentProject == null) return;
		currentProject = currentProject.withExperimentType(type);
		fireEvent(new ProjectEvent.ProjectUpdatedEvent(this, currentProject));
	}
}
