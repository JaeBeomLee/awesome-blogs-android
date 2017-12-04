package org.petabytes.awesomeblogs.search

import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AbsListView
import android.widget.EditText
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnTextChanged
import com.annimon.stream.function.Supplier
import com.jakewharton.rxrelay.PublishRelay
import com.squareup.coordinators.Coordinators
import kotlinx.android.synthetic.main.search.view.*
import kotlinx.android.synthetic.main.search_item.view.*
import org.jsoup.Jsoup
import org.petabytes.api.source.local.Entry
import org.petabytes.awesomeblogs.AwesomeBlogsApp
import org.petabytes.awesomeblogs.R
import org.petabytes.awesomeblogs.summary.SummaryActivity
import org.petabytes.awesomeblogs.util.Analytics
import org.petabytes.awesomeblogs.util.Strings
import org.petabytes.awesomeblogs.util.Views
import org.petabytes.coordinator.Coordinator
import org.petabytes.coordinator.RecyclerAdapter
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action0
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit


/**
 * Created by leejaebeom on 2017. 11. 29..
 */
class KSearchCoordinator(val context: Context, private val closeAction: Action0) : Coordinator(){
    @BindView(R.id.search) var searchView: EditText? = null
    @BindView(R.id.placeholder) var placeholderView: TextView? = null
    @BindView(R.id.recycler) var recyclerView: RecyclerView? = null

    val keywordRelay :PublishRelay<String> = PublishRelay.create()

    override fun attach(view: View) {
        super.attach(view)

        var adapter = RecyclerAdapter<Entry>({
            val v = LayoutInflater.from(context).inflate(R.layout.search_item, null, false)
            val coordinator = SearchItemCoordinator(context, Supplier {  searchView?.text.toString() })
            Coordinators.bind(v, {coordinator})
            return@RecyclerAdapter RecyclerAdapter.ViewHolder(v, coordinator)

        })

        view.recycler.adapter = adapter
        view.recycler.addOnScrollListener(
                object : RecyclerView.OnScrollListener(){
                override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) Views.hideSoftInput(searchView!!)
                }
            }
        )

        bind(keywordRelay.debounce(250, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread()).
                flatMap{ keyword -> AwesomeBlogsApp.get().api().search(keyword) },
                {
                    entries ->
                    if (entries.isEmpty()){
                        placeholderView?.text = context.getText(R.string.empty_search_results)
                        Views.setVisibleOrGone(placeholderView!!, true)
                        Views.setVisibleOrGone(recyclerView!!, false)
                    }else{
                        recyclerView?.scrollToPosition(0)
                    }
                })

//

        Analytics.event(Analytics.Event.VIEW_SEARCH)
    }

    @OnTextChanged(R.id.search)
    fun onSearchChanged(keyword: Editable){
        keywordRelay.call(keyword.toString().trim())
        placeholderView?.text = context.getText(R.string.search_3)
        Views.setVisibleOrGone(placeholderView!!, keyword.isEmpty())
        Views.setVisibleOrGone(recyclerView!!, keyword.isNotEmpty())
    }

    @OnClick(R.id.close)
    fun onCloseClick(){
        closeAction.call()
    }


    inner class SearchItemCoordinator(val context: Context, private val keywordSupplier: Supplier<String>) : Coordinator(), RecyclerAdapter.OnBindViewHolderListener<Entry>{
        @BindView(R.id.title)
        private var titleView: TextView? = null
        @BindView(R.id.summary)
        var summaryView: TextView? = null
        @BindView(R.id.author)
        var authorView: TextView? = null

        override fun onBindViewHolder(entry: Entry, position: Int) {
            titleView?.setTypeface(view.title.typeface, Typeface.BOLD)
            titleView?.text = Strings.colorizeBackground(entry.title,
                    keywordSupplier.get(), ContextCompat.getColor(context, R.color.search), true)
            authorView?.text = Strings.colorizeBackground(Entry.getFormattedAuthorUpdatedAt(entry),
                    keywordSupplier.get(), ContextCompat.getColor(context, R.color.search), true)

            bind(Observable.just(entry.summary)
                    .map{ it -> Jsoup.parse(it).text() }
                    .map{it -> it.substring(0, Math.min(200, it.length))}
                    .subscribeOn(Schedulers.io()),
                    {
                        summaryView?.text = it.trim()
                        titleView?.post { summaryView?.maxLines = 4 - titleView!!.lineCount }
                    }
            )

            view.setOnClickListener {
                context.startActivity(SummaryActivity.intent(context, entry.link, Analytics.Param.SEARCH))
                Analytics.event(Analytics.Event.VIEW_SEARCH_ITEM, object : java.util.HashMap<String, String>(2) {
                    init {
                        put(Analytics.Param.TITLE, entry.title)
                        put(Analytics.Param.LINK, entry.link)
                    }
                })
            }

            view.setBackgroundResource(if (position % 2 == 0) R.color.white else R.color.background_row)



        }

    }
}
