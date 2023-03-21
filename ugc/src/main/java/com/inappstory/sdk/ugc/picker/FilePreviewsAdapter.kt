package com.inappstory.sdk.ugc.picker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.inappstory.sdk.stories.utils.Sizes
import com.inappstory.sdk.ugc.R

internal class FilePreviewsAdapter : RecyclerView.Adapter<FilePreviewsHolder> {

    var picker: FilePicker
    private val previews = arrayListOf<FilePicker.FileData>()
    private val cache = FilePreviewsCache()
    var cameraCallback: OpenCameraClickCallback
    var clickCallback: FileClickCallback
    var noAccessCallback: NoAccessCallback
    var hasFileAccess = false
    var allowMultipleSelection = false
    var galleryAccessText: String
    var galleryFileMaxCount = 0
    var galleryFileLimitText: String

    constructor(
        context: Context?,
        hasFileAccess: Boolean,
        allowMultipleSelection: Boolean,
        mimeTypes: List<String>,
        clickCallback: FileClickCallback,
        cameraCallback: OpenCameraClickCallback,
        noAccessCallback: NoAccessCallback,
        galleryAccessText: String,
        galleryFileMaxCount: Int,
        galleryFileLimitText: String,
        pickerFilter: PickerFilter
    ) {
        this.noAccessCallback = noAccessCallback
        this.galleryFileMaxCount = galleryFileMaxCount
        this.galleryFileLimitText = galleryFileLimitText
        this.galleryAccessText = galleryAccessText
        this.cameraCallback = cameraCallback
        this.clickCallback = clickCallback
        this.hasFileAccess = hasFileAccess
        this.allowMultipleSelection = allowMultipleSelection
        picker = PhotoVideoPicker()
        if (hasFileAccess) {
            previews.addAll(
                picker.getImagesPath(
                    context!!,
                    pickerFilter,
                    mimeTypes
                )
            )
        }
    }

    override fun onViewRecycled(holder: FilePreviewsHolder) {
        super.onViewRecycled(holder)
        if (holder.path != null) {
            cache.remove(holder.path)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilePreviewsHolder {
        val v = when (viewType) {
            -1 -> LayoutInflater.from(parent.context).inflate(
                R.layout.cs_file_camera_cell,
                parent, false
            )
            -2 -> LayoutInflater.from(parent.context).inflate(
                R.layout.cs_file_no_access_cell,
                parent, false
            )
            else -> LayoutInflater.from(parent.context).inflate(
                R.layout.cs_file_picker_cell,
                parent, false
            )
        }
        v.layoutParams.width = Sizes.getScreenSize(v.context).x / 3
        v.layoutParams.height = 16 * v.layoutParams.width / 9
        return FilePreviewsHolder(v)
    }

    private val activePositions: ArrayList<Int> = arrayListOf()

    override fun onBindViewHolder(holder: FilePreviewsHolder, position: Int) {
        val intPos = Integer.valueOf(position - 1)
        if (position != 0) {
            if (hasFileAccess) {
                val (path, duration) = previews[position - 1]
                holder.itemView.isSelected = activePositions.contains(intPos)
                val iv = holder.itemView.findViewById<ImageView>(R.id.image)
                if (iv != null) {
                    holder.path = path
                    cache.loadPreview(path, iv, duration != null)
                }
                val count = holder.itemView.findViewById<TextView>(R.id.count)
                val videoDuration = holder.itemView.findViewById<TextView>(R.id.videoDuration)
                if (duration != null) {
                    videoDuration.visibility = View.VISIBLE
                    videoDuration.text = convertLongToTime(duration / 1000)
                } else {
                    videoDuration.visibility = View.GONE
                }
                if (activePositions.contains(intPos)) {
                    count.text = "${(activePositions.indexOf(position - 1) + 1)}"
                    count.visibility = View.VISIBLE
                } else {
                    count.visibility = View.GONE
                }
                holder.itemView.setOnClickListener {
                    if (activePositions.contains(intPos)) {
                        clickCallback.unselect(path)
                        activePositions.remove(intPos)
                    } else {
                        if (activePositions.size >= galleryFileMaxCount) {
                            Toast.makeText(
                                holder.itemView.context,
                                galleryFileLimitText,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }
                        activePositions.add(intPos)
                        clickCallback.select(path)
                        if (!allowMultipleSelection) {
                            val i: MutableIterator<Int> = activePositions.iterator()
                            while (i.hasNext()) {
                                val activePosition = i.next()
                                if (activePosition != intPos.toInt()) {
                                    clickCallback.unselect(previews[activePosition].name)
                                    i.remove()
                                }
                            }
                        }
                    }
                    notifyChanges()
                }
            } else {
                val text = holder.itemView.findViewById<TextView>(R.id.gallery_access_text)
                text.text = galleryAccessText
                holder.itemView.setOnClickListener { noAccessCallback.click() }
            }
        } else {
            holder.itemView.setOnClickListener { cameraCallback.open() }
        }
    }

    private fun notifyChanges() {
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 0) -1
        else if (!hasFileAccess && position == 1) -2
        else position

    @SuppressLint("DefaultLocale")
    private fun convertLongToTime(seconds: Long): String {
        val s = seconds % 60
        val m = seconds / 60 % 60
        val h = seconds / (60 * 60) % 24
        return if (seconds >= 3600)
            String.format("%02d:%02d:%02d", h, m, s)
        else
            String.format("%02d:%02d", m, s)
    }


    override fun getItemId(position: Int): Long = if (position == 0) -1 else position.toLong()

    override fun getItemCount(): Int {
        val count = (if (hasFileAccess) previews.size else 1) + 1
        Log.e("adapterChanges", "getItemCount $count")
        return count
    }
}