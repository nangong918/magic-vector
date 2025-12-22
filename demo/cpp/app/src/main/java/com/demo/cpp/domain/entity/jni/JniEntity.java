package com.demo.cpp.domain.entity.jni;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

public class JniEntity {
    public String str;
    public int intValue;
    public float floatValue;
    public double doubleValue;
    public boolean boolValue;
    public byte byteValue;
    public short shortValue;
    public long longValue;
    public char charValue;
    public byte[] byteArray;
    public int[] intArray;
    public float[] floatArray;
    public double[] doubleArray;
    public boolean[] boolArray;
    public List<Integer> intList;

    public JniEntity(){
        str = "hello world";
        intValue = 1;
        floatValue = 1.1f;
        doubleValue = 2.2;
        boolValue = true;
        byteValue = 0xf;
        shortValue = 121;
        longValue = System.currentTimeMillis();
        charValue = 'a';
        byteArray = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        intArray = new int[]{1,2,3,4,5};
        floatArray = new float[]{1.1f,2.2f,3.3f,4.4f,5.5f};
        doubleArray = new double[]{1.1,2.2,3.3,4.4,5.5};
        boolArray = new boolean[]{true,false,true,false,true};
        intList = List.of(1,2,3,4,5);
    }

    @NonNull
    @Override
    public String toString() {
        return "JniEntity{" +
                "str='" + str + '\'' +
                ", intValue=" + intValue +
                ", floatValue=" + floatValue +
                ", doubleValue=" + doubleValue +
                ", boolValue=" + boolValue +
                ", byteValue=" + byteValue +
                ", shortValue=" + shortValue +
                ", longValue=" + longValue +
                ", charValue=" + charValue +
                ", byteArray=" + Arrays.toString(byteArray) +
                ", intArray=" + Arrays.toString(intArray) +
                ", floatArray=" + Arrays.toString(floatArray) +
                ", doubleArray=" + Arrays.toString(doubleArray) +
                ", boolArray=" + Arrays.toString(boolArray) +
                ", intList=" + intList +
                '}';
    }
}
