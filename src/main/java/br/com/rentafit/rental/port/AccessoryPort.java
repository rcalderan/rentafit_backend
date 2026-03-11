package br.com.rentafit.rental.port;

import java.util.UUID;

/**
 * Porta de acesso ao componente Product (Accessory + Stock).
 *
 * <p>Gerencia reserva de estoque para acessórios catalogados.
 * Acessórios usam controle por quantidade (Stock.reserve/release),
 * diferente dos RentalItems que usam ProductStatus individual.</p>
 *
 * <p>Apenas acessórios com accessoryId preenchido passam por esta porta.
 * Metadados textuais (accessoryId == null) são ignorados.</p>
 */
public interface AccessoryPort {

    /**
     * Verifica se o acessório existe e possui estoque disponível.
     */
    boolean isAvailableInStock(UUID accessoryId);

    /**
     * Reserva 1 unidade do acessório.
     * Chamado ao FINALIZAR o contrato.
     */
    void reserveStock(UUID accessoryId, UUID userId);

    /**
     * Libera a reserva de 1 unidade do acessório.
     * Chamado ao processar a DEVOLUÇÃO do contrato.
     */
    void releaseStock(UUID accessoryId, UUID userId);
}

