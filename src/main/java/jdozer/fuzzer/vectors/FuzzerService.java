/**
    # JDozerFuzzer - Microservicio de Discovery
    # Copyright (C) 2024 Cristián Saéz V.
    # Licencia: GNU AGPLv3 (ver LICENSE)
 */
package jdozer.fuzzer.vectors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jdozer.fuzzer.vectors.dto.Operation;
import jdozer.fuzzer.vectors.dto.OperationParameter;
import jdozer.fuzzer.vectors.storage.StorageService;

@ApplicationScoped
public class FuzzerService {

    @Inject
    private StorageService storageService;

    private String fuzzer;
    private UUID fuzzerId;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void setFuzzerId(UUID fuzzerId) throws VectorException {
        this.fuzzerId = fuzzerId;
        this.init();
    }

    private void init() throws VectorException {
        try {
            this.fuzzer = this.storageService.findFuzzer(this.fuzzerId);
            if (Objects.isNull(this.fuzzer) || this.fuzzer.isEmpty()) {
                Log.error("VectorsService.setFuzzerId(): Invalid fuzzer ID: " + fuzzerId);
                throw new VectorException("Fuzzer not found!", "Invalid fuzzer ID: " + fuzzerId);
            }
        } catch (VectorException e) {
            throw e;
        } catch (Exception e) {
            Log.error("VectorsService.setFuzzerId(): " + e.getMessage());
            throw new RuntimeException("VectorsService failed!");
        }
    }

    public List<Operation> getOperations() {
        try {

            List<String> operationIds = JsonPath.read(this.fuzzer, "$.operationIds");
            List<Operation> operations = operationIds.stream()
                    .map(id -> {
                        String name = this.storageService.getOperationsName(id, this.fuzzerId);
                        if (Objects.nonNull(name) && !name.isEmpty()) {
                            Operation operation = new Operation();
                            operation.setName(name);
                            operation.setId(UUID.randomUUID());
                            return operation;
                        }
                        Log.error(
                                "VectorsService.getOperationsNames(): Invalid operation name for operation ID: " + id);
                        return null;
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (Log.isTraceEnabled()) {
                Log.trace("---------------");
                Log.trace("VectorsService.getOperations(): Found "
                        + operations.stream().map(o -> o.getName()).collect(Collectors.joining(", ")));
            }

            return operations;

        } catch (Exception e) {
            Log.error("VectorsService.getOperations(): " + e.getMessage());
            throw new RuntimeException("VectorsService operation fetching failed!");
        }
    }

    public List<OperationParameter> getParameters(Operation operation) {
        try {
            List<String> contextParameters = this.storageService.getContextParameters(getFuzzerId(), operation.getName());
            String operationJson = this.storageService.findOperation(operation.getName(), getFuzzerId());
            Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST).build();
            conf.addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

            return contextParameters.stream().map(context -> {
                String parameterSchema = this.getSchemaParameterContext(operationJson, context);
                if (Objects.isNull(parameterSchema) || parameterSchema.isEmpty())
                    return null;
                ReadContext pathContext = JsonPath.using(conf).parse(parameterSchema);
                List<String> strParameter = pathContext.read("$..[?(@.type == \"string\")]");
                List<String> pathAttributes = strParameter.stream().map(p -> {
                    String items = "\\['items'\\]";
                    String prop = "\\['properties'\\]";
                    return p.replaceAll(items, "\\[0\\]").replaceAll(prop, "").replaceAll("\\$", "");
                }).collect(Collectors.toList());
                return new OperationParameter(context, pathAttributes, Arrays.asList(), new String());
            }).filter(op -> !Objects.isNull(op)).collect(Collectors.toList());

        } catch (PathNotFoundException e) {
            Log.trace("VectorsService.getParameters(): PathNotFoundException: " + e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            Log.error("VectorsService.getParameters(): Exception: " + e.getMessage());
            throw new RuntimeException("VectorsService operation parameters fetching failed!");
        }
    }

    private String getSchemaParameterContext(String operationJson, String context) {
        try {

            String base = "";

            if (!Arrays.asList("payload", "headers", "path", "query").contains(context)) {
                Log.error("VectorsService.targets(): Invalid type: " + context);
                throw new RuntimeException("VectorsService schema fetching failed! Invalid type: " + context);
            }

            if (Arrays.asList("headers", "path", "query").contains(context)) {
                base = "/parameters";
            } else {
                base = "/req";
            }

            JsonNode rootNode = objectMapper.readTree(operationJson);
            JsonNode params = rootNode.at(base + "/" + context);
            return params.toPrettyString();

        } catch (PathNotFoundException e) {
            Log.warn("VectorsService.getSchemaParameterContext(): PathNotFoundException: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.error("VectorsService.getSchemaParameterContext(): Exception: " + e.getMessage());
            throw new RuntimeException("VectorsService schema fetching failed!");
        }
    }

    public UUID getFuzzerId() {
        return this.fuzzerId;
    }

}
