package com.fibelatti.pinboard.features.posts.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fibelatti.core.extension.afterTextChanged
import com.fibelatti.core.extension.children
import com.fibelatti.core.extension.gone
import com.fibelatti.core.extension.hideKeyboard
import com.fibelatti.core.extension.isKeyboardSubmit
import com.fibelatti.core.extension.textAsString
import com.fibelatti.core.extension.visible
import com.fibelatti.pinboard.R
import com.fibelatti.pinboard.core.android.base.BaseFragment
import com.fibelatti.pinboard.core.extension.applyAs
import com.fibelatti.pinboard.core.extension.blink
import com.fibelatti.pinboard.core.extension.navigateBack
import com.fibelatti.pinboard.features.mainActivity
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.fragment_add_post.*
import javax.inject.Inject

class PostAddFragment @Inject constructor() : BaseFragment() {

    companion object {
        @JvmStatic
        val TAG: String = PostAddFragment::class.java.simpleName
    }

    private val postAddViewModel by lazy { viewModelFactory.get<PostAddViewModel>(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(com.fibelatti.pinboard.R.layout.fragment_add_post, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity?.updateTitleLayout {
            setTitle(R.string.posts_add_title)
            setNavigateUp(R.drawable.ic_close) { navigateBack() }
        }

        mainActivity?.updateViews { bottomAppBar: BottomAppBar, fab: FloatingActionButton ->
            bottomAppBar.gone()
            fab.run {
                blink {
                    setImageResource(R.drawable.ic_done)
                    setOnClickListener {
                        // TODO
                    }
                }
            }
        }

        setupTagInput()
    }

    private fun setupTagInput() {
        fun createTagFromText(text: String, considerWhiteSpace: Boolean = true) {
            text.run {
                if (considerWhiteSpace) takeIf { it.endsWith(" ") }?.substringBefore(" ", "") else this
            }?.takeIf { it.isNotBlank() }?.let {
                addTag(it.trim())
                editTextTags.setText("")
            }
        }

        editTextTags.afterTextChanged { createTagFromText(it) }
        editTextTags.setOnEditorActionListener { _, actionId, event ->
            if (isKeyboardSubmit(actionId, event)) {
                createTagFromText(editTextTags.textAsString(), considerWhiteSpace = false)
            }
            return@setOnEditorActionListener true
        }
    }

    private fun addTag(value: String) {
        val chip = layoutInflater.inflate(R.layout.list_item_chip, chipGroupTags, false)
            .applyAs<View, Chip> {
                text = value
                setOnCloseIconClickListener { chipGroupTags.removeView(this) }
            }

        if (chipGroupTags.children.none { (it as? Chip)?.text == value }) {
            chipGroupTags.addView(chip, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainActivity?.updateViews { bottomAppBar: BottomAppBar, fab: FloatingActionButton ->
            bottomAppBar.hideKeyboard()
            bottomAppBar.visible()
        }
    }
}