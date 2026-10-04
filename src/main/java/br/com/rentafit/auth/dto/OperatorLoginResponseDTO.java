package br.com.rentafit.auth.dto;

public record OperatorLoginResponseDTO(String accessToken, String refreshToken, String tokenType,
                                       OperatorProfileResponseDTO profile) {
    @Override
    public String toString() {
        return "OperatorLoginResponseDTO[credentials=REDACTED]";
    }
}
