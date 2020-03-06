package com.blanke.mdwechat.hookers.main

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.blanke.mdwechat.*
import com.blanke.mdwechat.Objects
import com.blanke.mdwechat.config.AppCustomConfig
import com.blanke.mdwechat.config.HookConfig
import com.blanke.mdwechat.hookers.StatusBarHooker
import com.blanke.mdwechat.util.*
import com.blanke.mdwechat.util.LogUtil.logViewStackTracesXp
import com.blanke.mdwechat.util.LogUtil.logXp
import com.blankj.utilcode.util.BarUtils
import com.flyco.tablayout.CommonTabLayout
import com.flyco.tablayout.listener.CustomTabEntity
import com.flyco.tablayout.listener.OnTabSelectListener
import de.robv.android.xposed.XposedHelpers
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.*

object TabLayoutHook {
    fun newTabLayout(viewGroup: ViewGroup, indicatorGravity: Int = Gravity.BOTTOM, tabElevation: Float = 0F): CommonTabLayout {
        val primaryColor: Int = HookConfig.get_color_primary
        val secondaryColor: Int = HookConfig.get_color_secondary
        val get_color_tertiary: Int = HookConfig.get_color_tertiary
        val tipColor: Int = HookConfig.get_color_tip
        val context = viewGroup.context.createPackageContext(Common.MY_APPLICATION_PACKAGE, Context.CONTEXT_IGNORE_SECURITY)
        val resContext = viewGroup.context
        val tabLayout = CommonTabLayout(context)
        tabLayout.setBackgroundColor(primaryColor)
        tabLayout.textSelectColor = Color.WHITE
        val dp2 = ConvertUtils.dp2px(resContext, 1f)
        tabLayout.indicatorHeight = dp2.toFloat()
        tabLayout.indicatorColor = if (HookConfig.is_hook_tab_elevation) secondaryColor else primaryColor
        tabLayout.setIndicatorGravity(indicatorGravity)
        tabLayout.indicatorCornerRadius = dp2.toFloat()
        tabLayout.indicatorAnimDuration = 200
        tabLayout.elevation = tabElevation
        tabLayout.unreadBackground = tipColor
        tabLayout.unreadTextColor = primaryColor
        tabLayout.selectIconColor = secondaryColor
//        tabLayout.unSelectIconColor = Color.WHITE

        val mTabEntities = intArrayOf(0, 1, 2, 3)
//                .filterNot { HookConfig.is_hook_hide_wx_tab_2 && it == 2 }
//                .filterNot { HookConfig.is_hook_hide_wx_tab_3 && it == 3 }
                .mapTo(ArrayList<CustomTabEntity>()) {
                    object : CustomTabEntity.TabCustomData() {
                        override fun getTabIcon(): Drawable {
                            val drawable:Drawable=BitmapDrawable(resContext.resources, AppCustomConfig.getTabIcon(it))
                            return  if(HookConfig.is_tab_layout_filtered) DrawableUtils.setDrawableColor(drawable, get_color_tertiary) else drawable
                        }
                    }
                }
        tabLayout.setTabData(mTabEntities)

        tabLayout.setOnTabSelectListener(object : OnTabSelectListener {
            override fun onTabSelect(position: Int) {
                LogUtil.log("tab click position=$position")
                tabLayout.currentTab = position
                Objects.Main.LauncherUI_mViewPager.get()?.apply {
                    Methods.WxViewPager_selectedPage.invoke(this, position, false, false, 0)
                }
            }

            override fun onTabReselect(position: Int) {
            }
        })
        return tabLayout
    }

    fun measureHeight(view: ViewGroup): Int {
        val w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(w, w)
        return view.getMeasuredHeight()
    }

    // 返回高度,以便悬浮按钮使用(懒)
    fun addTabLayoutAtBottom(tabView: ViewGroup, height: Int) {
        val tabLayout = newTabLayout(tabView, Gravity.TOP, 5f)

        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val viewChild = tabView.getChildAt(0) as ViewGroup
        params.height = height

        viewChild.addView(tabLayout, 4, params)
        try {
            LogUtil.log("add tableyout success")
            Objects.Main.LauncherUI_mTabLayout = WeakReference(tabLayout)
            for (index in 0..3) {
                viewChild.getChildAt(index).visibility = View.GONE
            }
        } catch (e: Exception) {
            logXp(e)
        }
        // change main ActionBar elevation = 0
//        val actionBarLayout = ViewUtils.getParentView(tabView, 3) as ViewGroup
//        val actionBar = actionBarLayout.getChildAt(1)
//        actionBar.elevation = 0F
    }

