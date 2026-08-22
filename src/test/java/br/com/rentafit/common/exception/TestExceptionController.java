package br.com.rentafit.common.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
class TestExceptionController {

    @GetMapping("/test/not-found")
    public void notFound() {
        throw ResourceNotFoundException.forId("Entity", UUID.randomUUID());
    }

    @GetMapping("/test/invalid-argument")
    public void invalidArgument() {
        throw new IllegalArgumentException("Invalid value");
    }

    @GetMapping("/test/type-mismatch")
    public void typeMismatch(@RequestParam UUID id) {
        // intencionalmente vazio; falha de conversao acontece no parametro
    }

    @GetMapping("/test/internal")
    public void internal() {
        throw new RuntimeException("Unexpected");
    }
}
