package com.nelumbo.park.domain.model.stats;

import java.util.List;

public record PartnerRanking(RankingPeriod period, List<TopPartner> topPartners) {
}
