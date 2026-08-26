package ru.practicum.ewm.main.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {
    @NotEmpty
    private List<Long> requestIds;

    @NotNull
    private RequestUpdateStatus status;
}
