package com.lion328.paymentapi.truemoney;

public class InvalidPinException extends Exception {

    public InvalidPinException(String msg) {
        super(msg);
    }
}
