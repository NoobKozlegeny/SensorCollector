package com.example.gyrocollector.animations

import android.content.Context
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.example.gyrocollector.R
import com.example.gyrocollector.databinding.ActivityMainBinding

public class FloatBtnOptions(ctx: Context, private val binding: ActivityMainBinding) {

    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(ctx, R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(ctx, R.anim.to_bottom_anim) }
    private var clicked: Boolean = false

    public fun onListOptionsClicked() {
        Log.d("clicked", clicked.toString())
        setVisibility(clicked)
        setAnimation(clicked)
        setClickable(clicked)
    }

    private fun setVisibility(clicked: Boolean) {
        if (!clicked) {
            binding.floatBtnStartGathering.visibility = View.VISIBLE
            binding.floatBtnClearData.visibility = View.VISIBLE
            binding.floatBtnSaveData.visibility = View.VISIBLE
        }
        else {
            binding.floatBtnStartGathering.visibility = View.INVISIBLE
            binding.floatBtnClearData.visibility = View.INVISIBLE
            binding.floatBtnSaveData.visibility = View.INVISIBLE
        }
    }

    private fun setAnimation(clicked: Boolean) {
        if (!clicked) {
            binding.floatBtnStartGathering.startAnimation(fromBottom)
            binding.floatBtnClearData.startAnimation(fromBottom)
            binding.floatBtnSaveData.startAnimation(fromBottom)
            this.clicked = true
        }
        else {
            binding.floatBtnStartGathering.startAnimation(toBottom)
            binding.floatBtnClearData.startAnimation(toBottom)
            binding.floatBtnSaveData.startAnimation(toBottom)
            this.clicked = false
        }
    }

    private fun setClickable(clicked: Boolean) {
        Log.d("clicked", clicked.toString())
        if (!clicked) {
            binding.floatBtnStartGathering.isClickable = false
            binding.floatBtnClearData.isClickable = false
            binding.floatBtnSaveData.isClickable = false
        }
        else {
            binding.floatBtnStartGathering.isClickable = true
            binding.floatBtnClearData.isClickable = true
            binding.floatBtnSaveData.isClickable = true
        }
    }
}