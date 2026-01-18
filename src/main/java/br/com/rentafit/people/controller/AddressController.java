package br.com.rentafit.people.controller;

import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for address lookup operations
 */
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Address lookup and management APIs")
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/zipcode/{zipCode}")
    @Operation(
        summary = "Get address by ZIP code",
        description = "Looks up address by ZIP code. If not found locally, fetches from ViaCEP API and stores it."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Address found"),
        @ApiResponse(responseCode = "404", description = "Address not found"),
        @ApiResponse(responseCode = "400", description = "Invalid ZIP code format")
    })
    public ResponseEntity<AddressDTO> findByZipCode(@PathVariable String zipCode) {

        var address = addressService.findOrCreateByZipcode(zipCode);
        return ResponseEntity.ok(address.toDTO());
    }
}

