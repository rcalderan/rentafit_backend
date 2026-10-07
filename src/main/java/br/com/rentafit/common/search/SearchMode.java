package br.com.rentafit.common.search;

/**
 * Semântica de prefixo aplicada aos tokens da busca textual.
 */
public enum SearchMode {
    /** Somente o último token vira prefixo ("maria & sil:*"). Default — mais estrito. */
    PREFIX_LAST,
    /** Todo token vira prefixo ("maria:* & sil:*") — autocomplete agressivo. */
    PREFIX_ALL
}
