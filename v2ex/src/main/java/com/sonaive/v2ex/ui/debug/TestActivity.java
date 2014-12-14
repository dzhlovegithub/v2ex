/*
 * Copyright 2014 sonaive.com. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sonaive.v2ex.ui.debug;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.sonaive.v2ex.R;
import com.sonaive.v2ex.provider.V2exContract;
import com.sonaive.v2ex.sync.SyncHelper;
import com.sonaive.v2ex.sync.api.Api;
import com.sonaive.v2ex.ui.BaseActivity;
import com.sonaive.v2ex.ui.widgets.CollectionView;
import com.sonaive.v2ex.ui.widgets.DrawShadowFrameLayout;
import com.sonaive.v2ex.util.AccountUtils;
import com.sonaive.v2ex.util.UIUtils;

import static com.sonaive.v2ex.util.LogUtils.LOGD;
import static com.sonaive.v2ex.util.LogUtils.makeLogTag;
/**
 * Created by liutao on 12/1/14.
 */
public class TestActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = makeLogTag(TestActivity.class);
    private DrawShadowFrameLayout mDrawShadowFrameLayout;
    private View mButterBar;
    private TestFragment mFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_browse_images);
        mButterBar = findViewById(R.id.butter_bar);
        mDrawShadowFrameLayout = (DrawShadowFrameLayout) findViewById(R.id.main_content);

        registerHideableHeaderView(findViewById(R.id.headerbar));

        getLoaderManager().restartLoader(2, null, this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        enableActionBarAutoHide((CollectionView) findViewById(R.id.images_collection_view));
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();

        mFrag = (TestFragment) getFragmentManager().findFragmentById(R.id.images_fragment);
        if (mFrag != null) {
            // configure images fragment's top clearance to take our overlaid controls (Action Bar
            // ) into account.
            int actionBarSize = UIUtils.calculateActionBarSize(this);
            mDrawShadowFrameLayout.setShadowTopOffset(actionBarSize);
            mFrag.setContentTopClearance(actionBarSize
                    + getResources().getDimensionPixelSize(R.dimen.explore_grid_padding));
        }
        updateFragContentTopClearance();

    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        if (mFrag != null) {
            return mFrag.canCollectionViewScrollUp();
        }
        return super.canSwipeRefreshChildScrollUp();
    }

    @Override
    protected void onActionBarAutoShowOrHide(boolean shown) {
        super.onActionBarAutoShowOrHide(shown);
        mDrawShadowFrameLayout.setShadowVisible(shown, shown);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_SETTINGS;
    }

    @Override
    protected void requestDataRefresh() {
        super.requestDataRefresh();
        Bundle args = new Bundle();
        args.putString(Api.ARG_API_NAME, Api.API_LATEST);
        SyncHelper.requestManualSync(this, args);
    }

    // Updates the Sessions fragment content top clearance to take our chrome into account
    private void updateFragContentTopClearance() {
        mFrag = (TestFragment) getFragmentManager().findFragmentById(
                R.id.images_fragment);
        if (mFrag == null) {
            return;
        }


        final boolean butterBarVisible = mButterBar != null
                && mButterBar.getVisibility() == View.VISIBLE;

        int actionBarClearance = UIUtils.calculateActionBarSize(this);
        int butterBarClearance = butterBarVisible
                ? getResources().getDimensionPixelSize(R.dimen.butter_bar_height) : 0;
        int gridPadding = getResources().getDimensionPixelSize(R.dimen.explore_grid_padding);

        setProgressBarTopWhenActionBarShown(actionBarClearance + butterBarClearance);
        mDrawShadowFrameLayout.setShadowTopOffset(actionBarClearance + butterBarClearance);
        mFrag.setContentTopClearance(actionBarClearance + butterBarClearance + gridPadding);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, V2exContract.Feeds.CONTENT_URI,
                PROJECTION, null, null, V2exContract.Feeds.FEED_ID + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        onRefreshingStateChanged(false);
        if (data != null) {
            data.moveToPosition(-1);
            if (data.moveToFirst()) {
                String id = data.getString(data.getColumnIndex(V2exContract.Feeds.FEED_ID));
                String feedTitle = data.getString(data.getColumnIndex(V2exContract.Feeds.FEED_TITLE));
                String feedAuthor = data.getString(data.getColumnIndex(V2exContract.Feeds.FEED_MEMBER));
                String feedNode = data.getString(data.getColumnIndex(V2exContract.Feeds.FEED_NODE));
                int feedsCount = data.getCount();
                Toast.makeText(this, "feed_id=" + id +
                                ", feedTitle=" + feedTitle +
                                ", feedAuthor=" + feedAuthor +
                                ", feedNode=" + feedNode +
                                ", feedCount=" + feedsCount,
                                Toast.LENGTH_SHORT).show();
                LOGD(TAG, "feed_id=" + id +
                        ", feedTitle=" + feedTitle +
                        ", feedAuthor=" + feedAuthor +
                        ", feedNode=" + feedNode +
                        ", feedCount=" + feedsCount);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        loader = null;
    }

    private static final String[] PROJECTION = {
            V2exContract.Feeds.FEED_ID,
            V2exContract.Feeds.FEED_TITLE,
            V2exContract.Feeds.FEED_MEMBER,
            V2exContract.Feeds.FEED_NODE
    };
}