package com.nelumbo.park.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ParkingEarningsResponse {
    private String id;
    private String name;
    private Float totalEarnings;
    private Long vehicleCount;

    public ParkingEarningsResponse(String id, String name, Float totalEarnings, Long vehicleCount) {
        this.id = id;
        this.name = name;
        this.totalEarnings = totalEarnings;
        this.vehicleCount = vehicleCount;
    }

    public void addEarnings(Float amount) {
        this.totalEarnings += amount;
        this.vehicleCount += 1;
    }
}
