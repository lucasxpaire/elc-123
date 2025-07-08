package model;

public class Hamming {

    // Codifica 4 bits usando Hamming (7,4)
    public static byte codificar(byte bitsDados) {
        int d1 = (bitsDados >> 0) & 1;
        int d2 = (bitsDados >> 1) & 1;
        int d3 = (bitsDados >> 2) & 1;
        int d4 = (bitsDados >> 3) & 1;

        // bits de paridade conforme posição
        int p1 = d1 ^ d2 ^ d4;          // cobre posições 1, 3, 5, 7
        int p2 = d1 ^ d3 ^ d4;          // cobre posições 2, 3, 6, 7
        int p3 = d2 ^ d3 ^ d4;          // cobre posições 4, 5, 6, 7

        int hammingCode = (p1 << 6) | (p2 << 5) | (d1 << 4) | (p3 << 3) |
                          (d2 << 2) | (d3 << 1) | d4;

        return (byte) hammingCode;
    }

    // Decodifica e corrige um byte codificado com Hamming (7,4)
    public static byte decodificar(byte byteCodificado) {
        int h = byteCodificado & 0x7F; // apenas 7 bits

        int p1 = (h >> 6) & 1;
        int p2 = (h >> 5) & 1;
        int d1 = (h >> 4) & 1;
        int p3 = (h >> 3) & 1;
        int d2 = (h >> 2) & 1;
        int d3 = (h >> 1) & 1;
        int d4 = (h >> 0) & 1;

        // Síndromes: se diferentes de zero, indicam erro
        int s1 = p1 ^ d1 ^ d2 ^ d4;
        int s2 = p2 ^ d1 ^ d3 ^ d4;
        int s3 = p3 ^ d2 ^ d3 ^ d4;

        // Posição do bit com erro (1-based)
        int erroPos = (s3 << 2) | (s2 << 1) | s1;

        if (erroPos != 0) {
            // corrigir bit com erro
            int mask = 1 << (7 - erroPos); // converte 1–7 para índice 6–0
            h ^= mask;
        }

        // Extrair os dados corrigidos
        d1 = (h >> 4) & 1;
        d2 = (h >> 2) & 1;
        d3 = (h >> 1) & 1;
        d4 = (h >> 0) & 1;

        int dados = (d4 << 3) | (d3 << 2) | (d2 << 1) | d1;

        return (byte) dados;
    }
}
