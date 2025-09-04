package com.nelumbo.park.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndicatorResponse {
    private String plateNumber;
    private String modelVehicle;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm", timezone = "America/Bogota")
    private Date entryTime;
    
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm", timezone = "America/Bogota")
    private Date exitTime;

    private ParkingSimpleResponse parking;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ParkingSimpleResponse {
        private String name;
    }
}