package com.barryzea.wallhaven.Fragments

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.barryzea.wallhaven.Activities.ImageDetailActivity
import com.barryzea.wallhaven.Activities.MainActivity
import com.barryzea.wallhaven.Adapters.WallHavenAdapter
import com.barryzea.wallhaven.App.preferencesFilter
import com.barryzea.wallhaven.Dialogs.ContextMenuDialogCustom
import com.barryzea.wallhaven.Interfaces.WallHavenApiInterface
import com.barryzea.wallhaven.Listeners.RecyclerWallHavenListener
import com.barryzea.wallhaven.Models.WallHaven
import com.barryzea.wallhaven.Models.WallHavenData
import com.barryzea.wallhaven.R
import com.barryzea.wallhaven.RetrofitHelper.CallBackWithRetry
import com.barryzea.wallhaven.Util.PaginationRecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Response

class LastAddedFragment : Fragment() {

    private var PAGE_START=1
    private var TOTAL_PAGES=0
    private var current_page=PAGE_START
    private var imagesPerPage=24
    private lateinit var  linearLayoutProgressBar:LinearLayout
    private lateinit var recyclerViewLatest:RecyclerView
    private lateinit var wallHavenAdapter: WallHavenAdapter

    private lateinit var linearLayoutLastError:LinearLayout
    private lateinit var txtLastAddedError:TextView
    private lateinit var swipeRefreshLasted: SwipeRefreshLayout
    private lateinit var btnRefresh:ImageButton
    private lateinit var fbReturnToTop:FloatingActionButton
    private var listImages: ArrayList<WallHavenData> = ArrayList()
    private lateinit var staggeredGridLayoutManager:StaggeredGridLayoutManager
    private var isLoading:Boolean=false
    private var isLastPage:Boolean=false
    private var onScrollListener:RecyclerView.OnScrollListener?=null
    private var CATEGORY="110"
    private var PURITY="100"
    private var ATLEAST=""
    private var RATIOS=""
    private var API_KEY=""
    private lateinit var apiService:Call<WallHaven>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment


            val rootView = inflater.inflate(R.layout.fragment_last_added, container, false)
            activity?.setTitle(R.string.latestFragmentTitle)
            getPreferencesFilter()
            initViews(rootView)
            //*****************************
            setColumnsInStaggeredLayout()
            recyclerViewLatest.setHasFixedSize(true)
            recyclerViewLatest.itemAnimator = DefaultItemAnimator()
            recyclerViewLatest.layoutManager = staggeredGridLayoutManager

            //*****************************

            getLatestImages()
            setRecyclerViewOnScrollListener()
            swipeRefreshLasted.setOnRefreshListener {
                apiService.cancel()
                wallHavenAdapter.clear()
                ATLEAST=""
                RATIOS=""
                getPreferencesFilter()
                getLatestImages()
                current_page=1

            }
        btnRefresh.setOnClickListener {
            if(listImages.size>0){
                wallHavenAdapter.clear()
            }
            linearLayoutLastError.visibility=View.GONE
            linearLayoutProgressBar.visibility=View.VISIBLE

            ATLEAST=""
            RATIOS=""
            getPreferencesFilter()
            getLatestImages()
            current_page=1

        }
        fbReturnToTop.setOnClickListener {
            recyclerViewLatest.smoothScrollToPosition(0)
            fbReturnToTop.visibility=View.GONE
        }

