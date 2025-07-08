package application;

import static model.Quadro.aplicarBitStuffing;
import static model.Quadro.removerBitStuffing;
import model.CRC;

public class Teste {
    public static void main(String[] args) {
        // Testes com mensagens pequenas
        rodarTeste("Teste 1: Padrão simples", new byte[] { (byte) 0b01111100, (byte) 0b01001100 });
        rodarTeste("Teste 2: Sequência de 5 uns", new byte[] { (byte) 0b11111011, (byte) 0b11111011 });
        rodarTeste("Teste 3: Todos zeros", new byte[] { (byte) 0b00000000, (byte) 0b00000000 });
        rodarTeste("Teste 4: Todos uns", new byte[] { (byte) 0b11111111, (byte) 0b11111111 });
        rodarTeste("Teste 5: Alternância de bits", new byte[] { (byte) 0b10101010, (byte) 0b01010101 });

        // Teste com mensagem maior (32 bits)
        byte[] entradaGrande = new byte[] {
            (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11110000, (byte) 0b00001111
        };
        rodarTeste("Teste 6: Mensagem maior (32 bits)", entradaGrande);

        // Teste com mensagem ainda maior (64 bits)
        byte[] entradaMuitoGrande = new byte[] {
            (byte)0b11110000, (byte)0b00001111, (byte)0b10101010, (byte)0b01010101,
            (byte)0b11001100, (byte)0b00110011, (byte)0b11111111, (byte)0b00000000
        };
        rodarTeste("Teste 7: Mensagem muito maior (64 bits)", entradaMuitoGrande);

        // Demonstração dos três casos clássicos
        System.out.println("\n=== Demonstração de transmissão ===");
        byte[] dadosDemo = new byte[] { (byte) 0b10101010, (byte) 0b11001100, (byte) 0b11110000, (byte) 0b00001111 };
        byte crcDemo = CRC.calcularCrc(dadosDemo);
        byte[] frameDemo = new byte[dadosDemo.length + 1];
        System.arraycopy(dadosDemo, 0, frameDemo, 0, dadosDemo.length);
        frameDemo[frameDemo.length - 1] = crcDemo;

        // 1. Frame entregue com sucesso
        System.out.println("\n[Frame entregue com sucesso]");
        byte[] stuffed = aplicarBitStuffing(frameDemo);
        byte[] recebido = removerBitStuffing(stuffed);
        System.out.print("Bits recebidos: ");
        imprimirBits(recebido);
        System.out.println("CRC válido? " + CRC.verificarCRC(recebido));

        // 2. Frame perdido (simulação)
        System.out.println("\n[Frame perdido]");
        System.out.println("Nenhum dado recebido pelo receptor (simulação de perda).");

        // 3. Frame com erro (bit invertido)
        System.out.println("\n[Frame com erro]");
        byte[] stuffedComErro = stuffed.clone();
        stuffedComErro[0] ^= (1 << 5); // Inverte o 3º bit do primeiro byte
        byte[] recebidoErro = removerBitStuffing(stuffedComErro);
        System.out.print("Bits recebidos: ");
        imprimirBits(recebidoErro);
        System.out.println("CRC válido? " + CRC.verificarCRC(recebidoErro));
    }

    public static void rodarTeste(String nome, byte[] entrada) {
        System.out.println("\n" + nome);
        byte crc = CRC.calcularCrc(entrada);

        byte[] entradaCrc = new byte[entrada.length+1];
        System.arraycopy(entrada, 0, entradaCrc, 0, entrada.length);
        entradaCrc[entradaCrc.length-1] = crc;

        System.out.println("Dado original:");
        imprimirBits(entrada);

        System.out.println("Entrada com CRC:");
        imprimirBits(entradaCrc);

        byte[] comStuffing = aplicarBitStuffing(entradaCrc);
        System.out.println("Aplicado Bit-Stuffing:");
        imprimirBits(comStuffing);

        byte[] restaurado = removerBitStuffing(comStuffing);
        System.out.println("Removido Bit-Stuffing:");
        imprimirBits(restaurado);

        System.out.println("Testando verificacao CRC (deve ser TRUE):");
        System.out.println(CRC.verificarCRC(restaurado));
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