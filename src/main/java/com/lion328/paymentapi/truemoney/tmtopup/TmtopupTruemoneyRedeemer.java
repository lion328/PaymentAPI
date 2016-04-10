package com.lion328.paymentapi.truemoney.tmtopup;

import com.lion328.paymentapi.truemoney.InvalidPinException;
import com.lion328.paymentapi.truemoney.TruemoneyRedeemer;
import com.lion328.paymentapi.truemoney.TruemoneyUtil;
import com.lion328.paymentapi.truemoney.util.URLUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TmtopupTruemoneyRedeemer implements TruemoneyRedeemer {

    public static final URL REEDEEM_URL = URLUtil.constantURL("https://www.tmtopup.com/topup/?uid=${uid}");
    public static final URL STATUS_URL = URLUtil.constantURL("https://www.tmtopup.com/topup/tmn_status_new.php?cid=${cid}&hash=${hash}&x=${rand}");

    private static final String postData = "topup=true&tmn_password=${pin}&ref1=${ref1}&ref2=${ref2}&ref3=${ref3}&return_url=aHR0cDovL2V4YW1wbGUuY29tLwo%3D&success_url=aHR0cDovL2V4YW1wbGUuY29tLwo%3D";
    private static final Pattern cipherPattern = Pattern.compile("\\s*pin\\.replace\\('(\\d)','([A-Z])'\\);\\s*");

    private int uid;
    private URL redeemURL;

    public TmtopupTruemoneyRedeemer(int uid) {
        this.uid = uid;

        try {
            this.redeemURL = new URL(REEDEEM_URL.toString().replace("${uid}", Integer.toString(uid)));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // impossible
        }
    }

    @Override
    public synchronized BigDecimal redeem(String pin) throws Exception {
        return redeem(pin, "PaymentAPI", "PaymentAPI", "example@example.com");
    }

    public synchronized BigDecimal redeem(String pin, String ref1, String ref2, String email) throws Exception {
        if (!TruemoneyUtil.isValidPin(pin))
            throw new InvalidPinException("Invalid TrueMoney pin (" + pin + ") in format checking");

        HttpURLConnection connection = (HttpURLConnection) redeemURL.openConnection();
        connection.setRequestMethod("GET");

        char[] cipher = new char[10];
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;

        for (int i = 0; i < 10; ) {
            if ((line = reader.readLine()) == null) {
                if (i < 10)
                    throw new MissingCipherException("Can't complete cipher");
                break;
            }
            if (getCipher(line, cipher))
                i++;
        }

        List<String> cookies = connection.getHeaderFields().get("Set-Cookie");
        if (cookies == null)
            throw new Exception("No cookies received");

        String parsedCookie = String.join(";", cookies);

        connection = (HttpURLConnection) redeemURL.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Cookie", parsedCookie);

        Charset charset = StandardCharsets.UTF_8;

        String formData = postData;
        formData = formData.replace("${pin}", encodePin(pin, cipher));
        formData = formData.replace("${ref1}", URLEncoder.encode(ref1, charset.name()));
        formData = formData.replace("${ref2}", URLEncoder.encode(ref2, charset.name()));
        formData = formData.replace("${ref3}", URLEncoder.encode(email, charset.name()));

        connection.setDoOutput(true);
        connection.getOutputStream().write(formData.getBytes(charset));

        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }

        String reply = sb.toString();
        String[] replies = reply.split("\\|");

        if (reply.contains("ERROR|"))
            throw new TmtopupAPIException(reply.substring(reply.indexOf("ERROR|")));

        if (!reply.contains("SUCCEED|"))
            throw new TmtopupAPIException("Invalid TMTOPUP API response (" + reply + ")");

        String cid = replies[1];
        String hash = replies[2];

        while (true) {
            line = checkStatus(cid, hash);
            replies = line.trim().split("\\|");

            if (!replies[4].equals("false"))
                break;

            Thread.sleep(6000);
        }

        String status = replies[0];
        String message = replies[1];

        if (!status.equals("1")) {
            if (message.contains("รหัสบัตรเงินสดผิดพลาด"))
                throw new InvalidPinException("Unable to redeem");
            throw new TmtopupAPIException(message);
        }

        return new BigDecimal(replies[2]);
    }

    private String checkStatus(String cid, String hash) throws Exception {
        String charsetName = StandardCharsets.UTF_8.name();

        URL url = new URL(STATUS_URL.toString().replace("${cid}", URLEncoder.encode(cid, charsetName))
                .replace("${hash}", URLEncoder.encode(hash, charsetName))
                .replace("${rand}", URLEncoder.encode(Integer.toString(new Random().nextInt()), charsetName)));

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }

        return sb.toString();
    }

    public static boolean getCipher(String htmlSrc, char[] cipher) {
        Matcher matcher = cipherPattern.matcher(htmlSrc);

        if (!matcher.find())
            return false;

        matcher.reset();

        while (matcher.find())
            cipher[Integer.parseInt(matcher.group(1))] = matcher.group(2).charAt(0);

        return true;
    }

    public static char[] getCipher(String htmlSrc) {
        char[] cipher = new char[10];
        getCipher(htmlSrc, cipher);
        return cipher;
    }

    public static String encodePin(String pin, char[] cipher) {
        for (int i = 0; i <= 9; i++)
            pin = pin.replace((char) (i + '0'), cipher[i]);
        return pin;
    }
}
