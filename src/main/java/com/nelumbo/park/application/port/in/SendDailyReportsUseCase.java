package com.nelumbo.park.application.port.in;

public interface SendDailyReportsUseCase {

    /**
     * Builds today's report per partner and parking, uploads the Excel and PDF
     * files and queues one email per report.
     *
     * @return false when there was nothing to report
     */
    boolean sendDailyReports();
}
