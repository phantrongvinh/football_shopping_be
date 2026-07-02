package com.datn.project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datn.project.dto.adress.AddressRequest;
import com.datn.project.service.IAddressService;

@RestController
@RequestMapping(value = "/api/v1/addresses")
public class AdressController {

    @Autowired
    private IAddressService addressService;

    @GetMapping
    public ResponseEntity<?> getAddresses() {
        return ResponseEntity.ok(addressService.getAddresses()).getBody();
    }

    @PostMapping
    public ResponseEntity<?> addAddress(@RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.addAddress(request)).getBody();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable int id) {
        return ResponseEntity.ok(addressService.deleteAddress(id)).getBody();
    }

    @PatchMapping("/{id}/primary")
    public ResponseEntity<?> setPrimary(@PathVariable int id) {
        return ResponseEntity.ok(addressService.setPrimary(id)).getBody();
    }
}
