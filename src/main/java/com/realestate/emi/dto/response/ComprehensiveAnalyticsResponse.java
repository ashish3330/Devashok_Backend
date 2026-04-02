package com.realestate.emi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComprehensiveAnalyticsResponse {

    private PortfolioOverviewResponse portfolioOverview;

    private UpcomingEmiWindow upcomingThisWeek;    // next 7 days
    private UpcomingEmiWindow upcomingNextMonth;   // next 30 days

    private OverdueAnalysisResponse overdueAnalysis;

    private List<MonthlyTrendItem> monthlyTrend;  // last 12 months

    private DealDistributionResponse dealDistribution;
}
