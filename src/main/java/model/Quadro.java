package model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

public class Quadro {
    // flag de bit-stuffing
    public static final byte FLAG_BYTE = (byte) 0x07E;    // 01111110

    // Tipos de quadro
    public static final byte TIPO_DADOS = 0x01;
    public static final byte TIPO_CONFIRMACAO = 0x02;   // ACK

    private byte origem;
    private byte destino;
    private byte numeroSequencia;
    private byte tipo;
    private byte[] dados;   // Dados brutos
    private byte[] crc;     // ESTUDAR CRC

    // Construtor para criar um quadro de dados
    public Quadro(byte origem, byte destino, byte numeroSequencia, byte tipo, byte[] dados) {
        this.origem = origem;
        this.destino = destino;
        this.numeroSequencia = numeroSequencia;
        this.tipo = tipo;
        this.dados = dados;
        this.crc = new byte[0]; // CRC inicial vazio
    }

    // Construtor para recriar um quadro a partir de bytes (após unstuffing e parse)
    private Quadro(byte origem, byte destino, byte numeroSequencia, byte tipo, byte[] dados, byte[] crc) {
        this.origem = origem;
        this.destino = destino;
        this.numeroSequencia = numeroSequencia;
        this.tipo = tipo;
        this.dados = dados;
        this.crc = crc;
    }

    public byte getOrigem() {
        return origem;
    }

    public void setOrigem(byte origem) {
        this.origem = origem;
    }

    public byte getDestino() {
        return destino;
    }

    public void setDestino(byte destino) {
        this.destino = destino;
    }

    public byte getNumeroSequencia() {
        return numeroSequencia;
    }

    public void setNumeroSequencia(byte numeroSequencia) {
        this.numeroSequencia = numeroSequencia;
    }

    public byte getTipo() {
        return tipo;
    }

    public void setTipo(byte tipo) {
        this.tipo = tipo;
    }

    public byte[] getDados() {
        return dados;
    }

    public void setDados(byte[] dados) {
        this.dados = dados;
    }

    public byte[] getCrc() {
        return crc;
    }

    public void setCrc(byte[] crc) {
        this.crc = crc;
    }

    private static BitSet converterByteParaBitSet(byte[] bytes) {
        // BitSet = sequencia de bits, ele le do MSB para o LSB dentro de cada byte que colocarmos
        BitSet bitSet = new BitSet(bytes.length * 8);
        int bitIndex = 0;
        for (byte b : bytes) {
            // Lê o bit mais significativo(MSB) para o menos significativo(LSB)
            for (int i = 7 ; i >= 0 ; i--) {
                if (((b >> i) & 1) == 1) {
                    bitSet.set(bitIndex);
                }
                bitIndex++;
            }
        }
        return bitSet;
    }

    private static byte[] converterBitSetParaByte(BitSet bitSet, int numBits) {
        int minimoBytesNecessarios = (int) Math.ceil(numBits / 8.0);    // Math.cell arredonda pra cima pra garantir espaço suficiente
        byte[] bytes = new byte[minimoBytesNecessarios];
        for (int i = 0; i < numBits; i++) {
            if (bitSet.get(i)) {
                // i % 8 = determina posicao do bit dentro desse byte
                // 7 - (i & 8) = acha a posicao real do bit começando pelo MSB
                // 1 << (7 - (i & 8)) cria uma mascara
                // exemplo de mascara: 7 - (i % 8) resultar em 7, 1 << 7 resulta em 10000000
                // |= or bit a bit, entre valor atual e mascara, liga o bit na posicao correta
                // exemplo de or bit a bit:
                // X | 0 = X , X | 1 = 1
                // se bytes[i/8] é 10010000  e a mascara é 00000100, o resultado sera 10010100
                bytes[i / 8] |= (byte) (1 << (7 - (i % 8)));
            }
        }
        return bytes;
    }

    public static byte[] aplicarBitStuffing(byte[] dadosBrutos) {
        BitSet entradaBits = converterByteParaBitSet(dadosBrutos);
        BitSet saidaBits = new BitSet();
        int saidaIndex = 0;
        int contadorDeUns = 0;

        for (int i = 0; i < dadosBrutos.length * 8; i++) {
            boolean bitAtual = entradaBits.get(i);
            saidaBits.set(saidaIndex++, bitAtual);

            if (bitAtual) {
                contadorDeUns++;
            } else {
                contadorDeUns = 0;
            }

            if (contadorDeUns == 5) {
                saidaBits.set(saidaIndex++, false);
                contadorDeUns = 0;
            }
        }
        return converterBitSetParaByte(saidaBits, saidaIndex);
    }

