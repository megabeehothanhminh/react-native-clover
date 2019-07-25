package com.infuse.clover.bridge.payments;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import com.clover.connector.sdk.v3.PaymentConnector;
import com.clover.sdk.v3.connector.ExternalIdUtils;
import com.clover.sdk.v3.merchant.TipSuggestion;
import com.clover.sdk.v3.payments.DataEntryLocation;
import com.clover.sdk.v3.payments.TipMode;
import com.clover.sdk.v3.remotepay.ManualRefundRequest;
import com.clover.sdk.v3.remotepay.RefundPaymentRequest;
import com.clover.sdk.v3.remotepay.SaleRequest;
import com.clover.sdk.v3.remotepay.VoidPaymentRefundRequest;
import com.clover.sdk.v3.remotepay.VoidPaymentRequest;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;

import java.util.ArrayList;
import java.util.List;

public class BridgePaymentConnector {
    private PaymentConnector paymentConnector;
    private Promise paymentPromise;

    public BridgePaymentConnector(Context context, Account account, String raid) {
        BridgePaymentConnectorListener bridgePaymentConnectorListener = new BridgePaymentConnectorListener() {
            @Override
            public void onResult(WritableMap result) {
                paymentPromise.resolve(result);
            }
        };
        PaymentConnectorListener paymentConnectorListener = new PaymentConnectorListener(bridgePaymentConnectorListener);
        paymentConnector = new PaymentConnector(context, account, paymentConnectorListener, raid);
    }

    public void sale(ReadableMap options, Promise promise) {
        paymentPromise = promise;
        String externalPaymentId = getExternalPaymentId(options);

        // Check for validation errors
        List<String> basicRequiredFields = new ArrayList<>();
        basicRequiredFields.add("amount");
        List<String> validationErrors = validateOptions(options, basicRequiredFields);

        if (!validationErrors.isEmpty()) {
            promise.reject("validation_error", validationErrors.toString());
        } else if (externalPaymentId == null) {
            promise.reject("validation_error", "Missing required externalPaymentId or generateExternalPaymentId flag");
        } else {
            long amount = options.getInt("amount");

            SaleRequest saleRequest = new SaleRequest();
            saleRequest.setAmount(amount);
            saleRequest.setExternalId(externalPaymentId);

            // Set optional transaction settings
            if (options.hasKey(Payments.CARD_ENTRY_METHODS)) {
                saleRequest.setCardEntryMethods(options.getInt(Payments.CARD_ENTRY_METHODS));
            }
            if (options.hasKey(Payments.DISABLE_RESTART_TRANSACTION_ON_FAIL)) {
                saleRequest.setDisableRestartTransactionOnFail(options.getBoolean(Payments.DISABLE_RESTART_TRANSACTION_ON_FAIL));
            }
            if (options.hasKey(Payments.DISABLE_DUPLICATE_CHECKING)) {
                saleRequest.setDisableDuplicateChecking(options.getBoolean(Payments.DISABLE_DUPLICATE_CHECKING));
            }
            if (options.hasKey(Payments.DISABLE_PRINTING)) {
                saleRequest.setDisablePrinting(options.getBoolean(Payments.DISABLE_PRINTING));
            }
            if (options.hasKey(Payments.DISABLE_RECEIPT_SELECTION)) {
                saleRequest.setDisableReceiptSelection(options.getBoolean(Payments.DISABLE_RECEIPT_SELECTION));
            }
            if (options.hasKey(Payments.SIGNATURE_ENTRY_LOCATION)) {
                DataEntryLocation location = DataEntryLocation.valueOf(options.getString(Payments.SIGNATURE_ENTRY_LOCATION));
                saleRequest.setSignatureEntryLocation(location);
            }
            if (options.hasKey(Payments.SIGNATURE_THRESHOLD)) {
                saleRequest.setSignatureThreshold((long) options.getInt(Payments.SIGNATURE_THRESHOLD));
            }
            if (options.hasKey(Payments.AUTO_ACCEPT_SIGNATURE)) {
                saleRequest.setAutoAcceptSignature(options.getBoolean(Payments.AUTO_ACCEPT_SIGNATURE));
            }
            if (options.hasKey(Payments.TIP_AMOUNT)) {
                saleRequest.setTipAmount((long) options.getInt(Payments.TIP_AMOUNT));
            }
            if (options.hasKey(Payments.TIPPABLE_AMOUNT)
                    && !(options.getType(Payments.TIPPABLE_AMOUNT) == ReadableType.Null)) {
                saleRequest.setTippableAmount((long) options.getInt(Payments.TIPPABLE_AMOUNT));
            }
            if (options.hasKey(Payments.TIP_MODE)) {
                TipMode tipMode = TipMode.valueOf(options.getString(Payments.TIP_MODE));
                saleRequest.setTipMode(tipMode);
            }
            if (options.hasKey(Payments.TIP_SUGGESTIONS)) {
                ReadableArray tipSuggestions = options.getArray(Payments.TIP_SUGGESTIONS);
                saleRequest.setTipSuggestions(buildTipSuggestions(tipSuggestions));
            }

            paymentConnector.sale(saleRequest);
        }
    }

