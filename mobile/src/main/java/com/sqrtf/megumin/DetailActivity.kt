package com.sqrtf.megumin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.sqrtf.common.activity.BaseActivity
import com.sqrtf.common.api.ApiClient
import com.sqrtf.common.api.FavoriteChangeRequest
import com.sqrtf.common.cache.JsonUtil
import com.sqrtf.common.model.Bangumi
import com.sqrtf.common.model.BangumiDetail


class DetailActivity : BaseActivity() {

    companion object {
        public fun intent(context: Context, bgm: Bangumi): Intent {
            val intent = Intent(context, DetailActivity::class.java)
            val json = JsonUtil.toJson(bgm)
            intent.putExtra(INTENT_KEY_BANGMUMI, json)
            return intent
        }

        private val INTENT_KEY_BANGMUMI = "INTENT_KEY_BANGMUMI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        val json = intent.getStringExtra(INTENT_KEY_BANGMUMI)
        checkNotNull(json)
        val bgm = JsonUtil.fromJson(json, Bangumi::class.java)
        checkNotNull(bgm)

        setData(bgm!!)

        ApiClient.getInstance().getBangumiDetail(bgm.id)
                .withLifecycle()
                .subscribe({
                    setData(it.getData())
                }, {
                    toastErrors().accept(it)
                    finish()
                })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setData(detail: Bangumi) {
        var iv = findViewById(R.id.image) as ImageView?
        val ivCover = findViewById(R.id.image_cover) as ImageView
        val ctitle = findViewById(R.id.title) as TextView
        val subtitle = findViewById(R.id.subtitle) as TextView
        val info = findViewById(R.id.info) as TextView
        val summary = findViewById(R.id.summary) as TextView
        val summary2 = findViewById(R.id.summary2) as TextView
        val more = findViewById(R.id.button_more)
        val spinner = findViewById(R.id.spinner) as Spinner
        val recyclerView = findViewById(R.id.recycler_view) as RecyclerView

        iv?.let { Glide.with(this).load(detail.image).into(iv) }
        Glide.with(this).load(detail.image).into(ivCover)

        ctitle.text = detail.name_cn
        subtitle.text = detail.name
        info.text = detail.air_date + ", " + detail.eps + "集, " + detail.air_weekday

        if (!TextUtils.isEmpty(detail.summary)) {
            summary.text = detail.summary
            summary2.post {
                summary2.text = summary.text.toString().substring(summary.layout.getLineEnd(2))
            }
            more.setOnClickListener {
                summary2.setSingleLine(false)
                it.visibility = View.GONE
            }
        } else {
            summary.visibility = View.GONE
            summary2.visibility = View.GONE
            more.visibility = View.GONE
        }

        val adapter = ArrayAdapter.createFromResource(this,
                R.array.array_favorite, R.layout.spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(detail.favorite_status)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (detail.favorite_status == position) {
                    return
                }

                ApiClient.getInstance().uploadFavoriteStatus(detail.id, FavoriteChangeRequest(position))
                        .withLifecycle()
                        .subscribe({
                            detail.favorite_status = position
                        }, {
                            toastErrors()
                            spinner.setSelection(detail.favorite_status)
                        })
            }

        }

        if (detail is BangumiDetail && detail.episodes != null && detail.episodes.isNotEmpty()) {
            recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                    val button = v.findViewById(R.id.button) as Button
                }

                override fun onBindViewHolder(p0: RecyclerView.ViewHolder?, p1: Int) {
                    if (p0 is VH) {
                        val d = detail.episodes[p1]
                        if (d.status != 0) {
                            p0.button.text = (p1 + 1).toString() + ". " + d.name_cn
                            p0.button.setOnClickListener {
                                startActivity(PlayPaddingActivity.intent(p0.button.context, d.id))
                            }
                        } else {
                            p0.button.text = "未更新"
                            p0.button.setOnClickListener(null)
                        }
                    }
                }

                override fun onCreateViewHolder(p0: ViewGroup?, p1: Int): RecyclerView.ViewHolder {
                    return VH(LayoutInflater.from(p0!!.context).inflate(R.layout.rv_item_episode, p0, false))
                }

                override fun getItemCount(): Int {
                    return detail.episodes.size
                }

            }
        }

    }
}