package th.co.digio.chatapp.demo.groupchannel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import co.th.digio.chatapp.demo.R

/**
 * A fragment displaying an option to set the channel as Distinct.
 */
class SelectDistinctFragment : Fragment() {

    private var mCheckBox: CheckBox? = null
    private var mListener: DistinctSelectedListener? = null

    interface DistinctSelectedListener {
        fun onDistinctSelected(distinct: Boolean)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_select_distinct, container, false)

        (activity as CreateGroupChannelActivity).setState(CreateGroupChannelActivity.STATE_SELECT_DISTINCT)

        mListener = activity as CreateGroupChannelActivity?

        mCheckBox = rootView.findViewById(R.id.checkbox_select_distinct)
        mCheckBox!!.isChecked = true
        mCheckBox!!.setOnCheckedChangeListener { buttonView, isChecked -> mListener!!.onDistinctSelected(isChecked) }

        return rootView
    }

    companion object {

        fun newInstance(): SelectDistinctFragment {
            return SelectDistinctFragment()
        }
    }
}
