package com.sqrtf.megumin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.sqrtf.common.api.ApiClient
import com.sqrtf.common.model.Bangumi
import io.reactivex.Observable

/**
 * Created by roya on 2017/6/6.
 */
class FavoriteActivity : ListActivity() {
    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, FavoriteActivity::class.java)
        }
    }

    var loaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_favorite)
    }

    override fun onLoadData(): Observable<List<Bangumi>> {
        if (!loaded) {
            loaded = true
            return ApiClient.getInstance().getMyBangumi()
                    .flatMap { Observable.just(it.getData()) }
        } else {
            return Observable.create<List<Bangumi>> { it.onComplete() }
        }
    }
}