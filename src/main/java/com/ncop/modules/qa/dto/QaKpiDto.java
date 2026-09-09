package com.ncop.modules.qa.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QaKpiDto {
    private long pendingFormulaCount;
    private long specPendingCount;
    private long draftSavedCount;
    private long completedCount;
    private long queryRaisedCount;
    private long totalRfqs;
    private long completedTodayCount;
    private long myPendingTasksCount;
    private long overdueTasksCount;
    private long openQueriesCount;
    private long approvedMfrCount;
    private long totalMfrs;
}
