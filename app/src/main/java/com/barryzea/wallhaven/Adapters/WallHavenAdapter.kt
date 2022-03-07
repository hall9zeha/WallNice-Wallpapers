package com.barryzea.wallhaven.Adapters

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.barryzea.wallhaven.Activities.MainActivity
import com.barryzea.wallhaven.Database.DataBaseHandler
import com.barryzea.wallhaven.Listeners.RecyclerWallHavenListener
import com.barryzea.wallhaven.Models.WallHavenData
import com.barryzea.wallhaven.Models.WallHavenFavorites
import com.barryzea.wallhaven.Models.WallHavenMeta
import com.barryzea.wallhaven.R
import com.barryzea.wallhaven.Util.inflate
import com.barryzea.wallhaven.Util.loadToUrlProgress
import com.barryzea.wallhaven.Util.snackBar
import com.barryzea.wallhaven.Util.toast
import com.google.android.material.snackbar.Snackbar


class WallHavenAdapter(private val context: Context, private val activity:Activity, private val images:MutableList<WallHavenData>, private val pages: WallHavenMeta, private val listener: RecyclerWallHavenListener): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private   var imagesAll:MutableList<WallHavenData> = images
    val VIEW_TYPE_NORMAL=1
    private var VIEW_TYPE_LOADING=2
    private var isLoadingAdded:Boolean=false

    override fun getItemViewType(position: Int) = if (position == images.size -1 && isLoadingAdded)VIEW_TYPE_LOADING else VIEW_TYPE_NORMAL




   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):RecyclerView.ViewHolder{
        return when(viewType){
            VIEW_TYPE_NORMAL ->ViewHolder(parent.inflate(R.layout.wallpaper_cardview),context, activity)
            else -> ViewHolderLoading(parent.inflate(R.layout.item_loading))
        }
    }





   override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)
   {
       when(holder.itemViewType){
           VIEW_TYPE_NORMAL->(holder as ViewHolder).bind(imagesAll[position], listener,setBundleWallHaven(position))

       }
   }
    override fun getItemCount()=imagesAll.size

     fun setBundleWallHaven(position:Int):Bundle{

        var bundle = Bundle()

         bundle.putString("id",imagesAll[position].id)
         bundle.putString("short_url",imagesAll[position].short_url)
         bundle.putString("category",imagesAll[position].category)
         bundle.putString("views",imagesAll[position].views)
         bundle.putString("favorites",imagesAll[position].favorites)
         bundle.putString("resolution",imagesAll[position].resolution)
         bundle.putLong("file_size",imagesAll[position].file_size)
         bundle.putString("file_type",imagesAll[position].file_Type)
         bundle.putStringArray("colors",imagesAll[position].colors)
         bundle.putString("path",imagesAll[position].path)
         bundle.putString("createAt",imagesAll[position].created_at)
         bundle.putString("purity",imagesAll[position].purity)
         bundle.putInt("dimension_x",imagesAll[position].dimension_x)
         bundle.putInt("dimension_y",imagesAll[position].dimension_y)

         return bundle

    }
    class ViewHolder(itemView:View, context:Context, ac:Activity):RecyclerView.ViewHolder(itemView){
        private var db:DataBaseHandler= DataBaseHandler(context,ac)
        private var viewHeader:View=itemView.findViewById(R.id.viewBackgroundCategory)
        private var viewFooter:View=itemView.findViewById(R.id.viewBackGroundFooter)
        private var fav:WallHavenFavorites= WallHavenFavorites()
        private   val tvCategory :TextView=itemView.findViewById(R.id.tvCategory)
        private      val imgWallpaper: ImageView=itemView.findViewById(R.id.imgWallpaper)
        private  val btnFavorite: ImageView =itemView.findViewById(R.id.btnFavorite)
        private var btnViews:ImageView=itemView.findViewById(R.id.btnViews)
        private val tvViews:TextView=itemView.findViewById(R.id.tvViews)
        private val tvResolution:TextView=itemView.findViewById(R.id.tvResolution)
        private val progressBarLoading:ProgressBar=itemView.findViewById(R.id.progressBarLoading)
        private val cardView:CardView=itemView.findViewById(R.id.wallpaperCardView)
        private val set:ConstraintSet= ConstraintSet()
        private val mConstraintParent:ConstraintLayout=itemView.findViewById(R.id.parentConstraint)
        private val ac=ac
        private var statusFavorite=true
        fun bind(images:WallHavenData, listener: RecyclerWallHavenListener, bundle:Bundle)=with(itemView)
        {




            tvCategory.text=images.category
            tvViews.text=images.views
            tvResolution.text=images.resolution


            images.thumbs?.original?.let { imgWallpaper.loadToUrlProgress(it,progressBarLoading) }

            val ratio =String.format("%d:%d", images.dimension_x, images.dimension_y)
            set.clone(mConstraintParent)
            set.setDimensionRatio(imgWallpaper.id,ratio)
            set.applyTo(mConstraintParent)
            btnFavorite.visibility=View.VISIBLE
            setOnClickListener{listener.onClick(images,adapterPosition,bundle)}
            this.setOnLongClickListener { listener.onLongClick(images, adapterPosition, this); true }


            getPreferencesDefault(context,images)



        }
        private fun getPreferencesDefault(context:Context, images: WallHavenData){
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val purityPref=prefs.getBoolean("purityPreference",false)
            val viewsPreference=prefs.getBoolean("viewsPreference",false)
            val favPreference=prefs.getBoolean("favoritesPreference",false)
            val resolutionPreference=prefs.getBoolean("resolutionPreference", false)
            val categoryPreference=prefs.getBoolean("categoryPreference",false)


            if(purityPref){
                setColorBackgroundCardViewForPurity(images, context)
            }
            if(viewsPreference){
                btnViews.visibility=View.VISIBLE
                tvViews.visibility=View.VISIBLE
            }
            else{
                btnViews.visibility=View.GONE
                tvViews.visibility=View.GONE
            }
            if(favPreference){
                btnFavorite.visibility=View.VISIBLE
                initListenerFavorites(images,context)
                setFavoriteState(images)
            }
            else{
                btnFavorite.visibility=View.GONE
            }
            if(resolutionPreference){
                tvResolution.visibility=View.VISIBLE
            }
            else{
                tvResolution.visibility=View.GONE
            }
            if(categoryPreference){
                tvCategory.visibility=View.VISIBLE
            }
            else{
                tvCategory.visibility=View.GONE
            }
            //**************************************
          if(viewsPreference && favPreference){
                viewFooter.visibility=View.VISIBLE
            }
            else{
                viewFooter.visibility=View.GONE
            }
            if(resolutionPreference && categoryPreference){
                viewHeader.visibility=View.VISIBLE
            }
            else{
                viewHeader.visibility=View.GONE
            }


        }
        private fun initListenerFavorites(images:WallHavenData, context: Context){
            btnFavorite.setOnClickListener {
                if(statusFavorite){
                    statusFavorite=false
                    btnFavorite.setImageResource(R.drawable.ic_favorite_full_white)
                    saveFavorite(images)
                    MainActivity.refreshFavoritesBadge(context,ac)
                }
                else{
                    statusFavorite=true
                    btnFavorite.setImageResource(R.drawable.ic_favorite_outline)
                    deleteFavorite(images)
                    MainActivity.refreshFavoritesBadge(context,ac)

                }
            }
        }
        private fun setFavoriteState(images:WallHavenData){
            if(images.id?.let { db.checkFavoriteSave(it) } ==1){
                btnFavorite.setImageResource(R.drawable.ic_favorite_full_white)
            }
            else{
                btnFavorite.setImageResource(R.drawable.ic_favorite_outline)
            }
        }
        private fun setColorBackgroundCardViewForPurity(images:WallHavenData,ctx:Context){
            images.let{
                when(images.purity){
                    "sketchy"->cardView.setCardBackgroundColor(ContextCompat.getColor(ctx,R.color.yellow_500))
                    "nsfw"->cardView.setCardBackgroundColor(ContextCompat.getColor(ctx,android.R.color.holo_red_dark))
                   else->{
                       cardView.setCardBackgroundColor(ContextCompat.getColor(ctx,R.color.black_soft))

                   }

                }
            }
        }
        private fun saveFavorite(images: WallHavenData){

            fav.idImage= images.id.toString()
            fav.idUser=""
            fav.shortUrl=images.path.toString()
            fav.dirImageSave=images.thumbs!!.small
            fav.resolution=images.resolution.toString()
            fav.dimensionX=images.dimension_x
            fav.dimensionY=images.dimension_y
            fav.views=images.views.toString()
            fav.category=images.category.toString()
            fav.size= humanReadableByteCountBin(images.file_size)

            fav.date_added=System.currentTimeMillis()

            val result=db.addFavorite(fav)
            if(result>0){
                ac.snackBar(message=R.string.msgSaveFavorite, duration = Snackbar.LENGTH_LONG)
            }
            else
            {
                ac.snackBar(message=R.string.msgErrorSaveFavorite)
            }
        }
        private fun humanReadableByteCountBin(bytes: Long) = when {
            bytes == Long.MIN_VALUE || bytes < 0 -> "N/A"
            bytes < 1024L -> "$bytes B"
            bytes <= 0xfffccccccccccccL shr 40 -> "%.1f KiB".format(bytes.toDouble() / (0x1 shl 10))
            bytes <= 0xfffccccccccccccL shr 30 -> "%.1f MiB".format(bytes.toDouble() / (0x1 shl 20))
            bytes <= 0xfffccccccccccccL shr 20 -> "%.1f GiB".format(bytes.toDouble() / (0x1 shl 30))
            bytes <= 0xfffccccccccccccL shr 10 -> "%.1f TiB".format(bytes.toDouble() / (0x1 shl 40))
            bytes <= 0xfffccccccccccccL -> "%.1f PiB".format((bytes shr 10).toDouble() / (0x1 shl 40))
            else -> "%.1f EiB".format((bytes shr 20).toDouble() / (0x1 shl 40))
        }
        private fun deleteFavorite(images: WallHavenData){
            fav.idImage=images.id.toString()
            val result=db.deleteFavorite(fav)
            if(result>0){
                ac.snackBar(message=R.string.msgDislikeFavorite, duration=Snackbar.LENGTH_LONG)
            }
            else
            {
                ac.toast("No Eliminado Error")
            }
        }

    }
    class ViewHolderLoading(itemView:View): RecyclerView.ViewHolder(itemView) {


        fun bind()=with(itemView){

        }

    }
    fun add(w:WallHavenData){
        imagesAll.add(w)
        notifyItemInserted(imagesAll.size - 1)
    }
    fun addAll(wallpapersList : List<WallHavenData>){

        for(w:WallHavenData in wallpapersList )
        {
            add(w)
        }
    }

    /**
     * métodos para manejar el footer loading
     */
    fun remove(w:WallHavenData){
        var position:Int=imagesAll.indexOf(w)
        if(position>-1){
            imagesAll.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun addLoadingFooter(){
        isLoadingAdded=true
        add(WallHavenData())
    }
    fun removeLoadingFooter(){
        isLoadingAdded=false
        var position:Int=imagesAll.size-1
        var w:WallHavenData=getItem(position)
        if(w != null){
            imagesAll.removeAt(position)
            notifyItemRemoved(position)
        }
    }
    fun getItem(position:Int):WallHavenData{
        return imagesAll[position]
    }

    /**
     *
     */
    fun clear(){
        imagesAll.clear()
        notifyDataSetChanged()
    }



}