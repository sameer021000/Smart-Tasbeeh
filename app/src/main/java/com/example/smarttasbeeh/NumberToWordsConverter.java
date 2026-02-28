package com.example.smarttasbeeh;

public class NumberToWordsConverter {

    private static final String[] UNITS = {
            "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    /**
     * Converts a number into its textual representation in the Indian Numbering System.
     * Handles numbers up to 99,99,99,999 (9 digits).
     */
    public static String convert(int n) {
        if (n == 0) return UNITS[0];

        String words = "";

        if (n >= 10000000) { // Crores
            words += convert(n / 10000000) + " Crore ";
            n %= 10000000;
        }

        if (n >= 100000) { // Lakhs
            words += convert(n / 100000) + " Lakh ";
            n %= 100000;
        }

        if (n >= 1000) { // Thousands
            words += convert(n / 1000) + " Thousand ";
            n %= 1000;
        }

        if (n >= 100) { // Hundreds
            words += convert(n / 100) + " Hundred ";
            n %= 100;
        }

        if (n > 0) {
            if (!words.isEmpty()) {
                words += "And ";
            }

            if (n < 20) {
                words += UNITS[n];
            } else {
                words += TENS[n / 10];
                if ((n % 10) > 0) {
                    words += " " + UNITS[n % 10];
                }
            }
        }

        return words.trim();
    }
}
