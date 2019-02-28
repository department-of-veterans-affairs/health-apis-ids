package gov.va.api.health.ids.service.controller;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/** The error response is the payload returned to the caller should a failure occur. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ErrorResponse {
  long timestamp;
  String type;
  String message;

  /** Create a new error response based on the given exception. */
  public static ErrorResponse of(@NonNull Exception e) {
    return ErrorResponse.builder()
        .timestamp(System.currentTimeMillis())
        .type(e.getClass().getSimpleName())
        .message(e.getMessage())
        .build();
  }
}
