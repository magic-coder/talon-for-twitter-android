package com.klinker.android.twitter.ui.tweet_viewer.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ArrayListLoader;
import com.klinker.android.twitter.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.widgets.HoloTextView;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class ConversationFragment extends Fragment {
    private Context context;
    private View layout;
    private AppSettings settings;
    private long tweetId;

    public ConversationFragment(AppSettings settings, long tweetId) {
        this.settings = settings;
        this.tweetId = tweetId;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        layout = inflater.inflate(R.layout.conversation_fragment, null);
        AsyncListView replyList = (AsyncListView) layout.findViewById(R.id.listView);
        LinearLayout progressSpinner = (LinearLayout) layout.findViewById(R.id.list_progress);
        HoloTextView none = (HoloTextView) layout.findViewById(R.id.no_conversation);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        replyList.setItemManager(builder.build());

        new GetReplies(replyList, tweetId, progressSpinner, none).execute();

        return layout;
    }

    class GetReplies extends AsyncTask<String, Void, ArrayList<Status>> {

        private ListView listView;
        private long tweetId;
        private LinearLayout progressSpinner;
        private HoloTextView none;

        public GetReplies(ListView listView, long tweetId, LinearLayout progressBar, HoloTextView none) {
            this.listView = listView;
            this.tweetId = tweetId;
            this.progressSpinner = progressBar;
            this.none = none;
        }

        protected ArrayList<twitter4j.Status> doInBackground(String... urls) {
            Twitter twitter = Utils.getTwitter(context, settings);
            try {
                twitter4j.Status status = twitter.showStatus(tweetId);

                twitter4j.Status replyStatus = twitter.showStatus(status.getInReplyToStatusId());

                ArrayList<twitter4j.Status> replies = new ArrayList<twitter4j.Status>();

                try {
                    while(!replyStatus.getText().equals("")) {
                        replies.add(replyStatus);
                        Log.v("reply_status", replyStatus.getText());

                        replyStatus = twitter.showStatus(replyStatus.getInReplyToStatusId());
                    }
                } catch (Exception e) {
                    // the list of replies has ended, but we dont want to go to null
                }

                return replies;

            } catch (TwitterException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<twitter4j.Status> replies) {
            progressSpinner.setVisibility(View.GONE);

            try {
                if (replies.size() > 0) {
                    listView.setAdapter(new TimelineArrayAdapter(context, replies));
                    listView.setVisibility(View.VISIBLE);
                } else {
                    none.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                // none and it got the null object
                listView.setVisibility(View.GONE);
                none.setVisibility(View.VISIBLE);
            }
        }
    }
}
