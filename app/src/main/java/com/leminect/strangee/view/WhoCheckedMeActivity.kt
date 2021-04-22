package com.leminect.strangee.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.leminect.strangee.R
import com.leminect.strangee.adapter.StrangeeClickListener
import com.leminect.strangee.adapter.StrangeeGridAdapter
import com.leminect.strangee.databinding.ActivityWhoCheckedMeBinding
import com.leminect.strangee.model.Strangee
import com.leminect.strangee.utility.getFromSharedPreferences
import com.leminect.strangee.viewmodel.SavedStatus
import com.leminect.strangee.viewmodel.SavedViewModel
import com.leminect.strangee.viewmodel.WhoCheckedMeStatus
import com.leminect.strangee.viewmodel.WhoCheckedMeViewModel
import com.leminect.strangee.viewmodelfactory.SavedViewModelFactory
import com.leminect.strangee.viewmodelfactory.WhoCheckedMeViewModelFactory

class WhoCheckedMeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWhoCheckedMeBinding
    private lateinit var viewModel: WhoCheckedMeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_who_checked_me)

        setUpActionBar()
        binding.lifecycleOwner = this
        val pair = getFromSharedPreferences(this)
        val token = pair.first
        val user = pair.second

        val viewModelFactory = WhoCheckedMeViewModelFactory(token, user.userId)
        viewModel = ViewModelProvider(this, viewModelFactory).get(WhoCheckedMeViewModel::class.java)

        val adapter = StrangeeGridAdapter(StrangeeClickListener({ strangee ->
            viewModel.displaySavedProfile(strangee)
        },{},{ strangee ->
            viewModel.removeWhoCheckedMe(token, strangee.userId)
        }), true)

        binding.checkedRecyclerView.adapter = adapter

        viewModel.whoCheckedMeList.observe(this, Observer { list ->
            list?.let {
                adapter.submitList(list)
            }
        })

        binding.reloadButton.setOnClickListener {
            viewModel.getWhoCheckedMe(token, user.userId)
        }

        viewModel.status.observe(this, Observer { status ->
            status?.let {
                when (status) {
                    WhoCheckedMeStatus.LOADING -> {
                        binding.reloadButton.visibility = View.GONE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.loading_animation)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.visibility = View.GONE
                    }
                    WhoCheckedMeStatus.ERROR -> {
                        binding.reloadButton.visibility = View.VISIBLE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.internet_error_anim)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.text = getString(R.string.error_loading_profiles)
                        binding.errorTextView.visibility = View.VISIBLE
                    }
                    WhoCheckedMeStatus.FAILED -> {
                        binding.reloadButton.visibility = View.VISIBLE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.internet_error_anim)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.text = getString(R.string.failed_to_load_profiles)
                        binding.errorTextView.visibility = View.VISIBLE
                    }
                    WhoCheckedMeStatus.EMPTY -> {
                        binding.reloadButton.visibility = View.GONE
                        binding.statusAnimation.apply {
                            setAnimation(R.raw.no_results_found)
                            visibility = View.VISIBLE
                            if (!this.isAnimating) this.playAnimation()
                        }
                        binding.errorTextView.text = getString(R.string.nobody_visited)
                        binding.errorTextView.visibility = View.VISIBLE
                    }
                    WhoCheckedMeStatus.DONE -> {
                        binding.reloadButton.visibility = View.GONE
                        binding.statusAnimation.visibility = View.GONE
                        binding.errorTextView.visibility = View.GONE
                    }
                }
            }
        })

        viewModel.navigateToSelectedProfile.observe(this, Observer {
            it?.let {
                goToStrangeeProfile(it)
                viewModel.onDisplaySavedProfileComplete()
            }
        })
    }

    private fun goToStrangeeProfile(strangee: Strangee) {
        val intent: Intent = Intent(this, StrangeeProfileActivity::class.java)
        intent.putExtra("strangee_data", strangee)
        startActivity(intent)
    }

    private fun setUpActionBar() {
        val actionbar = supportActionBar
        actionbar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        actionbar?.setCustomView(R.layout.sign_up_action_bar)
        actionbar?.customView?.findViewById<TextView>(R.id.action_bar_text)?.text =
            getString(R.string.who_checked_me)

        supportActionBar?.customView?.findViewById<ImageView>(R.id.back_button)
            ?.setOnClickListener {
                onBackPressed()
            }
    }
}