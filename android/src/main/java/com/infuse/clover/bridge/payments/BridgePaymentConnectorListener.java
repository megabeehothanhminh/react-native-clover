package com.infuse.clover.bridge.payments;

import com.facebook.react.bridge.WritableMap;

interface BridgePaymentConnectorListener {
    void onResult(WritableMap result);
}

