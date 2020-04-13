package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.blueshift.Blueshift;
import com.blueshift.BlueshiftConstants;
import com.blueshift.BlueshiftLogger;
import com.blueshift.fcm.BlueshiftMessagingService;
import com.blueshift.inappmessage.InAppApiCallback;
import com.blueshift.model.Configuration;
import com.blueshift.model.UserInfo;
import com.blueshift.util.BlueshiftUtils;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.MParticleUser;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * This is an mParticle kit, used to extend the functionality of mParticle SDK. Most Kits are wrappers/adapters
 * to a 3rd party SDK, primarily used to map analogous public mParticle APIs onto a 3rd-party API/platform.
 *
 *
 * Follow the steps below to implement your kit:
 *
 *  - Edit ./build.gradle to add any necessary dependencies, such as your company's SDK
 *  - Rename this file/class, using your company name as the prefix, ie "AcmeKit"
 *  - View the javadocs to learn more about the KitIntegration class as well as the interfaces it defines.
 *  - Choose the additional interfaces that you need and have this class implement them,
 *    ie 'AcmeKit extends KitIntegration implements KitIntegration.PushListener'
 *
 *  In addition to this file, you also will need to edit:
 *  - ./build.gradle (as explained above)
 *  - ./README.md
 *  - ./src/main/AndroidManifest.xml
 *  - ./consumer-proguard.pro
 */
