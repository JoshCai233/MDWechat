package com.blanke.mdwechat.hookers

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.blanke.mdwechat.Classes
import com.blanke.mdwechat.Fields.ContactFragment_mListView
import com.blanke.mdwechat.Methods
import com.blanke.mdwechat.Objects
import com.blanke.mdwechat.WeChatHelper.defaultImageRippleDrawable
import com.blanke.mdwechat.WeChatHelper.drawableTransparent
import com.blanke.mdwechat.config.HookConfig
import com.blanke.mdwechat.hookers.base.Hooker
import com.blanke.mdwechat.hookers.base.HookerProvider
import com.blanke.mdwechat.hookers.main.BackgroundImageHook
import com.blanke.mdwechat.util.LogUtil
import com.blanke.mdwechat.util.NightModeUtils
import com.blanke.mdwechat.util.ViewUtils
import com.gcssloop.widget.RCRelativeLayout
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.getObjectField

object ContactHooker : HookerProvider {
    const val keyInit = "key_init"

    private val headTextColor: Int
        get() {
            return NightModeUtils.getContentTextColor()
        }

    private val titleTextColor: Int
        get() {
            return NightModeUtils.getTitleTextColor()
        }
    private val isHookTextColor: Boolean
        get() {
            return HookConfig.is_hook_main_textcolor || NightModeUtils.isNightMode()
        }

    override fun provideStaticHookers(): List<Hooker>? {
        return listOf(resumeHook)
    }

    private val resumeHook = Hooker {
        Methods.HomeFragment_lifecycles.forEach {
            XposedHelpers.findAndHookMethod(Classes.ContactFragment, it.name, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val fragment = param?.thisObject ?: return
//                    LogUtil.log("ContactFragment fragment=$fragment,${Classes.ContactFragment.name}")
                    val isInit = XposedHelpers.getAdditionalInstanceField(fragment, keyInit)
                    if (isInit != null) {
                        LogUtil.log("ContactFragment 已经hook过")
                        return
                    }
                    init(fragment)
                }

                private fun init(fragment: Any) {
                    val listView = ContactFragment_mListView.get(fragment)
                    if (listView != null && listView is ListView) {
                        LogUtil.logOnlyOnce("ContactFragment Done")
                        XposedHelpers.setAdditionalInstanceField(fragment, keyInit, true)
                        if (HookConfig.is_hook_tab_bg) {
                            BackgroundImageHook.setContactBitmap(listView)
                        }
//                        LogUtil.log("ContactFragment listview= $listView, ${listView.javaClass.name}")
                        if (listView.headerViewsCount > 0) {
                            val mHeaderViewInfos = getObjectField(listView, "mHeaderViewInfos") as ArrayList<*>
                            for (j in 0 until mHeaderViewInfos.size) {
                                val header = (mHeaderViewInfos[j] as ListView.FixedViewInfo).view
                                if (header != null) {
//                                        printViewTree(header, 0)
                                    if (header is ViewGroup) {
                                        val headLayout = header
                                        for (i in 0 until headLayout.childCount) {
                                            val item = headLayout.getChildAt(i)
                                            if (item !is ViewGroup || item.childCount == 0) {
                                                continue
                                            }
                                            val itemContent = item.getChildAt(0)
                                            var titleTextView: View?
                                            var headTextView: View?
                                            if (itemContent != null) {
                                                // 新的朋友 等几个 item
                                                itemContent.background = defaultImageRippleDrawable
//                                                LogUtil.log("-------------")
//                                                LogUtil.logViewStackTraces(itemContent)
//                                                LogUtil.log("-------------")
                                                if (itemContent is ViewGroup) {
                                                    val childView = itemContent.getChildAt(0)
                                                    childView.background = drawableTransparent
                                                    if (childView is TextView) {// 企业号
                                                        headTextView = childView // 我的企业 textView
                                                        itemContent.background = drawableTransparent
                                                        val lll = (itemContent.getChildAt(1) as ViewGroup)
                                                        for (m in 0 until lll.childCount) {
                                                            val comItem = (lll.getChildAt(m) as ViewGroup)
                                                            val ll = comItem.getChildAt(0) as ViewGroup
                                                            ll.background = defaultImageRippleDrawable
                                                            // 去掉分割线
                                                            ll.getChildAt(0).background = drawableTransparent
                                                            titleTextView = ViewUtils.getChildView(ll, 0, 1)
                                                            titleTextView?.apply {
                                                                this.background = drawableTransparent
                                                                if (this is TextView && isHookTextColor) {
                                                                    this.setTextColor(titleTextColor)
                                                                }
                                                            }
                                                        }
                                                        if (isHookTextColor) {
                                                            headTextView.setTextColor(headTextColor)
                                                        }
                                                    } else if (childView is ViewGroup) {// 新的朋友 群聊 公众号
                                                        var maskLayout = childView.getChildAt(0)
                                                        titleTextView = childView.getChildAt(1) // 公众号 textView
//                                                        LogUtil.log("-------------")
//                                                        LogUtil.logViewStackTraces(childView)
//                                                        LogUtil.log("-------------")
                                                        if (titleTextView == null) {// 企业微信联系人
                                                            maskLayout = ViewUtils.getChildView(childView, 0, 0, 0, 0)
                                                            titleTextView = ViewUtils.getChildView(childView, 0, 0, 0, 1)
                                                            ViewUtils.getChildView(childView, 0, 0)?.background = drawableTransparent
                                                        }
                                                        if (maskLayout != null && maskLayout is ViewGroup) {
                                                            val iv = maskLayout.getChildAt(0)
                                                            if (iv is ImageView) {
                                                                val roundLayout = RCRelativeLayout(Objects.Main.LauncherUI!!)
                                                                roundLayout.isRoundAsCircle = true
                                                                maskLayout.addView(roundLayout, iv.layoutParams)
                                                                maskLayout.removeView(iv)
                                                                roundLayout.addView(iv)
                                                            }
                                                        }
                                                        if (titleTextView != null) {
                                                            titleTextView.background = drawableTransparent
                                                            if (isHookTextColor) {
                                                                titleTextView.apply {
                                                                    if (this is TextView) {
                                                                        this.setTextColor(titleTextColor)
                                                                    } else if (this is ViewGroup) {
                                                                        val tv = this.getChildAt(0)
                                                                        if (tv is TextView) {
                                                                            tv.setTextColor(titleTextColor)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        LogUtil.logOnlyOnce("ContactFragment")
                    }
                }
            })
        }
    }
}