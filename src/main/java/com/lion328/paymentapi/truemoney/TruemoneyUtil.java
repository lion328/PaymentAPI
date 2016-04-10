package com.lion328.paymentapi.truemoney;

import java.util.regex.Pattern;

public class TruemoneyUtil {

    private static final Pattern pinPattern = Pattern.compile("^\\d{14}$");

    public static boolean isValidPin(String pin) {
        return pinPattern.matcher(pin).find();
    }
}
