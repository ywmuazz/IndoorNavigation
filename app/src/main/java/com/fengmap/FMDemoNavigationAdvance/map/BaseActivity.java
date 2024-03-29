package com.fengmap.FMDemoNavigationAdvance.map;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.fengmap.FMDemoNavigationAdvance.R;
import com.fengmap.FMDemoNavigationAdvance.utils.FileUtils;
import com.fengmap.FMDemoNavigationAdvance.utils.ViewHelper;
import com.fengmap.FMDemoNavigationAdvance.widget.NavigationBar;
import com.fengmap.android.FMErrorMsg;
import com.fengmap.android.analysis.navi.FMNaviAnalyser;
import com.fengmap.android.analysis.navi.FMNaviOption;
import com.fengmap.android.analysis.navi.FMNavigation;
import com.fengmap.android.analysis.navi.FMPointOption;
import com.fengmap.android.data.OnFMDownloadProgressListener;
import com.fengmap.android.map.FMMap;
import com.fengmap.android.map.FMMapUpgradeInfo;
import com.fengmap.android.map.FMMapView;
import com.fengmap.android.map.FMViewMode;
import com.fengmap.android.map.event.OnFMMapInitListener;
import com.fengmap.android.map.geometry.FMGeoCoord;
import com.fengmap.android.map.geometry.FMMapCoord;
import com.fengmap.android.map.layer.FMLocationLayer;

import java.io.IOException;

/**
 * 基类。
 *
 * @author yangbin@fengmap.com
 * @version 2.1.0
 */
public abstract class BaseActivity extends Activity implements View.OnClickListener, OnFMMapInitListener {
    public static final String EXTRA_TITLE = "EXTRA_TITLE";

    // 点移距离视图中心点超过最大距离5米，就会触发移动动画
    protected static final double NAVI_MOVE_CENTER_MAX_DISTANCE = 5;

    // 进入导航时地图显示级别
    protected static final int NAVI_ZOOM_LEVEL = 20;

    //這裡為我們的默認的起點
    //通過二維碼掃描後得到的真正的起點 可以在這裡進行初始化
    // 默认起点
    protected FMGeoCoord mStartCoord = new FMGeoCoord(1,
            new FMMapCoord(12961647.576796599, 4861814.63807118));
    // 默认终点
    protected FMGeoCoord mEndCoord = new FMGeoCoord(6,
            new FMMapCoord(12961699.79823795, 4861826.46384646));

    // 地图View
    protected FMMapView mMapView;
    // 地图操作对象
    protected FMMap mFMMap;

    // 定位图层
    protected FMLocationLayer mLocationLayer;

    // 导航对象
    protected FMNavigation mNavigation;
    // 导航配置
    protected FMNaviOption mNaviOption;

    // 起点标志物配置
    protected FMPointOption mStartOption = new FMPointOption();
    // 终点标志物配置
    protected FMPointOption mEndOption = new FMPointOption();

    // 路径是否计算完成
    protected boolean isRouteCalculated;

