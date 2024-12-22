package com.example.androidca

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class MyCustomAdapter(
    private val context: Context,
    protected var rankings: List<Ranking>
) : ArrayAdapter<Any?>(
    context, R.layout.row
) {
    init {
        addAll(*arrayOfNulls<Any>(rankings.size))
    }

    override fun getView(pos: Int, view: View?, parent: ViewGroup): View {
        var _view = view
        if (_view == null) {
            val inflater = context.getSystemService(
                Activity.LAYOUT_INFLATER_SERVICE
            ) as LayoutInflater

            _view = inflater.inflate(R.layout.row
                , parent, false)
        }

        val imageView = _view!!.findViewById<ImageView>(R.id.imageView)
        val id = context.resources.getIdentifier(
            "pic$pos",
            "drawable", context.packageName
        )
        imageView?.setImageResource(id)

        val textView1 = _view!!.findViewById<TextView>(R.id.textCol1)
        textView1?.text = rankings[pos].userName

        val textView2 = _view.findViewById<TextView>(R.id.textCol2)
        textView2?.text = "${rankings[pos].completionTime.toString()}s"

        val textView3 = _view.findViewById<TextView>(R.id.textCol3)
        textView3?.text = DateTimeConverter.toString(DateTimeConverter.fromString(rankings[pos].dateTime))

        return _view
    }
}