    fun addTabLayout(viewPagerLinearLayout: ViewGroup) {
        val primaryColor = HookConfig.get_color_primary

        val context = viewPagerLinearLayout.context.createPackageContext(Common.MY_APPLICATION_PACKAGE, Context.CONTEXT_IGNORE_SECURITY)
        val resContext = viewPagerLinearLayout.context
        // 7.0.7(?) 之后小程序下拉相关
        val isHideElevation = (WechatGlobal.wxVersion!! >= Version("7.0.7")) && (HookConfig.is_hook_hide_wx_tab)
        val tabElevation = if (!isHideElevation && HookConfig.is_hook_tab_elevation) 5F else 0F

        val tabLayout = newTabLayout(viewPagerLinearLayout, Gravity.BOTTOM, tabElevation)

        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val px48 = ConvertUtils.dp2px(resContext, 48f)
        params.height = px48
        if (WechatGlobal.wxVersion!! < Version("6.7.2")) {
            viewPagerLinearLayout.addView(tabLayout, 0, params)
        } else if (WechatGlobal.wxVersion!! < Version("7.0.0")) {// 672 wx布局更改
            val mockLayout = FrameLayout(context)
            var paddingTop = px48
            if (HookConfig.is_hook_hide_actionbar) {
                paddingTop = 0
            }
            mockLayout.setPadding(0, paddingTop, 0, 0)
            val viewpager = viewPagerLinearLayout.getChildAt(0)
            viewPagerLinearLayout.removeViewAt(0)
            mockLayout.addView(tabLayout, params)
            mockLayout.addView(viewpager)
            viewPagerLinearLayout.addView(mockLayout, 0)
        } else {
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.height = px48
            val cb = { actionHeight: Int ->
                params.topMargin = actionHeight + BarUtils.getStatusBarHeight() - 2
                if (WechatGlobal.wxVersion!! == Version("7.0.0")) {
                    // mock status bar
                    val statusView = View(context)
                    statusView.background = ColorDrawable(StatusBarHooker.getStatusBarColor())
                    statusView.elevation = 1F
                    val statusParam = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    statusParam.topMargin = 0
                    statusParam.height = BarUtils.getStatusBarHeight()
                    viewPagerLinearLayout.addView(statusView, 0, statusParam)
                }
                viewPagerLinearLayout.height
                viewPagerLinearLayout.setPadding(0, 0, 0, 0)
                viewPagerLinearLayout.requestLayout()
                //小程序下拉之后的填空
                if ((WechatGlobal.wxVersion!! >= Version("7.0.7")) && (HookConfig.is_hook_hide_wx_tab)) {
                    val params1 = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    params1.height = px48
                    params1.topMargin = actionHeight + BarUtils.getStatusBarHeight() - 2 - px48
                    val view = FrameLayout(context)
                    view.setBackgroundColor(primaryColor)
                    viewPagerLinearLayout.addView(view, 1, params1)
                }
                viewPagerLinearLayout.addView(tabLayout, 2, params)
            }
            if (!HookConfig.is_hook_hide_actionbar) {
                val mActionBar = Objects.Main.HomeUI_mActionBar.get()
                waitInvoke(100, true, {
                    XposedHelpers.callMethod(mActionBar, "getHeight") as Int > 0
                }, {
                    val actionHeight = XposedHelpers.callMethod(mActionBar, "getHeight") as Int
                    cb(actionHeight)
                })
            } else {
                cb(0)
            }
        }
        LogUtil.log("add tableyout success")
        Objects.Main.LauncherUI_mTabLayout = WeakReference(tabLayout)
        // change main ActionBar elevation = 0
        val actionBarLayout = ViewUtils.getParentView(viewPagerLinearLayout, 3) as ViewGroup
        val actionBar = actionBarLayout.getChildAt(1)
        actionBar.elevation = 0F
    }
}