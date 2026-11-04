package com.elevans.fiji.projectmanager.io;

import com.elevans.fiji.projectmanager.models.Project;
import com.elevans.fiji.projectmanager.models.Project.ExperimentType;
import com.elevans.fiji.projectmanager.models.ProjectImage;
import com.elevans.fiji.projectmanager.models.ProjectMetadata;
import com.elevans.fiji.projectmanager.models.ProjectMetadata.ChannelMetadata;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles serialization and deserialization of Project to/from JSON files.
 */
public class ProjectSerializer {

	private static final Logger log = LoggerFactory.getLogger(ProjectSerializer.class);
	private static final String SCHEMA_VERSION = "1.0";
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	private final ObjectMapper mapper;

	public ProjectSerializer() {
		this.mapper = createMapper();
	}

	private ObjectMapper createMapper() {
		ObjectMapper om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		SimpleModule module = new SimpleModule();
		module.addSerializer(Project.class, new ProjectJsonSerializer());
		module.addDeserializer(Project.class, new ProjectJsonDeserializer());
		om.registerModule(module);

		return om;
	}

	/**
	 * Serialize a Project to a JSON file.
	 */
	public void save(Project project, Path filePath) throws IOException {
		String json = mapper.writeValueAsString(project);
		Files.writeString(filePath, json);
		log.info("Saved project to {}", filePath);
	}

	/**
	 * Deserialize a Project from a JSON file.
	 */
	public Project load(Path filePath) throws IOException {
		String json = Files.readString(filePath);
		Project project = mapper.readValue(json, Project.class);
		log.info("Loaded project from {}: {}", filePath, project);
		return project;
	}

	/**
	 * Validate that all image file paths in a project exist on disk.
	 * Returns list of missing file paths.
	 */
	public List<String> validateImagePaths(Project project) {
		List<String> missing = new ArrayList<>();
		for (ProjectImage img : project.images()) {
			if (!Files.exists(Path.of(img.filePath()))) {
				missing.add(img.filePath());
			}
		}
		return missing;
	}

	// -- Custom Serializer --

	private static class ProjectJsonSerializer extends JsonSerializer<Project> {
		@Override
		public void serialize(Project project, JsonGenerator gen, SerializerProvider provider)
				throws IOException {
			gen.writeStartObject();

			gen.writeStringField("schemaVersion", SCHEMA_VERSION);
			gen.writeStringField("id", project.id());
			gen.writeStringField("name", project.name());
			gen.writeStringField("description", project.description());
			gen.writeStringField("experimentType", project.experimentType().name());
			gen.writeStringField("createdDate", project.createdDate().format(DATE_FMT));
			gen.writeStringField("modifiedDate", project.modifiedDate().format(DATE_FMT));

			gen.writeArrayFieldStart("images");
			for (ProjectImage img : project.images()) {
				writeImage(gen, img);
			}
			gen.writeEndArray();

			gen.writeEndObject();
		}

		private void writeImage(JsonGenerator gen, ProjectImage img) throws IOException {
			gen.writeStartObject();
			gen.writeStringField("id", img.id());
			gen.writeStringField("filePath", img.filePath());
			gen.writeStringField("imageName", img.imageName());
			gen.writeNumberField("orderIndex", img.orderIndex());
			if (img.groupKey() != null) {
				gen.writeStringField("groupKey", img.groupKey());
			}

			if (img.omeMetadata() != null) {
				gen.writeObjectFieldStart("omeMetadata");
				writeMetadata(gen, img.omeMetadata());
				gen.writeEndObject();
			}

			gen.writeEndObject();
		}

		private void writeMetadata(JsonGenerator gen, ProjectMetadata meta) throws IOException {
			// Dimensions
			gen.writeObjectFieldStart("dimensions");
			writeNullableInt(gen, "sizeX", meta.sizeX());
			writeNullableInt(gen, "sizeY", meta.sizeY());
			writeNullableInt(gen, "sizeZ", meta.sizeZ());
			writeNullableInt(gen, "sizeC", meta.sizeC());
			writeNullableInt(gen, "sizeT", meta.sizeT());
			if (meta.pixelType() != null) gen.writeStringField("pixelType", meta.pixelType());
			writeNullableDouble(gen, "physicalSizeX", meta.pixelPhysicalSizeX());
			writeNullableDouble(gen, "physicalSizeY", meta.pixelPhysicalSizeY());
			writeNullableDouble(gen, "physicalSizeZ", meta.pixelPhysicalSizeZ());
			gen.writeEndObject();

			// Channels
			gen.writeArrayFieldStart("channels");
			for (ChannelMetadata ch : meta.channels()) {
				gen.writeStartObject();
				gen.writeNumberField("index", ch.channelIndex());
				if (ch.channelName() != null) gen.writeStringField("name", ch.channelName());
				if (ch.fluor() != null) gen.writeStringField("fluor", ch.fluor());
				writeNullableInt(gen, "emissionWavelength", ch.emissionWavelength());
				writeNullableInt(gen, "excitationWavelength", ch.excitationWavelength());
				gen.writeEndObject();
			}
			gen.writeEndArray();

			// Acquisition, microscope, experimenter
			writeStringMap(gen, "acquisition", meta.acquisitionMetadata());
			writeStringMap(gen, "microscope", meta.microscopeInfo());
			writeStringMap(gen, "experimenter", meta.experimenterInfo());
		}

