package com.sendbird.android.sample.openchannel;

import android.os.Bundle;
import android.view.MenuItem;

import com.sendbird.android.OpenChannel;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.sendbird.android.UserListQuery;
import com.sendbird.android.sample.R;
import com.sendbird.android.sample.groupchannel.UserListAdapter;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Displays a list of the participants of a specified Open Channel.
 */

public class ParticipantListActivity extends AppCompatActivity {

    private UserListAdapter mListAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private String mChannelUrl;
    private OpenChannel mChannel;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_participant_list);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_participant_list);

        mChannelUrl = getIntent().getStringExtra(OpenChatFragment.EXTRA_CHANNEL_URL);
        mListAdapter = new UserListAdapter(this, false);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_participant_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_left_white_24_dp);
        }

        setUpRecyclerView();

        getChannelFromUrl();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setUpRecyclerView() {
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mListAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    /**
     * Gets the channel instance with the channel URL.
     */
    private void getChannelFromUrl() {
        OpenChannel.getChannel(mChannelUrl, new OpenChannel.OpenChannelGetHandler() {
            @Override
            public void onResult(OpenChannel openChannel, SendBirdException e) {
                mChannel = openChannel;

                getUserList();
            }
        });
    }

    private void getUserList() {
        UserListQuery userListQuery = mChannel.createParticipantListQuery();
        userListQuery.next(new UserListQuery.UserListQueryResultHandler() {
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


}
