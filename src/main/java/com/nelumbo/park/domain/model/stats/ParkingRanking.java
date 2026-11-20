package com.nelumbo.park.domain.model.stats;

import java.util.List;

public record ParkingRanking(RankingPeriod period, List<TopParking> topParkings) {
}
