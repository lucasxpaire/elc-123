package application;

import static model.Quadro.aplicarBitStuffing;
import static model.Quadro.removerBitStuffing;
import model.CRC;

public class Teste {
    public static void main(String[] args) {
        
        byte[] entrada = new byte[] { (byte) 0b01111100, (byte) 0b01001100 };
        
        byte crc = CRC.calcularCrc(entrada);
  
        byte[] entradaCrc = new byte[entrada.length+1];
        System.arraycopy(entrada, 0, entradaCrc, 0, entrada.length);
        entradaCrc[entradaCrc.length-1] = crc;

        
        System.out.println("Dado original:");
        imprimirBits(entrada);
        
        System.out.println("\nEntrada com CRC:");
        imprimirBits(entradaCrc);

        byte[] comStuffing = aplicarBitStuffing(entradaCrc);
        System.out.println("\nAplicado Bit-Stuffing:");
        imprimirBits(comStuffing);

        byte[] restaurado = removerBitStuffing(comStuffing);
        System.out.println("\nRemovido Bit-Stuffing:");
        imprimirBits(restaurado);
        
        
        System.out.println("\nTestando verificacao CRC: TRUE");
        System.out.println(CRC.verificarCRC(restaurado));
        
        System.out.println("\nTestando veririfacao CRC: FALSE");
        System.out.println(CRC.verificarCRC(comStuffing));
    
        
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
