package vn.com.phamvantruongit.apppaypal;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;

public class Config {

    // PayPal app configuration
    public static final String PAYPAL_CLIENT_ID = "AbLgy0hRsq0PmoGK-ws2-jlBIeBVKUUU0xRjbfW1-GAckylz_TDNsh1cMrIiSksc2wpqYC2PisTrKhko";
    public static final String PAYPAL_CLIENT_SECRET = "";

    public static final String PAYPAL_ENVIRONMENT = PayPalConfiguration.ENVIRONMENT_SANDBOX;
    public static final String PAYMENT_INTENT = PayPalPayment.PAYMENT_INTENT_SALE;
    public static final String DEFAULT_CURRENCY = "USD";

    // PayPal server urls
    public static final String URL_PRODUCTS = "http://192.168.0.103/PayPalServer/v1/products";
    public static final String URL_VERIFY_PAYMENT = "http://192.168.0.103/PayPalServer/v1/verifyPayment";

}