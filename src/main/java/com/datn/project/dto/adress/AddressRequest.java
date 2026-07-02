package com.datn.project.dto.adress;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {
    private String address;
    private boolean isPrimary;
    private String receiverName;
    private String receiverPhone;
}
