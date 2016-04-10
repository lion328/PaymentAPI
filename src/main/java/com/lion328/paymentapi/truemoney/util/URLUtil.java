package com.lion328.paymentapi.truemoney.util;

import java.net.MalformedURLException;
import java.net.URL;

public class URLUtil {

    public static URL constantURL(String s) {
        try {
            return new URL(s);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
