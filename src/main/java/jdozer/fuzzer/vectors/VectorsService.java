/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jdozer.fuzzer.vectors.dto.Operation;
import jdozer.fuzzer.vectors.dto.OperationParameter;
import jdozer.fuzzer.vectors.storage.KeyManager;
import jdozer.fuzzer.vectors.storage.StorageService;
import jdozer.fuzzer.vectors.storage.entity.Vector;

@ApplicationScoped
public class VectorsService {

    @Inject
    private FuzzerService fuzzerService;

    @Inject
    private StorageService storageService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final KeyManager keyManager = new KeyManager();

    public List<Operation> createVectors(UUID fuzzerId) throws Exception {
        try {

            this.fuzzerService.setFuzzerId(fuzzerId);
            List<Operation> operations = fuzzerService.getOperations();

            operations.forEach(operation -> {
                List<OperationParameter> operationParameters = fuzzerService.getParameters(operation);
                operation.setParameters(operationParameters);
                if (Log.isTraceEnabled()) {
                    Log.trace("VectorsService.createVectors():\nOperationName: "
                            + operation.getName() + "\n(parameters)"
                            + operation.getParameters().stream().filter(p -> !Objects.isNull(p)).map(p -> {
                                return "\n" + p.getContext() + ": " + String.join(", ", p.getPathStrAttributes());
                            }).collect(Collectors.joining("\n")));
                }
            });

            operations.forEach(operation -> {
                operation.getParameters().forEach(parameter -> {
                    int cant = this.getVectorsCant(parameter.getPathStrAttributes());
                    parameter.setVectors(this.getVectors(cant));
                    parameter.setDummy(this.getDummyValid(operation.getName(), parameter.getContext()));
                });
                if (Log.isTraceEnabled()) {
                    Log.trace("VectorsService.createVectors():\nOperationName: "
                            + operation.getName() + "\n(vectors)\n"
                            + operation.getParameters().stream().map(p -> {
                                return p.getContext() + ":\n" + p.getVectors().size();
                            }).collect(Collectors.joining("\n")));
                }
            });

            operations.forEach(operation -> {
                operation.getParameters().forEach(parameter -> {
                    parameter.getPathStrAttributes().forEach(attr -> {
                        parameter.getVectors().forEach(v -> {
                            try {
                                UUID uuid = UUID.randomUUID();
                                String vectorDummy = this.buildVector(v, parameter.getDummy(), attr, uuid);
                                this.vectorSave(operation.getName(), parameter.getContext(), uuid, vectorDummy);
                            } catch (VectorException e) {
                                Log.trace("Create dummy vector failed! " + e.getMessage());
                                Log.trace(operation.toString());
                            }
                        });
                    });
                });
            });

            return operations;

        } catch (Exception e) {
            Log.error("VectorsService.createVectors(): " + e.getMessage());
            throw new Exception("Created Vectors failed!");
        }
    }

    private String vectorSave(String operationName, String type, UUID uuid, String dummyShot) {
        String key = this.keyManager.forFake(this.fuzzerService.getFuzzerId(), operationName, type, uuid);
        this.storageService.save(key, dummyShot);
        Log.trace("VectorService.vectorSave(): " + key);
        return key;
    }

    private JsonNode buildDummyVector(JsonNode data, JsonPointer path, String valueBase64) throws VectorException {

        JsonNode dataNode = data;
        JsonPointer jsonPointer = path;
        Base64.Decoder decoder = Base64.getDecoder();
        String decoVal = new String(decoder.decode(valueBase64.toString()));
        JsonNode currentNode = new TextNode(decoVal.toString());

        while (jsonPointer.length() > 0) {

            if (dataNode.at(jsonPointer.head()).isMissingNode()) {
                if (isIntoArrayNode(jsonPointer)) {
                    currentNode = objectMapper.createArrayNode().add(currentNode);
                    jsonPointer = jsonPointer.head();
                } else {
                    currentNode = objectMapper.createObjectNode().set(getNodeName(jsonPointer), currentNode);
                    jsonPointer = jsonPointer.head();
                }
            }

            if (!dataNode.at(jsonPointer.head()).isMissingNode()) {
                JsonNode parentNode = dataNode.at(jsonPointer.head());
                if (parentNode.isArray()) {
                    ((ArrayNode) parentNode).add(currentNode);
                } else {
                    ((ObjectNode) parentNode).set(getNodeName(jsonPointer), currentNode);
                }
                break;
            }
        }

        return dataNode;
    }

    private String buildVector(Vector vector, String dummy, String path, UUID uuid) throws VectorException {
        try {

            ObjectNode root = (ObjectNode) objectMapper.readTree(dummy);
            JsonPointer pointer = this.jsonPointer(path);

            JsonNode data = this.getDecoderData(root);
            JsonNode dataVector = this.buildDummyVector(data, pointer, vector.getScript());
            root = this.setEncodeData(root, dataVector);

            root.replace("valid", BooleanNode.FALSE);
            root.replace("message", new TextNode(vector.getSummary()));
            root.replace("id", new TextNode(uuid.toString()));
            root.replace("property", new TextNode(path.toString()));

            return root.toString();

        } catch (Exception e) {
            Log.trace("VectorsService.buildVector(): Exception: " + e.getMessage());
            throw new VectorException("Vector creation failed!");
        }
    }

    private ObjectNode setEncodeData(ObjectNode root, JsonNode data) throws VectorException {
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            String enco = encoder.encodeToString(data.toString().getBytes());
            root.replace("data", new TextNode(enco));
            return root;
        } catch (Exception e) {
            Log.error("VectorsService.encodeData(): Exception: " + e.getMessage());
            throw new VectorException("The data could not be encoded.");
        }
    }

    private JsonNode getDecoderData(ObjectNode jsonDummy) throws VectorException {
        try {
            TextNode dataNode = (TextNode) jsonDummy.at("/data");
            String content = dataNode.textValue();
            Base64.Decoder decoder = Base64.getDecoder();
            return objectMapper.readTree(decoder.decode(content.toString()));
        } catch (Exception e) {
            Log.error("VectorService.getDecoderData(): Exception: " + e.getMessage());
            throw new VectorException("Decode data failed!");
        }
    }

    private String getNodeName(JsonPointer currentPointer) {
        return currentPointer.last().toString().replaceAll("/", "");
    }

    private Boolean isIntoArrayNode(JsonPointer currentPointer) {
        String current = currentPointer.last().toString();
        if (current.matches("(^\\/\\d)$")) {
            return true;
        }
        return false;
    }

    private JsonPointer jsonPointer(String jsonPath) {
        List<String> parts = new LinkedList<>(Arrays.asList(jsonPath.replaceAll("'|^\\$", "").split("\\[|\\]\\[|\\]")));
        parts.removeAll(List.of(""));
        return JsonPointer.compile("/".concat(String.join("/", parts)));
    }

    private String getDummyValid(String operationName, String context) {
        try {
            return this.storageService.getDummyValid(this.fuzzerService.getFuzzerId(), operationName, context);
        } catch (RuntimeException e) {
            Log.warn("VectorsService.getDummyVector(): " + e.getMessage());
            return null;
        }
    }

    private List<Vector> getVectors(int cant) {
        return this.storageService.getRndFuzzVector(cant);
    }

    private int getCantDummy(String operationName, String context) {
        return this.storageService.getDummyCant(this.fuzzerService.getFuzzerId(), operationName, context);
    }

    /**
     * Temporal implementation
     * 
     * @param pathAtributes
     * @return
     */
    private int getVectorsCant(List<String> pathAtributes) {
        return pathAtributes.size() * 10;
    }

}
