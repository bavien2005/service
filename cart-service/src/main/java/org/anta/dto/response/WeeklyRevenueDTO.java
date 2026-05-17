package org.anta.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WeeklyRevenueDTO {
    private String week;   // ví dụ "2025-W01"
    private Double revenue;
}
