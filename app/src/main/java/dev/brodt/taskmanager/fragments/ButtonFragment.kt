package dev.brodt.taskmanager.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import dev.brodt.taskmanager.R

/**
 * A reusable button fragment that can be used throughout the application.
 * This fragment provides a customizable button with configurable text and click listener.
 */
class ButtonFragment : Fragment() {
    private lateinit var button: Button
    private var buttonText: String = "Button"
    private var onButtonClickListener: OnButtonClickListener? = null

    interface OnButtonClickListener {
        fun onButtonClick()
    }

    companion object {
        private const val ARG_BUTTON_TEXT = "button_text"

        /**
         * Use this factory method to create a new instance of this fragment
         * with the provided button text.
         *
         * @param buttonText Text to display on the button.
         * @return A new instance of ButtonFragment.
         */
        fun newInstance(buttonText: String): ButtonFragment {
            val fragment = ButtonFragment()
            val args = Bundle()
            args.putString(ARG_BUTTON_TEXT, buttonText)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            buttonText = it.getString(ARG_BUTTON_TEXT) ?: "Button"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_button, container, false)
        button = view.findViewById(R.id.fragment_button)
        button.text = buttonText
        
        button.setOnClickListener {
            onButtonClickListener?.onButtonClick()
        }
        
        return view
    }

    /**
     * Set the text to be displayed on the button.
     *
     * @param text The text to display.
     */
    fun setButtonText(text: String) {
        buttonText = text
        if (::button.isInitialized) {
            button.text = text
        }
    }

    /**
     * Set a listener to be notified when the button is clicked.
     *
     * @param listener The listener to be notified.
     */
    fun setOnButtonClickListener(listener: OnButtonClickListener) {
        onButtonClickListener = listener
    }
}
