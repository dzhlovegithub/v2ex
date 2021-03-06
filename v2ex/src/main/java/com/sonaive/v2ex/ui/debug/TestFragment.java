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

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.sonaive.v2ex.R;
import com.sonaive.v2ex.ui.widgets.CollectionView;
import com.sonaive.v2ex.ui.widgets.CollectionViewCallbacks;
import com.sonaive.v2ex.util.ImageLoader;
import com.sonaive.v2ex.provider.V2exContract.*;

import static com.sonaive.v2ex.util.LogUtils.LOGD;
import static com.sonaive.v2ex.util.LogUtils.makeLogTag;
/**
 * LoaderCallbacks.OnLoadFinished called twice after screen rotation changes.
 * see "http://stackoverflow.com/questions/11293441/android-loadercallbacks-onloadfinished-called-twice"
 * and i put initLoader in onResume function instead of onActivityCreated or onCreate.
 * Created by liutao on 12/1/14.
 */
public class TestFragment extends Fragment implements CollectionViewCallbacks {

    private static final String TAG = makeLogTag(TestFragment.class);

    private static final String[] PROJECTION = {
        PicasaImages._ID,
        PicasaImages.PICASA_THUMB_URL,
        PicasaImages.PICASA_IMAGE_URL
    };

    // Constants that define the order of columns in the returned cursor
    private static final int IMAGE_THUMBURL_CURSOR_INDEX = 1;
    private static final int IMAGE_URL_CURSOR_INDEX = 2;

    private static final int GROUP_ID_NORMAL = 1001;

    ImageLoader mImageLoader;

    CollectionView mCollectionView = null;
    View mEmptyView = null;

    Cursor mCursor = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_images, container, false);
        mCollectionView = (CollectionView) root.findViewById(R.id.images_collection_view);
        mEmptyView = root.findViewById(android.R.id.empty);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mImageLoader = new ImageLoader(getActivity(), android.R.color.transparent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Initializes the CursorLoader
        getLoaderManager().initLoader(1, null, new PicasaImagesLoader());
    }

    public void setContentTopClearance(int clearance) {
        if (mCollectionView != null) {
            mCollectionView.setContentTopClearance(clearance);
        }
    }

    @Override
    public View newCollectionHeaderView(Context context, ViewGroup parent) {
        return null;
    }

    @Override
    public void bindCollectionHeaderView(Context context, View view, int groupId, String headerLabel) {

    }

    @Override
    public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.item_image, parent, false);
    }

    @Override
    public void bindCollectionItemView(Context context, View view, int groupId, int indexInGroup, int dataIndex, Object tag) {
        if (!mCursor.moveToPosition(dataIndex)) {
            return;
        }
        ImageView thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);

        String thumbUrl = mCursor.getString(IMAGE_THUMBURL_CURSOR_INDEX);
        if (TextUtils.isEmpty(thumbUrl)) {
            thumbnailView.setImageResource(android.R.color.transparent);
        } else {
            mImageLoader.loadImage(thumbUrl, thumbnailView);
        }
    }

    class PicasaImagesLoader implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(
                    getActivity(),                                     // Context
                    PicasaImages.CONTENT_URI,                          // Table to query
                    PROJECTION,                                        // Projection to return
                    null,                                              // No selection clause
                    null,                                              // No selection arguments
                    null                                               // Default sort order
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mCursor = data;
            if (mCursor != null) {
                updateCollectionView();
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    }

    public boolean canCollectionViewScrollUp() {
        return ViewCompat.canScrollVertically(mCollectionView, -1);
    }

    private void updateCollectionView() {
        LOGD(TAG, "Updating images collection view.");
        CollectionView.InventoryGroup curGroup = null;
        CollectionView.Inventory inventory = new CollectionView.Inventory();
        mCursor.moveToPosition(-1);
        int dataIndex = -1;
        int normalColumns = getResources().getInteger(R.integer.images_columns);

        boolean isEmpty = mCursor.getCount() == 0;

        while (mCursor.moveToNext()) {
            ++dataIndex;

            if (curGroup == null) {
                if (curGroup != null) {
                    inventory.addGroup(curGroup);
                }
                curGroup = new CollectionView.InventoryGroup(
                        GROUP_ID_NORMAL)
                        .setDataIndexStart(dataIndex)
                        .setShowHeader(false)
                        .setDisplayCols(normalColumns)
                        .setItemCount(1);
            } else {
                curGroup.incrementItemCount();
            }
        }

        if (curGroup != null) {
            inventory.addGroup(curGroup);
        }

        mCollectionView.setCollectionAdapter(this);
        mCollectionView.updateInventory(inventory);

        mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}
