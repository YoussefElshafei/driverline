package com.driverline.commentary.api;

import com.driverline.commentary.api.dto.CommentaryRequest;
import com.driverline.commentary.api.dto.CommentaryResponse;
import com.driverline.commentary.generation.CommentaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/commentary")
@Tag(name = "Commentary", description = "Plain-English variance commentary")
public class CommentaryController {

    private final CommentaryService commentaryService;

    public CommentaryController(CommentaryService commentaryService) {
        this.commentaryService = commentaryService;
    }

    @PostMapping
    @Operation(summary = "Write commentary for the given variances. Uses the language model when available and "
            + "its answer passes validation; otherwise returns rule-based commentary and the reason")
    public CommentaryResponse generate(@Valid @RequestBody CommentaryRequest request) {
        return commentaryService.generate(request);
    }
}
