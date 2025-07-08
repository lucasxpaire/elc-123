package model;

public class Hamming {
    public static byte codificar(byte bitsDados){
        
        int d1 = (bitsDados >> 0) & 1;
        int d2 = (bitsDados >> 1) & 1;
        int d3 = (bitsDados >> 2) & 1;
        int d4 = (bitsDados >> 3) & 1;
        
        //calcula bits paridade 
        
        int p1 = d1 ^d2 ^d4;
        int p2 = d2 ^d2 ^d4;
        int p3 = d3 ^d2 ^d4;
        
        int hammingCode = (p1 << 6) | (p2 << 5) | (d1 << 4) | (p3 << 3) |
                (d2 << 2) | ( d3 << 1) | (d4);
        
        return (byte) hammingCode;
    }
    
    public static byte decodificar(byte byteCodificado){
        int h = byteCodificado & 0x7F;
        
        int p1 = (h >> 6) & 1;
        int p2 = (h >> 5) & 1;
        int d1 = (h >> 4) & 1;
        int p3 = (h >> 3) & 1;
        int d2 = (h >> 2) & 1;
        int d3 = (h >> 1) & 1;
        int d4 = (h >> 0) & 1;
    
        int s1 = p1 ^ d1 ^ d2 ^d4;
        int s2 = p2 ^ d1 ^ d3 ^d4;
        int s3 = p3 ^ d2 ^ d3 ^d4;
        
        int erroPos = (s3 << 2) | (s2 << 1) | s1;
        
        if ( erroPos != 0){
            int mask = 1 << (7 - erroPos);
            h ^= mask;
        }

        d1 = (h >> 4) & 1;
        d2 = (h >> 2) & 1;
        d3 = (h >> 1) & 1;
        d4 = (h >> 0) & 1;
        
        int dados = (d4 << 3) | ( d3 << 2) | (d2 <<1) | d1;
        return (byte) dados;
    } 
}