    public static byte[] removerBitStuffing(byte[] dadosStuffed) {
        BitSet entradaBits = converterByteParaBitSet(dadosStuffed);
        BitSet saidaBits = new BitSet();
        int saidaIndex = 0;
        int contadorDeUns = 0;

        for (int i = 0; i < entradaBits.cardinality() + (dadosStuffed.length*8 - entradaBits.length()); i++) {
            boolean bitAtual = entradaBits.get(i);

            if (contadorDeUns == 5 && !bitAtual) {
                contadorDeUns = 0;
                continue;
            }

            saidaBits.set(saidaIndex++, bitAtual);

            if (bitAtual) {
                contadorDeUns++;
            } else {
                contadorDeUns = 0;
            }
        }
        return converterBitSetParaByte(saidaBits, saidaIndex);
    }

    public byte[] montarQuadroParaTransmissao() throws IOException {
        ByteArrayOutputStream conteudoInternoBruto = new ByteArrayOutputStream();

        // Cabeçalho (origem, destino, sequencia, tipo, tamanhoDados)
        conteudoInternoBruto.write(origem);
        conteudoInternoBruto.write(destino);
        conteudoInternoBruto.write(numeroSequencia);
        conteudoInternoBruto.write(tipo);
        conteudoInternoBruto.write((byte) dados.length);

        // Dados e CRC (alguém tem que fazer...)
        conteudoInternoBruto.write(dados);
        conteudoInternoBruto.write(crc);

        byte[] conteudoStuffed = aplicarBitStuffing(conteudoInternoBruto.toByteArray());

        ByteArrayOutputStream quadroCompleto = new ByteArrayOutputStream();
        quadroCompleto.write(FLAG_BYTE);
        quadroCompleto.write(conteudoStuffed);
        quadroCompleto.write(FLAG_BYTE);

        return quadroCompleto.toByteArray();
    }

    public static Quadro reconstruirQuadro(byte[] quadroRecebido) throws IOException {
        if (quadroRecebido == null || quadroRecebido.length < 7) {
            System.err.println("Quadro muito curto para ser válido. Tamanho: " + (quadroRecebido != null ? quadroRecebido.length : "null"));
            return null;
        }

        if (quadroRecebido[0] != FLAG_BYTE || quadroRecebido[quadroRecebido.length - 1] != FLAG_BYTE) {
            System.err.println("Flags de início/fim ausentes ou incorretos. Início: " + String.format("%02X", quadroRecebido[0]) + ", Fim: " + String.format("%02X", quadroRecebido[quadroRecebido.length - 1]));
            return null;
        }

        byte[] conteudoStuffed = Arrays.copyOfRange(quadroRecebido, 1, quadroRecebido.length - 1);
        byte[] conteudoUnstuffed = removerBitStuffing(conteudoStuffed);

        if (conteudoUnstuffed.length < 5) {
            System.err.println("Conteúdo unstuffed muito curto para o cabeçalho. Tamanho: " + conteudoUnstuffed.length);
            return null;
        }

        byte origem = conteudoUnstuffed[0];
        byte destino = conteudoUnstuffed[1];
        byte numeroSequencia = conteudoUnstuffed[2];
        byte tipo = conteudoUnstuffed[3];
        int tamanhoDadosBrutos = Byte.toUnsignedInt(conteudoUnstuffed[4]);

        if (5 + tamanhoDadosBrutos > conteudoUnstuffed.length) {
            System.err.println("Tamanho de dados declarado não corresponde ao quadro. Tamanho declarado: " + tamanhoDadosBrutos + ", Conteúdo unstuffed: " + conteudoUnstuffed.length);
            return null;
        }

        byte[] dadosExtraidos = Arrays.copyOfRange(conteudoUnstuffed, 5, 5 + tamanhoDadosBrutos);
        byte[] crcExtraido = new byte[0];

        // fazer a parte do crc

        return new Quadro(origem, destino, numeroSequencia, tipo, dadosExtraidos, crcExtraido);
    }

    @Override
    public String toString() {
        return "Quadro{" +
                "origem=" + origem +
                ", destino=" + destino +
                ", numeroSequencia=" + Byte.toUnsignedInt(numeroSequencia) +
                ", tipo=" + tipo +
                ", tamanhoDados=" + dados.length +
                ", dados=" + new String(dados) +
                ", crc=" + Arrays.toString(crc) +
                '}';
    }
}
