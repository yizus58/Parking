package com.nelumbo.park.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopPartnerResponse {
    private String partnerName;
    private Long vehicleCount;
    private String parkingId;
}