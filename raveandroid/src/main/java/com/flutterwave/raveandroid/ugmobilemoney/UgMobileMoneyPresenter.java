package com.flutterwave.raveandroid.ugmobilemoney;

import com.flutterwave.raveandroid.RavePayInitializer;
import com.flutterwave.raveandroid.ViewObject;
import com.flutterwave.raveandroid.data.DeviceIdGetter;
import com.flutterwave.raveandroid.data.events.ScreenLaunchEvent;
import com.flutterwave.raveandroid.rave_java_commons.Payload;
import com.flutterwave.raveandroid.rave_java_commons.RaveConstants;
import com.flutterwave.raveandroid.rave_logger.EventLogger;
import com.flutterwave.raveandroid.rave_presentation.data.PayloadBuilder;
import com.flutterwave.raveandroid.rave_presentation.data.PayloadEncryptor;
import com.flutterwave.raveandroid.rave_presentation.ugmobilemoney.UgMobileMoneyHandler;
import com.flutterwave.raveandroid.rave_remote.RemoteRepository;
import com.flutterwave.raveandroid.validators.AmountValidator;
import com.flutterwave.raveandroid.validators.PhoneValidator;

import java.util.HashMap;

import javax.inject.Inject;

import static com.flutterwave.raveandroid.rave_java_commons.RaveConstants.NG;
import static com.flutterwave.raveandroid.rave_java_commons.RaveConstants.UGX;
import static com.flutterwave.raveandroid.rave_java_commons.RaveConstants.fieldAmount;
import static com.flutterwave.raveandroid.rave_java_commons.RaveConstants.fieldPhone;
import static com.flutterwave.raveandroid.rave_java_commons.RaveConstants.validAmountPrompt;
import static com.flutterwave.raveandroid.rave_java_commons.RaveConstants.validPhonePrompt;

/**
 * Created by Jeremiah on 10/12/2018.
 */


public class UgMobileMoneyPresenter extends UgMobileMoneyHandler implements UgMobileMoneyUiContract.UserActionsListener {

    private UgMobileMoneyUiContract.View mView;

    @Inject
    EventLogger eventLogger;
    @Inject
    RemoteRepository networkRequest;
    @Inject
    AmountValidator amountValidator;
    @Inject
    PhoneValidator phoneValidator;
    @Inject
    DeviceIdGetter deviceIdGetter;
    @Inject
    PayloadEncryptor payloadEncryptor;

    @Inject
    public UgMobileMoneyPresenter(UgMobileMoneyUiContract.View mView) {
        super(mView);
        this.mView = mView;
    }

    @Override
    public void onDataCollected(HashMap<String, ViewObject> dataHashMap) {

         boolean valid = true;

        int amountID = dataHashMap.get(fieldAmount).getViewId();
        String amount = dataHashMap.get(fieldAmount).getData();
        Class amountViewType = dataHashMap.get(fieldAmount).getViewType();

        int phoneID = dataHashMap.get(fieldPhone).getViewId();
        String phone = dataHashMap.get(fieldPhone).getData();
        Class phoneViewType = dataHashMap.get(fieldPhone).getViewType();

        boolean isAmountValidated = amountValidator.isAmountValid(amount);
        boolean isPhoneValid = phoneValidator.isPhoneValid(phone);

        if (!isAmountValidated) {
            valid = false;
            mView.showFieldError(amountID, validAmountPrompt, amountViewType);
        }

        if (!isPhoneValid) {
                valid = false;
            mView.showFieldError(phoneID, validPhonePrompt, phoneViewType);
        }

         if (valid) {
             mView.onValidationSuccessful(dataHashMap);
         }

    }

    @Override
    public void processTransaction(HashMap<String, ViewObject> dataHashMap, RavePayInitializer ravePayInitializer) {

        if (ravePayInitializer!=null) {

            ravePayInitializer.setAmount(Double.parseDouble(dataHashMap.get(RaveConstants.fieldAmount).getData()));

            String deviceID = deviceIdGetter.getDeviceId();


            PayloadBuilder builder = new PayloadBuilder();
            builder.setAmount(ravePayInitializer.getAmount() + "")
//                    .setCountry(ravePayInitializer.getCountry())
                    .setCountry(NG) //Country has to be set to NG for UGX payments (as at 10/12/2018)
                    .setCurrency(ravePayInitializer.getCurrency())
                    .setEmail(ravePayInitializer.getEmail())
                    .setFirstname(ravePayInitializer.getfName())
                    .setLastname(ravePayInitializer.getlName())
                    .setIP(deviceID)
                    .setTxRef(ravePayInitializer.getTxRef())
                    .setMeta(ravePayInitializer.getMeta())
                    .setSubAccount(ravePayInitializer.getSubAccount())
                    .setNetwork(UGX)
                    .setPhonenumber(dataHashMap.get(fieldPhone).getData())
                    .setPBFPubKey(ravePayInitializer.getPublicKey())
                    .setIsPreAuth(ravePayInitializer.getIsPreAuth())
                    .setDevice_fingerprint(deviceID);

            if (ravePayInitializer.getPayment_plan() != null) {
                builder.setPaymentPlan(ravePayInitializer.getPayment_plan());
            }

            Payload body = builder.createUgMobileMoneyPayload();

            if (ravePayInitializer.getIsDisplayFee()) {
                fetchFee(body);
            } else {
                chargeUgMobileMoney(body, ravePayInitializer.getEncryptionKey());
            }
        }
    }

    @Override
    public void init(RavePayInitializer ravePayInitializer) {

        if (ravePayInitializer != null) {
            logEvent(new ScreenLaunchEvent("UG Mobile Money Fragment").getEvent(),
                    ravePayInitializer.getPublicKey());

            boolean isAmountValid = amountValidator.isAmountValid(ravePayInitializer.getAmount());
            if (isAmountValid) {
                mView.onAmountValidationSuccessful(String.valueOf(ravePayInitializer.getAmount()));
            }
        }

    }

    @Override
    public void onAttachView(UgMobileMoneyUiContract.View view) {
        this.mView = view;
    }

    @Override
    public void onDetachView() {
        this.mView = new NullUgMobileMoneyView();
    }
}


