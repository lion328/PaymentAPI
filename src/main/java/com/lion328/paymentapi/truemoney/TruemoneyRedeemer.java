package com.lion328.paymentapi.truemoney;

import java.math.BigDecimal;

public interface TruemoneyRedeemer {

    BigDecimal redeem(String pin) throws Exception;
}
