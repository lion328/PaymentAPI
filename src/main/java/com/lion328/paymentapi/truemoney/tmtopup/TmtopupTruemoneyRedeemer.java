package com.lion328.paymentapi.truemoney.tmtopup;

import com.lion328.paymentapi.truemoney.InvalidPinException;
import com.lion328.paymentapi.truemoney.TruemoneyRedeemer;
import com.lion328.paymentapi.truemoney.util.URLUtil;

import java.math.BigDecimal;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TmtopupTruemoneyRedeemer implements TruemoneyRedeemer {

    public static final URL REEDEEM_URL = URLUtil.constantURL("https://www.tmtopup.com/topup/?uid=${uid}");

    private static final Pattern cipherPattern = Pattern.compile("\\s*pin\\.replace\\('(\\d)','([A-Z])'\\);\\s*");

    private int uid;

    public TmtopupTruemoneyRedeemer(int uid) {
        this.uid = uid;
    }

    @Override
    public synchronized BigDecimal redeem(String pin) throws InvalidPinException {
        return null;
    }

    public static char[] getCipher(String htmlSrc) {
        char[] cipher = new char[10];

        Matcher matcher = cipherPattern.matcher(htmlSrc);

        while (matcher.find())
            cipher[Integer.parseInt(matcher.group(1))] = matcher.group(2).charAt(0);

        return cipher;
    }

    public static String encodePin(String pin, char[] cipher) {
        for (int i = 0; i <= 9; i++)
            pin = pin.replace((char) (i + '0'), cipher[i]);
        return pin;
    }
}
