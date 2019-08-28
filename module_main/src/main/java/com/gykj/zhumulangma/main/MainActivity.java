package com.gykj.zhumulangma.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.gykj.zhumulangma.common.App;
import com.gykj.zhumulangma.common.AppConstants;
import com.gykj.zhumulangma.common.bean.NavigateBean;
import com.gykj.zhumulangma.common.event.EventCode;
import com.gykj.zhumulangma.common.event.common.BaseActivityEvent;
import com.gykj.zhumulangma.common.event.common.BaseFragmentEvent;
import com.gykj.zhumulangma.common.mvvm.BaseActivity;
import com.gykj.zhumulangma.common.util.ToastUtil;
import com.gykj.zhumulangma.common.util.log.TLog;
import com.gykj.zhumulangma.common.widget.GlobalPlay;
import com.gykj.zhumulangma.main.fragment.MainFragment;
import com.ximalaya.ting.android.opensdk.auth.call.IXmlyAuthListener;
import com.ximalaya.ting.android.opensdk.auth.exception.XmlyException;
import com.ximalaya.ting.android.opensdk.auth.handler.XmlySsoHandler;
import com.ximalaya.ting.android.opensdk.auth.model.XmlyAuth2AccessToken;
import com.ximalaya.ting.android.opensdk.auth.model.XmlyAuthInfo;
import com.ximalaya.ting.android.opensdk.auth.utils.AccessTokenKeeper;
import com.ximalaya.ting.android.opensdk.datatrasfer.AccessTokenManager;
import com.ximalaya.ting.android.opensdk.datatrasfer.CommonRequest;
import com.ximalaya.ting.android.opensdk.datatrasfer.ILoginOutCallBack;
import com.ximalaya.ting.android.opensdk.httputil.XimalayaException;
import com.ximalaya.ting.android.opensdk.model.PlayableModel;
import com.ximalaya.ting.android.opensdk.model.advertis.Advertis;
import com.ximalaya.ting.android.opensdk.model.advertis.AdvertisList;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.XmPlayerManager;
import com.ximalaya.ting.android.opensdk.player.advertis.IXmAdsStatusListener;
import com.ximalaya.ting.android.opensdk.player.service.IXmPlayerStatusListener;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayerException;

import org.greenrobot.eventbus.EventBus;

import me.yokeyword.fragmentation.ISupportFragment;

import static com.gykj.zhumulangma.common.AppConstants.Ximalaya.REDIRECT_URL;

