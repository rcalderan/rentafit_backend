package br.com.rentafit.auth.event;

import br.com.rentafit.auth.domain.AccountTokenPurpose;

import java.time.OffsetDateTime;

/**
 * Published inside the transaction that issued a single-use link; the mail is sent only after commit.
 * The raw token lives only in memory — the database stores its SHA-256 hash.
 *
 * @param validUntil for UNLOCK, when the lock expires on its own; for ACTIVATION, when the link expires
 */
public record AccountLinkMailRequestedEvent(
        AccountTokenPurpose purpose,
        String recipientEmail,
        String recipientName,
        String rawToken,
        OffsetDateTime validUntil
) {
    @Override
    public String toString() {
        return "AccountLinkMailRequestedEvent[purpose=" + purpose + ", token=REDACTED]";
    }
}
