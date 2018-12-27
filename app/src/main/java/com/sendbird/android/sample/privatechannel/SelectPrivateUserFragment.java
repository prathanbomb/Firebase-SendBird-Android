package com.sendbird.android.sample.privatechannel;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.sendbird.android.UserListQuery;
import com.sendbird.android.sample.R;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A simple {@link Fragment} subclass.
 */
public class SelectPrivateUserFragment extends Fragment {

    private LinearLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private SelectablePrivateUserListAdapter mListAdapter;

    private UserListQuery mUserListQuery;
    private SelectPrivateUserFragment.UsersSelectedListener mListener;

    // To pass selected user IDs to the parent Activity.
    interface UsersSelectedListener {
        void onUserSelected(String userId);
    }

    static SelectPrivateUserFragment newInstance() {
        return new SelectPrivateUserFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_select_private_user, container, false);
        mRecyclerView = rootView.findViewById(R.id.recycler_select_private_user);
        mListAdapter = new SelectablePrivateUserListAdapter(getActivity(), false, true);

        mListAdapter.setSelectItemListener(new SelectablePrivateUserListAdapter.OnItemSelectedListener() {
            @Override
            public void OnItemSelected(User user) {
                mListener.onUserSelected(user.getUserId());
            }
        });

        mListener = (SelectPrivateUserFragment.UsersSelectedListener) getActivity();

        setUpRecyclerView();

        loadInitialUserList(15);

//        ((CreatePrivateChannelActivity) getActivity()).setState(CreatePrivateChannelActivity.STATE_SELECT_USERS);

        return rootView;
    }

    private void setUpRecyclerView() {
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mListAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mLayoutManager.findLastVisibleItemPosition() == mListAdapter.getItemCount() - 1) {
                    loadNextUserList(10);
                }
            }
        });
    }

    /**
     * Replaces current user list with new list.
     * Should be used only on initial load.
     */
    private void loadInitialUserList(int size) {
        mUserListQuery = SendBird.createUserListQuery();

        mUserListQuery.setLimit(size);
        mUserListQuery.next(new UserListQuery.UserListQueryResultHandler() {
            @Override
            public void onResult(List<User> list, SendBirdException e) {
                if (e != null) {
                    // Error!
                    return;
                }

                mListAdapter.setUserList(list);
            }
        });
    }

    private void loadNextUserList(int size) {
        mUserListQuery.setLimit(size);

        mUserListQuery.next(new UserListQuery.UserListQueryResultHandler() {
            @Override
            public void onResult(List<User> list, SendBirdException e) {
                if (e != null) {
                    // Error!
                    return;
                }

                for (User user : list) {
                    mListAdapter.addLast(user);
                }
            }
        });
    }

}
