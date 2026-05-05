package org.anta.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserMonthlyStatsResponse {
    private int year;
    private int month;   // 1-12
    private long count;  // số user trong tháng
}