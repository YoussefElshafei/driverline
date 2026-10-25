package com.driverline.planning.actual.importing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/actuals/import")
@Tag(name = "Actuals import", description = "Upload actuals from CSV or Excel")
public class ActualsImportController {

    private static final MediaType TEXT_CSV = MediaType.parseMediaType("text/csv");

    private final ActualsImportService importService;

    public ActualsImportController(ActualsImportService importService) {
        this.importService = importService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import actuals from a .csv or .xlsx file with columns year, month, account_code, "
            + "department_code, amount. All-or-nothing; returns every row error if the file is rejected")
    public ImportSummary importActuals(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream content = file.getInputStream()) {
            return importService.importFile(file.getOriginalFilename(), content);
        }
    }

    @GetMapping("/template")
    @Operation(summary = "Download an example CSV file with the required columns")
    public ResponseEntity<String> downloadTemplate() {
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("actuals-template.csv").build().toString())
                .body(ActualsFileFormat.TEMPLATE_CSV);
    }
}
