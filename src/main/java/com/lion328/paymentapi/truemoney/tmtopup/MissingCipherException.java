package com.lion328.paymentapi.truemoney.tmtopup;

public class MissingCipherException extends Exception {

    public MissingCipherException() {
        super();
    }

    public MissingCipherException(String s) {
        super(s);
    }
}