    public void refundPayment(ReadableMap options, Promise promise) {
        paymentPromise = promise;

        // Check for validation errors
        List<String> basicRequiredFields = new ArrayList<>();
        basicRequiredFields.add("paymentId");
        basicRequiredFields.add("orderId");
        List<String> validationErrors = validateOptions(options, basicRequiredFields);

        if (!validationErrors.isEmpty()) {
            promise.reject("validation_error", validationErrors.toString());
        } else {
            RefundPaymentRequest refundPaymentRequest = new RefundPaymentRequest();
            refundPaymentRequest.setOrderId(options.getString("orderId"));
            refundPaymentRequest.setPaymentId(options.getString("paymentId"));

            // Set refund amount or fullAmount
            if (options.hasKey("amount")) {
                refundPaymentRequest.setAmount((long) options.getInt("amount"));
            } else {
                refundPaymentRequest.setFullRefund(true);
            }

            // Set optional transaction settings
            // disablePrinting - permanent default true
            // disableReceiptSelection - permanent default true

            paymentConnector.refundPayment(refundPaymentRequest);
        }
    }

    public void manualRefund(ReadableMap options, Promise promise) {
        paymentPromise = promise;
        String externalPaymentId = getExternalPaymentId(options);

        // Check for validation errors
        List<String> basicRequiredFields = new ArrayList<>();
        basicRequiredFields.add("amount");
        List<String> validationErrors = validateOptions(options, basicRequiredFields);

        if (!validationErrors.isEmpty()) {
            promise.reject("validation_error", validationErrors.toString());
        } else if (externalPaymentId == null) {
            promise.reject("validation_error", "Missing required externalPaymentId or generateExternalPaymentId flag");
        } else {
            ManualRefundRequest manualRefundRequest = new ManualRefundRequest();
            manualRefundRequest.setAmount((long) options.getInt("amount"));
            manualRefundRequest.setExternalId(externalPaymentId);

            // Set optional transaction settings
            if (options.hasKey(Payments.CARD_ENTRY_METHODS)) {
                manualRefundRequest.setCardEntryMethods(options.getInt(Payments.CARD_ENTRY_METHODS));
            }
            if (options.hasKey(Payments.DISABLE_RESTART_TRANSACTION_ON_FAIL)) {
                manualRefundRequest.setDisableRestartTransactionOnFail(options.getBoolean(Payments.DISABLE_RESTART_TRANSACTION_ON_FAIL));
            }
            // disablePrinting - permanent default true
            // disableReceiptSelection - permanent default true
            // disableDuplicateChecking - permanent default false

            paymentConnector.manualRefund(manualRefundRequest);
        }
    }

    public void voidPayment(ReadableMap options, Promise promise) {
        paymentPromise = promise;

        // Check for validation errors
        List<String> basicRequiredFields = new ArrayList<>();
        basicRequiredFields.add("paymentId");
        basicRequiredFields.add("orderId");
        basicRequiredFields.add("voidReason");
        List<String> validationErrors = validateOptions(options, basicRequiredFields);

        if (!validationErrors.isEmpty()) {
            promise.reject("validation_error", validationErrors.toString());
        } else {
            VoidPaymentRequest voidPaymentRequest = new VoidPaymentRequest();
            voidPaymentRequest.setOrderId(options.getString("orderId"));
            voidPaymentRequest.setPaymentId(options.getString("paymentId"));
            voidPaymentRequest.setVoidReason(options.getString("voidReason"));

            paymentConnector.voidPayment(voidPaymentRequest);
        }
    }

    public void voidPaymentRefund(ReadableMap options, Promise promise) {
        paymentPromise = promise;
        String refundId = options.getString("refundId");
        String orderId = options.getString("orderId");

        VoidPaymentRefundRequest voidPaymentRefundRequest = new VoidPaymentRefundRequest();
        voidPaymentRefundRequest.setOrderId(orderId);
        voidPaymentRefundRequest.setRefundId(refundId);

        paymentConnector.voidPaymentRefund(voidPaymentRefundRequest);
    }

    private String getExternalPaymentId(ReadableMap options) {
        if (options.hasKey("externalPaymentId")) {
            return options.getString("externalPaymentId");
        } else if (options.hasKey("generateExternalPaymentId") && options.getBoolean("generateExternalPaymentId")) {
            return ExternalIdUtils.generateNewID();
        }
        return null;
    }

    private List<String> validateOptions(ReadableMap options, List<String> fields) {
        List<String> errors = new ArrayList<>();
        for (String field : fields) {
            if (!options.hasKey(field)) {
                errors.add("Missing required field: " + field);
            }
        }
        return errors;
    }

    private List<TipSuggestion> buildTipSuggestions(ReadableArray tips) {
        final String NAME_PARAMETER = "name";
        final String PERCENTAGE_PARAMETER = "percentage";
        List<TipSuggestion> tipSuggestions = new ArrayList<>();
        for (int i = 0; i < tips.size(); i++) {
            ReadableMap tip = tips.getMap(i);
            if (tip.hasKey(NAME_PARAMETER) && tip.getType(NAME_PARAMETER) == ReadableType.String
                    && tip.hasKey(PERCENTAGE_PARAMETER) && tip.getType(PERCENTAGE_PARAMETER) == ReadableType.Number) {
                TipSuggestion tipSuggestion = new TipSuggestion();
                tipSuggestion.setName(tip.getString(NAME_PARAMETER));
                tipSuggestion.setPercentage((long) tip.getInt(PERCENTAGE_PARAMETER));
                tipSuggestions.add(tipSuggestion);
            } else {
                Log.e("Payments", "Skipping invalid TipSuggestion at index " + i);
            }
        }
        return tipSuggestions;
    }
}
