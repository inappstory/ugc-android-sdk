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
    private val cache = FilePreviewsCache(false)
    private val cameraCallback: OpenCameraClickCallback
    private val storageCallback: OpenStorageClickCallback
    private val clickCallback: FileClickCallback
    private val noAccessCallback: NoAccessCallback
    private var hasFileAccess = false
    private var allowMultipleSelection = false
    private var galleryFileMaxCount = 0
    private val translations: MutableMap<String, String> = hashMapOf()

    constructor(
        context: Context?,
        hasFileAccess: Boolean,
        allowMultipleSelection: Boolean,
        mimeTypes: List<String>,
        clickCallback: FileClickCallback,
        cameraCallback: OpenCameraClickCallback,
        storageCallback: OpenStorageClickCallback,
        noAccessCallback: NoAccessCallback,
        galleryFileMaxCount: Int,
        translations: Map<String, String>,
        pickerFilter: PickerFilter
    ) {
        this.noAccessCallback = noAccessCallback
        this.translations.putAll(translations)
        this.galleryFileMaxCount = galleryFileMaxCount
        this.cameraCallback = cameraCallback
        this.storageCallback = storageCallback
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
                R.layout.cs_file_storage_cell,
                parent, false
            )

            -3 -> LayoutInflater.from(parent.context).inflate(
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
        val intPos = Integer.valueOf(position - 2)
        if (position > 1) {
            if (hasFileAccess) {
                val file = previews[position - 1]
                val path = file.name
                val duration = file.duration
                holder.itemView.isSelected = activePositions.contains(intPos)
                val iv = holder.itemView.findViewById<ImageView>(R.id.image)
                if (iv != null) {
                    holder.path = path
                    cache.loadPreview(
                        path,
                        iv,
                        file.unavailableByDuration || file.unavailableBySize,
                        duration != null
                    )
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
                if (file.unavailableByDuration || file.unavailableBySize) {
                    count.text = "!"
                    count.visibility = View.VISIBLE
                }
                holder.itemView.setOnClickListener {
                    if (file.unavailableByDuration) {
                        showToast(
                            holder.itemView.context,
                            translations["fileLimitVideoDuration"] ?: ""
                        )
                    } else if (file.unavailableBySize) {
                        showToast(
                            holder.itemView.context,
                            (if (file.type == "video") {
                                translations["fileLimitVideoSize"]
                            } else {
                                translations["fileLimitPhotoSize"]
                            }) ?: ""
                        )
                    } else if (activePositions.contains(intPos)) {
                        clickCallback.unselect(SelectedFile(path, file.type))
                        activePositions.remove(intPos)
                    } else {
                        if (activePositions.size >= galleryFileMaxCount) {
                            showToast(
                                holder.itemView.context,
                                translations["galleryFileLimitText"] ?: ""
                            )
                            return@setOnClickListener
                        }
                        activePositions.add(intPos)
                        Log.d("PhotoPicker", "Old $path")
                        clickCallback.select(SelectedFile(path, file.type))
                        if (!allowMultipleSelection) {
                            val i: MutableIterator<Int> = activePositions.iterator()
                            while (i.hasNext()) {
                                val activePosition = i.next()
                                if (activePosition != intPos.toInt()) {
                                    clickCallback.unselect(
                                        SelectedFile(
                                            previews[activePosition].name,
                                            previews[activePosition].type
                                        )
                                    )
                                    i.remove()
                                }
                            }
                        }
                    }
                    notifyChanges()
                }
            } else {
                val text = holder.itemView.findViewById<TextView>(R.id.gallery_access_text)
                text.text = translations["galleryAccessText"] ?: ""
                holder.itemView.setOnClickListener { noAccessCallback.click() }
            }
        } else if (position == 0) {
            holder.itemView.setOnClickListener { cameraCallback.open() }
        } else {
            holder.itemView.setOnClickListener { storageCallback.open() }
        }
    }

    private fun showToast(context: Context, text: String) {
        Toast.makeText(
            context,
            text,
            Toast.LENGTH_SHORT
        ).show()
    }

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

    private fun notifyChanges() {
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 0) -1
        else if (position == 1) -2
        else if (!hasFileAccess && position == 2) -3
        else position

    override fun getItemId(position: Int): Long =
        when (position) {
            0 -> -1
            1 -> -2
            else -> position.toLong()
        }

    override fun getItemCount(): Int {
        return (if (hasFileAccess) previews.size else 1) + 2
    }
}