package com.techno.economy.util;

import java.text.DecimalFormat;

public final class Format {

    private static final DecimalFormat DF = new DecimalFormat("#,##0.00");

    private Format() {}

    public static String money(double amount, String symbol) {
        return symbol + DF.format(amount);
    }

    public static String number(double amount) {
        return DF.format(amount);
    }
}