        return rootView


    }
    private fun initViews(rootView:View){
        recyclerViewLatest = rootView.findViewById(R.id.recyclerViewImagesLatest)

        linearLayoutLastError=rootView.findViewById(R.id.linearLayoutLastAddedError)
        txtLastAddedError=rootView.findViewById(R.id.textViewLastAddedError)
        swipeRefreshLasted=rootView.findViewById(R.id.swipeRefreshLastAddedList)
        btnRefresh=rootView.findViewById(R.id.btnReloadLastList)
        fbReturnToTop=rootView.findViewById(R.id.fbReturnToTopLast)
        linearLayoutProgressBar=rootView.findViewById(R.id.linearLayoutProgressBarLastAdded)
    }
    private fun getPreferencesFilter(){
        if(!preferencesFilter.categoriesMain.isNullOrEmpty()){
            CATEGORY= preferencesFilter.categoriesMain!!
        }
        if(!preferencesFilter.purityMain.isNullOrEmpty()){
            PURITY= preferencesFilter.purityMain!!
        }
        if(!preferencesFilter.resolutionMain.isNullOrEmpty())
        {
            ATLEAST= preferencesFilter.resolutionMain!!
        }
        if(!preferencesFilter.ratiosMain.isNullOrEmpty())
        {
            RATIOS= preferencesFilter.ratiosMain!!
        }
        if(!preferencesFilter.apiKey.isNullOrEmpty()){
            API_KEY= preferencesFilter.apiKey!!
        }
    }
    private fun setColumnsInStaggeredLayout(){
        val prefs=PreferenceManager.getDefaultSharedPreferences(context)
        val numColumnsPortrait= prefs.getString("columnsPortrait", "2")?.let { Integer.parseInt(it) }
        val numColumnsLandscape=prefs.getString("columnsLandscape", "4")?.let{Integer.parseInt(it)}


        if(activity?.resources?.configuration?.orientation== Configuration.ORIENTATION_PORTRAIT)
        {
            staggeredGridLayoutManager= StaggeredGridLayoutManager(numColumnsPortrait!!,StaggeredGridLayoutManager.VERTICAL)
        }
        else{
            staggeredGridLayoutManager= StaggeredGridLayoutManager(numColumnsLandscape!!,StaggeredGridLayoutManager.VERTICAL)
        }
    }
    private fun setRecyclerViewOnScrollListener(){
        onScrollListener=object:PaginationRecyclerView(staggeredGridLayoutManager,fbReturnToTop){
            override fun loadMoreItems() {
                isLoading=true
                current_page +=1
                getNextPageLatestImages()
            }

            override fun getTotalPageCount(): Int {
                return imagesPerPage
            }

            override fun isLastPage(): Boolean {
                return isLastPage
            }

            override fun isLoading(): Boolean {
                return isLoading
            }

        }
        recyclerViewLatest.addOnScrollListener(onScrollListener!!)
    }
    private fun getLatestImages(){
        apiService=WallHavenApiInterface.create().getImages("date_added","",CATEGORY,PURITY,ATLEAST,RATIOS,PAGE_START,API_KEY)
        apiService.enqueue(object: CallBackWithRetry<WallHaven>(apiService){
            override fun onFinalFailure(call: Call<WallHaven>, t: Throwable, numRetry: Int) {
                super.onFinalFailure(call, t, numRetry)
                if(numRetry==11){
                    context?.let {
                        swipeRefreshLasted.isRefreshing = false
                        linearLayoutLastError.visibility = View.VISIBLE
                        txtLastAddedError.text = getString(R.string.errorRetrofitNetwork)

                        linearLayoutProgressBar.visibility=View.GONE
                    }
                }
            }



            override fun onFinalResponse(call: Call<WallHaven>, response: Response<WallHaven>, numRetry: Int) {
                super.onFinalResponse(call, response, numRetry)
                if(numRetry==11){
                    context?.let {
                        wallHavenAdapter.clear()
                        linearLayoutLastError.visibility = View.VISIBLE
                        txtLastAddedError.text = getString(R.string.errorRetrofitNetwork)
                        swipeRefreshLasted.isRefreshing=false
                    }
                }
                response.body()?.let {
                    context?.let {
                        //cargamos la cantidad de imágenes encontradas en el toolbar de main activity
                        activity?.let {
                            (activity as MainActivity)!!.setFoundImagesCount(response.body()!!.meta.total)
                        }

                        //
                        isLoading = false
                        linearLayoutLastError.visibility = View.GONE
                        swipeRefreshLasted.isRefreshing = false
                        linearLayoutProgressBar.visibility=View.GONE

                        TOTAL_PAGES = response.body()!!.meta.last_page
                        listImages.addAll(response.body()!!.data)
                        wallHavenAdapter = (WallHavenAdapter(
                                context!!,
                                activity as Activity,
                                listImages,
                                response.body()!!.meta,
                                object : RecyclerWallHavenListener {
                                    override fun onClick(
                                            image: WallHavenData,
                                            position: Int,
                                            bundle: Bundle
                                    ) {
                                        val intent = Intent(context, ImageDetailActivity::class.java)
                                        intent.putExtras(bundle)
                                        startActivity(intent)
                                    }

                                    override fun onDelete(image: WallHavenData, position: Int) {

                                    }

                                    override fun onLongClick(image: WallHavenData, position: Int, view: View) {
                                        context?.let {
                                            val dialog= ContextMenuDialogCustom.newInstance(activity!!,context!!,image)
                                            dialog.show(activity!!.supportFragmentManager, "MyDialog")
                                        }
                                    }
                                }))

                        if (current_page == TOTAL_PAGES) {
                            isLastPage = true
                        } else {
                            wallHavenAdapter.addLoadingFooter()
                        }
                        recyclerViewLatest.adapter = wallHavenAdapter


                    }
                }
            }


        })

    }
    private fun getNextPageLatestImages(){


        apiService=WallHavenApiInterface.create().getImages("date_added","",CATEGORY,PURITY,ATLEAST,RATIOS,current_page,API_KEY)
        apiService.enqueue(object:CallBackWithRetry<WallHaven>(apiService){
            override fun onFinalResponse(call: Call<WallHaven>, response: Response<WallHaven>, numRetry: Int) {
                super.onFinalResponse(call, response, numRetry)
                if(numRetry==11){
                    context?.let {
                        wallHavenAdapter.clear()
                        linearLayoutLastError.visibility = View.VISIBLE
                        txtLastAddedError.text = getString(R.string.errorRetrofitNetwork)
                    }
                }
                response.body()?.let {
                    context?.let {
                        isLoading = false
                        linearLayoutProgressBar.visibility=View.GONE

                        linearLayoutLastError.visibility = View.GONE
                        wallHavenAdapter.removeLoadingFooter()
                        wallHavenAdapter.addAll(response.body()!!.data)

                        if (current_page == TOTAL_PAGES) {
                            isLastPage = true
                        } else {
                            wallHavenAdapter.addLoadingFooter()
                        }
                    }
                }
            }


            override fun onFinalFailure(call: Call<WallHaven>, t: Throwable, numRetry: Int) {
                super.onFinalFailure(call, t, numRetry)
                if(numRetry==11){
                    context?.let {
                        wallHavenAdapter.clear()
                        linearLayoutLastError.visibility = View.VISIBLE
                        txtLastAddedError.text = getString(R.string.errorRetrofitNetwork)
                        linearLayoutProgressBar.visibility=View.GONE
                        swipeRefreshLasted.isRefreshing=false
                    }
                }
            }


        })

    }

    override fun onDestroy() {
        if(onScrollListener !=null){
        recyclerViewLatest.removeOnScrollListener(onScrollListener!!)}
        apiService=WallHavenApiInterface.create().getImages("date_added","",CATEGORY,PURITY,ATLEAST,RATIOS,PAGE_START,API_KEY)

        if(apiService!=null) {
            apiService.cancel()
        }
        super.onDestroy()
    }

}