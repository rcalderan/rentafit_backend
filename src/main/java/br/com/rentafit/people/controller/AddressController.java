package br.com.rentafit.people.controller;

import br.com.rentafit.people.dto.AddressDTO;
import br.com.rentafit.people.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for address lookup operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Address lookup and management APIs")
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/find/{zipCode}")
    @Operation(
        summary = "Get address by ZIP code",
        description = "Looks up address by ZIP code. If not found locally, fetches from ViaCEP API and stores it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address found"),
            @ApiResponse(responseCode = "404", description = "Address not found"),
            @ApiResponse(responseCode = "400", description = "Invalid ZIP code format"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
            @ApiResponse(responseCode = "504", description = "ViaCEP service timeout")
    })
    public ResponseEntity<AddressDTO> findByZipCode(@PathVariable String zipCode) {

        AddressDTO address = addressService.findByZipCode(zipCode);
        return ResponseEntity.ok(address);
    }
}
