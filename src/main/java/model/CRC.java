package model;

public class CRC {
    private static final int POLINOMIO = 0xEDB88320;
    private static final int TAM = 8;
    
    public static byte calcularCrc(byte[] dados){
        int crc = 0;
        
        for (byte b: dados){
            crc ^= (b & 0xFF) << (TAM - 8);
            
            for(int i =0; i<8;i++){
                if((crc & 0x80) != 0) {
                    crc = (crc << 1) ^POLINOMIO;
                } else {
                    crc <<=1;
                }
            }
        }
        
        return (byte)(crc & 0xFF);
    }
    
    public static boolean verificarCRC(byte[] dados){
        return calcularCrc(dados) == 0;
    }
    
}
