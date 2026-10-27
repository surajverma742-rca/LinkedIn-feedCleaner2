package com.feedcleaner.linkedin

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private var enabled = true
    private var strictMode = false
    private var includeEngagementBait = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 250, 252))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 10)
            setBackgroundColor(Color.WHITE)
        }
        val title = TextView(this).apply {
            text = "FeedCleaner"
            textSize = 22f
            setTextColor(Color.rgb(20, 35, 55))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "Filter likely AI-style posts and engagement bait"
            textSize = 13f
            setTextColor(Color.DKGRAY)
        }
        status = TextView(this).apply {
            text = "Filtering is ON · Rules run on this device"
            textSize = 12f
            setTextColor(Color.rgb(30, 110, 70))
            setPadding(0, 8, 0, 2)
        }
        header.addView(title)
        header.addView(subtitle)
        header.addView(status)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 4, 14, 8)
            setBackgroundColor(Color.WHITE)
        }
        val master = Switch(this).apply {
            text = "Filtering"
            isChecked = enabled
            setOnCheckedChangeListener { _, checked ->
                enabled = checked
                updateStatus()
                runFilter()
            }
        }
        val strict = Switch(this).apply {
            text = "Strict"
            isChecked = strictMode
            setOnCheckedChangeListener { _, checked ->
                strictMode = checked
                runFilter()
            }
        }
        controls.addView(master, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        controls.addView(strict, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val bait = Switch(this).apply {
            text = "Also filter engagement bait (e.g. ‘Agree?’ / ‘Comment YES’)"
            isChecked = includeEngagementBait
            setPadding(18, 2, 18, 8)
            setBackgroundColor(Color.WHITE)
            setOnCheckedChangeListener { _, checked ->
                includeEngagementBait = checked
                runFilter()
            }
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 6, 12, 6)
            setBackgroundColor(Color.rgb(238, 242, 247))
        }
        val reload = Button(this).apply {
            text = "Reload feed"
            setOnClickListener { webView.reload() }
        }
        val restore = Button(this).apply {
            text = "Restore hidden"
            setOnClickListener { restorePosts() }
        }
        toolbar.addView(reload, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        toolbar.addView(restore, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url?.contains("linkedin.com") == true) {
                        view?.evaluateJavascript(filterScript(), null)
                        updateStatus("LinkedIn loaded · local filter active")
                    } else {
                        updateStatus("Open LinkedIn to filter your feed")
                    }
                }
            }
            setBackgroundColor(Color.WHITE)
        }

        root.addView(header)
        root.addView(controls)
        root.addView(bait)
        root.addView(toolbar)
        root.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        webView.loadUrl("https://www.linkedin.com/feed/")
    }

    private fun updateStatus(custom: String? = null) {
        status.text = custom ?: if (enabled) "Filtering is ON · Rules run on this device" else "Filtering is OFF · Posts are shown normally"
        status.setTextColor(if (enabled) Color.rgb(30, 110, 70) else Color.DKGRAY)
    }

    private fun filterScript(): String {
        val on = enabled
        val strict = strictMode
        val bait = includeEngagementBait
        return """
            (function(){
              const ENABLED = $on, STRICT = $strict, BAIT = $bait;
              if (window.__feedCleanerTimer) clearInterval(window.__feedCleanerTimer);
              const patterns = [
                /here'?s the thing nobody talks about/i,
                /let that sink in/i,
                /read that again/i,
                /agree\??\s*$/i,
                /i'?ll say it louder for the people in the back/i,
                /the future belongs to those who/i,
                /in today'?s fast-paced world/i,
                /game[- ]changer/i,
                /unlock your potential/i,
                /thoughts\??\s*$/i,
                /what do you think\??\s*$/i
              ];
              const baitPatterns = [
                /comment\s+['\"]?yes['\"]?/i,
                /type\s+['\"]?interested['\"]?/i,
                /drop\s+.*(below|comments)/i,
                /comment\s+['\"]?agree['\"]?/i,
                /repost\s+if\s+you\s+agree/i,
                /tag\s+someone\s+who/i
              ];
              // Restore previous decisions before applying changed settings.
              document.querySelectorAll('[data-fc-processed="1"]').forEach(post => {
                if (post.dataset.fcHidden === '1') post.style.display = post.dataset.fcOldDisplay || '';
                delete post.dataset.fcHidden;
                delete post.dataset.fcProcessed;
              });
              function textOf(el){ return (el.innerText || el.textContent || '').trim(); }
              function scan(){
                if (!location.hostname.endsWith('linkedin.com')) return;
                const candidates = document.querySelectorAll('div.feed-shared-update-v2, div[data-urn*="activity"], div.occludable-update');
                candidates.forEach(post => {
                  if (post.dataset.fcProcessed === '1') return;
                  const text = textOf(post);
                  if (!text || text.length < 45) return;
                  post.dataset.fcProcessed = '1';
                  const lower = text.toLowerCase();
                  const hashtagCount = (text.match(/#[\w]+/g) || []).length;
                  const emojiCount = (text.match(/[\u{1F300}-\u{1FAFF}]/gu) || []).length;
                  const hookMatch = patterns.some(p => p.test(text));
                  const baitMatch = BAIT && baitPatterns.some(p => p.test(text));
                  const formattingSignal = STRICT && ((hashtagCount >= 8) || (emojiCount >= 10) || (text.split('\n').length > 18 && text.length < 1800));
                  const genericSignal = STRICT && (/\b(5 lessons|7 lessons|3 secrets|10 tips)\b/i.test(text) && hashtagCount >= 3);
                  if (ENABLED && (hookMatch || baitMatch || formattingSignal || genericSignal)) {
                    post.dataset.fcHidden = '1';
                    post.dataset.fcOldDisplay = post.style.display || '';
                    post.style.display = 'none';
                  }
                });
              }
              window.__feedCleanerRestore = function(){
                document.querySelectorAll('[data-fc-hidden="1"]').forEach(post => {
                  post.style.display = post.dataset.fcOldDisplay || '';
                  delete post.dataset.fcHidden;
                  delete post.dataset.fcProcessed;
                });
              };
              window.__feedCleanerTimer = setInterval(scan, 1500);
              scan();
            })();
        """.trimIndent()
    }

    private fun runFilter() {
        if (::webView.isInitialized) {
            webView.evaluateJavascript(filterScript(), null)
            Toast.makeText(this, "Filter settings applied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restorePosts() {
        if (::webView.isInitialized) {
            webView.evaluateJavascript("if(window.__feedCleanerRestore) window.__feedCleanerRestore();", null)
            Toast.makeText(this, "Visible posts restored where possible", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
