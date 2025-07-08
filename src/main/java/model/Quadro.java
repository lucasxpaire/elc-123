package model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

public class Quadro {
    // flag de bit-stuffing
    // public static final byte FLAG_BYTE = (byte) 0x07E;    // 01111110
    public static final byte FLAG_BYTE = (byte) 0x7E;    // 01111110

    // Tipos de quadro
    public static final byte TIPO_DADOS = 0x01;
    public static final byte TIPO_CONFIRMACAO = 0x02;   // ACK

    private static final int TAMANHO_CRC = 1;

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
        this.crc = new byte[TAMANHO_CRC];
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

    // public static byte[] removerBitStuffing(byte[] dadosStuffed) {
    //     BitSet entradaBits = converterByteParaBitSet(dadosStuffed);
    //     BitSet saidaBits = new BitSet();
    //     int saidaIndex = 0;
    //     int contadorDeUns = 0;

    //     for (int i = 0; i < dadosStuffed.length * 8; i++) {
    //         boolean bitAtual = entradaBits.get(i);

    //         if (contadorDeUns == 5 && !bitAtual) {
    //             // Bit stuffed detectado (0 após cinco 1s), então pula
    //             contadorDeUns = 0;
    //             continue;
    //         }

    //         saidaBits.set(saidaIndex++, bitAtual);

    //         if (bitAtual) {
    //             contadorDeUns++;
    //         } else {
    //             contadorDeUns = 0;
    //         }
    //     }

    //     return converterBitSetParaByte(saidaBits, saidaIndex);
    // }

    public static byte[] removerBitStuffing(byte[] dadosStuffed) {
    BitSet entradaBits = converterByteParaBitSet(dadosStuffed);
    BitSet saidaBits = new BitSet();
    int saidaIndex = 0;
    int contadorDeUns = 0;

    int totalBits = dadosStuffed.length * 8;
    for (int i = 0; i < totalBits; i++) {
        boolean bitAtual = entradaBits.get(i);

        if (contadorDeUns == 5 && !bitAtual) {
            // bit stuffed detectado (0 após cinco 1s) — pula esse bit
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


    /*
    Monta o quadro completo, com o calculo do CRC, bit stuffing e flags
     */
    public byte[] montarQuadroParaTransmissao() throws IOException {
        ByteArrayOutputStream conteudoParaCrc = new ByteArrayOutputStream();

        // Cabeçalho (origem, destino, sequencia, tipo, tamanhoDados)
        conteudoParaCrc.write(origem);
        conteudoParaCrc.write(destino);
        conteudoParaCrc.write(numeroSequencia);
        conteudoParaCrc.write(tipo);
        conteudoParaCrc.write((byte) dados.length);
        conteudoParaCrc.write(dados);

        // Aplica Crc
        byte crcCalculado = CRC.calcularCrc(conteudoParaCrc.toByteArray());
        this.crc = new byte[]{crcCalculado};

        // Monta o conteudo (cabecalho + dados + crc)
        ByteArrayOutputStream conteudoInternoBruto = new ByteArrayOutputStream();
        conteudoInternoBruto.write(conteudoParaCrc.toByteArray());
        conteudoInternoBruto.write(this.crc);
        conteudoParaCrc.write(crc);

        // Aplica bit stuffing
        byte[] conteudoStuffed = aplicarBitStuffing(conteudoInternoBruto.toByteArray());

        // Adiciona flags inicio e fim
        ByteArrayOutputStream quadroCompleto = new ByteArrayOutputStream();
        quadroCompleto.write(FLAG_BYTE);
        quadroCompleto.write(conteudoStuffed);
        quadroCompleto.write(FLAG_BYTE);

        return quadroCompleto.toByteArray();
    }

    /*
    Reconstróio um quadrado, verificando flags, removendo bit stuffing e validando o CRC
     */
    public static Quadro reconstruirQuadro(byte[] quadroRecebido) throws IOException {
        if (quadroRecebido == null || quadroRecebido.length < 3) {
            System.err.println("Erro: Quadro recebido é muito curto ou nulo.");
            return null;
        }

        if (quadroRecebido[0] != FLAG_BYTE || quadroRecebido[quadroRecebido.length - 1] != FLAG_BYTE) {
            System.err.println("Erro: Flags de início/fim do quadro ausentes ou incorretas.");
            return null;
        }

        byte[] conteudoStuffed = Arrays.copyOfRange(quadroRecebido, 1, quadroRecebido.length - 1);
        byte[] conteudoUnstuffed = removerBitStuffing(conteudoStuffed);

        if (!CRC.verificarCRC(conteudoUnstuffed)) {
            System.err.println("Erro: Falha na verificação do CRC. O quadro está corrompido.");
            return null;
        }

        if (conteudoUnstuffed.length < 5 + TAMANHO_CRC) {
            System.err.println("Erro: Conteúdo do quadro (após unstuffing) muito curto.");
            return null;
        }

        byte origem = conteudoUnstuffed[0];
        byte destino = conteudoUnstuffed[1];
        byte numeroSequencia = conteudoUnstuffed[2];
        byte tipo = conteudoUnstuffed[3];
        int tamanhoDadosBrutos = Byte.toUnsignedInt(conteudoUnstuffed[4]);

        int fimDados = 5 + tamanhoDadosBrutos;
        if (fimDados > conteudoUnstuffed.length) {
            System.err.println("Tamanho de dados declarado não corresponde ao quadro. Tamanho declarado: " + tamanhoDadosBrutos + ", Conteúdo unstuffed: " + conteudoUnstuffed.length);
            return null;
        }

        byte[] dadosExtraidos = Arrays.copyOfRange(conteudoUnstuffed, 5, 5 + tamanhoDadosBrutos);
        byte[] crcExtraido = Arrays.copyOfRange(conteudoUnstuffed, fimDados, fimDados + TAMANHO_CRC);

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