		private void writeNullableInt(JsonGenerator gen, String field, Integer val) throws IOException {
			if (val != null) gen.writeNumberField(field, val);
		}

		private void writeNullableDouble(JsonGenerator gen, String field, Double val) throws IOException {
			if (val != null) gen.writeNumberField(field, val);
		}

		private void writeStringMap(JsonGenerator gen, String fieldName, Map<String, String> map) throws IOException {
			if (map != null && !map.isEmpty()) {
				gen.writeObjectFieldStart(fieldName);
				for (Map.Entry<String, String> entry : map.entrySet()) {
					gen.writeStringField(entry.getKey(), entry.getValue());
				}
				gen.writeEndObject();
			}
		}
	}

	// -- Custom Deserializer --

	private static class ProjectJsonDeserializer extends JsonDeserializer<Project> {
		@Override
		public Project deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			JsonNode root = p.getCodec().readTree(p);

			String id = root.get("id").asText();
			String name = root.get("name").asText();
			String description = root.has("description") ? root.get("description").asText() : "";
			ExperimentType type = ExperimentType.valueOf(root.get("experimentType").asText());
			LocalDateTime created = LocalDateTime.parse(root.get("createdDate").asText(), DATE_FMT);
			LocalDateTime modified = LocalDateTime.parse(root.get("modifiedDate").asText(), DATE_FMT);

			List<ProjectImage> images = new ArrayList<>();
			if (root.has("images")) {
				for (JsonNode imgNode : root.get("images")) {
					images.add(readImage(imgNode));
				}
			}

			return new Project(id, name, description, type, created, modified, images);
		}

		private ProjectImage readImage(JsonNode node) {
			String id = node.get("id").asText();
			String filePath = node.get("filePath").asText();
			String imageName = node.get("imageName").asText();
			int orderIndex = node.has("orderIndex") ? node.get("orderIndex").asInt() : 0;
			String groupKey = node.has("groupKey") ? node.get("groupKey").asText() : null;

			ProjectMetadata meta = null;
			if (node.has("omeMetadata")) {
				meta = readMetadata(node.get("omeMetadata"));
			}

			return new ProjectImage(id, filePath, imageName, meta, orderIndex, groupKey);
		}

		private ProjectMetadata readMetadata(JsonNode node) {
			// Dimensions
			JsonNode dims = node.has("dimensions") ? node.get("dimensions") : node;
			Integer sX = getInt(dims, "sizeX");
			Integer sY = getInt(dims, "sizeY");
			Integer sZ = getInt(dims, "sizeZ");
			Integer sC = getInt(dims, "sizeC");
			Integer sT = getInt(dims, "sizeT");
			String pType = getString(dims, "pixelType");
			Double physX = getDouble(dims, "physicalSizeX");
			Double physY = getDouble(dims, "physicalSizeY");
			Double physZ = getDouble(dims, "physicalSizeZ");

			// Channels
			List<ChannelMetadata> channels = new ArrayList<>();
			if (node.has("channels")) {
				for (JsonNode chNode : node.get("channels")) {
					channels.add(new ChannelMetadata(
						getInt(chNode, "index"),
						getString(chNode, "name"),
						getString(chNode, "fluor"),
						getInt(chNode, "emissionWavelength"),
						getInt(chNode, "excitationWavelength")
					));
				}
			}

			Map<String, String> acq = readStringMap(node, "acquisition");
			Map<String, String> micro = readStringMap(node, "microscope");
			Map<String, String> exp = readStringMap(node, "experimenter");

			return new ProjectMetadata(sX, sY, sZ, sC, sT, pType, physX, physY, physZ,
				channels, acq, micro, exp);
		}

		private Map<String, String> readStringMap(JsonNode parent, String field) {
			Map<String, String> map = new HashMap<>();
			if (parent.has(field)) {
				parent.get(field).fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().asText()));
			}
			return map;
		}

		private Integer getInt(JsonNode node, String field) {
			return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : null;
		}

		private Double getDouble(JsonNode node, String field) {
			return node.has(field) && !node.get(field).isNull() ? node.get(field).asDouble() : null;
		}

		private String getString(JsonNode node, String field) {
			return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
		}
	}
}
