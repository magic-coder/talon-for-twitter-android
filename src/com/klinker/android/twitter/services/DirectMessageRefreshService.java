package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.klinker.android.twitter.data.sq_lite.DMDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.NotificationUtils;
import com.klinker.android.twitter.utils.Utils;

import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class DirectMessageRefreshService extends IntentService {

    SharedPreferences sharedPrefs;

    public DirectMessageRefreshService() {
        super("DirectMessageRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Context context = getApplicationContext();
        AppSettings settings = new AppSettings(context);

        // if they have mobile data on and don't want to sync over mobile data
        if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
            return;
        }

        boolean update = false;
        int numberNew = 0;

        try {
            Twitter twitter = Utils.getTwitter(context, settings);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            User user = twitter.verifyCredentials();
            long lastId = sharedPrefs.getLong("last_direct_message_id_" + currentAccount, 0);
            Paging paging;
            if (lastId != 0) {
                paging = new Paging(1).sinceId(lastId);
            } else {
                paging = new Paging(1, 500);
            }

            List<DirectMessage> dm = twitter.getDirectMessages(paging);
            List<DirectMessage> sent = twitter.getSentDirectMessages(paging);

            if (dm.size() != 0) {
                sharedPrefs.edit().putLong("last_direct_message_id_" + currentAccount, dm.get(0).getId()).commit();
                numberNew = dm.size();
            } else {
                numberNew = 0;
            }

            DMDataSource dataSource = new DMDataSource(context);
            dataSource.open();

            for (DirectMessage directMessage : dm) {
                try {
                    dataSource.createDirectMessage(directMessage, currentAccount);
                } catch (Exception e) {
                    break;
                }
            }

            for (DirectMessage directMessage : sent) {
                try {
                    dataSource.createDirectMessage(directMessage, currentAccount);
                } catch (Exception e) {
                    break;
                }
            }

            dataSource.close();

            if (numberNew > 0) {
                int currentUnread = sharedPrefs.getInt("dm_unread_" + currentAccount, 0);
                sharedPrefs.edit().putInt("dm_unread_" + currentAccount, numberNew + currentUnread).commit();

                NotificationUtils.refreshNotification(context);
            }

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }
    }
}