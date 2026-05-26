package com.raksha.sentinelcore.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardised error envelope returned by {@code GlobalExceptionHandler}.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private int               status;
    private String            error;
    private String            message;
    private String            path;
    @Builder.Default
    private LocalDateTime     timestamp = LocalDateTime.now();
    private List<String>      violations;   // bean-validation field errors
}
