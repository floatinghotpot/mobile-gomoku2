package com.rjfun.cordova.plugin;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.mediation.admob.AdMobExtras;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import java.util.Iterator;
import java.util.Random;

import android.provider.Settings;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class represents the native implementation for the AdMob Cordova plugin.
 * This plugin can be used to request AdMob ads natively via the Google AdMob SDK.
 * The Google AdMob SDK is a dependency for this plugin.
 */
public class EasyAdMob extends CordovaPlugin {
    /** Common tag used for logging statements. */
    private static final String LOGTAG = "EasyAdMob";
    private static final String DEFAULT_PUBLISHER_ID = "ca-app-pub-6869992474017983/9375997553";
    
    /** Cordova Actions. */
    private static final String ACTION_SET_OPTIONS = "setOptions";
    private static final String ACTION_SHOW_BANNER = "showBanner";
    private static final String ACTION_REMOVE_BANNER = "removeBanner";
    private static final String ACTION_REQUEST_INTERSTITIAL = "requestInterstitial";
    private static final String ACTION_SHOW_INTERSTITIAL = "showInterstitial";
    
    /* options */
	private static final String OPT_PUBLISHER_ID = "publisherId";
	private static final String OPT_INTERSTITIAL_AD_ID = "interstitialAdId";
	private static final String OPT_AD_SIZE = "adSize";
	private static final String OPT_BANNER_AT_TOP = "bannerAtTop";
	private static final String OPT_OVERLAP = "overlap";
	private static final String OPT_OFFSET_TOPBAR = "offsetTopBar";
	private static final String OPT_IS_TESTING = "isTesting";
	private static final String OPT_AD_EXTRAS = "adExtras";
	private static final String OPT_AUTO_SHOW = "autoShow";
    
    /** The adView to display to the user. */
    private AdView adView = null;
    /** if want banner view overlap webview, we will need this layout */
    private RelativeLayout adViewLayout = null;
    
    /** The interstitial ad to display to the user. */
    private InterstitialAd interstitialAd = null;
    
    private String publisherId = DEFAULT_PUBLISHER_ID;
    private AdSize adSize = AdSize.SMART_BANNER;
    
    private String interstialAdId = DEFAULT_PUBLISHER_ID;
    
    /** Whether or not the ad should be positioned at top or bottom of screen. */
    private boolean bannerAtTop = false;
    /** Whether or not the banner will overlap the webview instead of push it up or down */
    private boolean bannerOverlap = false;
    private boolean offsetTopBar = false;
	private boolean isTesting = false;
	private boolean bannerShow = false;
	private JSONObject adExtras = null;

	private boolean autoShow = false;

    @Override
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
        PluginResult result = null;
        if (ACTION_SET_OPTIONS.equals(action)) {
            JSONObject options = inputs.optJSONObject(0);
            result = executeSetOptions(options, callbackContext);
            
        } else if (ACTION_SHOW_BANNER.equals(action)) {
            boolean show = inputs.optBoolean(0);
            JSONObject options = inputs.optJSONObject(1);
            result = executeShowBanner(show, options, callbackContext);
            
        } else if (ACTION_REMOVE_BANNER.equals(action)) {
            result = removeBanner(callbackContext);
            
        } else if (ACTION_REQUEST_INTERSTITIAL.equals(action)) {
        	JSONObject options = inputs.optJSONObject(0);
            result = requestInterstitial(options, callbackContext);
            
        } else if (ACTION_SHOW_INTERSTITIAL.equals(action)) {
            result = showInterstitial(callbackContext);
            
        } else {
            Log.d(LOGTAG, String.format("Invalid action passed: %s", action));
            result = new PluginResult(Status.INVALID_ACTION);
        }
        
        if(result != null) callbackContext.sendPluginResult( result );
        
