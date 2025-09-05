package com.ocean.shopping.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for updating notification preferences
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification preferences update request")
public class NotificationPreferencesRequest {

    @Schema(description = "Enable/disable email notifications", example = "true")
    @Builder.Default
    private Boolean emailEnabled = true;

    @Schema(description = "Enable/disable push notifications", example = "true")
    @Builder.Default
    private Boolean pushEnabled = true;

    @Schema(description = "Enable/disable SMS notifications", example = "false")
    @Builder.Default
    private Boolean smsEnabled = false;

    @Schema(description = "Enable/disable desktop notifications", example = "true")
    @Builder.Default
    private Boolean desktopEnabled = true;

    @Schema(description = "Notification type preferences (type -> enabled)")
    private Map<NotificationRequest.NotificationType, Boolean> typePreferences;

    @Schema(description = "Priority level preferences (priority -> enabled)")
    private Map<NotificationRequest.Priority, Boolean> priorityPreferences;

    @Schema(description = "Quiet hours start time (24-hour format)", example = "22")
    private Integer quietHoursStart;

    @Schema(description = "Quiet hours end time (24-hour format)", example = "8")
    private Integer quietHoursEnd;

    @Schema(description = "Timezone for quiet hours", example = "Asia/Taipei")
    private String timezone;
}