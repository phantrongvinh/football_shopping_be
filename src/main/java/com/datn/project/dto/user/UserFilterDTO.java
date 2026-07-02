package com.datn.project.dto.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterDTO {
    private String search;
    private LocalDate birthDayFrom;
    private LocalDate birthDayTo;
    private LocalDateTime createdAtFrom;
    private LocalDateTime createdAtTo;
    private String sortBy;
}
