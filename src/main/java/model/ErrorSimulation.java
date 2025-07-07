package model;

import java.util.Random;
import static model.Quadro.aplicarBitStuffing;
import static model.Quadro.removerBitStuffing;
import model.CRC;

public class ErrorSimulation {
    static Random random = new Random();

    public static void main(String[] args) {
        byte[] entrada = new byte[] { (byte) 0b01111100, (byte) 0b01001100 };
        byte crc = CRC.calcularCrc(entrada);
        byte[] entradaCrc = new byte[entrada.length + 1];
        System.arraycopy(entrada, 0, entradaCrc, 0, entrada.length);
        entradaCrc[entradaCrc.length - 1] = crc;

        simularErro("Original", entradaCrc, null);

        simularErro("Erro 1 bit", entradaCrc, arr -> inverterBit(arr.clone(), 0, 0));
        simularErro("Erro múltiplos bits", entradaCrc, arr -> inverterBit(arr.clone(), 0, 0, 3, 5));
        simularErro("Erro byte completo", entradaCrc, arr -> substituirByte(arr.clone(), 1, (byte) 0b11111111));
        simularErro("Erro burst (4 bits)", entradaCrc, arr -> erroBurst(arr.clone(), 0, 4));
    }

    public static void simularErro(String titulo, byte[] original, java.util.function.Function<byte[], byte[]> erroFunc) {
        System.out.println("\n==== " + titulo + " ====");
        byte[] modificado = erroFunc == null ? original : erroFunc.apply(original);

        System.out.println("Bits:");
        imprimirBits(modificado);

        System.out.println("CRC válido? " + CRC.verificarCRC(modificado));
    }

    public static byte[] inverterBit(byte[] data, int... bitPositions) {
        for (int bitPos : bitPositions) {
            int byteIndex = bitPos / 8;
            int bitIndex = bitPos % 8;
            data[byteIndex] ^= (1 << (7 - bitIndex));
        }
        return data;
    }

    public static byte[] substituirByte(byte[] data, int index, byte novoValor) {
        data[index] = novoValor;
        return data;
    }

    public static byte[] erroBurst(byte[] data, int inicioBit, int tamanho) {
        for (int i = 0; i < tamanho; i++) {
            int bitPos = inicioBit + i;
            int byteIndex = bitPos / 8;
            int bitIndex = bitPos % 8;
            data[byteIndex] ^= (1 << (7 - bitIndex));
        }
        return data;
    }

    public static void imprimirBits(byte[] bytes) {
        if (bytes == null) {
            System.out.println("null");
            return;
        }
        for (byte b : bytes) {
            for (int i = 7; i >= 0; i--) {
                System.out.print(((b >> i) & 1));
            }
            System.out.print(" ");
        }
        System.out.println();
    }
}
