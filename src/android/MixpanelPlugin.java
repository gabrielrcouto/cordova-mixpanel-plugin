package com.samz.cordova.mixpanel;

import android.content.Context;
import android.text.TextUtils;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import java.util.Map;
import java.util.HashMap;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MixpanelPlugin extends CordovaPlugin {

    private static String LOG_TAG = "MIXPANEL PLUGIN";
    private static MixpanelAPI mixpanel;

    private enum Action {


        // MIXPANEL API


        ALIAS("alias"),
        FLUSH("flush"),
        IDENTIFY("identify"),
        INIT("init"),
        RESET("reset"),
        TRACK("track"),
        //@MEMED
        REGISTER_SUPER_PROPERTIES("register_super_properties"),

        // PEOPLE API

        PEOPLE_SET("people_set"),
        PEOPLE_IDENTIFY("people_identify"),

        //@MEMED - PUSH NOTIFICATIONS

        PEOPLE_INIT_PUSH_HANDLING("people_init_push_handling");

        private final String name;
        private static final Map<String, Action> lookup = new HashMap<String, Action>();

        static {
            for (Action a : Action.values()) lookup.put(a.getName(), a);
        }

        private Action(String name) { this.name = name; }
        public String getName() { return name; }
        public static Action get(String name) { return lookup.get(name); }
    }


    /**
     * helper fn that logs the err and then calls the err callback
     */
    private void error(CallbackContext cbCtx, String message) {
        LOG.e(LOG_TAG, message);
        cbCtx.error(message);
    }


    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext cbCtx) {
        // throws JSONException
        Action act = Action.get(action);

        if (act == null){
            this.error(cbCtx, "unknown action");
            return false;
        }

        if (mixpanel == null && Action.INIT != act) {
            this.error(cbCtx, "you must initialize mixpanel first using \"init\" action");
            return false;
        }

        switch (act) {
            case ALIAS:
                return handleAlias(args, cbCtx);
            case FLUSH:
                return handleFlush(args, cbCtx);
            case IDENTIFY:
                return handleIdentify(args, cbCtx);
            case INIT:
                return handleInit(args, cbCtx);
            case RESET:
                return handleReset(args, cbCtx);
            case TRACK:
                return handleTrack(args, cbCtx);
            case PEOPLE_SET:
                return handlePeopleSet(args, cbCtx);
            case PEOPLE_IDENTIFY:
                return handlePeopleIdentify(args, cbCtx);
            case PEOPLE_INIT_PUSH_HANDLING:
                return handlePeopleInitPushHandling(args, cbCtx);
            case REGISTER_SUPER_PROPERTIES:
                return handleRegisterSuperProperties(args, cbCtx);
            default:
                this.error(cbCtx, "unknown action");
                return false;
        }
    }


    @Override
    public void onDestroy() {
        if (mixpanel != null) {
            mixpanel.flush();
        }
        super.onDestroy();
    }


    //************************************************
    //  ACTION HANDLERS
    //   - return true:
    //     - to indicate action was executed with correct arguments
    //     - also if the action from sdk has failed.
    //  - return false:
    //     - arguments were wrong
    //************************************************

    private boolean handleAlias(JSONArray args, final CallbackContext cbCtx) {
        String aliasId = args.optString(0, "");
        String originalId = args.optString(1, null);
        if (TextUtils.isEmpty(aliasId)) {
            this.error(cbCtx, "missing alias id");
            return false;
        }
        mixpanel.alias(aliasId, originalId);
        cbCtx.success();
        return true;
    }


    private boolean handleFlush(JSONArray args, final CallbackContext cbCtx) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mixpanel.flush();
                cbCtx.success();
            }
        };
        cordova.getThreadPool().execute(runnable);
        cbCtx.success();
        return true;
    }


    private boolean handleIdentify(JSONArray args, final CallbackContext cbCtx) {
        String uniqueId = args.optString(0, "");
        if (TextUtils.isEmpty(uniqueId)) {
            this.error(cbCtx, "missing unique id");
            return false;
        }
        mixpanel.identify(uniqueId);
        cbCtx.success();
        return true;
    }


    private boolean handleInit(JSONArray args, final CallbackContext cbCtx) {
        String token = args.optString(0, "");
        if (TextUtils.isEmpty(token)) {
            this.error(cbCtx, "missing token for mixpanel project");
            return false;
        }
        Context ctx = cordova.getActivity();
        mixpanel = MixpanelAPI.getInstance(ctx, token);
        cbCtx.success();
        return true;
    }


    private boolean handleReset(JSONArray args, final CallbackContext cbCtx) {
        mixpanel.reset();
        cbCtx.success();
        return true;
    }


    private boolean handleTrack(JSONArray args, final CallbackContext cbCtx) {
        String event = args.optString(0, "");
        if (TextUtils.isEmpty(event)) {
            this.error(cbCtx, "missing event name");
            return false;
        }

        JSONObject properties = args.optJSONObject(1);
        if (properties == null) {
            properties = new JSONObject();
        }
        mixpanel.track(event, properties);
        cbCtx.success();
        return true;
    }


    private boolean handlePeopleIdentify(JSONArray args, final CallbackContext cbCtx) {
        String distinctId = args.optString(0, "");
        if (TextUtils.isEmpty(distinctId)) {
            this.error(cbCtx, "missing distinct id");
            return false;
        }
        mixpanel.getPeople().identify(distinctId);
        cbCtx.success();
        return true;
    }


    private boolean handlePeopleSet(JSONArray args, final CallbackContext cbCtx) {
        JSONObject properties = args.optJSONObject(0);
        if (properties == null) {
            this.error(cbCtx, "missing people properties object");
            return false;
        }
        mixpanel.getPeople().set(properties);
        cbCtx.success();
        return true;
    }

    //@MEMED - Função nova
    private boolean handlePeopleInitPushHandling(JSONArray args, final CallbackContext cbCtx) {
        String google12Digit = args.optString(0, "");
        if (TextUtils.isEmpty(google12Digit)) {
            this.error(cbCtx, "missing 12 digit project number of your Google API");
            return false;
        }
        mixpanel.getPeople().initPushHandling(google12Digit);
        cbCtx.success();
        return true;
    }

    //@MEMED - Função nova
    private boolean handleRegisterSuperProperties(JSONArray args, final CallbackContext cbCtx) {
        JSONObject properties = args.optJSONObject(0);
        if (properties == null) {
            this.error(cbCtx, "missing properties object");
            return false;
        }
        mixpanel.registerSuperProperties(properties);
        cbCtx.success();
        return true;
    }
}