public class BlueshiftKit extends KitIntegration implements
        KitIntegration.EventListener,
        KitIntegration.CommerceListener,
        KitIntegration.UserAttributeListener,
        KitIntegration.PushListener,
        KitIntegration.IdentityListener {

    private static final String TAG = "BlueshiftKit";
    private static final String BLUESHIFT_EVENT_API_KEY = "eventApiKey";

    private static Configuration blueshiftConfiguration;

    public static void setBlueshiftConfig(@NonNull Configuration config) {
        blueshiftConfiguration = config;
    }

    public static void registerForInAppMessages(@NonNull Activity activity) {
        Blueshift.getInstance(activity).registerForInAppMessages(activity);
    }

    public static void unregisterForInAppMessages(@NonNull Activity activity) {
        Blueshift.getInstance(activity).unregisterForInAppMessages(activity);
    }

    public static void fetchInAppMessages(@NonNull Context context, InAppApiCallback callback) {
        Blueshift.getInstance(context).fetchInAppMessages(callback);
    }

    public static void displayInAppMessage(@NonNull Context context) {
        Blueshift.getInstance(context).displayInAppMessages();
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        if (blueshiftConfiguration == null) {
            BlueshiftLogger.d(TAG, "Blueshift configuration is not provided. Using the default one.");
            blueshiftConfiguration = new Configuration();
        }

        String apiKey = settings.get(BLUESHIFT_EVENT_API_KEY);
        if (KitUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("Blueshift requires a valid API key");
        } else {
            blueshiftConfiguration.setApiKey(apiKey);
        }

        // set app-icon as notification icon if not set
        if (blueshiftConfiguration.getAppIcon() == 0) {
            try {
                ApplicationInfo applicationInfo = getContext().getApplicationInfo();
                blueshiftConfiguration.setAppIcon(applicationInfo.icon);
            } catch (Exception e) {
                throw new IllegalArgumentException("Blueshift requires a valid app icon resource id");
            }
        }

        Blueshift.getInstance(context).initialize(blueshiftConfiguration);

        return null;
    }

    private int getResourceIdFromString(String resourceType, String resourceName) {
        try {
            if (!KitUtils.isEmpty(resourceType) && !KitUtils.isEmpty(resourceName)) {
                return getContext()
                        .getResources()
                        .getIdentifier(resourceName, resourceType, getContext().getPackageName());
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return -1;
    }

    private int getDrawableIdFromString(String resourceName) {
        return getResourceIdFromString("drawable", resourceName);
    }

    private int getStyleIdFromString(String resourceName) {
        return getResourceIdFromString("style", resourceName);
    }

    private Class getClassFromName(String classname) {
        Class<?> clazz = null;

        if (!KitUtils.isEmpty(classname)) {
            try {
                clazz = Class.forName(classname);
            } catch (ClassNotFoundException e) {
                BlueshiftLogger.e(TAG, e);
            }
        }

        return clazz;
    }

    private long getLongFromString(String longString) {
        long value = -1;

        try {
            value = Long.parseLong(longString);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return value;
    }

    private boolean getBooleanFromString(String boolString) {
        boolean value = false;

        try {
            if (!KitUtils.isEmpty(boolString)) {
                value = Boolean.valueOf(boolString);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return value;
    }

    @Override
    public String getName() {
        return "Blueshift";
    }

    // ** event logging support methods **

    private HashMap<String, Object> getExtras(Map<String, String> extras) {
        HashMap<String, Object> newExtras = new HashMap<>();

        if (extras != null) {
            for (Map.Entry<String, String> entry : extras.entrySet()) {
                newExtras.put(entry.getKey(), entry.getValue());
            }
        }

        return newExtras;
    }

    // ** KitIntegration.EventListener **

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        return null;
    }

    @Override
    public List<ReportingMessage> leaveBreadcrumb(String s) {
        return null;
    }

    @Override
    public List<ReportingMessage> logError(String s, Map<String, String> map) {
        return null;
    }

    @Override
    public List<ReportingMessage> logException(Exception e, Map<String, String> map, String s) {
        return null;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> map) {
        HashMap<String, Object> extras = getExtras(map);
        extras.put(BlueshiftConstants.KEY_SCREEN_VIEWED, screenName);

        Blueshift.getInstance(getContext())
                .trackEvent(BlueshiftConstants.EVENT_PAGE_LOAD, extras, false);

        List<ReportingMessage> messages = new LinkedList<>();
        messages.add(new ReportingMessage(this, ReportingMessage.MessageType.SCREEN_VIEW, System.currentTimeMillis(), map));
        return messages;
    }

    @Nullable
    @Override
    public List<ReportingMessage> logEvent(@NonNull MPEvent event) {
        HashMap<String, Object> extras = getExtras(event.getCustomAttributes());

        Blueshift.getInstance(getContext())
                .trackEvent(event.getEventName(), extras, false);

        List<ReportingMessage> messages = new LinkedList<>();
        messages.add(ReportingMessage.fromEvent(this, event));
        return messages;
    }

    // ** KitIntegration.CommerceListener **

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal bigDecimal, BigDecimal bigDecimal1, String s, Map<String, String> map) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent commerceEvent) {
        HashMap<String, Object> extras = getExtras(commerceEvent.getCustomAttributes());

        String eventName = commerceEvent.getEventName();
        if (eventName != null) {
            Blueshift.getInstance(getContext())
                    .trackEvent(eventName, extras, false);
        }

        List<ReportingMessage> messages = new LinkedList<>();
        messages.add(ReportingMessage.fromEvent(this, commerceEvent));
        return messages;
    }

    // ** KitIntegration.UserAttributeListener **

    @Override
    public void onIncrementUserAttribute(String s, int i, String s1, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onRemoveUserAttribute(String s, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetUserAttribute(String key, Object value, FilteredMParticleUser filteredMParticleUser) {
        UserInfo userInfo = UserInfo.getInstance(getContext());

        if (key != null) {
            switch (key) {
                case MParticle.UserAttributes.FIRSTNAME:
                    if (value != null) userInfo.setFirstname(String.valueOf(value));
                    break;
                case MParticle.UserAttributes.LASTNAME:
                    if (value != null) userInfo.setLastname(String.valueOf(value));
                    break;
                case MParticle.UserAttributes.GENDER:
                    if (value != null) userInfo.setGender(String.valueOf(value));
                    break;
                case MParticle.UserAttributes.AGE:
                    // No setter
                    break;
                case MParticle.UserAttributes.ADDRESS:
                    // No setter
                    break;
                case MParticle.UserAttributes.MOBILE_NUMBER:
                    // No setter
                    break;
                case MParticle.UserAttributes.CITY:
                    // No Setter
                    break;
                case MParticle.UserAttributes.STATE:
                    // No setter
                    break;
                case MParticle.UserAttributes.ZIPCODE:
                    // No setter
                    break;
                case MParticle.UserAttributes.COUNTRY:
                    // No setter
                    break;
            }

            userInfo.save(getContext());
        }
    }

    @Override
    public void onSetUserTag(String s, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetUserAttributeList(String s, List<String> list, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public void onSetAllUserAttributes(Map<String, String> map, Map<String, List<String>> map1, FilteredMParticleUser filteredMParticleUser) {

    }

    @Override
    public boolean supportsAttributeLists() {
        return false;
    }

    @Override
    public void onConsentStateUpdated(ConsentState consentState, ConsentState consentState1, FilteredMParticleUser filteredMParticleUser) {

    }

    // ** KitIntegration.PushListener **

    @Override
    public boolean willHandlePushMessage(Intent intent) {
        Configuration config = BlueshiftUtils.getConfiguration(getContext());
        if (config != null && !config.isPushEnabled()) {
            return false;
        }

        return BlueshiftMessagingService.isBlueshiftPush(getContext(), intent);
    }

    @Override
    public void onPushMessageReceived(Context context, Intent intent) {
        BlueshiftMessagingService.handlePushMessage(context, intent);
    }

    @Override
    public boolean onPushRegistration(String instanceId, String senderId) {
        return false; // Blueshift depends on mP to do the push registration
    }

    // ** KitIntegration.IdentityListener **

    @Override
    public void onIdentifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    @Override
    public void onLoginCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    @Override
    public void onLogoutCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    @Override
    public void onModifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest filteredIdentityApiRequest) {
        updateUser(mParticleUser);
    }

    @Override
    public void onUserIdentified(MParticleUser mParticleUser) {
        updateUser(mParticleUser);
    }

    private void updateUser(MParticleUser user) {
        if (user != null) {
            UserInfo userInfo = UserInfo.getInstance(getContext());

            String email = user.getUserIdentities().get(MParticle.IdentityType.Email);
            userInfo.setEmail(email);

            String customerId = user.getUserIdentities().get(MParticle.IdentityType.CustomerId);
            userInfo.setRetailerCustomerId(customerId);

            String fbId = user.getUserIdentities().get(MParticle.IdentityType.Facebook);
            userInfo.setFacebookId(fbId);

            userInfo.save(getContext());

            // whenever user is updated, and email is non-empty, we should call an identify
            if (email != null) {
                Blueshift.getInstance(getContext())
                        .identifyUserByEmail(email, null, false);
            }
        }
    }
}