@Route(path = AppConstants.Router.Main.A_MAIN)
public class MainActivity extends BaseActivity implements View.OnClickListener,
        MainFragment.onRootShowListener, IXmPlayerStatusListener, IXmAdsStatusListener {

    private XmlyAuthInfo mAuthInfo;
    private XmlySsoHandler mSsoHandler;
    private XmlyAuth2AccessToken mAccessToken;

    private GlobalPlay globalPlay;

    @Override
    protected int onBindLayout() {
        return R.layout.main_activity_main;
    }

    @Override
    public void initView() {
        setSwipeBackEnable(false);
        if (findFragment(MainFragment.class) == null) {
            MainFragment mainFragment = new MainFragment();
            mainFragment.setShowListener(this);
            loadRootFragment(R.id.fl_container, mainFragment);
        }
        globalPlay = fd(R.id.gp);

    }

    @Override
    public void initListener() {
        globalPlay.setOnClickListener(this);
        XmPlayerManager.getInstance(this).addPlayerStatusListener(this);
        XmPlayerManager.getInstance(this).addAdsStatusListener(this);
    }

    @Override
    public void initData() {
        new Handler().postDelayed(() -> {
            TLog.d(XmPlayerManager.getInstance(MainActivity.this).isPlaying());
            if (XmPlayerManager.getInstance(MainActivity.this).isPlaying()) {
                Track currSoundIgnoreKind = XmPlayerManager.getInstance(MainActivity.this).getCurrSoundIgnoreKind(true);
                if (null == currSoundIgnoreKind) {
                    return;
                }
                globalPlay.play(currSoundIgnoreKind.getCoverUrlSmall());
            }
        }, 500);

    }

    @Override
    protected boolean enableSimplebar() {
        return false;
    }


    @Override
    public void onClick(View v) {
        if (v == globalPlay) {
            if(null == XmPlayerManager.getInstance(this).getCurrSound(true)){
                Object navigation = ARouter.getInstance().build(AppConstants.Router.Home.F_RANK).navigation();
                if (null != navigation) {
                    EventBus.getDefault().post(new BaseActivityEvent<>(EventCode.MainCode.NAVIGATE,
                            new NavigateBean(AppConstants.Router.Home.F_RANK, (ISupportFragment) navigation,
                                    extraTransaction().setCustomAnimations(
                                            com.gykj.zhumulangma.common.R.anim.push_bottom_in,
                                            com.gykj.zhumulangma.common.R.anim.no_anim,
                                            com.gykj.zhumulangma.common.R.anim.no_anim,
                                            com.gykj.zhumulangma.common.R.anim.push_bottom_out))));
                }
            }else {
                XmPlayerManager.getInstance(this).play();
                Object navigation = ARouter.getInstance().build(AppConstants.Router.Home.F_PLAY_TRACK).navigation();
                if (null != navigation) {
                    EventBus.getDefault().post(new BaseActivityEvent<>(EventCode.MainCode.NAVIGATE,
                            new NavigateBean(AppConstants.Router.Home.F_PLAY_TRACK, (ISupportFragment) navigation)));
                }
            }
        }
    }

    @Override
    public void onRootShow(boolean isVisible) {
        if (isVisible)
            globalPlay.setBackgroundColor(Color.TRANSPARENT);
        else
            globalPlay.setBackground(getResources().getDrawable(R.drawable.shap_common_widget_play));
    }

    private void goLogin() {
        try {
            mAuthInfo = new XmlyAuthInfo(this, CommonRequest.getInstanse().getAppKey(), CommonRequest.getInstanse()
                    .getPackId(), REDIRECT_URL, CommonRequest.getInstanse().getAppKey());
        } catch (XimalayaException e) {
            e.printStackTrace();
        }
        mSsoHandler = new XmlySsoHandler(this, mAuthInfo);

        mSsoHandler.authorize(new CustomAuthListener());
    }

    @Override
    public void onPlayStart() {
        Track currSoundIgnoreKind = XmPlayerManager.getInstance(this).getCurrSoundIgnoreKind(true);
        if (null == currSoundIgnoreKind) {
            return;
        }
        globalPlay.play(currSoundIgnoreKind.getCoverUrlSmall());
    }

    @Override
    public void onPlayPause() {
        globalPlay.pause();
    }

    @Override
    public void onPlayStop() {
        globalPlay.pause();
    }

    @Override
    public void onSoundPlayComplete() {

    }

    @Override
    public void onSoundPrepared() {

    }

    @Override
    public void onSoundSwitch(PlayableModel playableModel, PlayableModel playableModel1) {

    }

    @Override
    public void onBufferingStart() {

    }

    @Override
    public void onBufferingStop() {

    }

    @Override
    public void onBufferProgress(int i) {

    }

    @Override
    public void onPlayProgress(int i, int i1) {
        globalPlay.setProgress((float) i / (float) i1);
    }

    @Override
    public boolean onError(XmPlayerException e) {
        return false;
    }

    @Override
    public void onStartGetAdsInfo() {

    }

    @Override
    public void onGetAdsInfo(AdvertisList advertisList) {

    }

    @Override
    public void onAdsStartBuffering() {
        globalPlay.setProgress(0);
    }

    @Override
    public void onAdsStopBuffering() {

    }

    @Override
    public void onStartPlayAds(Advertis advertis, int i) {
        Log.e(TAG, "onStartPlayAds: " + advertis);
        String imageUrl = advertis.getImageUrl();
        if (TextUtils.isEmpty(imageUrl)) {
            globalPlay.play(R.drawable.notification_default);
        } else {
            globalPlay.play(imageUrl);
        }
    }

    @Override
    public void onCompletePlayAds() {

    }

    @Override
    public void onError(int i, int i1) {

    }

    class CustomAuthListener implements IXmlyAuthListener {
        @Override
        public void onComplete(Bundle bundle) {
            parseAccessToken(bundle);
            App.registerLoginTokenChangeListener(MainActivity.this.getApplicationContext());
            EventBus.getDefault().post(new BaseFragmentEvent<>(EventCode.MainCode.LOGINSUCC));
            ToastUtil.showToast("登录成功");
        }

        @Override
        public void onXmlyException(final XmlyException e) {
            e.printStackTrace();
        }

        @Override
        public void onCancel() {
        }
    }

    private void parseAccessToken(Bundle bundle) {
        mAccessToken = XmlyAuth2AccessToken.parseAccessToken(bundle);
        if (mAccessToken.isSessionValid()) {
            AccessTokenManager.getInstanse().setAccessTokenAndUid(mAccessToken.getToken(), mAccessToken
                    .getRefreshToken(), mAccessToken.getExpiresAt(), mAccessToken.getUid());
        }
    }

    public void logout() {
        AccessTokenManager.getInstanse().loginOut(new ILoginOutCallBack() {
            @Override
            public void onSuccess() {
                if (mAccessToken != null && mAccessToken.isSessionValid()) {
                    AccessTokenKeeper.clear(MainActivity.this);
                    mAccessToken = new XmlyAuth2AccessToken();
                }
                CommonRequest.getInstanse().setITokenStateChange(null);
            }

            @Override
            public void onFail(int errorCode, String errorMessage) {
                CommonRequest.getInstanse().setITokenStateChange(null);
            }
        });

    }


    private long exitTime = 0;

    @Override
    public void onBackPressedSupport() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            pop();
        } else {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(this, "再按一次返回桌面", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                home.addCategory(Intent.CATEGORY_HOME);
                startActivity(home);
            }
        }
    }

    @Override
    public <T> void onEvent(BaseActivityEvent<T> event) {
        super.onEvent(event);
        switch (event.getCode()) {
            case EventCode.MainCode.NAVIGATE:
                NavigateBean navigateBean = (NavigateBean) event.getData();
                if (null == navigateBean.fragment) {
                    return;
                }
                switch (navigateBean.path) {
                    case AppConstants.Router.User.F_MESSAGE:
                        //登录拦截
                   /*     if (!AccessTokenManager.getInstanse().hasLogin()) {
                            goLogin();
                        } else {
                            start(navigateBean.fragment);
                        }*/
                        start(navigateBean.fragment);
                        break;
                    case AppConstants.Router.Home.F_PLAY_TRACK:
                        extraTransaction().setCustomAnimations(
                                com.gykj.zhumulangma.common.R.anim.push_bottom_in,
                                com.gykj.zhumulangma.common.R.anim.no_anim,
                                com.gykj.zhumulangma.common.R.anim.no_anim,
                                com.gykj.zhumulangma.common.R.anim.push_bottom_out).start(navigateBean.fragment);
                        break;
                    default:
                        if(navigateBean.extraTransaction!=null){
                            navigateBean.extraTransaction.start(navigateBean.fragment,navigateBean.launchMode);
                        }else {
                            start(navigateBean.fragment,navigateBean.launchMode);
                        }
                        break;
                }
                break;
            case EventCode.MainCode.HIDE_GP:
                globalPlay.hide();
                break;
            case EventCode.MainCode.SHOW_GP:
                globalPlay.show();
                break;
            case EventCode.MainCode.LOGIN:
                goLogin();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        XmPlayerManager.getInstance(this).removePlayerStatusListener(this);
        XmPlayerManager.getInstance(this).removeAdsStatusListener(this);
    }
}