    // 地图是否加载完成
    protected boolean isMapLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_base);

        setTitle();
    }

    /**
     * 加载地图数据
     */
    protected void openMapByPath() {
        mMapView = (FMMapView) findViewById(R.id.map_view);
        mFMMap = mMapView.getFMMap();
        mFMMap.setOnFMMapInitListener(this);
        //加载离线数据
        String path = FileUtils.getDefaultMapPath(this);
        mFMMap.openMapByPath(path);
    }

    /**
     * 添加底部布局。
     *
     * @param layoutId 资源id
     */
    public void setContentView(int layoutId) {
        View view = View.inflate(getBaseContext(), layoutId, null);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.BELOW, R.id.navigation_bar);

        RelativeLayout viewGroup = (RelativeLayout) findViewById(R.id.layout_root);
        viewGroup.addView(view, lp);

        View bottomView = ViewHelper.getView(BaseActivity.this, R.id.btn_start_navigation);
        bottomView.bringToFront();

        openMapByPath();
    }

    /**
     * 设置标题。
     */
    protected void setTitle() {
        int resId = getIntent().getIntExtra(EXTRA_TITLE, -1);
        NavigationBar navigationBarView = ViewHelper.getView(BaseActivity.this, R.id.navigation_bar);
        navigationBarView.setTitle(resId);
    }

    @Override
    public void onMapInitSuccess(String path) {
        //加载离线主题
        mFMMap.loadThemeByPath(FileUtils.getDefaultThemePath(this));
        //设定显示模式为2D
        mFMMap.setFMViewMode(FMViewMode.FMVIEW_MODE_2D);

        //定位层
        mLocationLayer = mFMMap.getFMLayerProxy().getFMLocationLayer();
        mFMMap.addLayer(mLocationLayer);

        //设置点击事件
        ViewHelper.setViewClickListener(BaseActivity.this, R.id.btn_start_navigation, this);
    }

    /**
     * 地图加载失败回调事件。
     *
     * @param path      地图所在sdcard路径
     * @param errorCode 失败加载错误码，可以通过{@link FMErrorMsg#getErrorMsg(int)}获取加载地图失败详情
     */
    @Override
    public void onMapInitFailure(String path, int errorCode) {
        //TODO 可以提示用户地图加载失败原因，进行地图加载失败处理
    }

    /**
     * 当{@link FMMap#openMapById(String, boolean)}设置openMapById(String, false)时地图不自动更新会
     * 回调此事件，可以调用{@link FMMap#upgrade(FMMapUpgradeInfo, OnFMDownloadProgressListener)}进行
     * 地图下载更新
     *
     * @param upgradeInfo 地图版本更新详情,地图版本号{@link FMMapUpgradeInfo#getVersion()},<br/>
     *                    地图id{@link FMMapUpgradeInfo#getMapId()}
     * @return 如果调用了{@link FMMap#upgrade(FMMapUpgradeInfo, OnFMDownloadProgressListener)}地图下载更新，
     * 返回值return true,因为{@link FMMap#upgrade(FMMapUpgradeInfo, OnFMDownloadProgressListener)}
     * 会自动下载更新地图，更新完成后会加载地图;否则return false。
     */
    @Override
    public boolean onUpgrade(FMMapUpgradeInfo upgradeInfo) {
        //TODO 获取到最新地图更新的信息，可以进行地图的下载操作
        return false;
    }

    /**
     * 地图销毁调用。
     */
    @Override
    public void onBackPressed() {
        if (!isMapLoaded)
            return;

        //地图销毁
        if (mFMMap != null) {
            mFMMap.onDestroy();
        }
        super.onBackPressed();
    }


    void analyzeNavigation(FMGeoCoord startPt, FMGeoCoord endPt) {
        // 设置起点
        Bitmap startBmp = BitmapFactory.decodeResource(getResources(), R.drawable.start);
        mStartOption.setBitmap(startBmp);
        mNavigation.setStartPoint(startPt);
        mNavigation.setStartOption(mStartOption);

        // 设置终点
        Bitmap endBmp = BitmapFactory.decodeResource(getResources(), R.drawable.end);
        mEndOption.setBitmap(endBmp);
        mNavigation.setEndPoint(endPt);
        mNavigation.setEndOption(mEndOption);

        // 路径规划
        int ret = mNavigation.analyseRoute(FMNaviAnalyser.FMNaviModule.MODULE_SHORTEST);
        if (ret == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_SUCCESS) {
            mNavigation.drawNaviLine();
            isRouteCalculated = true;
        }
    }
    /**
     * 切换楼层显示。
     */
    public abstract void updateLocateGroupView();


    /**
     * 开始导航。
     */
    public abstract void startNavigation();


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_navigation:
                if (isRouteCalculated) {
                    startNavigation();
                }
                break;
            default:
                break;
        }
    }

}