        return true;
    }
    
    private PluginResult executeSetOptions(JSONObject options, CallbackContext callbackContext) {
    	Log.w(LOGTAG, "executeSetOptions");
    	
    	this.setOptions( options );
    	
    	callbackContext.success();
    	return null;
	}
    
    private void setOptions( JSONObject options ) {
    	if(options.has(OPT_PUBLISHER_ID)) this.publisherId = options.optString( OPT_PUBLISHER_ID );
    	if(options.has(OPT_INTERSTITIAL_AD_ID)) this.interstialAdId = options.optString( OPT_INTERSTITIAL_AD_ID );
    	if(options.has(OPT_AD_SIZE)) this.adSize = adSizeFromString( options.optString( OPT_AD_SIZE ) );
    	if(options.has(OPT_BANNER_AT_TOP)) this.bannerAtTop = options.optBoolean( OPT_BANNER_AT_TOP );
    	if(options.has(OPT_OVERLAP)) this.bannerOverlap = options.optBoolean( OPT_OVERLAP );
    	if(options.has(OPT_OFFSET_TOPBAR)) this.offsetTopBar = options.optBoolean( OPT_OFFSET_TOPBAR );
    	if(options.has(OPT_IS_TESTING)) this.isTesting  = options.optBoolean( OPT_IS_TESTING );
    	if(options.has(OPT_AD_EXTRAS)) this.adExtras  = options.optJSONObject( OPT_AD_EXTRAS );
    	if(options.has(OPT_AUTO_SHOW)) this.autoShow  = options.optBoolean( OPT_AUTO_SHOW );
    }
    
    private PluginResult executeShowBanner(boolean show, JSONObject options, CallbackContext callbackContext) {
    	Log.w(LOGTAG, "executeShowBanner");
    	
    	this.bannerShow  = show;
    	
    	this.setOptions( options );

        if(this.publisherId.length() == 0) this.publisherId = DEFAULT_PUBLISHER_ID;

        Log.w(LOGTAG, String.format("publisherId: '%s'", this.publisherId));
        
        if(adView != null) {
        	if( show ) {
        		return requestBannerAd(options, callbackContext );
        	} else {
        		return showBannerAd(false, callbackContext);
        	}
        }
        
    	return createBannerAd(callbackContext);
	}

    private PluginResult createBannerAd(final CallbackContext callbackContext) {
    	Log.w(LOGTAG, "createBannerAd");
    	
        cordova.getActivity().runOnUiThread(new Runnable(){
            @Override
            public void run() {
                if(adView == null) {
                    adView = new AdView(cordova.getActivity());
                    adView.setAdUnitId(publisherId);
                    adView.setAdSize(adSize);
                    adView.setAdListener(new BannerListener());
                }
                if (adView.getParent() != null) {
                    ((ViewGroup)adView.getParent()).removeView(adView);
                }
                if(bannerOverlap) {
                    ViewGroup parentView = (ViewGroup) webView;
                    
                    adViewLayout = new RelativeLayout(cordova.getActivity());
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT);
                    parentView.addView(adViewLayout, params);
                    
                    RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                    params2.addRule(bannerAtTop ? RelativeLayout.ALIGN_PARENT_TOP : RelativeLayout.ALIGN_PARENT_BOTTOM);
                    adViewLayout.addView(adView, params2);
                    
                } else {
                    ViewGroup parentView = (ViewGroup) webView.getParent();
                    if (bannerAtTop) {
                        parentView.addView(adView, 0);
                    } else {
                        parentView.addView(adView);
                    }
                }
                adView.loadAd( buildAdRequest() );
                callbackContext.success();
            }
        });
        
        return null;
    }

    private PluginResult requestBannerAd(JSONObject options, final CallbackContext callbackContext) {
    	Log.w(LOGTAG, "requestBannerAd");
    	
    	this.setOptions( options );

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adView.loadAd( buildAdRequest() );
                if(callbackContext != null) callbackContext.success();
            }
        });
        
        return null;
    }
    
    private PluginResult showBannerAd(final boolean show, final CallbackContext callbackContext) {
    	Log.w(LOGTAG, "showBannerAd");
    	
        if(adView == null) {
            return new PluginResult(Status.ERROR, "adView is null, call createBannerView first.");
        }

        cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
            public void run() {
                adView.setVisibility( show ? View.VISIBLE : View.GONE );
                if(callbackContext != null) callbackContext.success();
            }
        });
        
        return null;
    }
    
    private PluginResult removeBanner(final CallbackContext callbackContext) {
	  	Log.w(LOGTAG, "removeBanner");
	  	
	  	cordova.getActivity().runOnUiThread(new Runnable() {
		    @Override
		    public void run() {
				if (adView != null) {
					ViewGroup parentView = (ViewGroup)adView.getParent();
					if(parentView != null) {
						parentView.removeView(adView);
					}
					adView = null;
				}
				if (adViewLayout != null) {
					ViewGroup parentView = (ViewGroup)adViewLayout.getParent();
					if(parentView != null) {
						parentView.removeView(adViewLayout);
					}
					adViewLayout = null;
				}
				callbackContext.success();
		    }
	  	});
	  	
	  	return null;
    }
    
    private AdRequest buildAdRequest() {
        AdRequest.Builder request_builder = new AdRequest.Builder();
        if (isTesting) {
            // This will request test ads on the emulator and deviceby passing this hashed device ID.
        	String ANDROID_ID = Settings.Secure.getString(cordova.getActivity().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            String deviceId = md5(ANDROID_ID).toUpperCase();
            request_builder = request_builder.addTestDevice(deviceId).addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
        }

        Bundle bundle = new Bundle();
        bundle.putInt("cordova", 1);
        if(adExtras != null) {
            Iterator<String> it = adExtras.keys();
            while (it.hasNext()) {
                String key = it.next();
                try {
                    bundle.putString(key, adExtras.get(key).toString());
                } catch (JSONException exception) {
                    Log.w(LOGTAG, String.format("Caught JSON Exception: %s", exception.getMessage()));
                }
            }
        }
        AdMobExtras adextras = new AdMobExtras(bundle);
        request_builder = request_builder.addNetworkExtras( adextras );
        AdRequest request = request_builder.build();
        
        return request;
    }
    
    private PluginResult requestInterstitial(JSONObject options, final CallbackContext callbackContext) {
    	this.setOptions( options );

    	if(this.interstialAdId.length() == 0) this.interstialAdId = this.publisherId;
    	if(this.interstialAdId.length() == 0) this.interstialAdId = DEFAULT_PUBLISHER_ID;

        cordova.getActivity().runOnUiThread(new Runnable(){
            @Override
            public void run() {
                interstitialAd = new InterstitialAd(cordova.getActivity());
                interstitialAd.setAdUnitId( interstialAdId );
                interstitialAd.setAdListener( new InterstitialListener() );
                
                interstitialAd.loadAd( buildAdRequest() );
                
                callbackContext.success();
            }
        });
        return null;
    }
    
	private PluginResult showInterstitial(CallbackContext callbackContext) {
        if(interstitialAd == null) {
            return new PluginResult(Status.ERROR, "call requestInterstitial first.");
        }
        
        final CallbackContext delayCallback = callbackContext;
        cordova.getActivity().runOnUiThread(new Runnable(){
			@Override
            public void run() {
				if( interstitialAd.isLoaded() ) {
					interstitialAd.show();
				}
				if(delayCallback != null) delayCallback.success();
            }
        });
        
        return null;
    }

    public class BasicListener extends AdListener {
        @Override
        public void onAdFailedToLoad(int errorCode) {
            webView.loadUrl(String.format(
                    "javascript:cordova.fireDocumentEvent('onFailedToReceiveAd', { 'error': %d, 'reason':'%s' });",
                    errorCode, getErrorReason(errorCode)));
        }
        
        @Override
        public void onAdLeftApplication() {
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onLeaveToAd');");
        }
    }
    
    private class BannerListener extends BasicListener {
        @Override
        public void onAdLoaded() {
            Log.w("AdMob", "BannerAdLoaded");
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onReceiveAd');");
            
            if(bannerShow) {
            	showBannerAd(true, null);
            }
        }

        @Override
        public void onAdOpened() {
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onPresentAd');");
        }
        
        @Override
        public void onAdClosed() {
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onDismissAd');");
        }
        
    }
    
    private class InterstitialListener extends BasicListener {
        @Override
        public void onAdLoaded() {
            Log.w("AdMob", "InterstitialAdLoaded");
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onReceiveInterstitialAd');");
            
            if(autoShow) {
            	showInterstitial(null);
            }
        }

        @Override
        public void onAdOpened() {
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onPresentInterstitialAd');");
        }
        
        @Override
        public void onAdClosed() {
            webView.loadUrl("javascript:cordova.fireDocumentEvent('onDismissInterstitialAd');");
        }
        
    }
    
    @Override
    public void onPause(boolean multitasking) {
        if (adView != null) {
            adView.pause();
        }
        super.onPause(multitasking);
    }
    
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        if (adView != null) {
            adView.resume();
        }
    }
    
    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }
    
    /**
     * Gets an AdSize object from the string size passed in from JavaScript.
     * Returns null if an improper string is provided.
     *
     * @param size The string size representing an ad format constant.
     * @return An AdSize object used to create a banner.
     */
    public static AdSize adSizeFromString(String size) {
        if ("BANNER".equals(size)) {
            return AdSize.BANNER;
        } else if ("IAB_MRECT".equals(size)) {
            return AdSize.MEDIUM_RECTANGLE;
        } else if ("IAB_BANNER".equals(size)) {
            return AdSize.FULL_BANNER;
        } else if ("IAB_LEADERBOARD".equals(size)) {
            return AdSize.LEADERBOARD;
        } else if ("SMART_BANNER".equals(size)) {
            return AdSize.SMART_BANNER;
        } else {
            return AdSize.SMART_BANNER;
        }
    }

    
    /** Gets a string error reason from an error code. */
    public String getErrorReason(int errorCode) {
      String errorReason = "";
      switch(errorCode) {
        case AdRequest.ERROR_CODE_INTERNAL_ERROR:
          errorReason = "Internal error";
          break;
        case AdRequest.ERROR_CODE_INVALID_REQUEST:
          errorReason = "Invalid request";
          break;
        case AdRequest.ERROR_CODE_NETWORK_ERROR:
          errorReason = "Network Error";
          break;
        case AdRequest.ERROR_CODE_NO_FILL:
          errorReason = "No fill";
          break;
      }
      return errorReason;
    }
    
    public static final String md5(final String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
        }
        return "";
    }
}

