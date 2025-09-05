
package com.nelumbo.park.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleOutDetailResponse {
    private String userId;
    private String username;
    private String email;
    private String parkingId;
    private String parking;
    private List<VehicleDetailResponse> vehicles;
    private Integer totalVehicles;
    private Float totalEarnings;
}