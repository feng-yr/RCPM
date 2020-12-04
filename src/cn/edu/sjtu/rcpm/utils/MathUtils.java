package cn.edu.sjtu.rcpm.utils;

import java.math.BigDecimal;

public abstract class MathUtils {
    
    private static final int DIV_SCALE = 10;
    
    public static double add(double value1, double value2){
        
        BigDecimal big1 = new BigDecimal(String.valueOf(value1));
        BigDecimal big2 = new BigDecimal(String.valueOf(value2));
        return big1.add(big2).doubleValue();
    }
    
    public static double add(String value1, String value2){
        
        BigDecimal big1 = new BigDecimal(value1);
        BigDecimal big2 = new BigDecimal(value2);
        return big1.add(big2).doubleValue();
    }
    
    public static double div(double value1, double value2){
        
        BigDecimal big1 = new BigDecimal(String.valueOf(value1));
        BigDecimal big2 = new BigDecimal(String.valueOf(value2));
        return big1.divide(big2, DIV_SCALE, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    
    public static double mul(double value1, double value2){
        
        BigDecimal big1 = new BigDecimal(String.valueOf(value1));
        BigDecimal big2 = new BigDecimal(String.valueOf(value2));
        return big1.multiply(big2).doubleValue();
    }
    
    public static double sub(double value1, double value2){
        
        BigDecimal big1 = new BigDecimal(String.valueOf(value1));
        BigDecimal big2 = new BigDecimal(String.valueOf(value2));
        return big1.subtract(big2).doubleValue();
    }
    
    public static double max(double value1, double value2) {
        
        BigDecimal big1 = new BigDecimal(value1);
        BigDecimal big2 = new BigDecimal(value2);
        return big1.max(big2).doubleValue();
    }
}