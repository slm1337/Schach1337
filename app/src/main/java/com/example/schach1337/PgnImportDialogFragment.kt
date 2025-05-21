package com.example.schach1337

import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment

class PgnImportDialogFragment : DialogFragment() {

    interface PgnImportListener {
        fun onPgnImported(pgnText: String)
    }

    private var listener: PgnImportListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.fragment_pgn_import_dialog)

            val pgnEditText = dialog.findViewById<EditText>(R.id.pgnEditText)
            val pasteButton = dialog.findViewById<Button>(R.id.pasteButton)
            val importButton = dialog.findViewById<Button>(R.id.importButton)
            val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

            pasteButton.setOnClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboard.primaryClip
                clipData?.getItemAt(0)?.text?.let { text ->
                    pgnEditText.setText(text)
                }
            }

            importButton.setOnClickListener {
                val pgnText = pgnEditText.text.toString()
                if (pgnText.isNotEmpty()) {
                    listener?.onPgnImported(pgnText)
                    dismiss()
                }
            }

            cancelButton.setOnClickListener {
                dismiss()
            }

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    fun setPgnImportListener(listener: PgnImportListener) {
        this.listener = listener
    }

    companion object {
        fun newInstance() = PgnImportDialogFragment()
    }
}