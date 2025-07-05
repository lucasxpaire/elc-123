package application;

import static model.Quadro.aplicarBitStuffing;
import static model.Quadro.removerBitStuffing;

public class Teste {
    public static void main(String[] args) {
//        byte[] entrada = new byte[] {(byte) 0b10101010};
//        BitSet resultado = Quadro.converterByteParaBitSet(entrada);
//
//        // Imprimir os bits que estão ligados
//        System.out.println("Bits ativos:");
//        for (int i = 0; i < entrada.length * 8; i++) {
//            if (resultado.get(i)) {
//                System.out.print("1");
//            } else {
//                System.out.print("0");
//            }
//        }

        byte[] entrada = new byte[] { (byte) 0b01111100, (byte) 0b01001100 };

        System.out.println("Dado original:");
        imprimirBits(entrada);

        byte[] comStuffing = aplicarBitStuffing(entrada);
        System.out.println("\nAplicado Bit-Stuffing:");
        imprimirBits(comStuffing);

        byte[] restaurado = removerBitStuffing(comStuffing);
        System.out.println("\nRemovido Bit-Stuffing:");
        imprimirBits(restaurado);
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
