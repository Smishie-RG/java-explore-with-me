package ru.practicum.ewm.main.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    @Column(name = "location_lat", nullable = false)
    private Float lat;

    @Column(name = "location_lon", nullable = false)
    private Float lon;
}
