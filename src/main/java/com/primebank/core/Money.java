package com.primebank.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/*
 English: Money utilities for integer cents arithmetic with half-up rounding.
 Español: Utilidades de dinero para aritmética en centavos enteros con redondeo half-up.
*/
public final class Money {
    private Money() {}

    /*
     English: Convert whole dollars to cents.
     Español: Convertir dólares enteros a centavos.
    */
    public static long dollarsToCents(long dollars) {
        return Math.multiplyExact(dollars, 100L);
    }

    /*
     English: Safe addition with overflow checks.
     Español: Suma segura con verificación de desbordamiento.
    */
    public static long add(long a, long b) {
        return Math.addExact(a, b);
    }

    /*
     English: Safe subtraction with overflow checks.
     Español: Resta segura con verificación de desbordamiento.
    */
    public static long subtract(long a, long b) {
        return Math.subtractExact(a, b);
    }

    /*
     English: Multiply amount in cents by basis points (bps) and return cents (half-up rounding).
     Español: Multiplicar un monto en centavos por puntos básicos (bps) y devolver centavos (redondeo half-up).
    */
    public static long multiplyBps(long amountCents, int bps) {
        long numerator = Math.multiplyExact(amountCents, (long) bps);
        long result = divRoundHalfUp(numerator, 10_000L);
        return result;
    }

    /*
     English: Compute percent of amount in cents with half-up rounding.
     Español: Calcular el porcentaje de un monto en centavos con redondeo half-up.
    */
    public static long percentOf(long amountCents, int percent) {
        long numerator = Math.multiplyExact(amountCents, (long) percent);
        return divRoundHalfUp(numerator, 100L);
    }

    /*
     English: Divide with half-up rounding preserving sign.
     Español: Dividir con redondeo half-up preservando el signo.
    */
    public static long divRoundHalfUp(long numerator, long denominator) {
        if (denominator == 0) throw new ArithmeticException("Division by zero");
        long quotient = numerator / denominator;
        long remainder = Math.abs(numerator % denominator);
        long half = (denominator % 2 == 0) ? (denominator / 2) : (denominator / 2 + 1);
        if (remainder >= half) {
            quotient += (numerator >= 0 ? 1 : -1);
        }
        return quotient;
    }

    /*
     English: Format cents as USD string.
     Español: Formatear centavos como cadena en USD.
    */
    public static String formatUsd(long cents) {
        long abs = Math.abs(cents);
        long dollars = abs / 100L;
        long rem = abs % 100L;
        String s = String.format("$%,d.%02d", dollars, rem);
        return cents < 0 ? ("-" + s) : s;
    }

    /*
     English: Parse human-readable amount (e.g., "10", "10.50", "$1,234.56") into cents using half-up rounding.
     Español: Parsear un monto legible (ej., "10", "10.50", "$1.234,56") a centavos usando redondeo half-up.
    */
    public static long parseCents(String input) {
        if (input == null) throw new IllegalArgumentException("Null amount");
        String clean = input.trim();
        clean = clean.replace("$", "").replace(",", "");
        if (clean.isEmpty()) throw new IllegalArgumentException("Empty amount");
        BigDecimal bd = new BigDecimal(clean);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        bd = bd.movePointRight(2);
        return bd.longValueExact();
    }
}
