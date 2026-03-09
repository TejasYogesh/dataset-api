package com.assignment.datasetapi.controller;

import com.assignment.datasetapi.dto.InsertRecordRequest;
import com.assignment.datasetapi.dto.InsertRecordResponse;
import com.assignment.datasetapi.dto.QueryParams;
import com.assignment.datasetapi.dto.QueryResponse;
import com.assignment.datasetapi.exception.GlobalExceptionHandler;
import com.assignment.datasetapi.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dataset API", description = "APIs for inserting and querying JSON dataset records")
public class DatasetController {
     private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ✅ Injected via constructor — depends on INTERFACE, not implementation (SOLID)
    private final DatasetService datasetService;

    public DatasetController() {
        this.datasetService = null;
    }
    @PostMapping("/{datasetName}/record")
    @Operation(
        summary = "Insert a JSON record into a dataset",
        description = "Accepts any valid JSON object and stores it under the specified dataset name."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Record inserted successfully"),
        @ApiResponse(responseCode = "400", description = "Empty or invalid request body"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<InsertRecordResponse> insertRecord(
            @Parameter(description = "Name of the dataset", example = "employee_dataset")
            @PathVariable String datasetName,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "JSON record to insert",
                content = @Content(examples = @ExampleObject(
                    value = "{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"}"
                ))
            )
            @RequestBody InsertRecordRequest request) {

        log.info("POST /api/dataset/{}/record", datasetName);

        InsertRecordResponse response = datasetService.insertRecord(datasetName, request);

        // 201 CREATED = semantically correct for a new resource being created
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{datasetName}/query")
    @Operation(
        summary = "Query records from a dataset",
        description = "Supports group-by (groupBy=<field>) or sort-by (sortBy=<field>&order=asc|desc). Provide exactly one operation."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Query executed successfully"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid query parameters"),
        @ApiResponse(responseCode = "404", description = "Dataset not found")
    })
    public ResponseEntity<QueryResponse> queryRecords(
            @Parameter(description = "Name of the dataset", example = "employee_dataset")
            @PathVariable String datasetName,

            @Parameter(description = "Field to group by", example = "department")
            @RequestParam(required = false) String groupBy,

            @Parameter(description = "Field to sort by", example = "age")
            @RequestParam(required = false) String sortBy,

            @Parameter(description = "Sort order: asc or desc", example = "asc")
            @RequestParam(required = false, defaultValue = "asc") String order) {

        log.info("GET /api/dataset/{}/query groupBy={} sortBy={} order={}", datasetName, groupBy, sortBy, order);

        // Build QueryParams object cleanly
        QueryParams params = new QueryParams();
        params.setGroupBy(groupBy);
        params.setSortBy(sortBy);
        params.setOrder(order);

        QueryResponse response = datasetService.queryRecords(datasetName, params);

        return ResponseEntity.ok(response);
    }
}
