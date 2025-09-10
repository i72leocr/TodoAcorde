package com.todoacorde.todoacorde;

import androidx.room.TypeConverter;

import java.util.Arrays;

public class PCPConverter {
    @TypeConverter
    public static String fromPCP(double[] pcp) {
        return Arrays.toString(pcp);
    }

    @TypeConverter
    public static double[] toPCP(String data) {
        String[] strings = data.replace("[", "").replace("]", "").split(", ");
        double[] pcp = new double[strings.length];
        for (int i = 0; i < strings.length; i++) {
            pcp[i] = Double.parseDouble(strings[i]);
        }
        return pcp;
    }
}
