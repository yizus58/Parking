package com.nelumbo.park.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParkingResponse {
    private String id;
    private String name;
    private String address;
    private int capacity;
    private Float costPerHour;
    private UserResponse owner;
}
