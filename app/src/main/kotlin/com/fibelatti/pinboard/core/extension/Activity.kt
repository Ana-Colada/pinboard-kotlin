package com.fibelatti.pinboard.core.extension

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import com.fibelatti.core.extension.inTransaction
import com.fibelatti.pinboard.R

fun FragmentActivity.popTo(tag: String) {
    for (fragment in supportFragmentManager.fragments.reversed()) {
        if (fragment.tag != tag) {
            supportFragmentManager.popBackStack()
        } else {
            break
        }
    }
}

fun FragmentActivity.slideFromTheRight(fragment: Fragment, tag: String, addToBackStack: Boolean = true) {
    inTransaction {
        setCustomAnimations(
            R.anim.slide_right_in,
            R.anim.slide_left_out,
            R.anim.slide_left_in,
            R.anim.slide_right_out
        )
        add(R.id.fragmentHost, fragment, tag)

        if (addToBackStack) {
            addToBackStack(tag)
        }
    }
}

fun FragmentActivity.slideUp(fragment: Fragment, tag: String, addToBackStack: Boolean = true) {
    inTransaction {
        setCustomAnimations(R.anim.slide_up, -1, -1, R.anim.slide_down)
        add(R.id.fragmentHost, fragment, tag)

        if (addToBackStack) {
            addToBackStack(tag)
        }
    }
}

inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T
) = lazy(LazyThreadSafetyMode.NONE) { bindingInflater.invoke(layoutInflater) }
