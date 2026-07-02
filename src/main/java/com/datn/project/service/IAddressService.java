package com.datn.project.service;

import org.springframework.http.ResponseEntity;

import com.datn.project.dto.adress.AddressRequest;

public interface IAddressService {

    ResponseEntity<?> getAddresses();

    ResponseEntity<?> addAddress(AddressRequest request);

    ResponseEntity<?> deleteAddress(int id);

    ResponseEntity<?> setPrimary(int id);
}